const express = require('express');
const Product = require('../models/Product');
const Customer = require('../models/Customer');
const Order = require('../models/Order');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Tìm kiếm sản phẩm - CẦN ĐĂNG NHẬP (tất cả user)
router.get('/products', async (req, res) => {
  try {
    const { name, category, minPrice, maxPrice } = req.query;
    const query = {};

    if (name) {
      query.name = { $regex: name, $options: 'i' };
    }
    if (category) {
      query.category = { $regex: category, $options: 'i' };
    }
    if (minPrice || maxPrice) {
      query.price = {};
      if (minPrice) query.price.$gte = Number(minPrice);
      if (maxPrice) query.price.$lte = Number(maxPrice);
    }

    const products = await Product.find(query).sort({ createdAt: -1 });
    res.json(products);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Các routes sau chỉ dành cho staff/admin
router.use(authorizeStaff);

// Tìm kiếm khách hàng
router.get('/customers', async (req, res) => {
  try {
    const { name, phone } = req.query;
    const query = {};

    if (name) {
      query.name = { $regex: name, $options: 'i' };
    }
    if (phone) {
      query.phone = { $regex: phone, $options: 'i' };
    }

    const customers = await Customer.find(query).sort({ createdAt: -1 });
    res.json(customers);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Tìm kiếm đơn hàng
router.get('/orders', async (req, res) => {
  try {
    const { customerId, status, startDate, endDate } = req.query;
    const query = {};

    if (customerId) {
      query.customerId = customerId;
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
      .populate('customerId', 'name phone email')
      .populate('userId', 'username')
      .populate('items.productId', 'name category price imageUrl')
      .sort({ createdAt: -1 });
    
    res.json(orders);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;

