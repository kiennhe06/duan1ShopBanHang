const express = require('express');
const Product = require('../models/Product');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực (đăng nhập)
router.use(authenticate);

// Lấy danh sách sản phẩm (có tìm kiếm) - CẦN ĐĂNG NHẬP (tất cả user)
router.get('/', async (req, res) => {
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

// Lấy thông tin một sản phẩm - CẦN ĐĂNG NHẬP (tất cả user)
router.get('/:id', async (req, res) => {
  try {
    const product = await Product.findById(req.params.id);
    if (!product) {
      return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
    }
    res.json(product);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Các routes sau chỉ dành cho staff/admin (thêm, sửa, xóa)
router.use(authorizeStaff);

// Thêm sản phẩm
router.post('/', async (req, res) => {
  try {
    // Đảm bảo imageUrl được trim và validate
    if (req.body.imageUrl !== undefined) {
      req.body.imageUrl = req.body.imageUrl ? req.body.imageUrl.trim() : '';
      // Nếu rỗng, set thành undefined để không lưu vào DB
      if (req.body.imageUrl === '') {
        req.body.imageUrl = undefined;
      }
    }
    
    const product = await Product.create(req.body);
    res.status(201).json(product);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Sửa thông tin sản phẩm
router.put('/:id', async (req, res) => {
  try {
    // Đảm bảo imageUrl được lưu đúng (trim và validate)
    if (req.body.imageUrl !== undefined) {
      if (req.body.imageUrl === null || req.body.imageUrl === '') {
        req.body.imageUrl = null; // Lưu null vào database
      } else {
        req.body.imageUrl = req.body.imageUrl.trim();
      }
    }
    
    const product = await Product.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    );
    if (!product) {
      return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
    }
    
    res.json(product);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});
// Xóa sản phẩm
router.delete('/:id', async (req, res) => {
  try {
    const product = await Product.findByIdAndDelete(req.params.id);
    if (!product) {
      return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
    }
    res.json({ message: 'Đã xóa sản phẩm thành công' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;
