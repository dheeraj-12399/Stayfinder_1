// StayFinder Backend Service
// Handles Cloudflare Turnstile token validation via Cloudflare Siteverify API
// Port: 3000 (reverse proxied by Nginx)

const http = require('http');
const fs = require('fs');
const path = require('path');

// Load environment variables from .env or .env.example if available
function loadEnv() {
  const envFiles = [
    path.resolve(__dirname, '../.env'),
    path.resolve(__dirname, '../.env.example')
  ];

  for (const envPath of envFiles) {
    if (fs.existsSync(envPath)) {
      const content = fs.readFileSync(envPath, 'utf8');
      const lines = content.split('\n');
      for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
          const [key, ...vals] = trimmed.split('=');
          const value = vals.join('=').trim();
          if (!process.env[key.trim()] && value) {
            process.env[key.trim()] = value;
          }
        }
      }
    }
  }
}

loadEnv();

const PORT = parseInt(process.env.DEFAULT_APP_PORT || '3000', 10);
// Secret key must exist ONLY on the backend.
const CLOUDFLARE_SITEVERIFY_URL = 'https://challenges.cloudflare.com/turnstile/v0/siteverify';
const TEST_PASSING_SECRET_KEY = '1x0000000000000000000000000000000AA';

const server = http.createServer(async (req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  // Health check endpoint
  if (req.method === 'GET' && (url.pathname === '/api/health' || url.pathname === '/health')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      service: 'StayFinder Cloudflare Turnstile Verification API',
      timestamp: new Date().toISOString()
    }));
    return;
  }

  // Turnstile verification endpoint: POST /api/verify-turnstile
  if (req.method === 'POST' && url.pathname === '/api/verify-turnstile') {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      // Protect against overly large payloads
      if (body.length > 1e5) {
        req.destroy();
      }
    });

    req.on('end', async () => {
      try {
        let payload = {};
        if (body.trim()) {
          try {
            payload = JSON.parse(body);
          } catch (e) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              success: false,
              message: 'Invalid JSON payload'
            }));
            return;
          }
        }

        const token = payload.token;
        if (!token || typeof token !== 'string' || !token.trim()) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: false,
            message: 'Turnstile verification token is required'
          }));
          return;
        }

        // Retrieve the backend-only Turnstile Secret Key
        const secretKey = process.env.TURNSTILE_SECRET_KEY || TEST_PASSING_SECRET_KEY;
        const clientIp = req.headers['x-forwarded-for']?.split(',')[0]?.trim() ||
                         req.socket.remoteAddress;

        // Build request for Cloudflare Siteverify
        const formData = new URLSearchParams();
        formData.append('secret', secretKey);
        formData.append('response', token);
        if (clientIp) {
          formData.append('remoteip', clientIp);
        }

        console.log(`[Turnstile] Validating token with Cloudflare Siteverify (length: ${token.length})...`);

        const cfResponse = await fetch(CLOUDFLARE_SITEVERIFY_URL, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: formData.toString()
        });

        const cfResult = await cfResponse.json();
        console.log(`[Turnstile] Siteverify response: success=${cfResult.success}`);

        if (cfResult && cfResult.success === true) {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: true
          }));
        } else {
          // Never return the Cloudflare secret key or internal debugging details
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: false,
            message: 'Security verification failed'
          }));
        }
      } catch (err) {
        console.error('[Turnstile] Verification error:', err.message);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: false,
          message: 'Security verification failed'
        }));
      }
    });
    return;
  }

  // Fallback for unknown routes
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({
    success: false,
    message: 'Not found'
  }));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`StayFinder Turnstile Verification Backend listening on port ${PORT}`);
});
