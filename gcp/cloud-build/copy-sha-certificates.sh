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
  echo "$apps_response" | grep -A 5 "\"packageName\"[[:space:]]*:[[:space:]]*\"$package_name\"" | grep -oE '"appId"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | grep -oE '"[^"]*"$' | tr -d '"' || true
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
    
    # Extract existing SHA hashes to avoid duplicates
    existing_hashes=""
    if command -v jq >/dev/null 2>&1; then
      existing_hashes=$(echo "$target_sha_response" | jq -r '.certificates[]?.shaHash // empty' 2>/dev/null || true)
    else
      existing_hashes=$(echo "$target_sha_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"' || true)
    fi
    
    # Copy each SHA certificate from global app to target app
    if command -v jq >/dev/null 2>&1; then
      # Use jq for robust parsing
      echo "🔧 Using jq for JSON parsing"
      
      # Extract arrays of SHA hashes and cert types using jq
      # Use a single jq command to extract both values to ensure perfect alignment
      sha_hashes=$(echo "$sha_response" | jq -r '.certificates[]? | .shaHash // empty' || true)
      cert_types=$(echo "$sha_response" | jq -r '.certificates[]? | .certType // "SHA_1"' || true)
      
      # Convert to arrays
      # Try mapfile (bash 4.0+), fall back to read for older bash versions
      # Note: We try to use the command and catch errors rather than checking for availability
      # since mapfile/readarray are builtins and may not be detectable with command -v in all environments
      if mapfile -t sha_array <<< "$sha_hashes" 2>/dev/null && mapfile -t type_array <<< "$cert_types" 2>/dev/null; then
        # mapfile succeeded - arrays are already populated
        :
      else
        # Fall back to read for older bash versions
        # Note: read -rd '' is expected to return non-zero exit code, hence || true
        IFS=$'\n' read -rd '' -a sha_array <<< "$sha_hashes" || true
        IFS=$'\n' read -rd '' -a type_array <<< "$cert_types" || true
      fi
      
      echo "🔍 Extracted ${#sha_array[@]} SHA hash(es) from certificates"
      
      # Verify array lengths match (should be equal for proper pairing)
      if [ "${#sha_array[@]}" -ne "${#type_array[@]}" ]; then
        echo "⚠️  WARNING: SHA array length (${#sha_array[@]}) does not match cert type array length (${#type_array[@]})"
        echo "⚠️  Will use SHA_1 as default for missing cert types"
      fi
      
      # Iterate through certificates using array indices
      certs_processed=0
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
          continue
        fi
        
        echo "📥 Adding SHA certificate: $sha_hash (type: $cert_type)"
        
        # Add SHA certificate to target app
        add_response=$(curl -s -w '\n%{http_code}' -X POST \
          -H "Authorization: Bearer $access_token" \
          -H "Content-Type: application/json" \
          "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha" \
          -d "{\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}")
        
        http_code=$(echo "$add_response" | tail -n1)
        
        if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
          echo "✅ Successfully added SHA certificate"
        else
          echo "⚠️  Failed to add SHA certificate (HTTP $http_code)"
          echo "Response: $(echo "$add_response" | head -n-1)"
        fi
      done
      
      if [ "$certs_processed" -eq "0" ]; then
        echo "⚠️  WARNING: No certificates were processed!"
        echo "🔍 This might indicate a problem with jq parsing or an empty certificate list."
        echo "🔍 Raw SHA response saved to /tmp/sha_response.json for debugging"
      else
        echo "✅ Processed $certs_processed certificate(s)"
      fi
    else
      # Fallback without jq - manual parsing
      echo "🔧 Using grep-based parsing (jq not available)"
      
      # Count certificates
      cert_count=$(echo "$sha_response" | grep -c '"shaHash"' || echo "0")
      echo "🔍 Found $cert_count certificate(s) to process"
      
      if [ "$cert_count" -gt "0" ]; then
        # Extract each SHA hash and cert type
        sha_hashes=$(echo "$sha_response" | grep -oE '"shaHash"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
        cert_types=$(echo "$sha_response" | grep -oE '"certType"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -oE '"[^"]*"$' | tr -d '"')
        
        # Convert to arrays
        IFS=$'\n' read -rd '' -a sha_array <<< "$sha_hashes" || true
        IFS=$'\n' read -rd '' -a type_array <<< "$cert_types" || true
        
        echo "🔍 Extracted ${#sha_array[@]} SHA hash(es)"
        
        # Iterate through certificates
        certs_added=0
        for i in "${!sha_array[@]}"; do
          sha_hash="${sha_array[$i]}"
          cert_type="${type_array[$i]:-SHA_1}"
          
          if [ -z "$sha_hash" ]; then
            echo "⚠️  Skipping empty SHA hash at index $i"
            continue
          fi
          
          # Check if SHA already exists
          if echo "$existing_hashes" | grep -qF "$sha_hash"; then
            echo "⏭️  SHA certificate already exists: $sha_hash (type: $cert_type)"
            continue
          fi
          
          echo "📥 Adding SHA certificate: $sha_hash (type: $cert_type)"
          
          # Add SHA certificate
          add_response=$(curl -s -w '\n%{http_code}' -X POST \
            -H "Authorization: Bearer $access_token" \
            -H "Content-Type: application/json" \
            "https://firebase.googleapis.com/v1beta1/projects/$firebase_project_id/androidApps/$target_app_id/sha" \
            -d "{\"shaHash\": \"$sha_hash\", \"certType\": \"$cert_type\"}")
          
          http_code=$(echo "$add_response" | tail -n1)
          
          if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
            echo "✅ Successfully added SHA certificate"
            certs_added=$((certs_added + 1))
          else
            echo "⚠️  Failed to add SHA certificate (HTTP $http_code)"
            echo "Response: $(echo "$add_response" | head -n-1)"
          fi
        done
        
        echo "✅ Added $certs_added out of ${#sha_array[@]} certificate(s)"
      else
        echo "⚠️  No certificates found to process"
      fi
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
