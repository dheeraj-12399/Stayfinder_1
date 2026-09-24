// Vercel Serverless Function: GET /api/health
// Health check endpoint for StayFinder verification service

module.exports = function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  res.status(200).json({
    status: 'ok',
    service: 'StayFinder Cloudflare Turnstile Verification API (Vercel Serverless)',
    timestamp: new Date().toISOString()
  });
};
