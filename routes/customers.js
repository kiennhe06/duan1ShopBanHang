const express = require('express');
const Customer = require('../models/Customer');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Thêm khách hàng - Customer và Staff/Admin đều có thể tạo
router.post('/', async (req, res) => {
  try {
    const customer = await Customer.create(req.body);
    res.status(201).json(customer);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Tìm khách hàng theo phone - Customer có thể tìm để check xem đã có chưa (phải đặt trước /:id)
router.get('/find', async (req, res) => {
  try {
    const { phone } = req.query;
    if (!phone) {
      return res.status(400).json({ message: 'Vui lòng cung cấp số điện thoại' });
    }
    
    // Nếu là customer, chỉ cho tìm customer của chính họ (theo email để đảm bảo bảo mật)
    if (req.user.role === 'customer') {
      // Chỉ tìm customer có email trùng với user email để đảm bảo bảo mật
      // Không cho tìm theo phone nếu không có email match để tránh leak thông tin customer khác
      const customer = await Customer.findOne({ 
        phone,
        email: req.user.email 
      });
      return res.json(customer ? [customer] : []);
    }
    
    // Staff/Admin có thể tìm tất cả
    const customers = await Customer.find({ phone: { $regex: phone, $options: 'i' } });
    res.json(customers);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Các routes sau chỉ dành cho staff/admin
router.use(authorizeStaff);

// Lấy danh sách khách hàng (có tìm kiếm)
router.get('/', async (req, res) => {
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

// Lấy thông tin một khách hàng
router.get('/:id', async (req, res) => {
  try {
    const customer = await Customer.findById(req.params.id);
    if (!customer) {
      return res.status(404).json({ message: 'Không tìm thấy khách hàng' });
    }
    res.json(customer);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Sửa thông tin khách hàng
router.put('/:id', async (req, res) => {
  try {
    const customer = await Customer.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    );
    if (!customer) {
      return res.status(404).json({ message: 'Không tìm thấy khách hàng' });
    }
    res.json(customer);
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

// Xóa khách hàng
router.delete('/:id', async (req, res) => {
  try {
    const customer = await Customer.findByIdAndDelete(req.params.id);
    if (!customer) {
      return res.status(404).json({ message: 'Không tìm thấy khách hàng' });
    }
    res.json({ message: 'Đã xóa khách hàng thành công' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Khóa/Mở khóa khách hàng
router.patch('/:id/block', async (req, res) => {
  try {
    const { isBlocked } = req.body;
    const customer = await Customer.findByIdAndUpdate(
      req.params.id,
      { isBlocked: isBlocked !== undefined ? isBlocked : true },
      { new: true, runValidators: true }
    );
    if (!customer) {
      return res.status(404).json({ message: 'Không tìm thấy khách hàng' });
    }
    res.json({
      message: customer.isBlocked ? 'Đã khóa tài khoản khách hàng' : 'Đã mở khóa tài khoản khách hàng',
      customer
    });
  } catch (error) {
    res.status(400).json({ message: error.message });
  }
});

module.exports = router;

