# StayFinder Cloudflare Turnstile Verification on Vercel

This is the minimal standalone web & serverless backend for **StayFinder Cloudflare Turnstile Verification**, ready to deploy on **Vercel** with zero configuration.

> **Note**: Do NOT deploy the native Android/Kotlin project to Vercel. Deploy only this web & serverless package.

---

## Architecture & Endpoints

### 1. Online Verification Web Page (HTTPS)
- **`GET /`** or **`GET /verify.html`**
  - **Function**: Minimal, dark-mode HTTPS web page containing the Cloudflare Turnstile widget.
  - **Query Parameters**: `?sitekey=<YOUR_PUBLIC_SITEKEY>` (optional, defaults to Cloudflare public test key).
  - **Android Bridge**:
    1. Direct JavaScript Bridge: `window.AndroidBridge.onTokenReceived(token)`
    2. Deep Link Fallback: `stayfinder://turnstile-callback?token=<TOKEN>`
  - **Security**: Contains ONLY public site keys. `TURNSTILE_SECRET_KEY` is NEVER exposed to this page.

### 2. Backend Serverless API (POST)
- **`POST /api/verify-turnstile`**
  - **Request Body**: `{ "token": "CLOUDFLARE_TURNSTILE_TOKEN" }`
  - **Function**: Server-side validation against Cloudflare Siteverify (`https://challenges.cloudflare.com/turnstile/v0/siteverify`) using the server-side `TURNSTILE_SECRET_KEY`.
  - **Success Response (200)**: `{ "success": true }`
  - **Failure Response (400)**: `{ "success": false, "message": "Security verification failed" }`

### 3. Health Check
- **`GET /api/health`**
  - **Function**: Returns `{ "status": "ok", "service": "stayfinder-turnstile-vercel" }`.

---

## Deployment Steps on Vercel

### Option 1: Vercel Web Dashboard (Recommended)

1. Import your repository into the [Vercel Dashboard](https://vercel.com/new).
2. Set the **Root Directory** to `vercel-backend` (or leave as `/` if deploying from the root repository).
3. Under **Environment Variables**, add:
   - **Key**: `TURNSTILE_SECRET_KEY`
   - **Value**: Your Cloudflare Turnstile Secret Key (or Cloudflare's test key `1x0000000000000000000000000000000AA`).
4. Click **Deploy**.

### Option 2: Vercel CLI

```bash
cd vercel-backend
npm i -g vercel
vercel
vercel env add TURNSTILE_SECRET_KEY
vercel --prod
```

---

## Exact Production URLs for Android App

Once deployed, Vercel provides your production domain (e.g., `https://stayfinder-turnstile.vercel.app`).

Configure these two values in your StayFinder Android `.env` file:

```properties
# 1. Online Turnstile Verification Web Page (opened by Android WebView)
TURNSTILE_PAGE_URL=https://<your-project>.vercel.app

# 2. Server-side Verification API Endpoint (validated before SMS OTP)
TURNSTILE_BACKEND_URL=https://<your-project>.vercel.app/api/verify-turnstile
```

---

## Android Security Flow

1. User enters mobile number in StayFinder `AuthScreen`.
2. StayFinder WebView loads `TURNSTILE_PAGE_URL`.
3. Cloudflare Turnstile widget presents challenge to user.
4. User completes challenge -> Token passed back to Android app via `AndroidBridge.onTokenReceived(token)` or `stayfinder://turnstile-callback?token=...`.
5. User taps **Send Verification Code**.
6. StayFinder calls `TURNSTILE_BACKEND_URL` (`POST /api/verify-turnstile`) with `{ "token": token }`.
7. Backend validates token with Cloudflare Siteverify using secret key.
8. Only upon successful server response (`{ "success": true }`), the native `PhoneAuthProvider.verifyPhoneNumber` executes to send the Firebase SMS OTP.

