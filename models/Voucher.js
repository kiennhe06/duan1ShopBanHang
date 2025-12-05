const mongoose = require('mongoose');

const voucherSchema = new mongoose.Schema({
  code: {
    type: String,
    required: [true, 'Vui lòng nhập mã voucher'],
    unique: true,
    trim: true,
    uppercase: true,
  },
  name: {
    type: String,
    required: [true, 'Vui lòng nhập tên voucher'],
    trim: true,
  },
  description: {
    type: String,
    trim: true,
  },
  discountType: {
    type: String,
    enum: ['percentage', 'fixed'],
    required: [true, 'Vui lòng chọn loại giảm giá'],
    default: 'percentage',
  },
  discountValue: {
    type: Number,
    required: [true, 'Vui lòng nhập giá trị giảm giá'],
    min: 0,
  },
  minOrderAmount: {
    type: Number,
    default: 0,
    min: 0,
  },
  maxDiscountAmount: {
    type: Number,
    default: null, // null = không giới hạn
    min: 0,
  },
  usageLimit: {
    type: Number,
    default: null, // null = không giới hạn
    min: 1,
  },
  usedCount: {
    type: Number,
    default: 0,
    min: 0,
  },
  startDate: {
    type: Date,
    required: [true, 'Vui lòng nhập ngày bắt đầu'],
  },
  endDate: {
    type: Date,
    required: [true, 'Vui lòng nhập ngày kết thúc'],
  },
  isActive: {
    type: Boolean,
    default: true,
  },
  applicableProducts: {
    type: [mongoose.Schema.Types.ObjectId],
    ref: 'Product',
    default: [], // empty = áp dụng cho tất cả sản phẩm
  },
  applicableCategories: {
    type: [String],
    default: [], // empty = áp dụng cho tất cả danh mục
  },
}, {
  timestamps: true,
});

// Index để tìm kiếm nhanh
voucherSchema.index({ code: 1 });
voucherSchema.index({ isActive: 1, startDate: 1, endDate: 1 });

// Method để kiểm tra voucher có hợp lệ không
voucherSchema.methods.isValid = function(orderAmount = 0) {
  const now = new Date();
  
  // Kiểm tra active
  if (!this.isActive) {
    return { valid: false, message: 'Voucher không còn hoạt động' };
  }
  
  // Kiểm tra thời gian
  if (now < this.startDate) {
    return { valid: false, message: 'Voucher chưa có hiệu lực' };
  }
  
  if (now > this.endDate) {
    return { valid: false, message: 'Voucher đã hết hạn' };
  }
  
  // Kiểm tra số lần sử dụng
  if (this.usageLimit !== null && this.usedCount >= this.usageLimit) {
    return { valid: false, message: 'Voucher đã hết lượt sử dụng' };
  }
  
  // Kiểm tra giá trị đơn hàng tối thiểu
  if (orderAmount < this.minOrderAmount) {
    return { valid: false, message: `Đơn hàng tối thiểu ${this.minOrderAmount.toLocaleString('vi-VN')}₫ để sử dụng voucher này` };
  }
  
  return { valid: true, message: 'Voucher hợp lệ' };
};

// Method để tính giá trị giảm giá
voucherSchema.methods.calculateDiscount = function(orderAmount) {
  let discount = 0;
  
  if (this.discountType === 'percentage') {
    discount = (orderAmount * this.discountValue) / 100;
    // Áp dụng max discount nếu có
    if (this.maxDiscountAmount !== null && discount > this.maxDiscountAmount) {
      discount = this.maxDiscountAmount;
    }
  } else if (this.discountType === 'fixed') {
    discount = this.discountValue;
    // Không được vượt quá giá trị đơn hàng
    if (discount > orderAmount) {
      discount = orderAmount;
    }
  }
  
  return Math.round(discount);
};

module.exports = mongoose.model('Voucher', voucherSchema);
