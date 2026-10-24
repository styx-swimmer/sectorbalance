import unittest
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from server import has_flask, app


class TestServer(unittest.TestCase):
    def test_server_app_structure(self):
        # Verify server module loads and has Flask app configured when flask is available
        if has_flask and app is not None:
            client = app.test_client()
            response = client.get("/")
            self.assertEqual(response.status_code, 200)
            data = response.get_json()
            self.assertEqual(data.get("status"), "healthy")

            health_resp = client.get("/health")
            self.assertEqual(health_resp.status_code, 200)


if __name__ == "__main__":
    unittest.main()
