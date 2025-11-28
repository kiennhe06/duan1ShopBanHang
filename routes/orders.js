const express = require('express');
const mongoose = require('mongoose');
const Order = require('../models/Order');
const Product = require('../models/Product');
const Customer = require('../models/Customer');
const Payment = require('../models/Payment');
const Voucher = require('../models/Voucher');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Lấy danh sách đơn hàng - Customer chỉ xem được đơn của mình, Staff/Admin xem tất cả
router.get('/', async (req, res) => {
  try {
    const { customerId, status, startDate, endDate } = req.query;
    const query = {};

    // Nếu là customer, chỉ cho xem đơn hàng của mình
    if (req.user.role === 'customer') {
      // Tìm orders theo userId (vì mỗi order đều có userId khi customer tạo)
      // Đây là cách đáng tin cậy nhất vì userId luôn có trong order và không phụ thuộc vào customer record
      query.userId = req.user._id;
    } else {
      // Staff/Admin có thể filter theo customerId
      if (customerId) {
        query.customerId = customerId;
      }
    }

    if (status) {
      query.status = status;
    }
    if (startDate || endDate) {
      query.createdAt = {};
      if (startDate) query.createdAt.$gte = new Date(startDate);
      if (endDate) query.createdAt.$lte = new Date(endDate);
    }

    const orders = await Order.find(query)
      .populate('customerId', 'name phone email address')
      .populate('userId', 'username')
      .populate('items.productId', 'name category price imageUrl')
      .sort({ createdAt: -1 });
    
    res.json(orders);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Lấy thông tin một đơn hàng - Customer chỉ xem được đơn của mình, Staff/Admin xem tất cả
router.get('/:id', async (req, res) => {
  try {
    const order = await Order.findById(req.params.id)
      .populate('customerId', 'name phone email address')
      .populate('userId', 'username email')
      .populate('items.productId', 'name category price imageUrl');
    
    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }
    
    // Nếu là customer, chỉ cho xem đơn hàng của mình
    if (req.user.role === 'customer') {
      // Kiểm tra userId thay vì customerId vì đáng tin cậy hơn
      if (order.userId._id.toString() !== req.user._id.toString()) {
        return res.status(403).json({ message: 'Bạn không có quyền xem đơn hàng này' });
      }
    }
    
    res.json(order);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Tạo đơn hàng - Customer và Staff/Admin đều có thể tạo (trước authorizeStaff)
router.post('/', async (req, res) => {
  // Với standalone MongoDB, không dùng transaction nhưng vẫn an toàn với $inc (atomic)
  // Chỉ dùng transaction nếu MongoDB là replica set
  // Để đơn giản và tránh lỗi, mặc định KHÔNG dùng transaction
  // Nếu muốn dùng transaction, cần cấu hình MongoDB thành replica set
  const useTransaction = false; // Tắt transaction để tránh lỗi với standalone MongoDB
  const session = null;

  try {
    const { customerId, items, paymentMethod, voucherCode, shippingFee } = req.body;

    // Validation: Kiểm tra customerId
    if (!customerId || !mongoose.Types.ObjectId.isValid(customerId)) {
      if (useTransaction && session) {
        await session.abortTransaction();
        session.endSession();
      }
      return res.status(400).json({ message: 'Customer ID không hợp lệ' });
    }

    // Validation: Kiểm tra customer có tồn tại không
    const customerQuery = useTransaction && session 
      ? Customer.findById(customerId).session(session)
      : Customer.findById(customerId);
    const customer = await customerQuery;
    
    if (!customer) {
      if (useTransaction && session) {
        await session.abortTransaction();
        session.endSession();
      }
      return res.status(404).json({ message: 'Không tìm thấy khách hàng' });
    }

    // Validation: Kiểm tra items
    if (!items || items.length === 0) {
      if (useTransaction && session) {
        await session.abortTransaction();
        session.endSession();
      }
      return res.status(400).json({ message: 'Đơn hàng phải có ít nhất một sản phẩm' });
    }

    // Validation: Kiểm tra payment method
    const validPaymentMethods = ['cash', 'card', 'online'];
    const paymentMethodValue = paymentMethod || 'cash';
    if (paymentMethod && !validPaymentMethods.includes(paymentMethod)) {
      if (useTransaction && session) {
        await session.abortTransaction();
        session.endSession();
      }
      return res.status(400).json({ 
        message: `Phương thức thanh toán không hợp lệ. Chỉ chấp nhận: ${validPaymentMethods.join(', ')}` 
      });
    }

    // Tính tổng tiền và kiểm tra tồn kho
    let totalAmount = 0;
    const itemsWithPrice = [];
    const productUpdates = [];

    for (const item of items) {
      // Kiểm tra product có tồn tại không
      const productQuery = useTransaction && session
        ? Product.findById(item.productId).session(session)
        : Product.findById(item.productId);
      const product = await productQuery;
      
      if (!product) {
        if (useTransaction && session) {
          await session.abortTransaction();
          session.endSession();
        }
        return res.status(404).json({ message: `Không tìm thấy sản phẩm ${item.productId}` });
      }

      // Kiểm tra tồn kho
      if (product.stock < item.quantity) {
        if (useTransaction && session) {
          await session.abortTransaction();
          session.endSession();
        }
        return res.status(400).json({ 
          message: `Sản phẩm ${product.name} không đủ tồn kho. Tồn kho hiện tại: ${product.stock}` 
        });
      }

      const itemTotal = product.price * item.quantity;
      totalAmount += itemTotal;

      itemsWithPrice.push({
        productId: product._id,
        quantity: item.quantity,
        price: product.price,
      });

      productUpdates.push({
        productId: product._id,
        quantity: item.quantity,
      });
    }

    // Xử lý phí vận chuyển
    const shippingFeeValue = shippingFee && shippingFee > 0 ? parseFloat(shippingFee) : 30000; // Mặc định 30,000₫

    // Xử lý voucher nếu có
    let discountAmount = 0;
    let voucherCodeValue = null;
    // Tính finalAmount = totalAmount + shippingFee - discountAmount
    let finalAmount = totalAmount + shippingFeeValue;

    if (voucherCode && voucherCode.trim()) {
      const voucher = await Voucher.findOne({ code: voucherCode.toUpperCase().trim() });
      
      if (voucher) {
        // Validate voucher với totalAmount (không bao gồm shipping)
        // Vì voucher thường áp dụng cho giá trị sản phẩm, không phải phí vận chuyển
        const validation = voucher.isValid(totalAmount);
        
        if (validation.valid) {
          // Tính discount dựa trên totalAmount (không bao gồm shipping)
          discountAmount = voucher.calculateDiscount(totalAmount);
          // finalAmount = totalAmount + shippingFee - discountAmount
          finalAmount = totalAmount + shippingFeeValue - discountAmount;
          voucherCodeValue = voucher.code;
          
          // Tăng số lần sử dụng voucher
          voucher.usedCount += 1;
          await voucher.save();
        } else {
          if (useTransaction && session) {
            await session.abortTransaction();
            session.endSession();
          }
          return res.status(400).json({ message: validation.message });
        }
      } else {
        if (useTransaction && session) {
          await session.abortTransaction();
          session.endSession();
        }
        return res.status(404).json({ message: 'Mã voucher không tồn tại' });
      }
    }

    // Trừ tồn kho (atomic operation với $inc)
    for (const update of productUpdates) {
      const updateOptions = useTransaction && session 
        ? { session }
        : {};
      await Product.findByIdAndUpdate(
        update.productId,
        { $inc: { stock: -update.quantity } },
        updateOptions
      );
    }

    // Tạo đơn hàng
    const orderData = {
      customerId,
      userId: req.user._id,
      items: itemsWithPrice,
      totalAmount,
      shippingFee: shippingFeeValue,
      voucherCode: voucherCodeValue,
      discountAmount,
      finalAmount,
      status: 'pending',
    };

    const createOptions = useTransaction && session
      ? { session }
      : {};
    const [order] = useTransaction && session
      ? await Order.create([orderData], createOptions)
      : [await Order.create(orderData)];

    // Xác định payment status
      let paymentStatus = 'pending';
      let paidAt = null;
      
      // Nếu là card hoặc online, coi như đã thanh toán thành công
      if (paymentMethodValue === 'card' || paymentMethodValue === 'online') {
        paymentStatus = 'success';
        paidAt = new Date();
        // Cập nhật order status thành paid
        order.status = 'paid';
      const saveOptions = useTransaction && session
        ? { session }
        : {};
      await order.save(saveOptions);
      }
      
      // Tạo payment record
    try {
      const paymentData = {
        orderId: order._id,
        amount: order.finalAmount, // Dùng finalAmount (đã trừ discount)
        method: paymentMethodValue,
        status: paymentStatus,
        paidAt: paidAt,
      };
      
      if (useTransaction && session) {
        await Payment.create([paymentData], { session });
      } else {
        await Payment.create(paymentData);
      }
    } catch (paymentError) {
      // Nếu tạo Payment fail, rollback nếu có transaction
      if (useTransaction && session) {
        await session.abortTransaction();
        session.endSession();
      } else {
        // Nếu không có transaction, xóa order đã tạo
        await Order.findByIdAndDelete(order._id);
        // Hoàn trả tồn kho
        for (const update of productUpdates) {
          await Product.findByIdAndUpdate(
            update.productId,
            { $inc: { stock: update.quantity } }
          );
        }
      }
      console.error('Error creating payment:', paymentError);
      return res.status(500).json({ 
        message: 'Không thể tạo thanh toán. Vui lòng thử lại sau.',
        error: paymentError.message 
      });
    }

    // Commit transaction nếu có
    if (useTransaction && session) {
      await session.commitTransaction();
      session.endSession();
    }

    // Populate order sau khi commit
    const populatedOrder = await Order.findById(order._id)
      .populate('customerId', 'name phone email')
      .populate('userId', 'username')
      .populate('items.productId', 'name category price imageUrl');

    res.status(201).json(populatedOrder);
  } catch (error) {
    // Rollback transaction nếu có lỗi
    if (useTransaction && session) {
      try {
        await session.abortTransaction();
      } catch (abortError) {
        console.error('Error aborting transaction:', abortError);
      }
      try {
        session.endSession();
      } catch (endError) {
        console.error('Error ending session:', endError);
      }
    }
    console.error('Error creating order:', error);
    
    // Nếu là lỗi transaction, tự động retry với fallback mode
    if (error.message && error.message.includes('Transaction numbers are only allowed')) {
      console.log('Phát hiện lỗi transaction, tự động chuyển sang fallback mode');
      // Không trả về lỗi, mà sẽ retry logic (nhưng đơn giản hơn là bỏ transaction từ đầu)
      // Vì đã có fallback ở trên, nên chỉ cần log
    }
    
    res.status(400).json({ 
      message: error.message || 'Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại.',
      error: error.message 
    });
  }
});

// Customer có thể hủy đơn hàng của chính mình
router.patch('/:id/cancel', async (req, res) => {
  try {
    const order = await Order.findById(req.params.id)
      .populate('customerId', 'name phone email address')
      .populate('userId', 'username email')
      .populate('items.productId', 'name category price imageUrl');

    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Kiểm tra quyền: customer chỉ có thể hủy đơn của mình
    if (req.user.role === 'customer') {
      // Kiểm tra userId hoặc customerId
      const isOwner = (order.userId && order.userId._id.toString() === req.user._id.toString()) ||
                      (order.customerId && order.customerId._id.toString() === req.user._id.toString());
      
      if (!isOwner) {
        // Nếu không match userId, thử tìm customer theo email
        const customer = await Customer.findOne({ email: req.user.email });
        if (!customer || order.customerId._id.toString() !== customer._id.toString()) {
          return res.status(403).json({ message: 'Bạn không có quyền hủy đơn hàng này' });
        }
      }
    }

    // Chỉ cho phép hủy nếu đơn hàng ở trạng thái pending, processing, hoặc paid
    if (!['pending', 'processing', 'paid'].includes(order.status)) {
      return res.status(400).json({ 
        message: `Không thể hủy đơn hàng ở trạng thái ${order.status}` 
      });
    }

    // Cập nhật trạng thái thành cancelled
    order.status = 'cancelled';
    await order.save();

    // Hoàn trả tồn kho
    for (const item of order.items) {
      const product = await Product.findById(item.productId);
      if (product) {
        product.stock += item.quantity;
        await product.save();
      }
    }

    const updatedOrder = await Order.findById(order._id)
      .populate('customerId', 'name phone email address')
      .populate('userId', 'username')
      .populate('items.productId', 'name category price imageUrl');

    res.json(updatedOrder);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Các routes sau chỉ dành cho staff/admin
router.use(authorizeStaff);

// Cập nhật trạng thái đơn hàng (chỉ staff/admin)
router.patch('/:id/status', async (req, res) => {
  try {
    const { status } = req.body;
    const validStatuses = ['pending', 'processing', 'delivering', 'paid', 'shipped', 'cancelled'];
    
    if (!validStatuses.includes(status)) {
      return res.status(400).json({ message: 'Trạng thái không hợp lệ' });
    }

    const order = await Order.findByIdAndUpdate(
      req.params.id,
      { status },
      { new: true, runValidators: true }
    )
      .populate('customerId', 'name phone email address')
      .populate('userId', 'username')
      .populate('items.productId', 'name category price imageUrl');

    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    res.json(order);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Xóa đơn hàng (chỉ admin)
router.delete('/:id', async (req, res) => {
  try {
    const order = await Order.findById(req.params.id);
    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Nếu đơn đã thanh toán, không cho xóa
    if (order.status === 'paid' || order.status === 'shipped') {
      return res.status(400).json({ message: 'Không thể xóa đơn hàng đã thanh toán hoặc đã giao' });
    }

    // Hoàn trả tồn kho
    for (const item of order.items) {
      const product = await Product.findById(item.productId);
      if (product) {
        product.stock += item.quantity;
        await product.save();
      }
    }

    await Order.findByIdAndDelete(req.params.id);
    res.json({ message: 'Đã xóa đơn hàng thành công' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;

