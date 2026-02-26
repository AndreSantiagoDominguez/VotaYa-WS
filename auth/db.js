require("dotenv").config();
const mysql = require("mysql2/promise");

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
});


async function initDB() {
  await pool.execute(`
    CREATE TABLE IF NOT EXISTS users (
      id VARCHAR(36) PRIMARY KEY,
      name VARCHAR(100) NOT NULL,
      email VARCHAR(255) UNIQUE NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);
  console.log("Base de datos inicializada");
}

async function createUser(id, name, email, passwordHash) {
  await pool.execute(
    "INSERT INTO users (id, name, email, password_hash) VALUES (?, ?, ?, ?)",
    [id, name, email, passwordHash]
  );
}

async function findByEmail(email) {
  const [rows] = await pool.execute(
    "SELECT id, name, email, password_hash FROM users WHERE email = ?",
    [email]
  );
  return rows[0] || null;
}

async function getUserCount() {
  const [rows] = await pool.execute("SELECT COUNT(*) as count FROM users");
  return rows[0].count;
}

module.exports = { initDB, createUser, findByEmail, getUserCount };
