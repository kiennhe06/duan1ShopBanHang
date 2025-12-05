const express = require('express');
const Voucher = require('../models/Voucher');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);

// Lấy danh sách voucher - Tất cả user có thể xem
router.get('/', async (req, res) => {
  try {
    const { code, isActive } = req.query;
    const query = {};

    if (code) {
      query.code = { $regex: code, $options: 'i' };
    }

    // Nếu là customer, chỉ lấy voucher active và còn hiệu lực
    if (req.user.role === 'customer') {
      query.isActive = true;
      const now = new Date();
      query.startDate = { $lte: now };
      query.endDate = { $gte: now };
    } else {
      // Staff/Admin có thể filter theo isActive
      if (isActive !== undefined) {
        query.isActive = isActive === 'true';
      }
    }

    let vouchers = await Voucher.find(query).sort({ createdAt: -1 });
    
    // Nếu là customer, filter thêm voucher đã hết lượt sử dụng
    if (req.user.role === 'customer') {
      vouchers = vouchers.filter(voucher => {
        // Nếu voucher có usageLimit, kiểm tra xem còn lượt không
        if (voucher.usageLimit !== null && voucher.usageLimit > 0) {
          return voucher.usedCount < voucher.usageLimit;
        }
        // Nếu không có giới hạn, luôn hiển thị
        return true;
      });
    }
    
    res.json(vouchers);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Lấy voucher theo code - Dùng để validate khi checkout
router.get('/code/:code', async (req, res) => {
  try {
    const { code } = req.params;
    const { orderAmount } = req.query; // Order amount để validate

    const voucher = await Voucher.findOne({ code: code.toUpperCase() });
    
    if (!voucher) {
      return res.status(404).json({ message: 'Mã voucher không tồn tại' });
    }

    // Validate voucher
    const validation = voucher.isValid(parseFloat(orderAmount || 0));
    
    if (!validation.valid) {
      return res.status(400).json({ 
        message: validation.message,
        voucher: null 
      });
    }

    res.json({
      voucher,
      discountAmount: voucher.calculateDiscount(parseFloat(orderAmount || 0)),
      message: validation.message
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Lấy voucher theo ID
router.get('/:id', async (req, res) => {
  try {
    const voucher = await Voucher.findById(req.params.id);
    if (!voucher) {
      return res.status(404).json({ message: 'Không tìm thấy voucher' });
    }
    res.json(voucher);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Các routes sau chỉ dành cho staff/admin
router.use(authorizeStaff);

// Tạo voucher mới
router.post('/', async (req, res) => {
  try {
    console.log('Creating voucher with data:', JSON.stringify(req.body, null, 2));
    
    // Đảm bảo code là uppercase
    if (req.body.code) {
      req.body.code = req.body.code.toUpperCase().trim();
    }
    
    // Validate required fields
    if (!req.body.code) {
      return res.status(400).json({ message: 'Mã voucher là bắt buộc' });
    }
    if (!req.body.name) {
      return res.status(400).json({ message: 'Tên voucher là bắt buộc' });
    }
    if (!req.body.discountType || !['percentage', 'fixed'].includes(req.body.discountType)) {
      return res.status(400).json({ message: 'Loại giảm giá không hợp lệ' });
    }
    if (!req.body.discountValue || req.body.discountValue <= 0) {
      return res.status(400).json({ message: 'Giá trị giảm giá phải lớn hơn 0' });
    }
    if (!req.body.startDate) {
      return res.status(400).json({ message: 'Ngày bắt đầu là bắt buộc' });
    }
    if (!req.body.endDate) {
      return res.status(400).json({ message: 'Ngày kết thúc là bắt buộc' });
    }
    
    // Validate dates
    const startDate = new Date(req.body.startDate);
    const endDate = new Date(req.body.endDate);
    
    if (isNaN(startDate.getTime())) {
      return res.status(400).json({ message: 'Ngày bắt đầu không hợp lệ' });
    }
    if (isNaN(endDate.getTime())) {
      return res.status(400).json({ message: 'Ngày kết thúc không hợp lệ' });
    }
    if (endDate <= startDate) {
      return res.status(400).json({ message: 'Ngày kết thúc phải sau ngày bắt đầu' });
    }
    
    // Validate percentage
    if (req.body.discountType === 'percentage' && req.body.discountValue > 100) {
      return res.status(400).json({ message: 'Phần trăm giảm giá không được vượt quá 100%' });
    }
    
    // Set defaults
    req.body.minOrderAmount = req.body.minOrderAmount || 0;
    req.body.isActive = req.body.isActive !== undefined ? req.body.isActive : true;
    req.body.usedCount = 0;
    
    // Chỉ set maxDiscountAmount nếu có và là percentage
    if (req.body.discountType !== 'percentage') {
      req.body.maxDiscountAmount = undefined;
    }
    
    const voucher = await Voucher.create(req.body);
    console.log('Voucher created successfully:', voucher._id);
    res.status(201).json(voucher);
  } catch (error) {
    console.error('Error creating voucher:', error);
    if (error.code === 11000) {
      return res.status(400).json({ message: 'Mã voucher đã tồn tại' });
    }
    if (error.name === 'ValidationError') {
      const messages = Object.values(error.errors).map(e => e.message).join(', ');
      return res.status(400).json({ message: messages });
    }
    res.status(400).json({ message: error.message || 'Có lỗi xảy ra khi tạo voucher' });
  }
});

// Cập nhật voucher
router.put('/:id', async (req, res) => {
  try {
    // Đảm bảo code là uppercase nếu có
    if (req.body.code) {
      req.body.code = req.body.code.toUpperCase().trim();
    }

    const voucher = await Voucher.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true, runValidators: true }
    );
    
    if (!voucher) {
      return res.status(404).json({ message: 'Không tìm thấy voucher' });
    }
    
    res.json(voucher);
  } catch (error) {
    if (error.code === 11000) {
      return res.status(400).json({ message: 'Mã voucher đã tồn tại' });
    }
    res.status(400).json({ message: error.message });
  }
});

// Xóa voucher
router.delete('/:id', async (req, res) => {
  try {
    const voucher = await Voucher.findByIdAndDelete(req.params.id);
    if (!voucher) {
      return res.status(404).json({ message: 'Không tìm thấy voucher' });
    }
    res.json({ message: 'Đã xóa voucher thành công' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Tăng số lần sử dụng (gọi khi order được tạo thành công)
router.patch('/:id/use', async (req, res) => {
  try {
    const voucher = await Voucher.findById(req.params.id);
    if (!voucher) {
      return res.status(404).json({ message: 'Không tìm thấy voucher' });
    }

    voucher.usedCount += 1;
    await voucher.save();

    res.json(voucher);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;
