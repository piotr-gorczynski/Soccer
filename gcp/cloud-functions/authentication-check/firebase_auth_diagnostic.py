#!/usr/bin/env python3
"""
Firebase Authentication Diagnostic Tool
This script helps diagnose common Firebase Authentication configuration issues.
"""

import firebase_admin
from firebase_admin import credentials, auth
import sys
import os

def check_firebase_auth():
    """Check Firebase Authentication configuration and provide diagnostics."""
    
    print("🔍 Firebase Authentication Diagnostic Tool")
    print("=" * 50)
    
    # Step 1: Check if Firebase Admin SDK can be initialized
    print("\n1. Checking Firebase Admin SDK initialization...")
    try:
        cred = credentials.ApplicationDefault()
        app = firebase_admin.initialize_app(cred)
        print("✅ Firebase Admin SDK initialized successfully")
        
        # Get project ID if available
        try:
            project_id = app.project_id
            print(f"📦 Project ID: {project_id}")
        except:
            print("⚠️  Project ID not available")
            
    except Exception as e:
        print(f"❌ Failed to initialize Firebase Admin SDK: {str(e)}")
        print(f"   Error type: {type(e).__name__}")
        
        if "default credentials" in str(e).lower():
            print("\n🔧 SOLUTION:")
            print("   This error indicates missing authentication credentials.")
            print("   In a Cloud Function environment, ensure:")
            print("   - The Cloud Function has a service account attached")
            print("   - The service account has Firebase Authentication Admin role")
            print("   - Application Default Credentials are properly configured")
            print("\n   For local testing:")
            print("   - Set GOOGLE_APPLICATION_CREDENTIALS environment variable")
            print("   - Or run: gcloud auth application-default login")
        
        return False
    
    # Step 2: Try to perform a test operation
    print("\n2. Testing Firebase Authentication operations...")
    try:
        # Try to get the user count (this requires Auth to be enabled)
        # This is a safe operation that doesn't create any data
        from firebase_admin import auth
        
        # Test if we can access auth service
        auth.get_user_by_email("nonexistent@test.com")
        
    except auth.UserNotFoundError:
        print("✅ Firebase Authentication is properly configured")
        print("   (UserNotFoundError for test email is expected)")
        return True
        
    except Exception as e:
        error_msg = str(e)
        print(f"❌ Firebase Authentication test failed: {error_msg}")
        
        if "CONFIGURATION_NOT_FOUND" in error_msg:
            print("\n🔧 SOLUTION:")
            print("   Firebase Authentication is not initialized for this project.")
            print("   Run the following to initialize Firebase Auth:")
            print("   gcloud builds submit --config gcp/cloud-build/deploy_firebase_with_auth.yaml")
            print("\n   Or manually:")
            print("   1. Go to Firebase Console > Authentication")
            print("   2. Click 'Get Started'")
            print("   3. Enable Email/Password authentication")
            
        elif "permission" in error_msg.lower() or "forbidden" in error_msg.lower():
            print("\n🔧 SOLUTION:")
            print("   Service account lacks sufficient permissions.")
            print("   Ensure the service account has these roles:")
            print("   - Firebase Authentication Admin")
            print("   - Identity Platform Admin")
            
        return False
    
    return True

def main():
    """Main diagnostic function."""
    
    # Check environment
    print(f"🖥️  Python version: {sys.version}")
    print(f"📁 Working directory: {os.getcwd()}")
    print(f"🔑 GOOGLE_APPLICATION_CREDENTIALS: {os.environ.get('GOOGLE_APPLICATION_CREDENTIALS', 'Not set')}")
    
    # Run diagnostics
    success = check_firebase_auth()
    
    print("\n" + "=" * 50)
    if success:
        print("✅ Firebase Authentication appears to be working correctly!")
    else:
        print("❌ Firebase Authentication has configuration issues.")
        print("   Please follow the solutions provided above.")
    
    print("\n📖 For more information, see:")
    print("   docs/ci-cd/README.md - Firebase Auth configuration guide")
    print("   gcp/cloud-build/deploy_firebase_with_auth.yaml - Auto-setup script")

if __name__ == '__main__':
    main()