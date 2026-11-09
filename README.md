# AccessFix

AccessFix audits a web page for accessibility issues, explains them in plain language and, with your approval, fixes them. An AI agent (Google Gemini by default) helps explain, plan and propose fixes. Every fix is checked by scanning the page again, and you can undo it.

You can use it two ways:

- **In a browser**, at http://localhost:8080.
- **From a terminal**, with commands such as `load`, `scan`, `fix` and `report`.

This guide starts from a computer with nothing installed.

## What you need

| Item | Why | Notes |
|---|---|---|
| Java 21 (JDK) | Builds and runs AccessFix | The only tool you install yourself |
| Internet access | The first build downloads Maven, the libraries and a copy of Node; the first page load downloads Chromium (about 150 MB) | Later runs work from these copies. AI features always need internet access |
| A Gemini API key | Explanations, alt-text suggestions, Ask, Plan and Fix | Free from Google AI Studio. Loading, scanning, undo, reports and WCAG search work without a key |
| About 1.5 GB of disk space | Libraries, Node and Chromium | |

You don't need to install Maven or Node. The project includes the Maven wrapper (`mvnw`), and the build downloads its own copy of Node.

## 1. Install Java 21

**Windows** (PowerShell):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

**macOS** (with [Homebrew](https://brew.sh)):

```bash
brew install --cask temurin@21
```

**Linux** (Ubuntu or Debian):

```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

Close and reopen the terminal, then check the version:

```bash
java -version
```

It should say version 21. If it shows another version, set `JAVA_HOME` to the Java 21 folder. The Maven wrapper uses `JAVA_HOME` when it's set.

## 2. Get the project

Clone the repository, or unzip the project folder, then open a terminal in it:

```bash
cd accessfix
```

On macOS and Linux, make the wrapper runnable (once):

```bash
chmod +x mvnw
```

## 3. Add your API key

1. Get a key from [Google AI Studio's API keys page](https://aistudio.google.com/apikey). Sign in with a Google account and select **Create API key**.
2. Copy `.env.example` to a new file called `.env` in the same folder.
3. In `.env`, paste your key after `GEMINI_API_KEY=`. Leave `ANTHROPIC_API_KEY` empty unless you switch to Claude (see [Settings](#settings)).

`.env` is listed in `.gitignore`, so Git won't commit it. Don't put the key in `application.properties`, because that file is committed.

AccessFix reads the key from an environment variable, so load `.env` into each new terminal before you start the app.

**macOS and Linux:**

```bash
set -a; . ./.env; set +a
```

**Windows (PowerShell):**

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  Set-Item "env:$($name.Trim())" $value.Trim()
}
```

## 4. Build

**macOS and Linux:**

```bash
./mvnw package -DskipTests
```

**Windows:**

```powershell
.\mvnw.cmd package -DskipTests
```

The first build takes a few minutes. It downloads Maven, the Java libraries and Node, builds the React app, and makes one file: `target/accessfix-0.1.0-SNAPSHOT.jar`. It ends with `BUILD SUCCESS`.

## 5. Run in a browser

```bash
java -jar target/accessfix-0.1.0-SNAPSHOT.jar
```

Wait for the line `Tomcat started on port 8080`, then open http://localhost:8080. To stop the app, press Ctrl+C in the terminal.

Try it with the sample page:

1. On the **Audit** page, under **Or upload an HTML file**, choose `samples/broken-page.html`, then select **Upload HTML file**. The first load downloads Chromium, so it can take a minute. You should see **Issues (7)**.
2. Select **Explain** on an issue, or **Suggest alt text** on an image issue.
3. Go to **Agent**. Ask a question in Ask mode. Or select **Plan**, create a plan and approve it, then select **Fix** and **Start**. Approve or reject each proposed fix.
4. Go to **Reports** to download the report or the fixed page.

You can also load a live page by its URL.

## 6. Run from a terminal

Load `.env` first (step 3), then put `cli` before the command:

```bash
java -jar target/accessfix-0.1.0-SNAPSHOT.jar cli load samples/broken-page.html
```

`load` prints an audit ID. The other commands, except `wcag`, take that ID:

| Command | What it does |
|---|---|
| `load <file or URL>` | Loads and scans a page, and prints the audit ID |
| `scan <id>` | Scans again and lists the issues |
| `explain <id> <issueId>` | Explains one issue |
| `alt <id> <issueId>` | Suggests alt text for an image, then applies it if you agree |
| `ask <id> <question>` | Asks the agent a question |
| `plan <id> [--goal "…"]` | Creates a plan, then asks you to approve it |
| `fix <id>` | Starts a fix session: approve or reject each fix (needs an approved plan) |
| `undo <id>` | Undoes the latest fix |
| `report <id> [-o report.md]` | Shows the Markdown report, or saves it with `-o` |
| `export <id> [-o fixed.html]` | Shows the fixed page's HTML, or saves it with `-o` |
| `wcag <words>` | Searches the WCAG 2.2 Level A and AA criteria |

Run `cli --help`, or `cli <command> --help`, for details. Each command runs on its own and exits. Audits are saved in the `data` folder, so an audit ID keeps working in later commands.

## Run the tests (optional)

```bash
./mvnw test
```

On Windows, use `.\mvnw.cmd test`. Expect 427 tests to pass, with 4 skipped. The skipped tests call the real AI service; they run only when `ACCESSFIX_LIVE_TESTS=true`. To skip building the React app during tests, add `-Dfrontend.skip=true`.

## Changing the React app (optional)

To see your changes as soon as you save, run the Vite dev server alongside the app. This needs [Node 22](https://nodejs.org) installed.

1. Start AccessFix (step 5).
2. In a second terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Open http://localhost:5173. The dev server sends API calls to port 8080.

Run `./mvnw package` again to put your changes into the jar.

## Settings

Settings are in `src/main/resources/application.properties`. Rebuild after changing them.

| Setting | Default | Meaning |
|---|---|---|
| `accessfix.llm` | `gemini` | `gemini` uses `GEMINI_API_KEY`; `claude` uses `ANTHROPIC_API_KEY` |
| `accessfix.gemini.model` | `gemini-3.5-flash-lite` | The Gemini model |
| `accessfix.storage` | `h2` | `h2` saves audits in `./data`; `memory` forgets them when the app stops |
| `server.port` | `8080` | The browser app's port |

## Using VS Code (optional)

Open the folder in VS Code and accept the recommended extensions. **Run and Debug** has configurations for the web server and the CLI, and they read `.env` for you.

## Troubleshooting

| Problem | What to do |
|---|---|
| `java -version` isn't 21, or the build says "release version 21 not supported" | Install Java 21 and set `JAVA_HOME` to it |
| `permission denied: ./mvnw` | Run `chmod +x mvnw` |
| The build fails in `vite build` the first time | Run the same command again |
| "The Gemini API key is missing" | Load `.env` into this terminal (step 3) and restart the app |
| "The Gemini API key isn't valid" | Check the key in `.env`, then load it and restart |
| "Gemini's rate limit was reached" | The free tier allows only a few requests a minute. Wait a minute, then select **Retry** or run the command again. Fixes that were already verified are kept |
| Port 8080 is already in use | Stop the other program, or change `server.port` |
| Loading a page fails on Linux with missing library errors | Install Chromium's system libraries: `./mvnw compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install-deps chromium"` (it asks for your password) |
| You want to start again with no saved audits | Stop the app and delete the `data` folder |

## More documents

- `docs/TRACEABILITY.md`: where each feature from the Stage 1 design is implemented, and its tests.
- `docs/DESIGN-CHANGES.md`: each difference from the Stage 1 design, and why.
- `docs/STARTER-README.md`: the original starter-code guide (build order, AI tips).
- `samples/README.md`: the issues expected on each sample page.
