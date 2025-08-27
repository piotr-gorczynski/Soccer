import firebase_admin
from firebase_admin import credentials, auth
from flask import Request, jsonify
import logging

# Initialize Firebase Admin SDK (for Google Cloud Functions)
try:
    cred = credentials.ApplicationDefault()  # Uses GCP service account automatically
    firebase_admin.initialize_app(cred)
    logging.info("Firebase Admin SDK initialized successfully")
except Exception as e:
    logging.error(f"Failed to initialize Firebase Admin SDK: {str(e)}")
    # We'll handle this error in the function calls

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
        # Check if Firebase Admin SDK is properly initialized
        if not firebase_admin._apps:
            return jsonify({
                "error": "Firebase Admin SDK not initialized", 
                "details": "Firebase Authentication may not be enabled for this project",
                "suggestion": "Run Firebase Authentication initialization via CI/CD pipeline"
            }), 500
            
        user = auth.create_user(email=email, password=password)
        logging.info(f"User created successfully: {user.uid}")
        return jsonify({"message": "User created successfully", "uid": user.uid}), 201
    except Exception as e:
        error_message = str(e)
        error_type = type(e).__name__
        
        # Provide specific guidance for common configuration issues
        if "default credentials" in error_message.lower() or "DefaultCredentialsError" in error_type:
            return jsonify({
                "error": "Authentication credentials not found",
                "details": "Firebase Admin SDK cannot find valid service account credentials",
                "suggestion": "Ensure the Cloud Function has proper service account permissions"
            }), 500
        elif "configuration not found" in error_message.lower() or "CONFIGURATION_NOT_FOUND" in error_message:
            return jsonify({
                "error": "Firebase Authentication not configured",
                "details": "Firebase Authentication must be initialized for this project",
                "suggestion": "Run 'gcloud builds submit --config gcp/cloud-build/deploy_firebase_with_auth.yaml' to initialize Firebase Auth"
            }), 500
        elif "already exists" in error_message.lower() or "EMAIL_EXISTS" in error_message:
            return jsonify({"error": "User with this email already exists"}), 409
        elif "invalid email" in error_message.lower() or "INVALID_EMAIL" in error_message:
            return jsonify({"error": "Invalid email format"}), 400
        elif "weak password" in error_message.lower() or "WEAK_PASSWORD" in error_message:
            return jsonify({"error": f"Password is too weak: {error_message}"}), 400
        else:
            logging.error(f"Unexpected error creating user: {error_type}: {error_message}")
            return jsonify({
                "error": f"Failed to create user: {error_message}",
                "error_type": error_type
            }), 500

def verify_token(request: Request):
    """Cloud Function to verify Firebase Auth ID token."""
    
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return jsonify({"error": "Unauthorized - Missing or invalid Authorization header"}), 401

    id_token = auth_header.split('Bearer ')[1]
    
    try:
        # Check if Firebase Admin SDK is properly initialized
        if not firebase_admin._apps:
            return jsonify({
                "error": "Firebase Admin SDK not initialized", 
                "details": "Firebase Authentication may not be enabled for this project"
            }), 500
            
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        logging.info(f"Token verified successfully for user: {uid}")
        return jsonify({"message": "Token verified successfully", "uid": uid}), 200
    except Exception as e:
        error_message = str(e)
        error_type = type(e).__name__
        
        if "default credentials" in error_message.lower() or "DefaultCredentialsError" in error_type:
            return jsonify({
                "error": "Authentication credentials not found",
                "details": "Firebase Admin SDK cannot find valid service account credentials"
            }), 500
        elif "invalid token" in error_message.lower() or "invalid" in error_message.lower():
            return jsonify({"error": "Invalid ID token"}), 401
        elif "expired" in error_message.lower():
            return jsonify({"error": "ID token has expired"}), 401
        elif "revoked" in error_message.lower():
            return jsonify({"error": "ID token has been revoked"}), 401
        else:
            logging.error(f"Unexpected error verifying token: {error_type}: {error_message}")
            return jsonify({
                "error": f"Error verifying ID token: {error_message}",
                "error_type": error_type
            }), 500
