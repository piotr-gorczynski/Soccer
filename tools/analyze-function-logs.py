#!/usr/bin/env python3
"""
Log analyzer for Firebase Cloud Functions, specifically for the checkNickname function.

This script analyzes logs from the checkNickname Cloud Function to:
1. Verify the function is working correctly
2. Detect if fallback modes are being triggered (indicating errors)
3. Check for permission or API errors
4. Analyze nickname moderation patterns
5. Provide a summary report

Usage:
    python3 tools/analyze-function-logs.py <log_file.json>

The log file should be in JSON format as exported from Cloud Logging or Firebase Console.
"""

import json
import sys
from datetime import datetime
from collections import defaultdict, Counter
from typing import Dict, List, Any


class LogAnalyzer:
    """Analyzes checkNickname function logs."""

    def __init__(self):
        self.total_requests = 0
        self.allowed_nicknames = 0
        self.blocked_nicknames = 0
        self.fallback_activations = 0
        self.errors = []
        self.fallback_reasons = Counter()
        self.blocked_reasons = []
        self.safety_rating_stats = defaultdict(lambda: defaultdict(int))
        self.timestamps = []
        self.all_logs = []

    def analyze_log_entry(self, entry: Dict[str, Any]):
        """Analyze a single log entry."""
        self.all_logs.append(entry)
        
        # Extract the log message
        text_payload = entry.get('textPayload', '')
        json_payload = entry.get('jsonPayload', {})
        timestamp = entry.get('timestamp', '')
        
        if timestamp:
            self.timestamps.append(timestamp)
        
        message = text_payload or json_payload.get('message', '')
        
        # Count total requests
        if 'checkNickname called with nickname:' in message:
            self.total_requests += 1
        
        # Detect allowed nicknames
        if 'checkNickname: Nickname ALLOWED:' in message:
            self.allowed_nicknames += 1
        
        # Detect blocked nicknames
        if 'checkNickname: Nickname BLOCKED' in message:
            self.blocked_nicknames += 1
            self.blocked_reasons.append({
                'timestamp': timestamp,
                'message': message
            })
        
        # Detect fallback activations (errors)
        if 'FALLBACK ACTIVATED' in message:
            self.fallback_activations += 1
            
            # Extract the reason for fallback
            if 'permission denied' in message.lower():
                self.fallback_reasons['Permission Denied'] += 1
            elif 'unavailable' in message.lower():
                self.fallback_reasons['Service Unavailable'] += 1
            elif 'authentication' in message.lower() or 'api key' in message.lower():
                self.fallback_reasons['Authentication Error'] += 1
            else:
                self.fallback_reasons['Unknown Error'] += 1
        
        # Detect errors
        if 'checkNickname: Vertex AI moderation FAILED' in message or \
           entry.get('severity', '') in ['ERROR', 'CRITICAL', 'ALERT', 'EMERGENCY']:
            self.errors.append({
                'timestamp': timestamp,
                'severity': entry.get('severity', 'UNKNOWN'),
                'message': message
            })
        
        # Extract safety ratings if present
        if 'Safety ratings for nickname:' in message:
            # Try to extract safety rating information
            # This is a simplified extraction - real implementation might need more robust parsing
            pass

    def generate_report(self) -> str:
        """Generate a comprehensive analysis report."""
        lines = []
        lines.append("=" * 80)
        lines.append("checkNickname Function Log Analysis Report")
        lines.append("=" * 80)
        lines.append("")
        
        # Time range
        if self.timestamps:
            lines.append(f"Time Range: {self.timestamps[0]} to {self.timestamps[-1]}")
            lines.append("")
        
        # Summary Statistics
        lines.append("SUMMARY STATISTICS")
        lines.append("-" * 80)
        lines.append(f"Total Requests:           {self.total_requests}")
        lines.append(f"Nicknames Allowed:        {self.allowed_nicknames}")
        lines.append(f"Nicknames Blocked:        {self.blocked_nicknames}")
        lines.append(f"Fallback Activations:     {self.fallback_activations}")
        lines.append(f"Errors Detected:          {len(self.errors)}")
        lines.append("")
        
        # Health Status
        lines.append("HEALTH STATUS")
        lines.append("-" * 80)
        
        if self.fallback_activations == 0 and len(self.errors) == 0:
            lines.append("✅ HEALTHY: Function is working correctly with no errors or fallbacks.")
        elif self.fallback_activations > 0:
            lines.append(f"⚠️  WARNING: Function triggered fallback mode {self.fallback_activations} times.")
            lines.append("   This means errors occurred and nicknames were allowed by default!")
            lines.append("   Inappropriate nicknames may have passed through.")
        else:
            lines.append(f"⚠️  WARNING: {len(self.errors)} errors detected.")
        lines.append("")
        
        # Fallback Analysis
        if self.fallback_activations > 0:
            lines.append("FALLBACK REASONS")
            lines.append("-" * 80)
            for reason, count in self.fallback_reasons.most_common():
                lines.append(f"  {reason}: {count} occurrences")
            lines.append("")
            
            lines.append("RECOMMENDED ACTIONS:")
            if self.fallback_reasons.get('Permission Denied', 0) > 0:
                lines.append("  1. Enable Vertex AI API:")
                lines.append("     gcloud services enable aiplatform.googleapis.com")
                lines.append("")
                lines.append("  2. Grant Vertex AI User role to the service account:")
                lines.append("     Run: gcp/cloud-build/grant_vertex_ai_user_to_appengine_sa.yaml")
            
            if self.fallback_reasons.get('Service Unavailable', 0) > 0:
                lines.append("  - Service Unavailable errors may be temporary. Check Vertex AI status.")
            
            if self.fallback_reasons.get('Authentication Error', 0) > 0:
                lines.append("  - Authentication errors indicate API key or credentials issues.")
                lines.append("    Check service account configuration.")
            lines.append("")
        
        # Blocked Nicknames Analysis
        if self.blocked_nicknames > 0:
            lines.append("BLOCKED NICKNAMES")
            lines.append("-" * 80)
            lines.append(f"Total blocked: {self.blocked_nicknames}")
            lines.append("")
            lines.append("Recent blocks:")
            for i, block in enumerate(self.blocked_reasons[-5:], 1):  # Show last 5
                lines.append(f"  {i}. {block['timestamp']}")
                # Extract nickname from message if possible
                if 'nickname:' in block['message']:
                    parts = block['message'].split('nickname:')
                    if len(parts) > 1:
                        nickname_part = parts[1].strip().split()[0]
                        lines.append(f"     Nickname: {nickname_part}")
            lines.append("")
        
        # Error Details
        if self.errors:
            lines.append("ERROR DETAILS")
            lines.append("-" * 80)
            for i, error in enumerate(self.errors[-10:], 1):  # Show last 10 errors
                lines.append(f"  {i}. [{error['severity']}] {error['timestamp']}")
                lines.append(f"     {error['message'][:200]}")  # Truncate long messages
                lines.append("")
        
        # Final Assessment
        lines.append("=" * 80)
        lines.append("FINAL ASSESSMENT")
        lines.append("=" * 80)
        
        if self.fallback_activations == 0 and len(self.errors) == 0:
            lines.append("✅ The checkNickname function is operating normally.")
            lines.append("   All nicknames are being properly moderated by Vertex AI.")
        else:
            lines.append("❌ The checkNickname function has issues that need attention!")
            lines.append("")
            if self.fallback_activations > 0:
                lines.append("   CRITICAL: Fallback mode was activated, meaning:")
                lines.append("   - Vertex AI moderation is not working properly")
                lines.append("   - Inappropriate nicknames are being allowed through")
                lines.append("   - Users may set offensive or inappropriate nicknames")
                lines.append("")
                lines.append("   Fix the Vertex AI configuration immediately!")
        
        lines.append("=" * 80)
        
        return "\n".join(lines)


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage: python3 tools/analyze-function-logs.py <log_file.json>")
        print("")
        print("The log file should be in JSON format as exported from Cloud Logging or Firebase Console.")
        print("You can download logs using:")
        print("  - Firebase Console: Functions > checkNickname > Logs > Download")
        print("  - gcloud CLI: gcloud logging read '<filter>' --format=json > logs.json")
        sys.exit(1)
    
    log_file = sys.argv[1]
    
    try:
        with open(log_file, 'r', encoding='utf-8') as f:
            logs = json.load(f)
    except FileNotFoundError:
        print(f"Error: File '{log_file}' not found.")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Failed to parse JSON: {e}")
        sys.exit(1)
    
    # Handle both array of logs and object with logs array
    if isinstance(logs, dict):
        # Check for common structures
        if 'entries' in logs:
            log_entries = logs['entries']
        elif 'logs' in logs:
            log_entries = logs['logs']
        else:
            # Assume it's a single log entry
            log_entries = [logs]
    elif isinstance(logs, list):
        log_entries = logs
    else:
        print("Error: Unexpected log file format. Expected JSON array or object with 'entries' or 'logs' field.")
        sys.exit(1)
    
    print(f"Analyzing {len(log_entries)} log entries...")
    print("")
    
    analyzer = LogAnalyzer()
    
    for entry in log_entries:
        analyzer.analyze_log_entry(entry)
    
    # Generate and print the report
    report = analyzer.generate_report()
    print(report)


if __name__ == '__main__':
    main()
