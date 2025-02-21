import firebase_admin
from firebase_admin import credentials, auth
from flask import Request, jsonify

# Initialize Firebase Admin SDK (for Google Cloud Functions)
cred = credentials.ApplicationDefault()  # Uses GCP service account automatically
firebase_admin.initialize_app(cred)

def create_user(request: Request):
    """Cloud Function to create a Firebase user with email and password."""
    
    request_json = request.get_json(silent=True)
    if not request_json:
        return jsonify({"error": "Missing JSON body"}), 400

    email = request_json.get("email")
    password = request_json.get("password")

    if not email or not password:
        return jsonify({"error": "Missing email or password"}), 400

    try:
        user = auth.create_user(email=email, password=password)
        return jsonify({"message": "User created successfully", "uid": user.uid}), 201
    except Exception as e:
        return jsonify({"error": f"Failed to create user: {str(e)}"}), 400

def verify_token(request: Request):
    """Cloud Function to verify Firebase Auth ID token."""
    
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return jsonify({"error": "Unauthorized"}), 401

    id_token = auth_header.split('Bearer ')[1]
    
    try:
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        return jsonify({"message": "Token verified successfully", "uid": uid}), 200
    except Exception as e:
        return jsonify({"error": f"Error verifying ID token: {str(e)}"}), 401
