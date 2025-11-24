const express = require("express");
const router = express.Router();
const User = require("../models/user.model");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");

// Đăng ký khách hàng
router.post("/register", async (req, res) => {
    try {
        const { name, email, password } = req.body;

        const existed = await User.findOne({ email });
        if (existed) return res.status(400).json({ message: "Email đã tồn tại" });

        const hashed = await bcrypt.hash(password, 10);

        const user = new User({
            name,
            email,
            password: hashed,
            role: "customer"
        });

        await user.save();

        res.json({ message: "Đăng ký thành công" });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});
// Đăng nhập
router.post("/login", async (req, res) => {
    try {
        const { email, password } = req.body;

        const user = await User.findOne({ email });
        if (!user) return res.status(400).json({ message: "Sai email hoặc mật khẩu" });

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) return res.status(400).json({ message: "Sai email hoặc mật khẩu" });

        const token = jwt.sign(
            { id: user._id, role: user.role },
            process.env.JWT_SECRET,
            { expiresIn: "1d" }
        );

        res.json({
            message: "Đăng nhập thành công",
            token
        });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});
