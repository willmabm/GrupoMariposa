const express = require('express');
const router = express.Router();
const Product = require('../models/Product');
const Customer = require('../models/Customer');

// Obtener un producto por ID
router.get('/products/:id', async (req, res) => {
  try {
    const product = await Product.findOne({ productId: req.params.id }).lean();
    if (!product) return res.status(404).json({ error: 'Producto no encontrado' });
    res.json(product);
  } catch (error) {
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// Obtener un cliente por ID
router.get('/customers/:id', async (req, res) => {
  try {
    const customer = await Customer.findOne({ customerId: req.params.id }).lean();
    if (!customer) return res.status(404).json({ error: 'Cliente no encontrado' });
    res.json(customer);
  } catch (error) {
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

module.exports = router;