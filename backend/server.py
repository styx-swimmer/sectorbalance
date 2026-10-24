"""
Cloud Run HTTP Server for Sector Rebalance Platform.
Listens on $PORT (default 8080) and handles:
  - GET  /         : Health check (for Cloud Run startup/liveness probes)
  - GET  /health   : Health check
  - POST /         : Daily pipeline trigger (Cloud Scheduler)
  - POST /run      : Daily pipeline trigger
  - GET  /run      : Daily pipeline trigger (convenient browser/curl trigger)
  - GET  /status   : Returns latest cached calculation snapshot
"""
import os
import json
import traceback
from datetime import datetime

# Add current directory to Python path
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from main import run_daily_pipeline

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
          { "profile": "SHOCK_ABSORBER" | "WEALTH_COMPOUNDER", "capital": 100000.0 }
        """
        try:
            req_data = {}
            if request.method == "POST" and request.is_json:
                req_data = request.get_json(silent=True) or {}
            elif request.args:
                req_data = request.args.to_dict()

            profile = req_data.get("profile") or req_data.get("risk_profile") or "SHOCK_ABSORBER"
            capital_raw = req_data.get("capital", 100000.0)
            try:
                capital = float(capital_raw)
            except (ValueError, TypeError):
                capital = 100000.0

            result = run_daily_pipeline(risk_profile=profile, capital=capital)
            return jsonify({
                "status": "success",
                "triggered_at": datetime.utcnow().isoformat() + "Z",
                "risk_profile": profile,
                "result": result
            }), 200

        except Exception as e:
            traceback.print_exc()
            return jsonify({
                "status": "error",
                "error": str(e),
                "traceback": traceback.format_exc()
            }), 500

    @app.route("/status", methods=["GET"])
    def get_status():
        """Returns latest locally saved snapshot if available."""
        snapshot_path = os.path.join(os.path.dirname(__file__), "mock_firestore_output.json")
        if os.path.exists(snapshot_path):
            try:
                with open(snapshot_path, "r") as f:
                    data = json.load(f)
                return jsonify({"status": "success", "data": data}), 200
            except Exception as e:
                return jsonify({"status": "error", "message": str(e)}), 500
        return jsonify({
            "status": "no_snapshot",
            "message": "No local snapshot found. Trigger /run first."
        }), 404

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
            else:
                self.send_response(404)
                self.end_headers()

        def do_POST(self):
            self._handle_run()

        def _handle_run(self):
            try:
                result = run_daily_pipeline()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                resp = {"status": "success", "result": result}
                self.wfile.write(json.dumps(resp).encode("utf-8"))
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "error", "error": str(e)}).encode("utf-8"))

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
