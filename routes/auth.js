const express = require('express');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

// Đăng ký
router.post('/register', async (req, res) => {
  try {
    const { username, email, password, role } = req.body;

    // Kiểm tra email có bị khóa không (nếu có email)
    if (email) {
      const Customer = require('../models/Customer');
      const blockedCustomer = await Customer.findOne({ 
        email: email.toLowerCase(),
        isBlocked: true 
      });
      if (blockedCustomer) {
        return res.status(403).json({ message: 'Email này đã bị khóa và không thể đăng ký' });
      }
    }

    // Kiểm tra user đã tồn tại
    const userExists = await User.findOne({ $or: [{ email }, { username }] });
    if (userExists) {
      return res.status(400).json({ message: 'Tên đăng nhập hoặc email đã tồn tại' });
    }

    const user = await User.create({
      username,
      email,
      password,
      role: role || 'customer', // Mặc định là customer nếu không chỉ định
    });

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, {
      expiresIn: '30d',
    });

    res.status(201).json({
      message: 'Đăng ký thành công',
      token,
      user: {
        id: user._id,
        username: user.username,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Đăng nhập
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    
    console.log('Login attempt:', { username, hasPassword: !!password });

    if (!username || !password) {
      console.log('Missing username or password');
      return res.status(400).json({ message: 'Vui lòng nhập tên đăng nhập và mật khẩu' });
    }

    const user = await User.findOne({ username });
    if (!user) {
      console.log('User not found:', username);
      return res.status(401).json({ message: 'Tên đăng nhập hoặc mật khẩu không đúng' });
    }

    console.log('User found:', user.username, 'Role:', user.role);
    
    const isMatch = await user.matchPassword(password);
    if (!isMatch) {
      console.log('Password mismatch for user:', username);
      return res.status(401).json({ message: 'Tên đăng nhập hoặc mật khẩu không đúng' });
    }
    
    console.log('Password match successful for user:', username);

    // Kiểm tra nếu là customer, xem tài khoản có bị khóa không
    if (user.role === 'customer' && user.email) {
      const Customer = require('../models/Customer');
      const customer = await Customer.findOne({ 
        email: user.email.toLowerCase(),
        isBlocked: true 
      });
      if (customer) {
        return res.status(403).json({ message: 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.' });
      }
    }

    // Kiểm tra JWT_SECRET
    if (!process.env.JWT_SECRET) {
      console.error('JWT_SECRET is not set!');
      return res.status(500).json({ 
        message: 'Cấu hình server không đúng. Vui lòng liên hệ quản trị viên.',
        error: 'JWT_SECRET missing'
      });
    }

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, {
      expiresIn: '30d',
    });

    console.log('Login successful for user:', username, 'Token generated');

    // Đảm bảo luôn trả về JSON hợp lệ với headers đúng
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    return res.status(200).json({
      message: 'Đăng nhập thành công',
      token,
      user: {
        id: user._id,
        username: user.username,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    console.error('Login error:', error);
    console.error('Error stack:', error.stack);
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    return res.status(500).json({ 
      message: error.message || 'Có lỗi xảy ra khi đăng nhập',
      error: error.toString()
    });
  }
});

// Lấy thông tin user hiện tại
router.get('/me', authenticate, async (req, res) => {
  try {
    // Kiểm tra nếu là customer, xem tài khoản có bị khóa không
    if (req.user.role === 'customer' && req.user.email) {
      const Customer = require('../models/Customer');
      const customer = await Customer.findOne({ 
        email: req.user.email.toLowerCase(),
        isBlocked: true 
      });
      if (customer) {
        return res.status(403).json({ 
          message: 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.',
          blocked: true
        });
      }
    }

  res.json({
    user: {
      id: req.user._id,
      username: req.user.username,
      email: req.user.email,
      role: req.user.role,
    },
  });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;

