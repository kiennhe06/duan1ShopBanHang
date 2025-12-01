const express = require('express');
const Review = require('../models/Review');
const Product = require('../models/Product');
const Customer = require('../models/Customer');
const Order = require('../models/Order');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Lấy đánh giá của một sản phẩm - Tất cả user đều có thể xem
router.get('/product/:productId', async (req, res) => {
  try {
    const reviews = await Review.find({ productId: req.params.productId })
      .populate('customerId', 'name phone email')
      .sort({ createdAt: -1 });
    
    // Tính rating trung bình
    const ratings = reviews.map(r => r.rating);
    const averageRating = ratings.length > 0
      ? (ratings.reduce((a, b) => a + b, 0) / ratings.length).toFixed(2)
      : 0;

    res.json({
      reviews,
      averageRating: parseFloat(averageRating),
      totalReviews: reviews.length,
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Thêm đánh giá - Customer và Staff/Admin đều có thể tạo
router.post('/', async (req, res) => {
  try {
    const { productId, customerId, rating, comment } = req.body;

    // Kiểm tra sản phẩm tồn tại
    const product = await Product.findById(productId);
    if (!product) {
      return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
    }

    // Nếu là customer, tự động lấy customerId từ user và kiểm tra đã mua chưa
    let finalCustomerId = customerId;
    let orderId = null;
    
    if (req.user.role === 'customer' && !customerId) {
      const customer = await Customer.findOne({ email: req.user.email });
      if (customer) {
        finalCustomerId = customer._id;
        
        // Tìm order có chứa sản phẩm này (dùng để kiểm tra và lấy orderId)
        const order = await Order.findOne({
          $or: [
            { customerId: customer._id },
            { userId: req.user._id }
          ],
          status: { $in: ['pending', 'processing', 'delivering', 'paid', 'shipped'] },
          'items.productId': productId
        }).sort({ createdAt: -1 }); // Lấy order mới nhất
        
        if (!order) {
          return res.status(403).json({ message: 'Bạn chỉ có thể đánh giá sản phẩm đã mua' });
        }
        
        // Lưu orderId từ order đã tìm được
        orderId = order._id;
      } else {
        // Nếu không tìm thấy customer, thử tìm theo userId trong order
        const order = await Order.findOne({
          userId: req.user._id,
          status: { $in: ['pending', 'processing', 'delivering', 'paid', 'shipped'] },
          'items.productId': productId
        }).sort({ createdAt: -1 });
        
        if (!order) {
          return res.status(403).json({ message: 'Bạn chỉ có thể đánh giá sản phẩm đã mua' });
        }
        
        // Lưu orderId từ order đã tìm được
        orderId = order._id;
        
        // Tạo customer record nếu chưa có
        const newCustomer = await Customer.create({
          name: req.user.username || 'Customer',
          email: req.user.email,
          phone: '',
          address: ''
        });
        finalCustomerId = newCustomer._id;
      }
    } else if (req.user.role === 'customer' && customerId) {
      // Nếu có customerId trong request, vẫn tìm orderId
      const order = await Order.findOne({
        $or: [
          { customerId: finalCustomerId },
          { userId: req.user._id }
        ],
        status: { $in: ['pending', 'processing', 'delivering', 'paid', 'shipped'] },
        'items.productId': productId
      }).sort({ createdAt: -1 });
      
      if (order) {
        orderId = order._id;
      }
    }

    // Kiểm tra đã đánh giá chưa
    const existingReview = await Review.findOne({
      productId,
      customerId: finalCustomerId
    });
    
    if (existingReview) {
      return res.status(400).json({ message: 'Bạn đã đánh giá sản phẩm này rồi' });
    }

    const review = await Review.create({
      productId,
      customerId: finalCustomerId,
      orderId,
      rating,
      comment,
    });

    const populatedReview = await Review.findById(review._id)
      .populate('productId', 'name category price imageUrl')
      .populate('customerId', 'name phone email')
      .populate('orderId', '_id createdAt totalAmount status');

    res.status(201).json(populatedReview);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Customer lấy đánh giá của chính mình
router.get('/my-reviews', async (req, res) => {
  try {
    if (req.user.role !== 'customer') {
      return res.status(403).json({ message: 'Chỉ dành cho customer' });
    }

    // Tìm customer theo email của user
    const customer = await Customer.findOne({ email: req.user.email });
    if (!customer) {
      // Trả về mảng rỗng nếu chưa có customer record
      return res.status(200).json([]);
    }

    const reviews = await Review.find({ customerId: customer._id })
      .populate('productId', 'name category price imageUrl')
      .populate('customerId', 'name phone email')
      .populate('orderId', '_id createdAt totalAmount status')
      .sort({ createdAt: -1 });
    
    // Đảm bảo luôn trả về mảng, không phải null
    const reviewsArray = reviews || [];
    return res.status(200).json(reviewsArray);
  } catch (error) {
    console.error('Error in /my-reviews:', error);
    return res.status(500).json({ 
      message: error.message || 'Có lỗi xảy ra khi tải đánh giá',
      error: error.toString()
    });
  }
});

// Các routes sau chỉ dành cho staff/admin
router.use(authorizeStaff);

// Lấy danh sách đánh giá (có lọc theo rating)
router.get('/', async (req, res) => {
  try {
    const { productId, customerId, rating } = req.query;
    const query = {};

    if (productId) {
      query.productId = productId;
    }
    if (customerId) {
      query.customerId = customerId;
    }
    if (rating) {
      query.rating = Number(rating);
    }

    const reviews = await Review.find(query)
      .populate('productId', 'name category')
      .populate('customerId', 'name phone email isBlocked')
      .sort({ createdAt: -1 });
    
    res.json(reviews);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Lấy thông tin một đánh giá
router.get('/:id', async (req, res) => {
  try {
    const review = await Review.findById(req.params.id)
      .populate('productId', 'name category')
      .populate('customerId', 'name phone email isBlocked');
    
    if (!review) {
      return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
    }
    res.json(review);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Sửa đánh giá
router.put('/:id', async (req, res) => {
  try {
    const review = await Review.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    )
      .populate('productId', 'name category')
      .populate('customerId', 'name phone email isBlocked');

    if (!review) {
      return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
    }
    res.json(review);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Admin phản hồi đánh giá
router.post('/:id/reply', async (req, res) => {
  try {
    const { reply } = req.body;
    
    if (!reply || reply.trim().length === 0) {
      return res.status(400).json({ message: 'Phản hồi không được để trống' });
    }

    const review = await Review.findByIdAndUpdate(
      req.params.id,
      { 
        adminReply: reply.trim(),
        adminRepliedAt: new Date()
      },
      { new: true, runValidators: true }
    )
      .populate('productId', 'name category')
      .populate('customerId', 'name phone email isBlocked');

    if (!review) {
      return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
    }
    
    res.json(review);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Xóa phản hồi admin
router.delete('/:id/reply', async (req, res) => {
  try {
    const review = await Review.findByIdAndUpdate(
      req.params.id,
      { 
        adminReply: null,
        adminRepliedAt: null
      },
      { new: true, runValidators: true }
    )
      .populate('productId', 'name category')
      .populate('customerId', 'name phone email isBlocked');

    if (!review) {
      return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
    }
    
    res.json(review);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Xóa đánh giá
router.delete('/:id', async (req, res) => {
  try {
    const review = await Review.findByIdAndDelete(req.params.id);
    if (!review) {
      return res.status(404).json({ message: 'Không tìm thấy đánh giá' });
    }
    res.json({ message: 'Đã xóa đánh giá thành công' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;

