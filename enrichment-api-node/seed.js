const mongoose = require('mongoose');
const Product = require('./models/Product');
const Customer = require('./models/Customer');

async function seed() {
  await mongoose.connect('mongodb://localhost:27017/enrichment_db');
  await Product.deleteMany({});
  await Customer.deleteMany({});
  await Product.insertMany([
    { productId: 'product-789', name: 'Laptop', description: 'Laptop 15"', price: 999 },
    { productId: 'product-790', name: 'Mouse', description: 'Mouse inalámbrico', price: 25 }
  ]);
  await Customer.insertMany([
    { customerId: 'customer-456', name: 'Ana Pérez', active: true },
    { customerId: 'customer-999', name: 'Cliente Inactivo', active: false }
  ]);
  console.log('Datos semilla insertados');
  process.exit(0);
}
seed();