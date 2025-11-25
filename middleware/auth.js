const jwt = require('jsonwebtoken');
const User = require('../models/User');

// Xác thực token
const authenticate = async (req, res, next) => {
  try {
    const token = req.header('Authorization')?.replace('Bearer ', '');
    
    if (!token) {
      return res.status(401).json({ message: 'Không có token, truy cập bị từ chối' });
    }

    if (!process.env.JWT_SECRET) {
      return res.status(500).json({ message: 'Cấu hình server không hợp lệ' });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.id).select('-password');
    
    if (!user) {
      return res.status(401).json({ message: 'Token không hợp lệ' });
    }

    // Kiểm tra nếu là customer, xem tài khoản có bị khóa không
    if (user.role === 'customer' && user.email) {
      const Customer = require('../models/Customer');
      const customer = await Customer.findOne({ 
        email: user.email.toLowerCase(),
        isBlocked: true 
      });
      if (customer) {
        return res.status(403).json({ 
          message: 'Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.',
          blocked: true
        });
      }
    }

    req.user = user;
    next();
  } catch (error) {
    if (error.name === 'JsonWebTokenError' || error.name === 'TokenExpiredError') {
      return res.status(401).json({ message: 'Token không hợp lệ hoặc đã hết hạn' });
    }
    res.status(401).json({ message: 'Token không hợp lệ' });
  }
};

// Phân quyền admin
const authorizeAdmin = (req, res, next) => {
  if (req.user.role !== 'admin') {
    return res.status(403).json({ message: 'Chỉ admin mới có quyền truy cập' });
  }
  next();
};

// Phân quyền admin hoặc staff
const authorizeStaff = (req, res, next) => {
  if (req.user.role !== 'admin' && req.user.role !== 'staff') {
    return res.status(403).json({ message: 'Không có quyền truy cập' });
  }
  next();
};

module.exports = { authenticate, authorizeAdmin, authorizeStaff };

