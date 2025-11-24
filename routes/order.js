// routes/order.js
const express = require('express');
const router = express.Router();
const Cart = require('../models/cart');
const Order = require('../models/order');
const Product = require('../models/Product');

// Middleware giả lập xác thực user
const authMiddleware = (req, res, next) => {
    if (!req.body.userId) return res.status(401).json({ message: "Unauthorized" });
    next();
};

// =======================
// 1️⃣ Tạo đơn hàng từ giỏ hàng
// POST /orders/create
router.post('/create', async (req, res) => {
    const { userId } = req.body;
    try {
        const cart = await Cart.findOne({ userId }).populate('items.productId');
        if (!cart || cart.items.length === 0) return res.status(400).json({ message: 'Cart is empty' });

        const items = cart.items.map(i => ({
            productId: i.productId._id,
            quantity: i.quantity,
            price: i.productId.price
        }));

        const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0);

        const order = new Order({ userId, items, total });
        await order.save();

        // Xóa giỏ hàng sau khi tạo đơn
        await Cart.deleteOne({ userId });

        res.json(order);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// =======================
// 2️⃣ Xem danh sách đơn hàng của user
// GET /orders/:userId
router.get('/:userId', authMiddleware, async (req, res) => {
    try {
        const orders = await Order.find({ userId: req.params.userId }).sort({ createdAt: -1 });
        res.json(orders);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// =======================
// 3️⃣ Xem chi tiết đơn hàng
// GET /orders/detail/:orderId
router.get('/detail/:orderId', authMiddleware, async (req, res) => {
    try {
        const order = await Order.findById(req.params.orderId).populate('items.productId');
        if (!order) return res.status(404).json({ message: 'Order not found' });
        res.json(order);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// =======================
// 4️⃣ Cập nhật trạng thái đơn hàng
// PUT /orders/update-status
router.put('/update-status', authMiddleware, async (req, res) => {
    const { orderId, status } = req.body; // status: pending, completed, cancelled
    try {
        const order = await Order.findById(orderId);
        if (!order) return res.status(404).json({ message: 'Order not found' });

        order.status = status;
        await order.save();
        res.json(order);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// =======================
// 5️⃣ Thanh toán COD (đơn giản)
// POST /orders/pay/cod
router.post('/pay/cod', authMiddleware, async (req, res) => {
    const { orderId } = req.body;
    try {
        const order = await Order.findById(orderId);
        if (!order) return res.status(404).json({ message: 'Order not found' });

        order.status = 'pending'; // COD vẫn là pending
        await order.save();
        res.json({ message: 'Order placed with COD', order });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// =======================
// 6️⃣ Thanh toán online VNPay (demo)
// POST /orders/pay/vnpay
router.post('/pay/vnpay', authMiddleware, async (req, res) => {
    const { orderId } = req.body;
    try {
        const order = await Order.findById(orderId);
        if (!order) return res.status(404).json({ message: 'Order not found' });

        // Giả lập tạo URL thanh toán VNPay
        const vnpUrl = `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?amount=${order.total}&orderId=${order._id}`;
        
        res.json({ message: 'Redirect user to VNPay', url: vnpUrl });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

module.exports = router;
