import os
from flask import Flask, request, jsonify

# Create Flask app instance
app = Flask(__name__)

# Retrieve the secret key from an environment variable
SECRET_KEY = os.environ.get("SECRET_KEY")

@app.route('/service-check', methods=['GET'])
def service_check():
    """
    Endpoint to check service status.
    Verifies a secret key before returning the status.
    """
    # Extract secret key from request headers
    key = request.headers.get('X-Secret-Key')

    # Validate the secret key
    if key != SECRET_KEY:
        return jsonify({"error": "Unauthorized"}), 403

    # If the secret key matches, return active status
    return jsonify({"status": "Active"}), 200

