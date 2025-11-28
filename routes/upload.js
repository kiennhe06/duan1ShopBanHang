const express = require('express');
const upload = require('../middleware/upload');
const { authenticate, authorizeStaff } = require('../middleware/auth');
const path = require('path');

const router = express.Router();

// Tất cả routes đều cần xác thực và quyền staff/admin
router.use(authenticate);
router.use(authorizeStaff);

// Upload ảnh
router.post('/image', upload.single('image'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: 'Không có file được upload' });
    }

    // Trả về URL của ảnh đã upload
    const imageUrl = `/uploads/images/${req.file.filename}`;
    res.json({
      message: 'Upload ảnh thành công',
      imageUrl: imageUrl,
      filename: req.file.filename
    });
  } catch (error) {
    res.status(500).json({ message: 'Lỗi khi upload ảnh', error: error.message });
  }
});

module.exports = router;



