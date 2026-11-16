<#
AccessFix CLI demo for Windows. Announces each feature, waits for you to press Enter, then runs its command.

Run from the project root, after building once with:  .\mvnw.cmd package -DskipTests
    powershell -ExecutionPolicy Bypass -File .\demo.ps1
    powershell -ExecutionPolicy Bypass -File .\demo.ps1 -LiveFix

Options:
    -LiveFix        type your own y/n answers in the fix session instead of the scripted ones
    -RejectReason   the reason given when the scripted answers reject a fix
    -Jar            path to the jar
    -Sample         page to audit
#>
param(
    [switch]$LiveFix,
    [string]$RejectReason = 'Too vague. Please use more specific wording that says what this element is for.',
    [string]$Jar = 'target/accessfix-0.1.0-SNAPSHOT.jar',
    [string]$Sample = 'samples/broken-page.html'
)

$Goal = 'fix the sign-up form first'
$Question = 'Which issue should be fixed first, and why?'

# Show non-ASCII characters from the agent correctly, and send input without a byte order mark.
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
} catch { }

# Waits for Enter. Stops the demo if there is no input.
function Wait-ForEnter {
    $reply = $null
    try {
        $reply = Read-Host 'Press Enter to run this feature'
    } catch { }
    if ($null -eq $reply) {
        Write-Host 'No input, stopping.'
        exit 1
    }
}

# Prints the feature heading and description, then waits for Enter.
function Feature([string]$Title, [string]$Description) {
    Write-Host ''
    Write-Host "=== $Title ===" -ForegroundColor Cyan
    Write-Host $Description -ForegroundColor DarkGray
    Wait-ForEnter
}

function Note([string]$Text) {
    Write-Host "($Text)" -ForegroundColor DarkGray
}

function Write-CommandLine($CommandArgs) {
    Write-Host ('$ java -jar {0} cli {1}' -f $Jar, ($CommandArgs -join ' ')) -ForegroundColor Yellow
}

# Runs one AccessFix CLI command. The property names are quoted so PowerShell passes them whole.
function Af {
    Write-CommandLine $args
    & java '-Dstdout.encoding=UTF-8' '-Dfile.encoding=UTF-8' -jar $Jar cli @args
}

# The same, with lines sent to the command's standard input (the y/n and Enter answers).
function AfInput {
    param([string[]]$Lines)
    Write-CommandLine $args
    $Lines | & java '-Dstdout.encoding=UTF-8' '-Dfile.encoding=UTF-8' -jar $Jar cli @args
}

# ---- Checks ----------------------------------------------------------------------------------

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host 'Java 21 is not on the PATH.'
    exit 1
}
if (-not (Test-Path $Jar)) {
    Write-Host "Jar not found: $Jar. Build it first: .\mvnw.cmd package -DskipTests"
    exit 1
}
if (-not (Test-Path $Sample)) {
    Write-Host "Sample page not found: $Sample. Run this from the project root."
    exit 1
}

# Load .env into this session, the same way the README does.
if (Test-Path .env) {
    Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
        $name, $value = $_ -split '=', 2
        Set-Item "env:$($name.Trim())" $value.Trim()
    }
}
if ((-not $env:GEMINI_API_KEY) -or ($env:GEMINI_API_KEY -eq 'paste-your-key-here')) {
    Write-Host 'Warning: GEMINI_API_KEY is not set. The AI features (explain, alt, ask, plan, fix) will fail.' -ForegroundColor Red
}

# ---- F01 Page loading ------------------------------------------------------------------------

Feature 'F01 Page loading' 'Loads the page in headless Chromium, stores a snapshot and runs the first scan.'
Af load $Sample | Tee-Object -Variable loadOut
$Id = $null
$match = $loadOut | Select-String -Pattern '^Audit ID:\s*(\S+)' | Select-Object -First 1
if ($match) { $Id = $match.Matches[0].Groups[1].Value }
if (-not $Id) {
    Write-Host 'Could not read the audit ID from the load output. Stopping.'
    exit 1
}

# ---- F02 Scan --------------------------------------------------------------------------------

Feature 'F02 Scan' 'Re-scans the stored page with the six rules and lists each issue with its WCAG criterion.'
Af scan $Id | Tee-Object -Variable scanOut
$IssueIds = $scanOut | ForEach-Object { ("$_".Trim() -split '\s+')[0] }
$ImgIssue = $IssueIds | Where-Object { $_ -like 'image-alt:*' } | Select-Object -First 1
$FormIssue = $IssueIds | Where-Object { $_ -like 'form-label:*' } | Select-Object -First 1
if (-not $ImgIssue) { Write-Host 'Warning: no image-alt issue found, so the alt text step will not work.' }
if (-not $FormIssue) { Write-Host 'Warning: no form-label issue found, so the explain step will not work.' }

# ---- F03 Explanation -------------------------------------------------------------------------

Feature 'F03 Explanation' 'The agent explains one issue in plain language: what is wrong, who is affected, how to fix it.'
Af explain $Id $FormIssue

# ---- F07 Alt text ----------------------------------------------------------------------------

Feature 'F07 Alt text' 'The agent suggests alt text from a screenshot of the first image. The fix is then verified.'
Note 'script answers: accepts the suggestion'
AfInput @('', '') alt $Id $ImgIssue

# ---- F05 WCAG retrieval ----------------------------------------------------------------------

Feature 'F05 WCAG retrieval' 'Searches the 55 WCAG 2.2 Level A and AA criteria. The agent uses the same search as a tool.'
Af wcag form labels

# ---- F04 Ask mode ----------------------------------------------------------------------------

Feature 'F04 Ask mode' 'Read-only agent mode: answers a question about the audit using list_issues, inspect_element and WCAG search.'
Af ask $Id $Question

# ---- F06 Plan mode ---------------------------------------------------------------------------

Feature 'F06 Plan mode' 'The agent submits a ranked remediation plan, our code validates it, and the user approves it.'
Note 'script answers: y, to approve the plan'
AfInput @('y') plan $Id 

# ---- F08, F09, F11 Fix session ---------------------------------------------------------------

Feature 'F08 Fix mode, F09 Verification, F11 Memory of rejections' 'Each proposed fix shows before and after code. An approved fix is applied and re-scanned. A rejection is saved with its reason, and the agent proposes something different.'

Note 'Fix mode is interactive: approve or reject proposals and enter rejection reasons.'
Af fix $Id


# ---- F10 Report ------------------------------------------------------------------------------

Feature 'F10 Report' 'Prints the Markdown report: latest issues, verified fixes, escalated issues with reasons.'
Af report $Id

Feature 'F10 Report (export)' 'Exports the fixed page as HTML.'
Af export $Id '-o' 'fixed.html'

Write-Host ''
Write-Host "Demo finished. Audit ID: $Id" -ForegroundColor Cyan
