import sys
import os
import unittest
from unittest.mock import patch, MagicMock
from flask import Flask

# Add the service-check directory to the Python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../service-check')))

# Patch google.auth.default before importing the module so that Cloud Logging
# initialization in main.py does not attempt to read actual credentials.
with patch("google.auth.default", return_value=(None, "dummy-project")):
    from main import service_check

class TestServiceCheck(unittest.TestCase):

    def setUp(self):
        # Create a Flask test client
        self.app = Flask(__name__).test_client()
        self.secret_key = "projects/690882718361/secrets/soccer_secret_key/versions/latest"

    @patch("main.cloud_logging.Client")
    @patch("main.get_secret")
    def test_service_check_success(self, mock_get_secret, mock_log_client):
        mock_log_client.return_value = MagicMock(setup_logging=MagicMock())
        # Mock the secret key retrieval
        mock_get_secret.return_value = self.secret_key

        # Simulate a request with the correct header
        headers = {"X-Secret-Key": self.secret_key}
        with self.app.application.test_request_context(headers=headers) as context:
            response = service_check(context.request)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json, {"status": "Active"})

    @patch("main.cloud_logging.Client")
    @patch("main.get_secret")
    def test_service_check_missing_header(self, mock_get_secret, mock_log_client):
        mock_log_client.return_value = MagicMock(setup_logging=MagicMock())
        # Mock the secret key retrieval
        mock_get_secret.return_value = self.secret_key

        # Simulate a request without the header
        with self.app.application.test_request_context() as context:
            response = service_check(context.request)

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json, {"error": "Missing secret key"})

    @patch("main.cloud_logging.Client")
    @patch("main.get_secret")
    def test_service_check_unauthorized(self, mock_get_secret, mock_log_client):
        mock_log_client.return_value = MagicMock(setup_logging=MagicMock())
        # Mock the secret key retrieval
        mock_get_secret.return_value = self.secret_key

        # Simulate a request with an incorrect header
        headers = {"X-Secret-Key": "wrong-secret-key"}
        with self.app.application.test_request_context(headers=headers) as context:
            response = service_check(context.request)

        self.assertEqual(response.status_code, 403)
        self.assertEqual(response.json, {"error": "Unauthorized"})

    @patch("main.cloud_logging.Client")
    @patch("main.get_secret")
    def test_service_check_internal_error(self, mock_get_secret, mock_log_client):
        mock_log_client.return_value = MagicMock(setup_logging=MagicMock())
        # Mock an exception during secret key retrieval
        mock_get_secret.side_effect = Exception("Secret retrieval failed")

        # Simulate a request with the header
        headers = {"X-Secret-Key": self.secret_key}
        with self.app.application.test_request_context(headers=headers) as context:
            response = service_check(context.request)

        self.assertEqual(response.status_code, 500)
        self.assertEqual(response.json, {"error": "Internal server error"})

if __name__ == "__main__":
    unittest.main()
