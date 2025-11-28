const mongoose = require('mongoose');

const orderItemSchema = new mongoose.Schema({
  productId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Product',
    required: true,
  },
  quantity: {
    type: Number,
    required: true,
    min: 1,
  },
  price: {
    type: Number,
    required: true,
    min: 0,
  },
});

const orderSchema = new mongoose.Schema({
  customerId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Customer',
    required: true,
  },
  userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  items: [orderItemSchema],
  totalAmount: {
    type: Number,
    required: true,
    min: 0,
  },
  voucherCode: {
    type: String,
    default: null,
    trim: true,
    uppercase: true,
  },
  voucherId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Voucher',
    default: null,
  },
  discountAmount: {
    type: Number,
    default: 0,
    min: 0,
  },
  shippingFee: {
    type: Number,
    default: 0,
    min: 0,
  },
  finalAmount: {
    type: Number,
    required: true,
    min: 0,
  },
  status: {
    type: String,
    enum: ['pending', 'processing', 'delivering', 'paid', 'shipped', 'cancelled'],
    default: 'pending',
  },
}, {
  timestamps: true,
});

module.exports = mongoose.model('Order', orderSchema);
