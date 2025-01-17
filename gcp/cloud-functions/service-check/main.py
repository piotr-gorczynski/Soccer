from flask import Flask, request, jsonify
from google.cloud import secretmanager

app = Flask(__name__)

def get_secret():
    client = secretmanager.SecretManagerServiceClient()
    secret_path = "projects/690882718361/secrets/soccer_secret_key/versions/latest"
    response = client.access_secret_version(request={"name": secret_path})
    return response.payload.data.decode("UTF-8")

@app.route('/service-check', methods=['GET'])
def service_check():
    secret_key = get_secret()
    key = request.headers.get('X-Secret-Key')

    if key != secret_key:
        return jsonify({"error": "Unauthorized"}), 403

    return jsonify({"status": "Active"}), 200
