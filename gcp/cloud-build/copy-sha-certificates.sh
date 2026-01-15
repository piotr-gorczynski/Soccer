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

# Find the global app ID
global_app_id=""
echo "🔍 Searching for global app ID..."

# Fetch a page of Android apps from Firebase
fetch_apps_page() {
  local page_token="$1"
  local url="https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps"

  if [ -n "$page_token" ]; then
    url="${url}?pageToken=${page_token}"
  fi

  curl -s -H "Authorization: Bearer $access_token" "$url"
}

# Helper to extract app ID for a package name from a response payload
get_app_id_for_package() {
  local package_name="$1"
  local response="$2"

  if command -v jq >/dev/null 2>&1; then
    echo "$response" | jq -r ".apps[] | select(.packageName == \"$package_name\") | .appId" 2>/dev/null || true
    return
  fi

  if command -v python3 >/dev/null 2>&1; then
    python3 - "$package_name" <<'PY' <<<"$response"
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
  # Normalize JSON by removing whitespace so we can match packageName reliably.
  compact_response=$(echo "$response" | tr -d '[:space:]')

  # Normalize JSON into one object per line to handle compact responses
  app_block=$(echo "$compact_response" | tr '{' '\n' | tr '}' '\n' | grep -F "\"packageName\":\"$package_name\"" | head -1 || true)

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

# Helper to extract nextPageToken from a response payload
get_next_page_token() {
  local response="$1"

  if command -v jq >/dev/null 2>&1; then
    echo "$response" | jq -r '.nextPageToken // empty' 2>/dev/null || true
    return
  fi

  if command -v python3 >/dev/null 2>&1; then
    python3 - <<'PY' <<<"$response"
import json
import sys

data = json.load(sys.stdin)
token = data.get("nextPageToken") or ""
if token:
    print(token)
PY
    return
  fi

  echo "$response" | grep -oE '"nextPageToken"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed -E 's/.*"nextPageToken"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/'
}

# Find app ID across paginated Firebase app list
find_app_id_across_pages() {
  local package_name="$1"
  local page_token=""
  local page=1
  local app_id=""
  local next_page_token=""

  while :; do
    if [ -n "$page_token" ]; then
      echo "🔄 Fetching Firebase app list page $page..." >&2
    else
      echo "🔍 Fetching list of Android apps..." >&2

    fi

    apps_response=$(fetch_apps_page "$page_token")
    echo "$apps_response" > /tmp/apps_response_page_${page}.json

    app_id=$(get_app_id_for_package "$package_name" "$apps_response")
    if [ -n "$app_id" ]; then
      echo "$app_id"
      return
    fi

    next_page_token=$(get_next_page_token "$apps_response")
    if [ -z "$next_page_token" ]; then
      break
    fi

    page_token="$next_page_token"
    page=$((page + 1))
  done
}

# Parse JSON to find app ID for global package
# The response format is: "apps": [{"name": "projects/.../androidApps/APP_ID", "packageName": "..."}]
global_app_id=$(find_app_id_across_pages "$global_package_name")

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
echo "🔍 DEBUG: First 500 chars of SHA response:"
echo "$sha_response" | head -c 500
echo ""
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
      echo "❌ ERROR: Failed to extract source SHA hashes for verification (jq exit code: $jq_exit_code)"
      echo "🔍 This is critical - verification is mandatory"
      echo "🔍 Check /tmp/sha_response.json for the raw API response"
      exit 1
    else
      extracted_count=$(echo "$source_sha_hashes" | grep -c . || echo "0")
      echo "🔍 Extracted $extracted_count source SHA hash(es) for verification"
      if [ "$extracted_count" -eq "0" ]; then
        echo "❌ ERROR: Extracted 0 source SHA hashes but sha_count=$sha_count"
        echo "🔍 This indicates a mismatch in how SHAs are counted vs extracted"
        echo "🔍 Check /tmp/sha_response.json for the raw API response"
        exit 1
      fi
      echo "🔍 DEBUG: Source SHA hashes for verification:"
      echo "$source_sha_hashes" | while IFS= read -r hash; do
        echo "  - $hash"
      done
    fi
  else
    source_sha_hashes=$(echo "$sha_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
    extracted_count=$(echo "$source_sha_hashes" | grep -c . || echo "0")
    echo "🔍 Extracted $extracted_count source SHA hash(es) for verification"
    if [ "$extracted_count" -eq "0" ]; then
      echo "❌ ERROR: Extracted 0 source SHA hashes but sha_count=$sha_count"
      echo "🔍 This indicates a problem with grep-based extraction"
      echo "🔍 Check /tmp/sha_response.json for the raw API response"
      exit 1
    fi
    echo "🔍 DEBUG: Source SHA hashes for verification:"
    echo "$source_sha_hashes" | while IFS= read -r hash; do
      echo "  - $hash"
    done
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
      target_app_id=$(find_app_id_across_pages "$package_name")
      
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
        
        # Refetch the app list (including pagination)
        echo "🔄 Refetching Firebase app list..."
        
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
      echo "🔍 DEBUG: Raw sha_hashes variable content (showing first 500 chars):"
      echo "$sha_hashes" | head -c 500
      echo ""
      
      echo "🔍 DEBUG: Extracting certificate types from JSON..."
      cert_types=$(echo "$sha_response" | jq -r '.certificates[]? | .certType // "SHA_1"')
      jq_exit_code=$?
      if [ $jq_exit_code -ne 0 ]; then
        echo "❌ ERROR: jq failed to parse certificate types (exit code: $jq_exit_code)"
        echo "🔍 Check /tmp/sha_response.json for the raw API response"
        exit 1
      fi
      echo "🔍 DEBUG: Certificate types extracted successfully"
      echo "🔍 DEBUG: Raw cert_types variable content (showing first 500 chars):"
      echo "$cert_types" | head -c 500
      echo ""
      
      # Check if SHA hashes extraction resulted in empty string (critical requirement)
      if [ -z "$sha_hashes" ]; then
        # If SHA hashes are empty, check if cert types are also empty to provide better error message
        if [ -z "$cert_types" ]; then
          echo "❌ ERROR: Both SHA hashes and certificate types extraction resulted in empty strings"
          echo "🔍 This means jq succeeded but found no certificates"
          echo "🔍 Check /tmp/sha_response.json - the 'certificates' array may be empty or malformed"
        else
          echo "❌ ERROR: SHA hashes extraction resulted in empty string (but cert types were found)"
          echo "🔍 This means jq succeeded but found no SHA hashes in certificates"
          echo "🔍 Check /tmp/sha_response.json - the certificates may be missing shaHash field"
        fi
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
      echo "🔍 DEBUG: SHA array contents:"
      for i in "${!sha_array[@]}"; do
        echo "  [$i]: '${sha_array[$i]}' (length: ${#sha_array[$i]})"
      done
      echo "🔍 DEBUG: Cert type array contents:"
      for i in "${!type_array[@]}"; do
        echo "  [$i]: '${type_array[$i]}'"
      done
      
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
        echo "🔍 DEBUG: Request URL: https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha"
        echo "🔍 DEBUG: Request payload: {\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}"
        
        # Add SHA certificate to target app
        add_response=$(curl -s -w '\n%{http_code}' -X POST \
          -H "Authorization: Bearer $access_token" \
          -H "Content-Type: application/json" \
          "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha" \
          -d "{\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}")
        
        http_code=$(echo "$add_response" | tail -n1)
        response_body=$(echo "$add_response" | head -n-1)
        
        echo "🔍 DEBUG: HTTP Response Code: $http_code"
        echo "🔍 DEBUG: Response Body (first 500 chars): $(echo "$response_body" | head -c 500)"
        
        if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
          echo "✅ Successfully added SHA certificate"
          certs_added=$((certs_added + 1))
        else
          echo "❌ FAILED to add SHA certificate (HTTP $http_code)"
          echo "❌ Full Response Body: $response_body"
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
        echo "🔍 DEBUG: Raw sha_hashes variable content (showing first 500 chars):"
        echo "$sha_hashes" | head -c 500
        echo ""
        
        # Check if SHA hashes extraction succeeded
        if [ -z "$sha_hashes" ]; then
          echo "❌ ERROR: Grep-based SHA hash extraction resulted in empty string"
          echo "🔍 Check /tmp/sha_response.json - the JSON may not match expected format"
          exit 1
        fi
        
        echo "🔍 DEBUG: Extracting certificate types using grep..."
        # Note: cert_types can be empty if certType field is missing (we'll default to SHA_1)
        cert_types=$(echo "$sha_response" | grep -oE '"certType"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
        echo "🔍 DEBUG: Certificate types extracted"
        echo "🔍 DEBUG: Raw cert_types variable content (showing first 500 chars):"
        echo "$cert_types" | head -c 500
        echo ""
        
        # Convert to arrays
        # Note: read -rd '' returns non-zero when reaching EOF, which is expected, hence || true
        IFS=$'\n' read -rd '' -a sha_array <<< "$sha_hashes" || true
        IFS=$'\n' read -rd '' -a type_array <<< "$cert_types" || true
        
        echo "🔍 Extracted ${#sha_array[@]} SHA hash(es)"
        echo "🔍 DEBUG: SHA array contents:"
        for i in "${!sha_array[@]}"; do
          echo "  [$i]: '${sha_array[$i]}' (length: ${#sha_array[$i]})"
        done
        echo "🔍 DEBUG: Cert type array contents:"
        for i in "${!type_array[@]}"; do
          echo "  [$i]: '${type_array[$i]}'"
        done
        
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
          echo "🔍 DEBUG: Request URL: https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha"
          echo "🔍 DEBUG: Request payload: {\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}"
          
          # Add SHA certificate
          add_response=$(curl -s -w '\n%{http_code}' -X POST \
            -H "Authorization: Bearer $access_token" \
            -H "Content-Type: application/json" \
            "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha" \
            -d "{\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}")
          
          http_code=$(echo "$add_response" | tail -n1)
          response_body=$(echo "$add_response" | head -n-1)
          
          echo "🔍 DEBUG: HTTP Response Code: $http_code"
          echo "🔍 DEBUG: Response Body (first 500 chars): $(echo "$response_body" | head -c 500)"
          
          if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
            echo "✅ Successfully added SHA certificate"
            certs_added=$((certs_added + 1))
          else
            echo "❌ FAILED to add SHA certificate (HTTP $http_code)"
            echo "❌ Full Response Body: $response_body"
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
      echo "🔍 DEBUG: Verification response (first 500 chars):"
      echo "$verification_response" | head -c 500
      echo ""
      
      # Extract SHA hashes from verification response
      verified_hashes=""
      if command -v jq >/dev/null 2>&1; then
        verified_hashes=$(echo "$verification_response" | jq -r '.certificates[]?.shaHash // empty')
        jq_exit_code=$?
        if [ $jq_exit_code -ne 0 ]; then
          echo "❌ VERIFICATION FAILED: Failed to parse verification response with jq (exit code: $jq_exit_code)"
          echo "🔍 Cannot verify if certificates were actually added"
          exit 1
        fi
      else
        verified_hashes=$(echo "$verification_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"' || true)
      fi
      
      if [ -n "$verified_hashes" ]; then
        verified_count=$(echo "$verified_hashes" | grep -c . || echo "0")
        echo "✅ VERIFICATION: Found $verified_count SHA certificate(s) in target app after copy"
        echo "🔍 DEBUG: Verified hashes in target app:"
        echo "$verified_hashes" | while IFS= read -r hash; do
          echo "  - $hash"
        done
        
        # Compare with source SHA certificates - all should be present
        echo "🔍 VERIFICATION: Checking if all source SHAs are present in target..."
        verification_failed=0
        missing_shas=""
        
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
            missing_shas="${missing_shas}${source_hash}\n"
          fi
        done <<< "$source_sha_hashes"
        
        if [ "$verification_failed" -eq "1" ]; then
          echo "❌ VERIFICATION FAILED: Not all source SHA certificates are present in target app!"
          echo "🔍 Expected all source SHAs to be in target, but some are missing."
          echo "🔍 Missing SHAs:"
          echo -e "$missing_shas"
          echo "🔍 This indicates the copying process failed silently despite HTTP success codes."
          echo "🔍 Summary of what should have been copied but is missing:"
          echo "  - Expected to copy: $(echo "$source_sha_hashes" | grep -c . || echo "0") SHA(s)"
          echo "  - Actually present: $verified_count SHA(s)"
          echo "  - Missing: $(echo -e "$missing_shas" | grep -c . || echo "0") SHA(s)"
          exit 1
        else
          echo "✅ VERIFICATION PASSED: All source SHA certificates are present in target app!"
        fi
      else
        echo "⚠️  VERIFICATION: No SHA certificates found in target app after copy"
        # Only fail if we expected to have added certificates
        if [ "${certs_added:-0}" -gt "0" ]; then
          echo "❌ VERIFICATION FAILED: Expected to find certificates after adding $certs_added, but found none!"
          echo "🔍 This indicates a serious problem - API reported success but certificates are not in Firebase"
          exit 1
        elif [ "${certs_skipped_existing:-0}" -eq "0" ]; then
          echo "❌ VERIFICATION FAILED: No certificates found in target, and none were marked as already existing!"
          echo "🔍 This means source has certificates but target has none - copying completely failed!"
          exit 1
        fi
      fi
    else
      echo "---"
      echo "❌ VERIFICATION FAILED: Source SHA hashes not available - cannot verify!"
      echo "🔍 This should not happen - source_sha_hashes should have been extracted earlier"
      exit 1
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
