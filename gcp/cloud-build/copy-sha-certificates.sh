#!/bin/bash
# Script to copy SHA certificates from global Firebase app to variant apps
# This script is called from deploy_firebase.yaml Step 8
set -e

firebase_project_id=$(cat /workspace/SOCCER_PROJECT_ID.txt)
global_package_name="piotr_gorczynski.soccer2"
package_names=("piotr_gorczynski.soccer2" "piotr_gorczynski.soccer2.bd")

echo "🔑 Starting SHA certificate copy process..."
echo "📱 Global package: $global_package_name"

# Generate access token for Firebase Management API
access_token=$(gcloud auth print-access-token)

# Get list of all Android apps with their app IDs
echo "🔍 Fetching list of Android apps..."
apps_response=$(curl -s -H "Authorization: Bearer $access_token" \
  "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps")

# Extract app IDs and package names using grep (jq alternative)
echo "$apps_response" > /tmp/apps_response.json

# Find the global app ID
global_app_id=""
echo "🔍 Searching for global app ID..."

# Helper to get app ID for a package name
get_app_id_for_package() {
  local package_name="$1"

  if command -v jq >/dev/null 2>&1; then
    echo "$apps_response" | jq -r ".apps[] | select(.packageName == \"$package_name\") | .appId" 2>/dev/null || true
    return
  fi

  if command -v python3 >/dev/null 2>&1; then
    python3 - "$package_name" <<'PY' <<<"$apps_response"
import json
import sys

package = sys.argv[1]
data = json.load(sys.stdin)
apps = data.get("apps") or data.get("result") or []
for app in apps:
    if app.get("packageName") == package:
        app_id = app.get("appId") or ""
        if not app_id and app.get("name"):
            app_id = app["name"].split("/")[-1]
        if app_id:
            print(app_id)
            break
PY
    return
  fi

  # Fallback without jq/python3 - extract app ID for the package
  # Normalize JSON into one object per line to handle compact responses
  app_block=$(echo "$apps_response" | tr '{' '\n' | tr '}' '\n' | grep -F "\"packageName\":\"$package_name\"" | head -1 || true)

  if [ -z "$app_block" ]; then
    return
  fi

  app_id=$(echo "$app_block" | grep -oE '"appId"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed -E 's/.*"appId"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/')
  if [ -n "$app_id" ]; then
    echo "$app_id"
    return
  fi

  name_id=$(echo "$app_block" | grep -oE '"name"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed -E 's/.*"name"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/' | awk -F'/' '{print $NF}')
  if [ -n "$name_id" ]; then
    echo "$name_id"
  fi
}

# Parse JSON to find app ID for global package
# The response format is: "apps": [{"name": "projects/.../androidApps/APP_ID", "packageName": "..."}]
global_app_id=$(get_app_id_for_package "$global_package_name")

if [ -z "$global_app_id" ]; then
  echo "⚠️  Could not find app ID for global package: $global_package_name"
  echo "⚠️  SHA certificate copy will be skipped."
  echo "ℹ️  Please add SHA certificates manually in Firebase Console."
  exit 0
fi

echo "✅ Found global app ID: $global_app_id"

# Get SHA certificates from global app
echo "🔍 Fetching SHA certificates from global app..."
sha_response=$(curl -s -H "Authorization: Bearer $access_token" \
  "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$global_app_id/sha")

echo "$sha_response" > /tmp/sha_response.json
echo "🔍 SHA response saved to /tmp/sha_response.json for debugging"
echo "🔍 SHA response contains $(echo "$sha_response" | wc -c) bytes"
echo "🔍 Checking for 'certificates' field in response..."

# Check if there are any SHA certificates
if echo "$sha_response" | grep -q '"certificates"'; then
  echo "✅ Found SHA certificates in global app"
  
  # Extract SHA certificates
  if command -v jq >/dev/null 2>&1; then
    sha_count=$(echo "$sha_response" | jq '.certificates | length' 2>/dev/null || echo "0")
  else
    sha_count=$(echo "$sha_response" | grep -c '"shaHash"' || echo "0")
  fi
  
  echo "📋 Number of SHA certificates found: $sha_count"
  
  if [ "$sha_count" -eq "0" ]; then
    echo "⚠️  No SHA certificates found in global app"
    echo "ℹ️  Please add SHA certificates to the global app first in Firebase Console."
    echo "🔍 Check /tmp/sha_response.json to see the actual API response"
    exit 0
  fi
  
  # Extract source SHA hashes once for later verification (avoiding redundant extraction)
  echo "🔍 Extracting source SHA hashes for verification..."
  source_sha_hashes=""
  if command -v jq >/dev/null 2>&1; then
    source_sha_hashes=$(echo "$sha_response" | jq -r '.certificates[]?.shaHash // empty')
    jq_exit_code=$?
    if [ $jq_exit_code -ne 0 ]; then
      echo "⚠️  WARNING: Failed to extract source SHA hashes for verification"
      echo "🔍 Verification step will be skipped"
    else
      echo "🔍 Extracted $(echo "$source_sha_hashes" | grep -c . || echo "0") source SHA hash(es) for verification"
    fi
  else
    source_sha_hashes=$(echo "$sha_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
    echo "🔍 Extracted $(echo "$source_sha_hashes" | grep -c . || echo "0") source SHA hash(es) for verification"
  fi
  
  # Iterate through other packages and copy SHA certificates
  for package_name in "${package_names[@]}"; do
    # Skip the global package
    if [ "$package_name" = "$global_package_name" ]; then
      echo "⏭️  Skipping global package: $package_name"
      continue
    fi
    
    echo "---"
    echo "🔍 Processing package: $package_name"
    
    # Find app ID for this package with retry logic
    # (to handle Firebase API propagation delays after app creation)
    target_app_id=""
    max_retries=3
    retry_count=0
    retry_delay=5  # Start with 5 seconds
    
    while [ $retry_count -le $max_retries ]; do
      target_app_id=$(get_app_id_for_package "$package_name")
      
      # If we found the app ID, break out of retry loop
      if [ -n "$target_app_id" ]; then
        if [ $retry_count -gt 0 ]; then
          if [ $retry_count -eq 1 ]; then
            echo "✅ App ID found after 1 retry"
          else
            echo "✅ App ID found after $retry_count retries"
          fi
        fi
        break
      fi
      
      # If not found and we haven't exceeded max retries, wait and refetch
      if [ $retry_count -lt $max_retries ]; then
        retry_count=$((retry_count + 1))
        echo "⏳ App ID not found, waiting ${retry_delay}s before retry $retry_count/$max_retries..."
        sleep $retry_delay
        
        # Refetch the app list
        echo "🔄 Refetching Firebase app list..."
        apps_response=$(curl -s -H "Authorization: Bearer $access_token" \
          "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps")
        
        # Increase delay for next retry (exponential backoff)
        retry_delay=$((retry_delay * 2))
      else
        # Max retries exceeded
        retry_count=$((retry_count + 1))
        break
      fi
    done
    
    # Check if we found the app ID after all retries
    if [ -z "$target_app_id" ]; then
      # retry_count represents number of retries (not including initial attempt)
      total_attempts=$((retry_count))
      echo "⚠️  Could not find app ID for package: $package_name (tried $total_attempts time(s))"
      echo "⚠️  Skipping SHA certificate copy for this package."
      echo "ℹ️  The app may not exist or there may be an API propagation delay."
      echo "ℹ️  You can manually copy SHA certificates in Firebase Console or re-run the deployment."
      continue
    fi
    
    echo "✅ Found target app ID: $target_app_id"
    
    # Get existing SHA certificates for target app
    echo "🔍 Checking existing SHA certificates in target app..."
    target_sha_response=$(curl -s -H "Authorization: Bearer $access_token" \
      "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha")
    
    echo "🔍 Target app SHA response saved for debugging"
    echo "$target_sha_response" > /tmp/target_sha_response_${package_name}.json
    echo "🔍 Target app SHA response contains $(echo "$target_sha_response" | wc -c) bytes"
    
    # Extract existing SHA hashes to avoid duplicates
    existing_hashes=""
    if command -v jq >/dev/null 2>&1; then
      existing_hashes=$(echo "$target_sha_response" | jq -r '.certificates[]?.shaHash // empty' 2>/dev/null || true)
    else
      existing_hashes=$(echo "$target_sha_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"' || true)
    fi
    
    if [ -n "$existing_hashes" ]; then
      existing_count=$(echo "$existing_hashes" | grep -c . || echo "0")
      echo "🔍 Found $existing_count existing SHA certificate(s) in target app"
    else
      echo "🔍 No existing SHA certificates found in target app"
    fi
    
    # Copy each SHA certificate from global app to target app
    if command -v jq >/dev/null 2>&1; then
      # Use jq for robust parsing
      echo "🔧 Using jq for JSON parsing"
      
      # Extract arrays of SHA hashes and cert types using jq
      # Use a single jq command to extract both values to ensure perfect alignment
      # CRITICAL: Do NOT use || true here - we need to catch jq errors!
      echo "🔍 DEBUG: Extracting SHA hashes from JSON..."
      sha_hashes=$(echo "$sha_response" | jq -r '.certificates[]? | .shaHash // empty')
      jq_exit_code=$?
      if [ $jq_exit_code -ne 0 ]; then
        echo "❌ ERROR: jq failed to parse SHA hashes (exit code: $jq_exit_code)"
        echo "🔍 Check /tmp/sha_response.json for the raw API response"
        exit 1
      fi
      echo "🔍 DEBUG: SHA hashes extracted successfully"
      
      echo "🔍 DEBUG: Extracting certificate types from JSON..."
      cert_types=$(echo "$sha_response" | jq -r '.certificates[]? | .certType // "SHA_1"')
      jq_exit_code=$?
      if [ $jq_exit_code -ne 0 ]; then
        echo "❌ ERROR: jq failed to parse certificate types (exit code: $jq_exit_code)"
        echo "🔍 Check /tmp/sha_response.json for the raw API response"
        exit 1
      fi
      echo "🔍 DEBUG: Certificate types extracted successfully"
      
      # Check if BOTH extractions resulted in empty strings
      if [ -z "$sha_hashes" ] && [ -z "$cert_types" ]; then
        echo "❌ ERROR: Both SHA hashes and certificate types extraction resulted in empty strings"
        echo "🔍 This means jq succeeded but found no certificates"
        echo "🔍 Check /tmp/sha_response.json - the 'certificates' array may be empty or malformed"
        exit 1
      fi
      
      # Check if extraction resulted in empty strings (after checking both are empty)
      if [ -z "$sha_hashes" ]; then
        echo "❌ ERROR: SHA hashes extraction resulted in empty string"
        echo "🔍 This means jq succeeded but found no SHA hashes in certificates"
        echo "🔍 Check /tmp/sha_response.json - the certificates may be missing shaHash field"
        exit 1
      fi
      
      # Convert to arrays
      # Try mapfile (bash 4.0+), fall back to read for older bash versions
      # Note: We try to use the command and catch errors rather than checking for availability
      # since mapfile/readarray are builtins and may not be detectable with command -v in all environments
      echo "🔍 DEBUG: Converting to bash arrays..."
      if mapfile -t sha_array <<< "$sha_hashes" 2>/dev/null && mapfile -t type_array <<< "$cert_types" 2>/dev/null; then
        # mapfile succeeded - arrays are already populated
        echo "🔍 DEBUG: Used mapfile for array conversion"
      else
        # Fall back to read for older bash versions
        # Note: read -rd '' is expected to return non-zero exit code, hence || true
        IFS=$'\n' read -rd '' -a sha_array <<< "$sha_hashes" || true
        IFS=$'\n' read -rd '' -a type_array <<< "$cert_types" || true
        echo "🔍 DEBUG: Used read for array conversion (mapfile not available)"
      fi
      
      echo "🔍 Extracted ${#sha_array[@]} SHA hash(es) from certificates"
      
      # Verify array lengths match (should be equal for proper pairing)
      if [ "${#sha_array[@]}" -ne "${#type_array[@]}" ]; then
        echo "⚠️  WARNING: SHA array length (${#sha_array[@]}) does not match cert type array length (${#type_array[@]})"
        echo "⚠️  Will use SHA_1 as default for missing cert types"
      fi
      
      # Iterate through certificates using array indices
      certs_processed=0
      certs_added=0
      certs_skipped_existing=0
      certs_failed=0
      
      for i in "${!sha_array[@]}"; do
        sha_hash="${sha_array[$i]}"
        cert_type="${type_array[$i]:-SHA_1}"
        
        # Skip empty SHA hashes (can happen with empty input)
        if [ -z "$sha_hash" ]; then
          echo "⚠️  Skipping empty SHA hash at index $i"
          continue
        fi
        
        certs_processed=$((certs_processed + 1))
        echo "🔍 Processing certificate $certs_processed: $sha_hash"
        
        # Check if SHA already exists in target app
        if echo "$existing_hashes" | grep -qF "$sha_hash"; then
          echo "⏭️  SHA certificate already exists: $sha_hash (type: $cert_type)"
          certs_skipped_existing=$((certs_skipped_existing + 1))
          continue
        fi
        
        echo "📥 Adding SHA certificate: $sha_hash (type: $cert_type)"
        echo "🔍 DEBUG: Making POST request to Firebase API..."
        
        # Add SHA certificate to target app
        add_response=$(curl -s -w '\n%{http_code}' -X POST \
          -H "Authorization: Bearer $access_token" \
          -H "Content-Type: application/json" \
          "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha" \
          -d "{\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}")
        
        http_code=$(echo "$add_response" | tail -n1)
        response_body=$(echo "$add_response" | head -n-1)
        
        echo "🔍 DEBUG: HTTP Response Code: $http_code"
        if [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
          # Only log response body on failure to avoid logging sensitive data unnecessarily
          echo "🔍 DEBUG: Error Response Body (sanitized): $(echo "$response_body" | head -c 200)..."
        fi
        
        if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
          echo "✅ Successfully added SHA certificate"
          certs_added=$((certs_added + 1))
        else
          echo "❌ FAILED to add SHA certificate (HTTP $http_code)"
          echo "Response (first 200 chars): $(echo "$response_body" | head -c 200)"
          certs_failed=$((certs_failed + 1))
        fi
      done
      
      echo "---"
      echo "📊 SUMMARY for package: $package_name"
      echo "  Total certificates processed: $certs_processed"
      echo "  Successfully added: $certs_added"
      echo "  Skipped (already exist): $certs_skipped_existing"
      echo "  Failed: $certs_failed"
      
      if [ "$certs_processed" -eq "0" ]; then
        echo "❌ CRITICAL ERROR: No certificates were processed!"
        echo "🔍 This indicates a problem with jq parsing or array initialization."
        echo "🔍 Raw SHA response saved to /tmp/sha_response.json for debugging"
        echo "🔍 Review the DEBUG output above to identify the issue"
        exit 1
      fi
      
      # Check if we had certificates to add but all failed
      expected_to_add=$((certs_processed - certs_skipped_existing))
      if [ "$expected_to_add" -gt "0" ] && [ "$certs_added" -eq "0" ]; then
        echo "❌ CRITICAL ERROR: Expected to add $expected_to_add certificate(s) but added 0!"
        echo "🔍 All certificate additions failed. Check the error messages above."
        exit 1
      fi
      
      if [ "$certs_failed" -gt "0" ]; then
        echo "⚠️  WARNING: $certs_failed certificate(s) failed to add"
        echo "⚠️  Some certificates were not copied successfully"
        # Don't exit here - we'll track this per-package and report at the end
      fi
      
      if [ "$certs_added" -gt "0" ]; then
        echo "✅ Successfully added $certs_added certificate(s)"
      elif [ "$certs_skipped_existing" -gt "0" ]; then
        echo "ℹ️  All certificates already exist in target app (nothing to add)"
      fi
    else
      # Fallback without jq - manual parsing
      echo "🔧 Using grep-based parsing (jq not available)"
      
      # Count certificates
      cert_count=$(echo "$sha_response" | grep -c '"shaHash"' || echo "0")
      echo "🔍 Found $cert_count certificate(s) to process"
      
      if [ "$cert_count" -gt "0" ]; then
        # Extract each SHA hash and cert type
        echo "🔍 DEBUG: Extracting SHA hashes using grep..."
        sha_hashes=$(echo "$sha_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
        echo "🔍 DEBUG: SHA hashes extracted"
        
        echo "🔍 DEBUG: Extracting certificate types using grep..."
        cert_types=$(echo "$sha_response" | grep -oE '"certType"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
        echo "🔍 DEBUG: Certificate types extracted"
        
        # Convert to arrays
        IFS=$'\n' read -rd '' -a sha_array <<< "$sha_hashes" || true
        IFS=$'\n' read -rd '' -a type_array <<< "$cert_types" || true
        
        echo "🔍 Extracted ${#sha_array[@]} SHA hash(es)"
        
        # Iterate through certificates
        certs_processed=0
        certs_added=0
        certs_skipped_existing=0
        certs_failed=0
        
        for i in "${!sha_array[@]}"; do
          sha_hash="${sha_array[$i]}"
          cert_type="${type_array[$i]:-SHA_1}"
          
          if [ -z "$sha_hash" ]; then
            echo "⚠️  Skipping empty SHA hash at index $i"
            continue
          fi
          
          certs_processed=$((certs_processed + 1))
          
          # Check if SHA already exists
          if echo "$existing_hashes" | grep -qF "$sha_hash"; then
            echo "⏭️  SHA certificate already exists: $sha_hash (type: $cert_type)"
            certs_skipped_existing=$((certs_skipped_existing + 1))
            continue
          fi
          
          echo "📥 Adding SHA certificate: $sha_hash (type: $cert_type)"
          echo "🔍 DEBUG: Making POST request to Firebase API..."
          
          # Add SHA certificate
          add_response=$(curl -s -w '\n%{http_code}' -X POST \
            -H "Authorization: Bearer $access_token" \
            -H "Content-Type: application/json" \
            "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha" \
            -d "{\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}")
          
          http_code=$(echo "$add_response" | tail -n1)
          response_body=$(echo "$add_response" | head -n-1)
          
          echo "🔍 DEBUG: HTTP Response Code: $http_code"
          if [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
            # Only log response body on failure to avoid logging sensitive data unnecessarily
            echo "🔍 DEBUG: Error Response Body (sanitized): $(echo "$response_body" | head -c 200)..."
          fi
          
          if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
            echo "✅ Successfully added SHA certificate"
            certs_added=$((certs_added + 1))
          else
            echo "❌ FAILED to add SHA certificate (HTTP $http_code)"
            echo "Response (first 200 chars): $(echo "$response_body" | head -c 200)"
            certs_failed=$((certs_failed + 1))
          fi
        done
        
        echo "---"
        echo "📊 SUMMARY for package: $package_name"
        echo "  Total certificates processed: $certs_processed"
        echo "  Successfully added: $certs_added"
        echo "  Skipped (already exist): $certs_skipped_existing"
        echo "  Failed: $certs_failed"
        
        if [ "$certs_processed" -eq "0" ]; then
          echo "❌ CRITICAL ERROR: No certificates were processed!"
          echo "🔍 This indicates a problem with grep parsing or array initialization."
          echo "🔍 Raw SHA response saved to /tmp/sha_response.json for debugging"
          exit 1
        fi
        
        # Check if we had certificates to add but all failed
        expected_to_add=$((certs_processed - certs_skipped_existing))
        if [ "$expected_to_add" -gt "0" ] && [ "$certs_added" -eq "0" ]; then
          echo "❌ CRITICAL ERROR: Expected to add $expected_to_add certificate(s) but added 0!"
          echo "🔍 All certificate additions failed. Check the error messages above."
          exit 1
        fi
        
        if [ "$certs_failed" -gt "0" ]; then
          echo "⚠️  WARNING: $certs_failed certificate(s) failed to add"
          echo "⚠️  Some certificates were not copied successfully"
        fi
        
        if [ "$certs_added" -gt "0" ]; then
          echo "✅ Successfully added $certs_added certificate(s)"
        elif [ "$certs_skipped_existing" -gt "0" ]; then
          echo "ℹ️  All certificates already exist in target app (nothing to add)"
        fi
      else
        echo "⚠️  No certificates found to process"
      fi
    fi
    
    # VERIFICATION STEP: Re-fetch SHA certificates from target app to confirm they were added
    # Only perform verification if we have source hashes to compare against
    if [ -n "$source_sha_hashes" ]; then
      echo "---"
      echo "🔍 VERIFICATION: Re-fetching SHA certificates from target app to confirm changes..."
      verification_response=$(curl -s -H "Authorization: Bearer $access_token" \
        "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha")
      
      echo "$verification_response" > /tmp/verification_response_${package_name}.json
      echo "🔍 Verification response saved to /tmp/verification_response_${package_name}.json"
      
      # Extract SHA hashes from verification response
      verified_hashes=""
      if command -v jq >/dev/null 2>&1; then
        verified_hashes=$(echo "$verification_response" | jq -r '.certificates[]?.shaHash // empty')
        jq_exit_code=$?
        if [ $jq_exit_code -ne 0 ]; then
          echo "⚠️  WARNING: Failed to parse verification response with jq (exit code: $jq_exit_code)"
          echo "🔍 Verification step will be skipped"
          verified_hashes=""
        fi
      else
        verified_hashes=$(echo "$verification_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"' || true)
      fi
      
      if [ -n "$verified_hashes" ]; then
        verified_count=$(echo "$verified_hashes" | grep -c . || echo "0")
        echo "✅ VERIFICATION: Found $verified_count SHA certificate(s) in target app after copy"
        
        # Compare with source SHA certificates - all should be present
        echo "🔍 VERIFICATION: Checking if all source SHAs are present in target..."
        verification_failed=0
        
        # Use the pre-extracted source_sha_hashes instead of re-extracting
        while IFS= read -r source_hash; do
          if [ -z "$source_hash" ]; then
            continue
          fi
          
          if echo "$verified_hashes" | grep -qF "$source_hash"; then
            echo "  ✅ $source_hash - PRESENT"
          else
            echo "  ❌ $source_hash - MISSING!"
            verification_failed=1
          fi
        done <<< "$source_sha_hashes"
        
        if [ "$verification_failed" -eq "1" ]; then
          echo "❌ VERIFICATION FAILED: Not all source SHA certificates are present in target app!"
          echo "🔍 Expected all source SHAs to be in target, but some are missing."
          echo "🔍 This indicates the copying process failed silently."
          exit 1
        else
          echo "✅ VERIFICATION PASSED: All source SHA certificates are present in target app!"
        fi
      else
        echo "⚠️  VERIFICATION: No SHA certificates found in target app after copy"
        # Only fail if we expected to have added certificates
        if [ "${certs_added:-0}" -gt "0" ]; then
          echo "❌ VERIFICATION FAILED: Expected to find certificates after adding $certs_added, but found none!"
          exit 1
        fi
      fi
    else
      echo "---"
      echo "⚠️  VERIFICATION: Skipping verification (source SHA hashes not available)"
    fi
    
    echo "✅ Completed SHA certificate copy for package: $package_name"
  done
  
  echo "---"
  echo "✅ SHA certificate copy process completed successfully!"
else
  echo "⚠️  No SHA certificates found in global app"
  echo "ℹ️  Please add SHA certificates to the global app first in Firebase Console."
  echo "ℹ️  Once added, re-run this deployment to copy them to other packages."
fi
