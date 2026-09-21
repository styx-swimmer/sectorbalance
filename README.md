# Sector Rebalance Platform: Shannon's Demon & Market Regimes

A quantitative portfolio platform built on empirical research (1998–Present) testing sector rotation and portfolio rebalancing mechanics.

The platform implements **Drift-Triggered Systematic Rebalancing (Shannon's Demon)** across all **11 SPDR Sector ETFs** with a **3-State Hidden Markov Model (HMM)** daily regime indicator pinned at the top.

---

## 🏛️ System Architecture

```
sector/
├── backend/                             # Python computation engine & GCP Cloud Functions
│   ├── main.py                          # Cloud Run / CLI daily pipeline runner
│   ├── config.py                        # 11 SPDR tickers, drift tolerances (5% & 10%), Firestore schema
│   ├── data_fetcher.py                  # yfinance & Massive.com (Polygon) market data client
│   ├── hmm_regime.py                    # 3-State Gaussian HMM (Low Vol Grind / Choppy / Crash)
│   ├── rebalance_engine.py              # Shannon's Demon 11-sector drift calculator & order generator
│   ├── performance_tracker.py          # Tracks Rebalancing Premium (+alpha bps) vs SPY & Drift
│   ├── firestore_sync.py                # Cloud Firestore sync & local snapshot backup
│   ├── requirements.txt                 # Dependencies
│   ├── Dockerfile                       # Container deployment for Cloud Run
│   └── tests/                           # Unit tests (unittest / pytest)
│
├── android/                             # Android Studio Project (Kotlin + Jetpack Compose)
│   ├── build.gradle.kts                 # Root Gradle configuration
│   ├── settings.gradle.kts
│   ├── gradle/libs.versions.toml        # Version Catalog
│   └── app/
│       ├── build.gradle.kts             # App Gradle with Compose & Firebase Firestore
│       └── src/main/
│           ├── AndroidManifest.xml
│           └── java/com/sector/rebalance/
│               ├── SectorApp.kt         # Application class
│               ├── MainActivity.kt      # Main Activity with Compose Navigation (4 tabs)
│               ├── data/                # Data models & Firestore repository with offline fallback
│               ├── domain/              # Risk profiles (5% vs 10%) & Rebalance calculator
│               └── ui/
│                   ├── components/      # MarketRegimeBanner (top card) & SectorRowItem
│                   ├── dashboard/       # 11-sector drift meters & status badges
│                   ├── action/          # Rebalance trade orders (Trim X, Buy Y) & rationale
│                   ├── analytics/       # Shannon's Demon Rebalancing Premium charts & research
│                   ├── settings/        # Profile selection & backend info
│                   └── theme/           # Modern Dark Quant theme (Material 3)
│
├── docs/                                # Static site for Google Hosting
│   ├── index.html                       # Quantitative whitepaper & strategy overview
│   ├── privacy.html                     # Privacy Policy (App Store / Google Play compliant)
│   ├── terms.html                       # Terms of Service & Financial Disclaimers
│   └── styles.css                       # Responsive dark stylesheet
│
├── .gitignore
└── README.md
```

---

## 🔬 Core Strategy & Research Takeaways

1. **Equal Weight 11 SPDR Universe ($w^* = 1/11 \approx 9.0909\%$)**:
   - `XLB` (Materials), `XLE` (Energy), `XLF` (Financials), `XLI` (Industrials), `XLK` (Technology), `XLP` (Consumer Staples), `XLU` (Utilities), `XLV` (Health Care), `XLY` (Consumer Discretionary), `XLC` (Communication Services), `XLRE` (Real Estate).
2. **Shannon's Demon Rebalancing Engine**:
   - Systematically harvests cross-sectional volatility: trims outperforming sectors and buys undervalued laggards whenever drift exceeds the threshold band.
   - **Wealth Compounder (10% Drift Band)**: Lets strong momentum run longer, minimizes turnover friction, maximizes long-term compounding.
   - **Shock Absorber (5% Drift Band)**: Tighter risk control, frequent volatility harvesting, cut drawdowns in half during market shocks.
3. **Daily Hidden Markov Model (HMM) Regimes (Top of App)**:
   - 🟢 **Low Volatility / Slow Grind Up**: Bullish, steady compounding environment (100% invested).
   - 🟡 **Choppy / Sideways**: Mean-reverting environment (optimal conditions for Shannon's Demon volatility harvesting).
   - 🔴 **High Volatility / Crash**: High-risk turbulent shock (signals defensive 50% equity / 50% cash preservation).

---

## 🚀 Quickstart: Backend Pipeline

### 1. Run Unit Tests
```bash
python3 -m unittest discover -s backend/tests
```

### 2. Run Daily Calculation Locally
```bash
python3 backend/main.py --profile SHOCK_ABSORBER --capital 100000
```
This fetches prices, executes the 3-state HMM, calculates 11-sector drift, generates rebalance orders, computes the Rebalancing Premium, and saves the output to `backend/mock_firestore_output.json` (and syncs to Google Cloud Firestore if GCP credentials are configured).

### 3. Deploy to Google Cloud Functions or Cloud Run
```bash
# Cloud Function deployment
gcloud functions deploy sector-rebalance-daily \
  --runtime python311 \
  --trigger-http \
  --entry-point cloud_function_entrypoint \
  --source ./backend \
  --allow-unauthenticated

# Or Cloud Run container deployment
gcloud run deploy sector-rebalance-service \
  --source ./backend \
  --platform managed \
  --region us-central1
```
Use **Google Cloud Scheduler** to trigger the endpoint daily at market close (e.g., `0 21 * * 1-5`).

---

## 📱 Running the Android App in Android Studio

1. Open **Android Studio**.
2. Select **Open** and choose the `/android` directory:
   `/Users/kok/Documents/net26/sector/android`
3. Android Studio will sync Gradle dependencies automatically.
4. To connect to your live Google Cloud Firestore database:
   - Download `google-services.json` from your Firebase Console.
   - Place it into `/Users/kok/Documents/net26/sector/android/app/google-services.json`.
   - Uncomment `alias(libs.plugins.google.services)` in `android/app/build.gradle.kts`.
5. Run on an Android Emulator or physical device (Target SDK 35, Min SDK 26).

---

## 🌐 Deploying Docs & Legal to Google Hosting

The `/docs` directory is self-contained static HTML/CSS ready for Google Cloud Storage or Firebase Hosting:

```bash
# Deploy to Firebase Hosting
firebase deploy --only hosting

# Or deploy to Google Cloud Storage bucket
gsutil -m rsync -r -d ./docs gs://YOUR_BUCKET_NAME/
gsutil web set -m index.html -e index.html gs://YOUR_BUCKET_NAME/
```

---

## 📦 Version Control (GitHub)

To push this repository to your GitHub account:
```bash
git add .
git commit -m "Initial commit: Shannon's Demon 11-Sector Rebalance Engine & Android App"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/sector-rebalance.git
git push -u origin main
```
