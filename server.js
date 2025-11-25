require('dotenv').config();
const express = require('express');
const cors = require('cors');
const connectDB = require('./config/database');

// Kết nối database
connectDB();

const app = express();

// Middleware
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Logging middleware để debug
app.use((req, res, next) => {
  if (req.path.startsWith('/api/')) {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
  }
  next();
});

// Routes API
app.use('/api/auth', require('./routes/auth'));
app.use('/api/customers', require('./routes/customers'));
app.use('/api/products', require('./routes/products'));
app.use('/api/orders', require('./routes/orders'));
app.use('/api/payments', require('./routes/payments'));
app.use('/api/reviews', require('./routes/reviews'));
app.use('/api/search', require('./routes/search'));
app.use('/api/reports', require('./routes/reports'));
app.use('/api/vouchers', require('./routes/vouchers'));
app.use('/api/upload', require('./routes/upload'));

// Serve static files for admin web dashboard
app.use(express.static('public'));

// Serve uploaded images
app.use('/uploads', express.static('uploads'));

// Route mặc định - redirect to admin login
app.get('/', (req, res) => {
  res.redirect('/index.html');
});

// Route /admin - redirect to admin login
app.get('/admin', (req, res) => {
  res.redirect('/index.html');
});

// Xử lý lỗi 404 - chỉ cho API routes, không cho static files
app.use('/api/*', (req, res) => {
  res.status(404).json({ message: 'Không tìm thấy route API' });
});

// Xử lý lỗi server
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ message: 'Lỗi server', error: err.message });
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server đang chạy tại port ${PORT}`);
});

