// routes/order.js
const express = require('express');
const router = express.Router();
const Cart = require('../models/cart');
const Order = require('../models/order');
const Product = require('../models/Product');

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

module.exports = router;
