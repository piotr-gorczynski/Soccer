from flask import Flask, jsonify, request, Response
from google.cloud import secretmanager
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Function to get the secret key from Google Cloud Secret Manager
def get_secret():
    client = secretmanager.SecretManagerServiceClient()
    name = "projects/690882718361/secrets/soccer_secret_key/versions/latest"
    response = client.access_secret_version(name=name)
    secret = response.payload.data.decode("UTF-8")
    logger.info(f"Retrieved secret key: {secret}")  # Log the retrieved secret key
    return secret

# Service check endpoint
def service_check(request):
    try:
        secret_key = get_secret()

        # Check if the header contains the required key
        if 'X-Secret-Key' not in request.headers:
            logger.info("X-Secret-Key header is missing")
            return Response(jsonify({"error": "Missing secret key"}).data, status=400, mimetype="application/json")

        # Log the provided secret key from headers
        provided_key = request.headers['X-Secret-Key']
        logger.info(f"X-Secret-Key from headers: {provided_key}")

        # Verify the provided key
        if provided_key != secret_key:
            logger.info("Provided X-Secret-Key does not match the secret key")
            return Response(jsonify({"error": "Unauthorized"}).data, status=403, mimetype="application/json")

        return Response(jsonify({"status": "Active"}).data, status=200, mimetype="application/json")
    except Exception as e:
        logger.error(f"An error occurred: {e}")
        return Response(jsonify({"error": "Internal server error"}).data, status=500, mimetype="application/json")

# Flask route for testing locally
@app.route("/service-check", methods=["GET"])
def service_check_endpoint():
    return service_check(request)

if __name__ == "__main__":
    app.run(debug=True)
