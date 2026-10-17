# Getting Audiobooks into the Light Tool Library — step by step

You already did the hard part back in July (Android Studio is installed, your
GitHub token works, the project is set up). This guide starts from a cold Mac
and ends with your tool submitted to Light. About 45 minutes.

Five phases:

- A. Open the project and make sure it still builds (~10 min)
- B. Take a screenshot of the tool for the submission (~10 min)
- C. Put your code on GitHub, publicly (~10 min)
- D. Submit on Light's dashboard (~10 min)
- E. What happens next (reading, 1 min)

One thing to know before you start: I updated the project to Light's newest
SDK. The audiobook player now uses Light's official audio system, which means
books keep playing when the screen is off — that limitation from July is gone.

---

## A. Open the project and make sure it builds

A1. Open **Android Studio** (from your Applications folder).

A2. If it opens straight into the project (title bar says light-sdk or
audiobooks-light-tool), good. If you see a welcome screen instead, click
**Open** and navigate to:

```
My Drive → Claude Cowork → OUTPUTS → Lightphone app → audiobooks-light-tool
```

A3. Let it sync. A progress bar runs along the bottom — it's downloading the
updated Light SDK pieces. Wait until it stops (can take a few minutes). If a
blue banner appears saying "Sync Now", click it.

A4. Click the **Terminal** tab at the bottom of the Android Studio window,
then paste these two lines, pressing Enter after each:

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```
./gradlew :tool:assembleDebug
```

A5. Wait a few minutes. You want the words **BUILD SUCCESSFUL**.

- If you see BUILD SUCCESSFUL → continue to phase B.
- If you see an error → copy the red text, paste it to Claude, stop here.
  (The code was updated to Light's newest SDK and this is its first real
  compile — if anything's off, Claude fixes it, not you.)

---

## B. Take a screenshot of the tool

Light requires at least one picture of your tool for approval. You'll run it
on the fake Light Phone and photograph it. One wrinkle: the project is now
set to "real phone" mode, so running on the emulator needs two small,
temporary changes.

B1. **Point the tool at the emulator.** In the file panel on the left:
if the top of the panel says "Android", click it and switch to **Project**
view. Expand **audiobooks-light-tool → tool** and double-click
**lighttool.toml**. Find these two lines near the bottom:

```
# serverPackage = "com.thelightphone.sdk.emulator"
serverPackage = "com.lightos"
```

Swap which one has the `#` in front (the `#` means "ignored"), so it reads:

```
serverPackage = "com.thelightphone.sdk.emulator"
# serverPackage = "com.lightos"
```

Save with ⌘S.

B2. **Allow the emulator setting.** In the same Project view, at the very
top level of the project, double-click **gradle.properties** and add this
line at the bottom, then save:

```
systemProp.lightSdk.allowAltServerPackage=true
```

B3. **Start the fake phone.** In the Terminal tab:

```
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools:$HOME/Library/Android/sdk/emulator"
```

```
emulator -avd Light_Phone_III -writable-system &
```

Wait for the phone window to reach a home screen (1–3 minutes).

B4. **Run the tool.** In the toolbar at the top of Android Studio, make sure
the two dropdowns say **tool** and **Light Phone III**, then click the green
**▶**. After a minute or two, the AUDIOBOOKS screen appears on the phone.

B5. **Make it look good, then snap it.** Best screenshot = a book list or the
player. If the emulator kept your July test books, open one. If it's empty,
load one book: in the Terminal run `adb forward tcp:8765 tcp:8765`, tap
**+ LOAD BOOKS OVER WI-FI** on the phone, open **http://localhost:8765** in
your Mac's browser, upload a few MP3s, then tap the book and press play.

Click the **camera icon** in the emulator's side toolbar. The screenshot
saves to your Desktop.

B6. **Undo the two temporary changes** (important — the submitted version
must point at the real phone):

- In **lighttool.toml**, swap the `#` back so `serverPackage = "com.lightos"`
  is the active line. Save.
- In **gradle.properties**, delete the `systemProp.lightSdk...` line you
  added. Save.

---

## C. Put your code on GitHub, publicly

Light only accepts open-source tools — their servers build your tool straight
from your public GitHub repo. The friendliest way to publish is GitHub
Desktop, a free app.

C1. In your browser, go to **https://desktop.github.com**, download GitHub
Desktop, open the downloaded file, and drag the app to Applications. Open it.

C2. It asks you to sign in — click **Sign in to GitHub.com**, which opens
your browser; log in with your GitHub account (the one from July) and click
**Authorize**. Back in the app, click through the setup (defaults are fine).

C3. Menu **File → Add Local Repository** → click **Choose...** and pick:

```
My Drive → Claude Cowork → OUTPUTS → Lightphone app → audiobooks-light-tool
```

Click **Add Repository**.

C4. The left side shows a long list of changed files — that's your Audiobooks
tool plus the SDK update. In the lower-left corner there's a small text box
labeled "Summary (required)". Type:

```
Audiobooks tool v1.1.0
```

Click the blue **Commit** button beneath it.

C5. At the top of the window click **Publish repository** (or **Push origin**
if that's what the button says). In the dialog:

- Name: `audiobooks-light-tool` is fine
- **UNCHECK "Keep this code private"** — this is the one checkbox that
  matters; Light requires the code to be public
- Click **Publish Repository**

C6. Confirm it worked: in your browser go to
`https://github.com/YOUR_USERNAME/audiobooks-light-tool` — you should see
the code without being logged in. Copy this URL; you need it in phase D.

---

## D. Submit on Light's dashboard

D1. In your browser, log in at **https://dashboard.thelightphone.com** (the
same place you manage your Light Phone).

D2. Go to **Settings → Account → Developer Account** and enable developer
mode if it isn't already on.

D3. Go to **Manage Custom Tools → Submit New Tool**:

- Paste your GitHub URL from C6
- The package name (`com.davidhobbet.audiobooks`) is read from your repo
  automatically — note it's permanent once registered
- Upload the screenshot from phase B (from your Desktop)

D4. Open your new tool's detail page and click **Submit build**:

- Git ref: type `main`
- Changelog / description, paste:

```
Initial release. Plays DRM-free MP3 audiobooks: one folder per book, one MP3 per chapter. Resume per book, chapter navigation, 15-second skips, playback speed, sleep timer, and wi-fi book loading. Background playback via detached audio.
```

D5. Wait for the build (their servers compile your code):

- **Success** → a "download apk" button appears and the version shows
  "pending approval". You're done — go to phase E.
- **Error** → click "view details" in the Builds section, copy the error
  text, and paste it to Claude.

---

## E. What happens next

- Light's team reviews the build and emails you feedback or approval.
- The Tool Library goes live in **November**. Once Audiobooks is approved
  and the library is live, you install it on your actual Light Phone from
  the dashboard, like any official tool.
- On the real phone, the wi-fi upload page works directly at the address
  shown on screen — no terminal, no bridge command, ever.
- Questions for Light directly: tools@thelightphone.com

## If something breaks

- **"command not found"** in the terminal → re-run the `export` line from
  that phase (they reset every time the terminal closes).
- **Build fails in A5 or D5** → paste the error to Claude. Don't try to fix
  it yourself; it's my code.
- **GitHub Desktop won't publish** → make sure you're signed in (GitHub
  Desktop → Settings → Accounts).
- **Dashboard looks different from this guide** → Light ships changes
  quickly; describe what you see to Claude and we'll navigate it together.
