#!/usr/bin/env bash
# AccessFix CLI demo. Announces each feature, waits for you to press Enter, then runs its command.
#
# Run from the project root, after building once with:  ./mvnw package -DskipTests
#   chmod +x demo.sh
#   ./demo.sh
#
# Options (environment variables):
#   LIVE_FIX=1        type your own y/n answers in the fix session instead of the scripted ones
#   REJECT_REASON=..  the reason given when the scripted answers reject a fix
#   JAR=...           path to the jar
#   SAMPLE=...        page to audit

set -u

JAR="${JAR:-target/accessfix-0.1.0-SNAPSHOT.jar}"
SAMPLE="${SAMPLE:-samples/broken-page.html}"
LIVE_FIX="${LIVE_FIX:-0}"
REJECT_REASON="${REJECT_REASON:-Too vague. Please use more specific wording that says what this element is for.}"
GOAL="fix the sign-up form first"
QUESTION="Which issue should be fixed first, and why?"

if [ -t 1 ]; then
  BOLD=$'\033[1m'; CYAN=$'\033[36m'; YELLOW=$'\033[33m'; DIM=$'\033[2m'; OFF=$'\033[0m'
else
  BOLD=; CYAN=; YELLOW=; DIM=; OFF=
fi

# Waits for Enter. Stops the demo if the input has ended.
wait_for_enter() {
  printf '%sPress Enter to run this feature...%s ' "$BOLD" "$OFF"
  if ! read -r _unused; then
    printf '\nNo input, stopping.\n'
    exit 1
  fi
}

# Prints the feature heading and description, then waits for Enter.
feature() {
  printf '\n%s=== %s ===%s\n' "$BOLD$CYAN" "$1" "$OFF"
  printf '%s%s%s\n' "$DIM" "$2" "$OFF"
  wait_for_enter
}

note() { printf '%s(%s)%s\n' "$DIM" "$1" "$OFF"; }

# Shows the command line, then runs it. The command line goes to stderr so it is not captured.
af() {
  printf '%s$ java -jar %s cli %s%s\n' "$YELLOW" "$JAR" "$*" "$OFF" >&2
  java -jar "$JAR" cli "$@"
}

# ---- Checks ----------------------------------------------------------------------------------

command -v java >/dev/null 2>&1 || { echo "Java 21 is not on the PATH."; exit 1; }
[ -f "$JAR" ] || { echo "Jar not found: $JAR. Build it first: ./mvnw package -DskipTests"; exit 1; }
[ -f "$SAMPLE" ] || { echo "Sample page not found: $SAMPLE. Run this from the project root."; exit 1; }

if [ -f .env ]; then
  set -a; . ./.env; set +a
fi
if [ -z "${GEMINI_API_KEY:-}" ] || [ "$GEMINI_API_KEY" = "paste-your-key-here" ]; then
  echo "Warning: GEMINI_API_KEY is not set. The AI features (explain, alt, ask, plan, fix) will fail."
fi

# ---- F01 Page loading ------------------------------------------------------------------------

feature "F01 Page loading" "Loads the page in headless Chromium, stores a snapshot and runs the first scan."
OUT=$(af load "$SAMPLE" | tee /dev/stderr)
ID=$(printf '%s\n' "$OUT" | sed -n 's/^Audit ID: *//p' | head -n 1)
[ -n "$ID" ] || { echo "Could not read the audit ID from the load output. Stopping."; exit 1; }

# ---- F02 Scan --------------------------------------------------------------------------------

feature "F02 Scan" "Re-scans the stored page with the six rules and lists each issue with its WCAG criterion."
SCAN=$(af scan "$ID" | tee /dev/stderr)
IMG_ISSUE=$(printf '%s\n' "$SCAN" | awk '$1 ~ /^image-alt:/ { print $1; exit }')
FORM_ISSUE=$(printf '%s\n' "$SCAN" | awk '$1 ~ /^form-label:/ { print $1; exit }')
[ -n "$IMG_ISSUE" ] || echo "Warning: no image-alt issue found, so the alt text step will not work."
[ -n "$FORM_ISSUE" ] || echo "Warning: no form-label issue found, so the explain step will not work."

# ---- F03 Explanation -------------------------------------------------------------------------

feature "F03 Explanation" "The agent explains one issue in plain language: what is wrong, who is affected, how to fix it."
af explain "$ID" "$FORM_ISSUE"

# ---- F07 Alt text ----------------------------------------------------------------------------

feature "F07 Alt text" "The agent suggests alt text from a screenshot of the first image. The fix is then verified."
note "script answers: accepts the suggestion"
printf '\n' | af alt "$ID" "$IMG_ISSUE"

# ---- F05 WCAG retrieval ----------------------------------------------------------------------

feature "F05 WCAG retrieval" "Searches the 55 WCAG 2.2 Level A and AA criteria. The agent uses the same search as a tool."
af wcag form labels

# ---- F04 Ask mode ----------------------------------------------------------------------------

feature "F04 Ask mode" "Read-only agent mode: answers a question about the audit using list_issues, inspect_element and WCAG search."
af ask "$ID" "$QUESTION"

# ---- F06 Plan mode ---------------------------------------------------------------------------

feature "F06 Plan mode" "The agent submits a ranked remediation plan, our code validates it, and the user approves it."
note "script answers: y, to approve the plan"
printf 'y\n' | af plan "$ID" --goal "$GOAL"

# ---- F08, F09, F11 Fix session ---------------------------------------------------------------

feature "F08 Fix mode, F09 Verification, F11 Memory of rejections" \
  "Each proposed fix shows before and after code. An approved fix is applied and re-scanned. A rejection is saved with its reason, and the agent proposes something different."
if [ "$LIVE_FIX" = "1" ]; then
  note "LIVE_FIX is on: type y or n yourself, and give a reason when you reject"
  af fix "$ID"
else
  note "script answers: approve the first fix, reject the second with a reason, approve the rest"
  { printf 'y\nn\n%s\n' "$REJECT_REASON"; yes y | head -n 20; } | af fix "$ID"
fi

# ---- F09 Undo --------------------------------------------------------------------------------

feature "F09 Undo" "Undoes the most recent fix and re-scans, so the issue count goes back up."
af undo "$ID"

# ---- F10 Report ------------------------------------------------------------------------------

feature "F10 Report" "Prints the Markdown report: latest issues, verified fixes, escalated issues with reasons."
af report "$ID"

feature "F10 Report (export)" "Exports the fixed page as HTML."
af export "$ID" -o fixed.html

printf '\n%sDemo finished. Audit ID: %s%s\n' "$BOLD$CYAN" "$ID" "$OFF"
