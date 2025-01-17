from flask import Flask, request, jsonify
from google.cloud import secretmanager
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)

def get_secret():
    """Retrieve the secret key from Google Cloud Secret Manager."""
    try:
        client = secretmanager.SecretManagerServiceClient()
        secret_path = "projects/690882718361/secrets/soccer_secret_key/versions/latest"
        response = client.access_secret_version(request={"name": secret_path})
        return response.payload.data.decode("UTF-8")
    except Exception as e:
        logging.error("Error accessing secret: %s", e)
        raise

@app.route('/service-check', methods=['GET'])
def service_check():
    """Service check endpoint."""
    try:
        secret_key = get_secret()
    except Exception:
        return jsonify({"error": "Internal server error"}), 500

    key = request.headers.get('X-Secret-Key')
    if not key:
        logging.warning("Missing 'X-Secret-Key' header.")
        return jsonify({"error": "Missing secret key"}), 400

    if key != secret_key:
        logging.warning("Unauthorized access attempt.")
        return jsonify({"error": "Unauthorized"}), 403

    logging.info("Service check successful.")
    return jsonify({"status": "Active"}), 200

if __name__ == '__main__':
    app.run(debug=True)
