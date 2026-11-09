require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const enrichmentRoutes = require('./routes/enrichment');

const app = express();
app.use(express.json());
app.use('/api', enrichmentRoutes);

const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/enrichment_db';

mongoose.connect(MONGO_URI)
  .then(() => {
    app.listen(PORT, () => console.log(`API de enriquecimiento en puerto ${PORT}`));
  })
  .catch(err => console.error('Error conectando a MongoDB', err));