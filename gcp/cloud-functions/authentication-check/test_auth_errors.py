#!/usr/bin/env python3
"""
Test script to verify improved error handling in Firebase authentication.
This test demonstrates the enhanced error messages that help diagnose configuration issues.
"""

import unittest
from unittest.mock import patch, MagicMock
import json
from main import create_user, verify_token
from flask import Flask


class TestAuthenticationErrors(unittest.TestCase):
    
    def setUp(self):
        """Set up test fixtures."""
        self.app = Flask(__name__)
        
    def test_create_user_missing_credentials(self):
        """Test that we get a helpful error when credentials are missing."""
        # Create a mock request
        with self.app.test_request_context(
            json={"email": "test@test.com", "password": "TestPassword123"},
            content_type='application/json',
            method='POST'
        ) as ctx:
            # Mock the auth.create_user to raise DefaultCredentialsError
            with patch('main.auth.create_user') as mock_create:
                mock_create.side_effect = Exception("Your default credentials were not found")
                
                response = create_user(ctx.request)
                
                self.assertEqual(response[1], 500)  # HTTP 500
                response_data = response[0].get_json()
                self.assertIn("Authentication credentials not found", response_data["error"])
                self.assertIn("service account permissions", response_data["suggestion"])
    
    def test_create_user_configuration_not_found(self):
        """Test that we get a helpful error when Firebase Auth is not configured."""
        with self.app.test_request_context(
            json={"email": "test@test.com", "password": "TestPassword123"},
            content_type='application/json',
            method='POST'
        ) as ctx:
            # Mock the auth.create_user to raise CONFIGURATION_NOT_FOUND error
            with patch('main.auth.create_user') as mock_create:
                mock_create.side_effect = Exception("CONFIGURATION_NOT_FOUND: Firebase Authentication is not configured")
                
                response = create_user(ctx.request)
                
                self.assertEqual(response[1], 500)  # HTTP 500
                response_data = response[0].get_json()
                self.assertIn("Firebase Authentication not configured", response_data["error"])
                self.assertIn("deploy_firebase_with_auth.yaml", response_data["suggestion"])
    
    def test_create_user_generic_firebase_error(self):
        """Test generic Firebase error handling."""
        with self.app.test_request_context(
            json={"email": "test@test.com", "password": "TestPassword123"},
            content_type='application/json',
            method='POST'
        ) as ctx:
            # Mock the auth.create_user to raise a generic Firebase error
            with patch('main.auth.create_user') as mock_create:
                mock_create.side_effect = Exception("Some Firebase error")
                
                response = create_user(ctx.request)
                
                self.assertEqual(response[1], 500)  # HTTP 500
                response_data = response[0].get_json()
                self.assertIn("Failed to create user", response_data["error"])
                self.assertIn("error_type", response_data)
    
    def test_missing_email_or_password(self):
        """Test missing email or password."""
        with self.app.test_request_context(
            json={"email": "test@test.com"},  # Missing password
            content_type='application/json',
            method='POST'
        ) as ctx:
            response = create_user(ctx.request)
            
            self.assertEqual(response[1], 400)  # HTTP 400
            response_data = response[0].get_json()
            self.assertIn("Missing email or password", response_data["error"])
    
    def test_missing_json_body(self):
        """Test missing JSON body."""
        with self.app.test_request_context(method='POST') as ctx:
            response = create_user(ctx.request)
            
            self.assertEqual(response[1], 400)  # HTTP 400
            response_data = response[0].get_json()
            self.assertIn("Missing JSON body", response_data["error"])
    
    def test_successful_user_creation(self):
        """Test successful user creation with proper mocking."""
        with self.app.test_request_context(
            json={"email": "new@test.com", "password": "TestPassword123"},
            content_type='application/json',
            method='POST'
        ) as ctx:
            # Mock successful user creation
            mock_user = MagicMock()
            mock_user.uid = "test-uid-123"
            
            with patch('main.auth.create_user') as mock_create:
                with patch('main.firebase_admin._apps', {'default': True}):  # Mock Firebase initialization
                    mock_create.return_value = mock_user
                    
                    response = create_user(ctx.request)
                    
                    self.assertEqual(response[1], 201)  # HTTP 201 Created
                    response_data = response[0].get_json()
                    self.assertIn("User created successfully", response_data["message"])
                    self.assertEqual("test-uid-123", response_data["uid"])


if __name__ == '__main__':
    print("Testing Firebase Authentication error handling improvements...")
    unittest.main(verbosity=2)