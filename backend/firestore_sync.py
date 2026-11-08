"""
Firestore Synchronization Client.
Pushes and reads daily calculations to/from Google Cloud Firestore:
  - market_regime/daily_state
  - sectors_current/latest
  - portfolio_state/current
  - performance_history/latest
"""
import os
import json
from datetime import datetime
from typing import Dict, Any, Optional
from config import FIRESTORE_COLLECTIONS, DEFAULT_PROJECT_ID


class FirestoreSync:
    def __init__(self, project_id: Optional[str] = None):
        self.project_id = (
            project_id
            or os.environ.get("GOOGLE_CLOUD_PROJECT")
            or os.environ.get("GCP_PROJECT")
            or DEFAULT_PROJECT_ID
        )
        self.db = None
        self._init_client()

    def _init_client(self):
        try:
            from google.cloud import firestore
            if self.project_id:
                self.db = firestore.Client(project=self.project_id)
            else:
                self.db = firestore.Client()
            print(f"[FirestoreSync] Connected to Firestore (project={self.db.project})")
        except Exception as e:
            print(f"[FirestoreSync] Notice: Running in standalone/local mode ({e}). Outputs will use local JSON cache.")
            self.db = None

    def get_portfolio_state(self) -> Optional[Dict[str, Any]]:
        """Reads latest portfolio_state from Firestore or local cache."""
        if self.db is not None:
            try:
                doc = self.db.collection(FIRESTORE_COLLECTIONS["PORTFOLIO_STATE"]).document("current").get()
                if doc.exists:
                    return doc.to_dict()
            except Exception as e:
                print(f"[FirestoreSync] Warning reading portfolio_state from Firestore: {e}")

        local_data = self._read_local_snapshot()
        return local_data.get("portfolio_state") if local_data else None

    def get_latest_state(self) -> Optional[Dict[str, Any]]:
        """
        Reads complete latest snapshot from Firestore.
        Returns dict with keys: market_regime, sectors_current, portfolio_state, performance_history, timestamp.
        """
        if self.db is not None:
            try:
                regime_doc = self.db.collection(FIRESTORE_COLLECTIONS["MARKET_REGIME"]).document("daily_state").get()
                sectors_doc = self.db.collection(FIRESTORE_COLLECTIONS["SECTORS_CURRENT"]).document("latest").get()
                portfolio_doc = self.db.collection(FIRESTORE_COLLECTIONS["PORTFOLIO_STATE"]).document("current").get()
                perf_doc = self.db.collection(FIRESTORE_COLLECTIONS["PERFORMANCE_HISTORY"]).document("latest").get()

                if portfolio_doc.exists and regime_doc.exists:
                    portfolio_data = portfolio_doc.to_dict()
                    regime_data = regime_doc.to_dict()
                    sectors_data = sectors_doc.to_dict().get("sectors", {}) if sectors_doc.exists else {}
                    perf_data = perf_doc.to_dict() if perf_doc.exists else {}

                    return {
                        "timestamp": portfolio_data.get("updated_at", datetime.utcnow().isoformat() + "Z"),
                        "market_regime": regime_data,
                        "sectors_current": sectors_data,
                        "portfolio_state": portfolio_data,
                        "performance_history": perf_data,
                    }
            except Exception as e:
                print(f"[FirestoreSync] Warning reading full state from Firestore: {e}")

        # Fallback to local snapshot
        return self._read_local_snapshot()

    def sync_all(
        self,
        regime_data: Dict[str, Any],
        sectors_data: Dict[str, Any],
        portfolio_state: Dict[str, Any],
        performance_data: Dict[str, Any],
    ) -> bool:
        """
        Syncs all state dictionaries to Firestore and updates local cache.
        """
        payload = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "market_regime": regime_data,
            "sectors_current": sectors_data,
            "portfolio_state": portfolio_state,
            "performance_history": performance_data,
        }

        # 1. If connected to live Firestore, write documents
        synced_firestore = False
        if self.db is not None:
            try:
                # 1a. Market Regime
                self.db.collection(FIRESTORE_COLLECTIONS["MARKET_REGIME"]).document("daily_state").set({
                    **regime_data,
                    "updated_at": payload["timestamp"],
                })

                # 1b. Sectors Current
                self.db.collection(FIRESTORE_COLLECTIONS["SECTORS_CURRENT"]).document("latest").set({
                    "sectors": sectors_data,
                    "updated_at": payload["timestamp"],
                })

                # 1c. Portfolio State
                self.db.collection(FIRESTORE_COLLECTIONS["PORTFOLIO_STATE"]).document("current").set({
                    **portfolio_state,
                    "updated_at": payload["timestamp"],
                })

                # 1d. Performance History
                self.db.collection(FIRESTORE_COLLECTIONS["PERFORMANCE_HISTORY"]).document("latest").set({
                    **performance_data,
                    "updated_at": payload["timestamp"],
                })

                print("[FirestoreSync] Successfully synced all collections to Google Cloud Firestore.")
                synced_firestore = True
            except Exception as ex:
                print(f"[FirestoreSync] Error uploading to Firestore: {ex}")

        # 2. Update local disk snapshots (both in /tmp and backend dir for local dev)
        for export_path in [
            "/tmp/sector_latest_snapshot.json",
            os.path.join(os.path.dirname(__file__), "mock_firestore_output.json"),
        ]:
            try:
                with open(export_path, "w") as f:
                    json.dump(payload, f, indent=2)
            except Exception:
                pass

        return synced_firestore

    def _read_local_snapshot(self) -> Optional[Dict[str, Any]]:
        for path in [
            "/tmp/sector_latest_snapshot.json",
            os.path.join(os.path.dirname(__file__), "mock_firestore_output.json"),
        ]:
            if os.path.exists(path):
                try:
                    with open(path, "r") as f:
                        return json.load(f)
                except Exception:
                    pass
        return None
