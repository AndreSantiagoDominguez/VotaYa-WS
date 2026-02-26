const { Router } = require("express");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const { v4: uuidv4 } = require("uuid");
const db = require("./db");

const router = Router();

const JWT_SECRET = process.env.JWT_SECRET || "votaya-secret-key-cambiar-en-prod";
const SALT_ROUNDS = 10;

// ── Registro ──
router.post("/register", async (req, res) => {
  try {
    const { name, email, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({
        error: "Se requiere name, email y password",
      });
    }

    if (password.length < 6) {
      return res.status(400).json({
        error: "La contraseña debe tener al menos 6 caracteres",
      });
    }

    const existing = await db.findByEmail(email.toLowerCase().trim());
    if (existing) {
      return res.status(409).json({
        error: "El email ya está registrado",
      });
    }

    const id = uuidv4();
    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);

    await db.createUser(id, name.trim(), email.toLowerCase().trim(), passwordHash);

    const token = jwt.sign(
      { id, name: name.trim(), email: email.toLowerCase().trim() },
      JWT_SECRET,
      { expiresIn: "7d" }
    );

    console.log(`Usuario registrado: ${name} (${email})`);

    res.status(201).json({
      token,
      user: { id, name: name.trim(), email: email.toLowerCase().trim() },
    });
  } catch (err) {
    console.error("Error en registro:", err.message);
    res.status(500).json({ error: "Error interno del servidor" });
  }
});

// ── Login ──
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        error: "Se requiere email y password",
      });
    }

    const user = await db.findByEmail(email.toLowerCase().trim());

    if (!user) {
      return res.status(401).json({ error: "Credenciales inválidas" });
    }

    const valid = await bcrypt.compare(password, user.password_hash);

    if (!valid) {
      return res.status(401).json({ error: "Credenciales inválidas" });
    }

    const token = jwt.sign(
      { id: user.id, name: user.name, email: user.email },
      JWT_SECRET,
      { expiresIn: "7d" }
    );

    console.log(`Login exitoso: ${token} ${user.name} (${user.email})`);

    res.json({
      token,
      user: { id: user.id, name: user.name, email: user.email },
    });
  } catch (err) {
    console.error("Error en login:", err.message);
    res.status(500).json({ error: "Error interno del servidor" });
  }
});

module.exports = router;
