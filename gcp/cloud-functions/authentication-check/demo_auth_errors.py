#!/usr/bin/env python3
"""
Example demonstrating the improved Firebase authentication error handling.
This shows how the enhanced error messages help diagnose common issues.
"""

from main import create_user, verify_token
from flask import Flask
import json

def test_authentication_scenarios():
    """Test various authentication scenarios to demonstrate improved error handling."""
    
    app = Flask(__name__)
    
    print("🧪 Testing Firebase Authentication Error Handling\n")
    
    # Test 1: Missing JSON body
    print("Test 1: Missing JSON body")
    with app.test_request_context(method='POST') as ctx:
        response = create_user(ctx.request)
        print(f"Status: {response[1]}")
        print(f"Response: {json.dumps(response[0].get_json(), indent=2)}\n")
    
    # Test 2: Missing email or password
    print("Test 2: Missing password")
    with app.test_request_context(
        json={"email": "test@test.com"},
        content_type='application/json',
        method='POST'
    ) as ctx:
        response = create_user(ctx.request)
        print(f"Status: {response[1]}")
        print(f"Response: {json.dumps(response[0].get_json(), indent=2)}\n")
    
    # Test 3: Actual Firebase credentials error (this will occur in real environment)
    print("Test 3: Real Firebase credentials error")
    with app.test_request_context(
        json={"email": "test@example.com", "password": "TestPassword123"},
        content_type='application/json',
        method='POST'
    ) as ctx:
        response = create_user(ctx.request)
        print(f"Status: {response[1]}")
        print(f"Response: {json.dumps(response[0].get_json(), indent=2)}\n")
    
    # Test 4: Token verification without proper auth header
    print("Test 4: Token verification without auth header")
    with app.test_request_context(method='POST') as ctx:
        response = verify_token(ctx.request)
        print(f"Status: {response[1]}")
        print(f"Response: {json.dumps(response[0].get_json(), indent=2)}\n")
    
    # Test 5: Token verification with invalid token
    print("Test 5: Token verification with invalid token")
    with app.test_request_context(
        headers={'Authorization': 'Bearer invalid-token'},
        method='POST'
    ) as ctx:
        response = verify_token(ctx.request)
        print(f"Status: {response[1]}")
        print(f"Response: {json.dumps(response[0].get_json(), indent=2)}\n")

if __name__ == '__main__':
    test_authentication_scenarios()