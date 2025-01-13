import os
from flask import Flask, request, jsonify
from google.cloud import secretmanager

# Create Flask app instance
app = Flask(__name__)

# Function to access the secret
def get_secret():
    """
    Retrieves the secret value from Google Secret Manager.
    """
    client = secretmanager.SecretManagerServiceClient()
    # Resource path for the secret
    secret_path = "projects/690882718361/secrets/soccer_secret_key/versions/latest"
    # Access the secret version
    response = client.access_secret_version(request={"name": secret_path})
    # Return the secret value
    return response.payload.data.decode("UTF-8")

@app.route('/service-check', methods=['GET'])
def service_check():
    """
    Endpoint to check service status.
    Verifies a secret key before returning the status.
    """
    # Retrieve the secret value
    secret_key = get_secret()

    # Extract secret key from request headers
    key = request.headers.get('X-Secret-Key')

    # Validate the secret key
    if key != secret_key:
        return jsonify({"error": "Unauthorized"}), 403

    # If the secret key matches, return active status
    return jsonify({"status": "Active"}), 200
