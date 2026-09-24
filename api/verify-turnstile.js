// Vercel Serverless Function: POST /api/verify-turnstile
// Validates Cloudflare Turnstile token on the backend using TURNSTILE_SECRET_KEY

const CLOUDFLARE_SITEVERIFY_URL = 'https://challenges.cloudflare.com/turnstile/v0/siteverify';
const TEST_PASSING_SECRET_KEY = '1x0000000000000000000000000000000AA';

module.exports = async function handler(req, res) {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,POST');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, Authorization'
  );

  // Handle preflight OPTIONS request
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Only POST is allowed for verification
  if (req.method !== 'POST') {
    return res.status(405).json({
      success: false,
      message: 'Method not allowed'
    });
  }

  try {
    // Parse payload safely
    let payload = req.body;
    if (typeof payload === 'string') {
      try {
        payload = JSON.parse(payload);
      } catch (e) {
        return res.status(400).json({
          success: false,
          message: 'Invalid JSON payload'
        });
      }
    }

    const token = payload?.token;

    // Validate that the token exists
    if (!token || typeof token !== 'string' || !token.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Turnstile verification token is required'
      });
    }

    // Backend-only secret key (falls back to Cloudflare test passing secret if not configured)
    const secretKey = process.env.TURNSTILE_SECRET_KEY || TEST_PASSING_SECRET_KEY;

    // Client IP (optional, helps Cloudflare risk assessment)
    const clientIp = req.headers['x-forwarded-for']?.split(',')[0]?.trim() ||
                     req.socket?.remoteAddress;

    const formData = new URLSearchParams();
    formData.append('secret', secretKey);
    formData.append('response', token.trim());
    if (clientIp) {
      formData.append('remoteip', clientIp);
    }

    const cfResponse = await fetch(CLOUDFLARE_SITEVERIFY_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: formData.toString()
    });

    const cfResult = await cfResponse.json();

    if (cfResult && cfResult.success === true) {
      return res.status(200).json({
        success: true
      });
    } else {
      // Never return the Cloudflare secret key or internal error codes to client
      return res.status(400).json({
        success: false,
        message: 'Security verification failed'
      });
    }
  } catch (error) {
    console.error('Turnstile verification error:', error.message);
    return res.status(500).json({
      success: false,
      message: 'Security verification failed'
    });
  }
};
