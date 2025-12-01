const express = require('express');
const Order = require('../models/Order');
const Payment = require('../models/Payment');
const Product = require('../models/Product');
const Customer = require('../models/Customer');
const Review = require('../models/Review');
const { authenticate, authorizeStaff } = require('../middleware/auth');

const router = express.Router();

// Tất cả routes đều cần xác thực
router.use(authenticate);
router.use(authorizeStaff);

// Báo cáo doanh thu theo ngày
router.get('/revenue/daily', async (req, res) => {
  try {
    const { date } = req.query;
    const targetDate = date ? new Date(date) : new Date();
    const startOfDay = new Date(targetDate);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(targetDate);
    endOfDay.setHours(23, 59, 59, 999);

    const payments = await Payment.find({
      status: 'success',
      paidAt: { $gte: startOfDay, $lte: endOfDay },
    }).populate('orderId');

    const totalRevenue = payments.reduce((sum, payment) => sum + payment.amount, 0);
    const totalOrders = await Order.countDocuments({
      createdAt: { $gte: startOfDay, $lte: endOfDay },
    });
    const totalCustomers = await Customer.countDocuments({
      createdAt: { $gte: startOfDay, $lte: endOfDay },
    });

    res.json({
      date: startOfDay.toISOString().split('T')[0],
      totalRevenue,
      totalOrders,
      totalCustomers,
      successfulPayments: payments.length,
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Báo cáo doanh thu theo tháng
router.get('/revenue/monthly', async (req, res) => {
  try {
    const { year, month } = req.query;
    const targetYear = year ? parseInt(year) : new Date().getFullYear();
    const targetMonth = month ? parseInt(month) - 1 : new Date().getMonth();

    const startOfMonth = new Date(targetYear, targetMonth, 1);
    const endOfMonth = new Date(targetYear, targetMonth + 1, 0, 23, 59, 59, 999);

    const payments = await Payment.find({
      status: 'success',
      paidAt: { $gte: startOfMonth, $lte: endOfMonth },
    }).populate('orderId');

    const totalRevenue = payments.reduce((sum, payment) => sum + payment.amount, 0);
    const totalOrders = await Order.countDocuments({
      createdAt: { $gte: startOfMonth, $lte: endOfMonth },
    });
    const totalCustomers = await Customer.countDocuments({
      createdAt: { $gte: startOfMonth, $lte: endOfMonth },
    });

    res.json({
      year: targetYear,
      month: targetMonth + 1,
      totalRevenue,
      totalOrders,
      totalCustomers,
      successfulPayments: payments.length,
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Sản phẩm bán chạy nhất
router.get('/products/bestsellers', async (req, res) => {
  try {
    const { limit = 10, startDate, endDate } = req.query;
    
    // Build date filter
    const dateFilter = {};
    if (startDate || endDate) {
      dateFilter.createdAt = {};
      if (startDate) {
        const start = new Date(startDate);
        start.setHours(0, 0, 0, 0);
        dateFilter.createdAt.$gte = start;
      }
      if (endDate) {
        const end = new Date(endDate);
        end.setHours(23, 59, 59, 999);
        dateFilter.createdAt.$lte = end;
      }
    }
    
    const orders = await Order.find({ 
      status: { $in: ['paid', 'shipped'] },
      ...dateFilter
    })
      .populate('items.productId', 'name category price imageUrl');

    // Tính tổng số lượng bán của mỗi sản phẩm
    const productSales = {};
    
    orders.forEach(order => {
      order.items.forEach(item => {
        const productId = item.productId._id.toString();
        if (!productSales[productId]) {
          productSales[productId] = {
            productId: item.productId._id,
            name: item.productId.name,
            category: item.productId.category,
            totalSold: 0,
            totalRevenue: 0,
          };
        }
        productSales[productId].totalSold += item.quantity;
        productSales[productId].totalRevenue += item.price * item.quantity;
      });
    });

    const bestsellers = Object.values(productSales)
      .sort((a, b) => b.totalSold - a.totalSold)
      .slice(0, parseInt(limit));

    res.json(bestsellers);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Sản phẩm tồn kho thấp
router.get('/products/low-stock', async (req, res) => {
  try {
    const { threshold = 5 } = req.query;
    const products = await Product.find({
      stock: { $lt: parseInt(threshold) },
    }).sort({ stock: 1 });

    res.json(products);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Tỷ lệ đánh giá theo sao
router.get('/products/ratings', async (req, res) => {
  try {
    const { startDate, endDate } = req.query;
    
    // Build date filter
    const dateFilter = {};
    if (startDate || endDate) {
      dateFilter.createdAt = {};
      if (startDate) {
        const start = new Date(startDate);
        start.setHours(0, 0, 0, 0);
        dateFilter.createdAt.$gte = start;
      }
      if (endDate) {
        const end = new Date(endDate);
        end.setHours(23, 59, 59, 999);
        dateFilter.createdAt.$lte = end;
      }
    }
    
    const reviews = await Review.find(dateFilter);
    
    const ratingDistribution = {
      5: 0,
      4: 0,
      3: 0,
      2: 0,
      1: 0,
    };

    reviews.forEach(review => {
      ratingDistribution[review.rating]++;
    });

    const totalReviews = reviews.length;
    const ratingPercentages = {};
    Object.keys(ratingDistribution).forEach(rating => {
      ratingPercentages[rating] = totalReviews > 0
        ? ((ratingDistribution[rating] / totalReviews) * 100).toFixed(2)
        : 0;
    });

    res.json({
      distribution: ratingDistribution,
      percentages: ratingPercentages,
      totalReviews,
      averageRating: totalReviews > 0
        ? (reviews.reduce((sum, r) => sum + r.rating, 0) / totalReviews).toFixed(2)
        : 0,
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// Tổng hợp báo cáo
router.get('/summary', async (req, res) => {
  try {
    const { startDate, endDate } = req.query;
    
    let startOfPeriod, endOfPeriod;
    
    if (startDate && endDate) {
      // Sử dụng date range được chọn
      startOfPeriod = new Date(startDate);
      startOfPeriod.setHours(0, 0, 0, 0);
      endOfPeriod = new Date(endDate);
      endOfPeriod.setHours(23, 59, 59, 999);
    } else {
      // Mặc định: hôm nay và tháng này
      const today = new Date();
      startOfPeriod = new Date(today);
      startOfPeriod.setHours(0, 0, 0, 0);
      endOfPeriod = new Date(today);
      endOfPeriod.setHours(23, 59, 59, 999);

      const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
      const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 59, 999);
    }

    // Doanh thu trong khoảng thời gian - tính từ orders (paid/shipped) hoặc payments
    const periodPayments = await Payment.find({
      status: 'success',
      paidAt: { $gte: startOfPeriod, $lte: endOfPeriod },
    });
    const periodRevenueFromPayments = periodPayments.reduce((sum, p) => sum + p.amount, 0);
    
    // Tính từ orders (nếu không có payment thì tính từ order totalAmount)
    const periodOrders = await Order.find({
      createdAt: { $gte: startOfPeriod, $lte: endOfPeriod },
      status: { $in: ['paid', 'shipped'] },
    });
    const periodRevenueFromOrders = periodOrders.reduce((sum, o) => sum + o.totalAmount, 0);
    const periodRevenue = periodRevenueFromPayments > 0 ? periodRevenueFromPayments : periodRevenueFromOrders;

    // Số đơn hàng trong khoảng thời gian
    const periodOrdersCount = await Order.countDocuments({
      createdAt: { $gte: startOfPeriod, $lte: endOfPeriod },
    });

    // Nếu có date range, chỉ tính trong khoảng đó
    // Nếu không, tính cả hôm nay và tháng này
    let todayRevenue = periodRevenue;
    let todayOrdersCount = periodOrdersCount;
    let monthRevenue = periodRevenue;

    if (!startDate || !endDate) {
      // Tính riêng hôm nay
      const today = new Date();
      const startOfDay = new Date(today);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(today);
      endOfDay.setHours(23, 59, 59, 999);

      const todayPayments = await Payment.find({
        status: 'success',
        paidAt: { $gte: startOfDay, $lte: endOfDay },
      });
      const todayRevenueFromPayments = todayPayments.reduce((sum, p) => sum + p.amount, 0);
      
      const todayOrders = await Order.find({
        createdAt: { $gte: startOfDay, $lte: endOfDay },
        status: { $in: ['paid', 'shipped'] },
      });
      const todayRevenueFromOrders = todayOrders.reduce((sum, o) => sum + o.totalAmount, 0);
      todayRevenue = todayRevenueFromPayments > 0 ? todayRevenueFromPayments : todayRevenueFromOrders;
      todayOrdersCount = await Order.countDocuments({
        createdAt: { $gte: startOfDay, $lte: endOfDay },
      });

      // Tính tháng này
      const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
      const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0, 23, 59, 59, 999);

      const monthPayments = await Payment.find({
        status: 'success',
        paidAt: { $gte: startOfMonth, $lte: endOfMonth },
      });
      const monthRevenueFromPayments = monthPayments.reduce((sum, p) => sum + p.amount, 0);
      
      const monthOrders = await Order.find({
        createdAt: { $gte: startOfMonth, $lte: endOfMonth },
        status: { $in: ['paid', 'shipped'] },
      });
      const monthRevenueFromOrders = monthOrders.reduce((sum, o) => sum + o.totalAmount, 0);
      monthRevenue = monthRevenueFromPayments > 0 ? monthRevenueFromPayments : monthRevenueFromOrders;
    }

    // Tổng số đơn (tất cả thời gian)
    const totalOrders = await Order.countDocuments();

    // Tổng số khách
    const totalCustomers = await Customer.countDocuments();

    // Sản phẩm tồn kho thấp
    const lowStockProducts = await Product.countDocuments({ stock: { $lt: 5 } });

    // Tổng số sản phẩm
    const totalProducts = await Product.countDocuments();

    res.json({
      today: {
        revenue: todayRevenue,
        orders: todayOrdersCount,
      },
      thisMonth: {
        revenue: monthRevenue,
      },
      period: startDate && endDate ? {
        revenue: periodRevenue,
        orders: periodOrdersCount,
        startDate: startOfPeriod.toISOString().split('T')[0],
        endDate: endOfPeriod.toISOString().split('T')[0],
      } : null,
      totals: {
        orders: totalOrders,
        customers: totalCustomers,
        products: totalProducts,
        lowStockProducts,
      },
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;

