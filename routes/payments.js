const express = require('express');
const Payment = require('../models/Payment');
const Order = require('../models/Order');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);
router.use(authorizeStaff);

// Lấy danh sách thanh toán
router.get('/', async (req, res) => {
  try {
    const { orderId } = req.query;
    const query = {};

    if (orderId) {
      query.orderId = orderId;
    }

    const payments = await Payment.find(query)
      .populate({
        path: 'orderId',
        select: 'totalAmount status createdAt',
        populate: [
          {
            path: 'customerId',
            select: 'name phone email'
          },
          {
            path: 'items.productId',
            select: 'name category price'
          }
        ]
      })
      .sort({ createdAt: -1 });
    
    // Filter out payments with null orderId (nếu order bị xóa)
    const validPayments = payments.filter(payment => payment.orderId != null);
    
    res.json(validPayments);
  } catch (error) {
    console.error('Error fetching payments:', error);
    res.status(500).json({ message: error.message });
  }
});

// Lấy lịch sử thanh toán của một đơn hàng
// PHẢI ĐẶT TRƯỚC route /:id để tránh conflict
router.get('/order/:orderId', async (req, res) => {
  try {
    const payments = await Payment.find({ orderId: req.params.orderId })
      .populate({
        path: 'orderId',
        select: 'totalAmount status createdAt',
        populate: [
          {
            path: 'customerId',
            select: 'name phone email'
          },
          {
            path: 'items.productId',
            select: 'name category price'
          }
        ]
      })
      .sort({ createdAt: -1 });
    
    // Filter out payments with null orderId
    const validPayments = payments.filter(payment => payment.orderId != null);
    
    res.json(validPayments);
  } catch (error) {
    console.error('Error fetching payments by order:', error);
    res.status(500).json({ message: error.message });
  }
});

// Lấy thông tin một thanh toán
// PHẢI ĐẶT SAU route /order/:orderId để tránh conflict
router.get('/:id', async (req, res) => {
  try {
    // Kiểm tra nếu là route /order/:orderId (không phải ObjectId hợp lệ)
    if (req.params.id === 'order') {
      return res.status(400).json({ message: 'Invalid payment ID' });
    }
    
    const payment = await Payment.findById(req.params.id)
      .populate({
        path: 'orderId',
        select: 'totalAmount status createdAt',
        populate: [
          {
            path: 'customerId',
            select: 'name phone email'
          },
          {
            path: 'items.productId',
            select: 'name category price'
          }
        ]
      });
    
    if (!payment) {
      return res.status(404).json({ message: 'Không tìm thấy thanh toán' });
    }
    
    // Kiểm tra nếu orderId bị null (order đã bị xóa)
    if (!payment.orderId) {
      return res.status(404).json({ message: 'Đơn hàng liên quan đã bị xóa' });
    }
    
    res.json(payment);
  } catch (error) {
    console.error('Error fetching payment:', error);
    res.status(500).json({ message: error.message });
  }
});

// Tạo thanh toán
router.post('/', async (req, res) => {
  try {
    const { orderId, amount, method } = req.body;

    const order = await Order.findById(orderId);
    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    const payment = await Payment.create({
      orderId,
      amount,
      method,
      status: 'pending',
    });

    const populatedPayment = await Payment.findById(payment._id)
      .populate({
        path: 'orderId',
        select: 'totalAmount status createdAt',
        populate: [
          {
            path: 'customerId',
            select: 'name phone email'
          },
          {
            path: 'items.productId',
            select: 'name category price'
          }
        ]
      });
    
    if (!populatedPayment || !populatedPayment.orderId) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng liên quan' });
    }
    
    res.status(201).json(populatedPayment);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Cập nhật trạng thái thanh toán
router.patch('/:id/status', async (req, res) => {
  try {
    const { status } = req.body;
    const validStatuses = ['pending', 'success', 'failed'];
    
    if (!validStatuses.includes(status)) {
      return res.status(400).json({ message: 'Trạng thái không hợp lệ' });
    }

    const updateData = { status };
    if (status === 'success') {
      updateData.paidAt = new Date();
      // Cập nhật trạng thái đơn hàng thành paid
      const payment = await Payment.findById(req.params.id);
      if (payment && payment.orderId) {
        await Order.findByIdAndUpdate(payment.orderId, { status: 'paid' });
      }
    }

    const payment = await Payment.findByIdAndUpdate(
      req.params.id,
      updateData,
      { new: true, runValidators: true }
    )
      .populate({
        path: 'orderId',
        select: 'totalAmount status createdAt',
        populate: [
          {
            path: 'customerId',
            select: 'name phone email'
          },
          {
            path: 'items.productId',
            select: 'name category price'
          }
        ]
      });

    if (!payment) {
      return res.status(404).json({ message: 'Không tìm thấy thanh toán' });
    }

    if (!payment.orderId) {
      return res.status(404).json({ message: 'Đơn hàng liên quan đã bị xóa' });
    }

    res.json(payment);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Thanh toán lại (nếu thất bại)
router.post('/:id/retry', async (req, res) => {
  try {
    const payment = await Payment.findById(req.params.id);
    if (!payment) {
      return res.status(404).json({ message: 'Không tìm thấy thanh toán' });
    }

    if (payment.status !== 'failed') {
      return res.status(400).json({ message: 'Chỉ có thể thử lại thanh toán đã thất bại' });
    }

    payment.status = 'pending';
    payment.paidAt = undefined;
    await payment.save();

    const populatedPayment = await Payment.findById(payment._id)
      .populate({
        path: 'orderId',
        select: 'totalAmount status createdAt',
        populate: [
          {
            path: 'customerId',
            select: 'name phone email'
          },
          {
            path: 'items.productId',
            select: 'name category price'
          }
        ]
      });
    
    if (!populatedPayment || !populatedPayment.orderId) {
      return res.status(404).json({ message: 'Đơn hàng liên quan đã bị xóa' });
    }
    
    res.json(populatedPayment);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

module.exports = router;

