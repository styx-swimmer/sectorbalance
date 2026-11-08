"""
Cloud Run HTTP Server for Sector Rebalance Platform.
Listens on $PORT (default 8080) and handles:
  - GET  /            : Health check (for Cloud Run startup/liveness probes)
  - GET  /health      : Health check
  - POST /            : Daily pipeline trigger (Cloud Scheduler)
  - POST /run         : Daily pipeline trigger
  - GET  /run         : Daily pipeline trigger (convenient browser/curl trigger)
  - GET  /status      : Returns latest Firestore calculation snapshot (with live compute fallback)
  - POST /rebalance   : Executes rebalance back to equal weight
  - GET  /rebalance   : Executes rebalance back to equal weight
"""
import os
import json
import traceback
from datetime import datetime

# Add current directory to Python path
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from main import run_daily_pipeline
from firestore_sync import FirestoreSync

# In-memory runtime cache for warm instances
_CACHED_PAYLOAD = None

try:
    from flask import Flask, request, jsonify
    has_flask = True
except ImportError:
    has_flask = False

if has_flask:
    app = Flask(__name__)

    @app.route("/", methods=["GET"])
    def root():
        """Health check endpoint for Cloud Run startup/liveness probes."""
        return jsonify({
            "status": "healthy",
            "service": "sector-rebalance-backend",
            "time": datetime.utcnow().isoformat() + "Z",
            "message": "Sector Rebalance & HMM Regime Engine is running."
        }), 200

    @app.route("/health", methods=["GET"])
    def health():
        return jsonify({"status": "healthy"}), 200

    @app.route("/", methods=["POST"])
    @app.route("/run", methods=["GET", "POST"])
    def trigger_pipeline():
        """
        Executes daily pipeline.
        Accepts optional JSON payload:
          { "profile": "SHOCK_ABSORBER" | "WEALTH_COMPOUNDER", "capital": 100000.0, "reset": false }
        """
        global _CACHED_PAYLOAD
        try:
            req_data = {}
            if request.method == "POST" and request.is_json:
                req_data = request.get_json(silent=True) or {}
            elif request.args:
                req_data = request.args.to_dict()

            profile = req_data.get("profile") or req_data.get("risk_profile") or "SHOCK_ABSORBER"
            capital_raw = req_data.get("capital", 100000.0)
            reset_raw = req_data.get("reset", False)
            try:
                capital = float(capital_raw)
            except (ValueError, TypeError):
                capital = 100000.0
            reset_flag = str(reset_raw).lower() in ("true", "1", "yes")

            result = run_daily_pipeline(risk_profile=profile, capital=capital, reset_to_equal_weight=reset_flag)
            _CACHED_PAYLOAD = result

            return jsonify({
                "status": "success",
                "triggered_at": datetime.utcnow().isoformat() + "Z",
                "risk_profile": profile,
                "data": result,
                "result": result
            }), 200

        except Exception as e:
            traceback.print_exc()
            return jsonify({
                "status": "error",
                "error": str(e),
                "traceback": traceback.format_exc()
            }), 500

    @app.route("/rebalance", methods=["GET", "POST"])
    def trigger_rebalance():
        """Explicitly rebalances portfolio positions back to equal weight."""
        global _CACHED_PAYLOAD
        try:
            result = run_daily_pipeline(risk_profile="SHOCK_ABSORBER", reset_to_equal_weight=True)
            _CACHED_PAYLOAD = result
            return jsonify({
                "status": "rebalance_executed",
                "triggered_at": datetime.utcnow().isoformat() + "Z",
                "data": result,
                "result": result
            }), 200
        except Exception as e:
            return jsonify({"status": "error", "error": str(e)}), 500

    @app.route("/status", methods=["GET"])
    def get_status():
        """
        Returns latest calculated state.
        Priority:
          1. Firestore live database query
          2. In-memory runtime cache
          3. Fresh pipeline calculation on demand
        """
        global _CACHED_PAYLOAD
        try:
            # 1. Try reading live from Firestore
            sync_client = FirestoreSync()
            live_data = sync_client.get_latest_state()

            if live_data and live_data.get("portfolio_state"):
                _CACHED_PAYLOAD = live_data
                return jsonify({"status": "success", "data": live_data}), 200

            # 2. Check in-memory cache
            if _CACHED_PAYLOAD is not None:
                return jsonify({"status": "success", "data": _CACHED_PAYLOAD}), 200

            # 3. If no state exists anywhere, calculate live right now
            print("[Server] No prior state found in Firestore or cache. Computing live state now...")
            computed = run_daily_pipeline()
            _CACHED_PAYLOAD = computed
            return jsonify({"status": "success", "data": computed}), 200

        except Exception as e:
            traceback.print_exc()
            return jsonify({"status": "error", "message": str(e)}), 500

else:
    # Standalone standard library fallback if Flask is not installed
    from http.server import HTTPServer, BaseHTTPRequestHandler

    class StandaloneHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path in ("/", "/health"):
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                resp = {"status": "healthy", "service": "sector-rebalance-backend"}
                self.wfile.write(json.dumps(resp).encode("utf-8"))
            elif self.path.startswith("/run"):
                self._handle_run()
            elif self.path.startswith("/status"):
                self._handle_status()
            else:
                self.send_response(404)
                self.end_headers()

        def do_POST(self):
            if self.path.startswith("/status"):
                self._handle_status()
            else:
                self._handle_run()

        def _handle_run(self):
            try:
                result = run_daily_pipeline()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                resp = {"status": "success", "data": result, "result": result}
                self.wfile.write(json.dumps(resp).encode("utf-8"))
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "error", "error": str(e)}).encode("utf-8"))

        def _handle_status(self):
            sync_client = FirestoreSync()
            live_data = sync_client.get_latest_state()
            if not live_data:
                live_data = run_daily_pipeline()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            resp = {"status": "success", "data": live_data}
            self.wfile.write(json.dumps(resp).encode("utf-8"))

        def log_message(self, format, *args):
            sys.stderr.write(f"[{datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S')}] {format % args}\n")

    app = None


def start_server():
    port = int(os.environ.get("PORT", 8080))
    host = "0.0.0.0"
    print(f"Starting Sector Rebalance HTTP server on {host}:{port}...")
    if has_flask and app is not None:
        app.run(host=host, port=port)
    else:
        httpd = HTTPServer((host, port), StandaloneHandler)
        print(f"Running standalone HTTP server on {host}:{port}")
        httpd.serve_forever()


if __name__ == "__main__":
    start_server()
