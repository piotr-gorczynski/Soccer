import firebase_admin
from firebase_admin import credentials
from firebase_admin import auth
from flask import Request

# Initialize the Firebase Admin SDK
cred = credentials.ApplicationDefault()
firebase_admin.initialize_app(cred)

def verify_token(request: Request):
    # Extract the ID token from the Authorization header
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return 'Unauthorized', 401

    id_token = auth_header.split('Bearer ')[1]
    try:
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        return f'User ID: {uid}', 200
    except Exception as e:
        return f'Error verifying ID token: {e}', 401
