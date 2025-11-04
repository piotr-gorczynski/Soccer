#!/usr/bin/env python3
"""
Check Translation Completeness

This script verifies that all languages present in the mobile app's
string resources are also present in the firebase/seed directory.

Usage:
    python3 tools/check-translation-completeness.py

Exit codes:
    0 - All translations are complete
    1 - Some translations are missing in firebase/seed
"""

import os
import sys
from pathlib import Path


def get_mobile_languages(repo_root):
    """Extract language codes from mobile app res/values-* directories."""
    mobile_langs = set()
    res_dir = repo_root / "mobile" / "app" / "src" / "main" / "res"
    
    if not res_dir.exists():
        print(f"❌ Error: Mobile app resource directory not found: {res_dir}")
        sys.exit(1)
    
    for item in res_dir.iterdir():
        if item.is_dir():
            if item.name.startswith("values-"):
                # Extract language code from values-XX format
                lang_code = item.name.replace("values-", "")
                mobile_langs.add(lang_code)
            elif item.name == "values":
                # values/ directory is the default (English)
                mobile_langs.add("en")
    
    return mobile_langs


def get_firebase_languages(repo_root):
    """Extract language codes from firebase/seed tournament_rules files."""
    firebase_langs = set()
    firebase_seed_dir = repo_root / "firebase" / "seed"
    
    if not firebase_seed_dir.exists():
        print(f"❌ Error: Firebase seed directory not found: {firebase_seed_dir}")
        sys.exit(1)
    
    for item in firebase_seed_dir.iterdir():
        if item.is_file() and item.name.startswith("tournament_rules_") and item.name.endswith(".json"):
            # Extract language code from tournament_rules_XX.json format
            lang_code = item.name.replace("tournament_rules_", "").replace(".json", "")
            firebase_langs.add(lang_code)
    
    return firebase_langs


def main():
    # Get repository root (script is in tools/ directory)
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent
    
    print("=" * 70)
    print("Translation Completeness Check")
    print("=" * 70)
    print()
    
    # Get language sets
    mobile_langs = get_mobile_languages(repo_root)
    firebase_langs = get_firebase_languages(repo_root)
    
    print(f"Mobile app languages ({len(mobile_langs)}): {sorted(mobile_langs)}")
    print(f"Firebase seed languages ({len(firebase_langs)}): {sorted(firebase_langs)}")
    print()
    
    # Check for missing languages
    missing_in_firebase = mobile_langs - firebase_langs
    extra_in_firebase = firebase_langs - mobile_langs
    
    all_complete = True
    
    if missing_in_firebase:
        all_complete = False
        print("❌ MISSING: Languages in mobile app but NOT in firebase/seed:")
        for lang in sorted(missing_in_firebase):
            print(f"   - {lang} (needs tournament_rules_{lang}.json)")
        print()
    else:
        print("✅ All mobile app languages are present in firebase/seed")
        print()
    
    if extra_in_firebase:
        print("ℹ️  EXTRA: Languages in firebase/seed but NOT in mobile app:")
        for lang in sorted(extra_in_firebase):
            print(f"   - {lang} (consider adding mobile/app/src/main/res/values-{lang}/strings.xml)")
        print()
    
    # Summary
    print("=" * 70)
    if all_complete:
        print("✅ SUCCESS: All translations are complete!")
        print("=" * 70)
        return 0
    else:
        print("❌ FAILURE: Some translations are incomplete!")
        print("=" * 70)
        return 1


if __name__ == "__main__":
    sys.exit(main())
