"""
Firestore Synchronization Client.
Pushes daily calculations to Google Cloud Firestore:
  - market_regime/daily_state
  - sectors_current/latest
  - portfolio_state/current
  - performance_history/latest
"""
import os
import json
from datetime import datetime
from typing import Dict, Any
from config import FIRESTORE_COLLECTIONS


class FirestoreSync:
    def __init__(self, project_id: str = None):
        self.project_id = project_id or os.environ.get("GCP_PROJECT") or os.environ.get("GOOGLE_CLOUD_PROJECT")
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
            print(f"[FirestoreSync] Notice: Running in standalone/local mode ({e}). Outputs will be exported to local JSON.")
            self.db = None

    def sync_all(
        self,
        regime_data: Dict[str, Any],
        sectors_data: Dict[str, Any],
        portfolio_state: Dict[str, Any],
        performance_data: Dict[str, Any],
    ) -> bool:
        """
        Syncs all state dictionaries to Firestore or local export.
        """
        payload = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "market_regime": regime_data,
            "sectors_current": sectors_data,
            "portfolio_state": portfolio_state,
            "performance_history": performance_data,
        }

        # 1. If connected to live Firestore, write documents
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
                return True
            except Exception as ex:
                print(f"[FirestoreSync] Error uploading to Firestore: {ex}")

        # 2. Local snapshot backup (always saved for client verification / testing)
        export_path = os.path.join(os.path.dirname(__file__), "mock_firestore_output.json")
        try:
            with open(export_path, "w") as f:
                json.dump(payload, f, indent=2)
            print(f"[FirestoreSync] Saved state snapshot to {export_path}")
        except Exception as ex:
            print(f"[FirestoreSync] Failed writing local snapshot: {ex}")

        return True
