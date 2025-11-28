const mongoose = require('mongoose');

const productSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, 'Vui lòng nhập tên sản phẩm'],
    trim: true,
  },
  category: {
    type: String,
    required: [true, 'Vui lòng nhập danh mục'],
    trim: true,
  },
  price: {
    type: Number,
    required: [true, 'Vui lòng nhập giá'],
    min: 0,
  },
  stock: {
    type: Number,
    required: [true, 'Vui lòng nhập số lượng tồn kho'],
    min: 0,
    default: 0,
  },
  description: {
    type: String,
    trim: true,
  },
  imageUrl: {
    type: String,
    trim: true,
  },
  sizes: {
    type: [String],
    default: ['S', 'M', 'L', 'XL'],
  },
}, {
  timestamps: true,
});

module.exports = mongoose.model('Product', productSchema);

