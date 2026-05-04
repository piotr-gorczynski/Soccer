#!/usr/bin/env python3
"""
Fix NullPointerException from getEnclosingMethod() in Android Java files.

Replaces:
  "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
with:
  ".METHODNAME"
where METHODNAME is the actual Java method name that directly contains the pattern.
"""

import re
import os

BASE_DIR = (
    "/home/runner/work/Soccer/Soccer/mobile/app/src/main/java/piotr_gorczynski/soccer2"
)

TARGET_FILES = [
    "GameActivity.java",
    "FirebaseAuthManager.java",
    "RegulationActivity.java",
    "SoccerApp.java",
    "InvitationsActivity.java",
    "TournamentLobbyActivity.java",
    "WaitingActivity.java",
    "TournamentAdapter.java",
    "MatchAdapter.java",
    "UniversalLoginActivity.java",
    "FriendsListActivity.java",
    "Field.java",
]

# Regex: matches "." + Objects.requireNonNull(new Object(){} OR new Object() {...whitespace...})
# .getClass().getEnclosingMethod()).getName()
# Works for both single-line and multi-line variants.
PATTERN = re.compile(
    r'"\."\s*\+\s*(?:java\.util\.)?Objects\.requireNonNull\('
    r'new Object\(\)\s*\{[^}]*\}'
    r'\.getClass\(\)\.getEnclosingMethod\(\)\)\.getName\(\)',
    re.DOTALL,
)

# Keywords that can appear at start of a block statement but are NOT method names
CONTROL_KEYWORDS = frozenset({
    "if", "else", "for", "while", "do", "try", "catch", "finally",
    "switch", "synchronized", "case", "default",
})

# Keywords that look like identifiers but cannot be method names
NON_METHOD_NAMES = frozenset({
    "if", "else", "for", "while", "do", "try", "catch", "finally",
    "switch", "synchronized", "new", "return", "throw", "super", "this",
    "assert", "case", "default", "instanceof", "class", "interface", "enum",
    "static", "import", "package",
})


def strip_strings_from_line(line: str) -> str:
    """
    Remove string and char literals from a line so braces inside them
    are not counted. Also stops at a // line comment.
    """
    result = []
    i = 0
    in_str = False
    in_char = False
    prev_bs = False
    while i < len(line):
        c = line[i]
        if prev_bs:
            prev_bs = False
            i += 1
            continue
        if c == "\\" and (in_str or in_char):
            prev_bs = True
            i += 1
            continue
        if c == '"' and not in_char:
            in_str = not in_str
            i += 1
            continue
        if c == "'" and not in_str:
            in_char = not in_char
            i += 1
            continue
        if not in_str and not in_char:
            # Stop at line comment
            if c == "/" and i + 1 < len(line) and line[i + 1] == "/":
                break
            result.append(c)
        i += 1
    return "".join(result)


def find_method_name_for_brace(lines: list, brace_line_idx: int) -> str | None:
    """
    Determine whether the (first unmatched) opening brace on brace_line_idx
    starts a named method or constructor.  Returns the method name, or None
    if this is a lambda, control-flow block, anonymous class body, etc.
    """
    # Build context from up to 5 preceding lines + the brace line itself,
    # stripping string literals and joining with a space.
    start = max(0, brace_line_idx - 5)
    context_parts = []
    for j in range(start, brace_line_idx + 1):
        context_parts.append(strip_strings_from_line(lines[j]))
    text = " ".join(context_parts).rstrip()

    # Locate the opening brace we care about (the last { in the text)
    brace_pos = text.rfind("{")
    if brace_pos < 0:
        return None
    before_brace = text[:brace_pos].rstrip()

    # ── Lambda check ────────────────────────────────────────────────────────
    # A lambda ends the signature with ->  before {
    if re.search(r"->\s*$", before_brace):
        return None

    # ── Strip optional throws clause ────────────────────────────────────────
    before_brace = re.sub(r"\bthrows\s+[\w\s,<>.\[\]]+$", "", before_brace).rstrip()

    # ── Anonymous class check ───────────────────────────────────────────────
    # Pattern: new TypeName(...) {   or   new TypeName<...>(...) {
    # The text before { ends with ) after stripping throws.
    # Check: is there a 'new' keyword that governs the final (...) ?
    # Strategy: look for  'new  <ident-or-generic>  (...) '  as a suffix.
    if re.search(r"\bnew\s+[\w<>?,\[\]\s.@]+\s*\([^{)]*\)\s*$", before_brace):
        return None

    # ── No-paren blocks: else, try, finally, do, static  ───────────────────
    # These don't have a parameter list.
    if not re.search(r"\)\s*$", before_brace):
        # Ends with a word (not ')'), check what word
        last_word_m = re.search(r"\b(\w+)\s*$", before_brace)
        if last_word_m:
            last_word = last_word_m.group(1)
            if last_word in CONTROL_KEYWORDS or last_word in {
                "static", "class", "interface", "enum"
            }:
                return None
        # Empty or only whitespace / annotations → not a named method
        return None

    # ── Extract the method name from  NAME(params)  before the { ────────────
    # Find the matching opening paren for the last closing paren
    rp_pos = len(before_brace) - 1
    while rp_pos >= 0 and before_brace[rp_pos] != ")":
        rp_pos -= 1
    if rp_pos < 0:
        return None

    depth = 0
    lp_pos = -1
    for k in range(rp_pos, -1, -1):
        ch = before_brace[k]
        if ch == ")":
            depth += 1
        elif ch == "(":
            depth -= 1
            if depth == 0:
                lp_pos = k
                break
    if lp_pos < 0:
        return None

    text_before_paren = before_brace[:lp_pos].rstrip()
    id_match = re.search(r"\b(\w+)\s*$", text_before_paren)
    if not id_match:
        return None

    name = id_match.group(1)
    if name in NON_METHOD_NAMES:
        return None

    # Ensure 'new' doesn't precede the name (constructor call in expression)
    pos = id_match.start()
    before_name = text_before_paren[:pos].rstrip()
    if re.search(r"\bnew\s*$", before_name):
        return None

    return name


def find_enclosing_method(lines: list, start_line_idx: int) -> str:
    """
    Walk backward from start_line_idx, tracking brace depth, to find
    the innermost named Java method that contains this position.

    Returns the method name, or 'UNKNOWN' if none could be determined.
    """
    depth = 0
    for i in range(start_line_idx, -1, -1):
        clean = strip_strings_from_line(lines[i])
        # Discard everything from // comment onward
        comment_idx = clean.find("//")
        if comment_idx >= 0:
            clean = clean[:comment_idx]
        # Walk chars right-to-left on this line
        for ch in reversed(clean):
            if ch == "}":
                depth += 1
            elif ch == "{":
                if depth > 0:
                    depth -= 1
                else:
                    # Found an enclosing opening brace
                    method_name = find_method_name_for_brace(lines, i)
                    if method_name:
                        return method_name
                    # It's a lambda / control-flow / anonymous class body;
                    # do NOT change depth — keep scanning for an outer method.
    return "UNKNOWN"


def process_file(filepath: str) -> tuple[str, int]:
    """
    Read the file, replace every getEnclosingMethod() pattern with the
    hardcoded method name, and return (new_content, replacement_count).
    """
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    lines = content.split("\n")

    replacements = []
    for match in PATTERN.finditer(content):
        start_pos = match.start()
        line_idx = content[:start_pos].count("\n")
        method_name = find_enclosing_method(lines, line_idx)
        replacements.append((match.start(), match.end(), method_name))
        print(f"    line {line_idx + 1:4d}: enclosing method = {method_name}")

    if not replacements:
        print("    (no occurrences found)")
        return content, 0

    # Apply replacements in reverse order to preserve character positions
    new_content = content
    for start, end, method_name in reversed(replacements):
        replacement = f'".{method_name}"'
        new_content = new_content[:start] + replacement + new_content[end:]

    # Remove import java.util.Objects if it's no longer needed
    if "import java.util.Objects;" in new_content:
        remaining_uses = len(re.findall(r"\bObjects\.", new_content))
        if remaining_uses == 0:
            new_content = re.sub(r"import java\.util\.Objects;\n", "", new_content)
            print("    Removed unused import java.util.Objects")

    return new_content, len(replacements)


def main():
    total = 0
    for filename in TARGET_FILES:
        filepath = os.path.join(BASE_DIR, filename)
        if not os.path.exists(filepath):
            print(f"[MISSING] {filepath}")
            continue
        print(f"\nProcessing {filename} ...")
        new_content, count = process_file(filepath)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"  → {count} replacement(s)")
        total += count
    print(f"\nTotal replacements: {total}")


if __name__ == "__main__":
    main()
