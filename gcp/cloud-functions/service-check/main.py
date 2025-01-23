from flask import Flask, jsonify, request, Response
from google.cloud import secretmanager
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)


# Test logging
logging.info("Logging is initialized successfully.")

# Function to get the secret key from Google Cloud Secret Manager
def get_secret():
    logging.info("Starting secret retrieval...")
    try:
        client = secretmanager.SecretManagerServiceClient()
        name = "projects/690882718361/secrets/soccer_secret_key/versions/latest"
        response = client.access_secret_version(name=name)
        secret = response.payload.data.decode("UTF-8")
        logging.info(f"Retrieved secret key: {secret}")  # Log the retrieved secret key
        return secret
    except Exception as e:
        logging.error(f"Failed to retrieve secret: {e}")
        raise

# Service check endpoint
def service_check(request):
    logging.info("Service-check endpoint invoked.")
    try:
        secret_key = get_secret()

        # Check if the header contains the required key
        if 'X-Secret-Key' not in request.headers:
            logging.info("X-Secret-Key header is missing")
            return Response(jsonify({"error": "Missing secret key"}).data, status=400, mimetype="application/json")

        # Log the provided secret key from headers
        provided_key = request.headers['X-Secret-Key']
        logging.info(f"X-Secret-Key from headers: {provided_key}")

        # Verify the provided key
        if provided_key != secret_key:
            logging.info("Provided X-Secret-Key does not match the secret key")
            return Response(jsonify({"error": "Unauthorized"}).data, status=403, mimetype="application/json")

        logging.info("Service-check completed successfully.")
        return Response(jsonify({"status": "Active"}).data, status=200, mimetype="application/json")
    except Exception as e:
        logging.error(f"An error occurred: {e}")
        return Response(jsonify({"error": "Internal server error"}).data, status=500, mimetype="application/json")

# Flask route for testing locally
@app.route("/service-check", methods=["GET"])
def service_check_endpoint():
    logging.info("Handling GET request for /service-check")
    return service_check(request)

if __name__ == "__main__":
    app.run(debug=True)
