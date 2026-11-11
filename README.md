# Writing Assistant (Floating Paste Button)

A minimal, polished Android productivity app that provides a floating button over editable text fields. Tapping the button opens a compact menu to instantly insert the current date, time, day of the week, or customizable saved text snippets into any text field across Android.

## Features

1. **Real Paste Accessibility Overlay (`TYPE_ACCESSIBILITY_OVERLAY`)**
   - Automatically appears when an editable text field gains focus and hides when unfocused.
   - Draggable, snaps cleanly to the left or right screen edge, and remembers its vertical position.
   - Customizable size (36dp–56dp) and opacity (40%–100%).
   - Uses **real clipboard paste** (`ACTION_PASTE`) on `findFocus(FOCUS_INPUT)`, inserting naturally at the cursor without touching hint text or resetting editor state (Enter, autosave, undo all work smoothly in Google Keep, Notes, and messaging apps).
   - Automatically restores the user's prior clipboard after 300 ms.
   - Never appears on lock screen or system UI (`com.android.systemui`).
   - Strictly ignores password fields (`isPassword`) for security.
   - Zero long-lived node references to prevent host app crashes.

2. **Compact Tap Menu**
   - Quick insertion of:
     - **Date**: Formatted with user-chosen default format or custom pattern.
     - **Time**: Formatted in 12h or 24h with seconds or AM/PM.
     - **Day**: Full (`Monday`) or short (`Mon`).
     - **Saved Snippets**: Scrollable list of templates with live variable substitution.
   - Non-intrusive card layout that dismisses upon selection or tap-outside.
   - Fallback: If paste returns false, keeps text on clipboard with toast *"Copied, paste manually"*.

3. **Format System & Bangla Localization**
   - Supports standard format tokens (`dd`, `d`, `MM`, `M`, `MMM`, `MMMM`, `yy`, `yyyy`, `EEEE`, `EEE`, `hh`, `h`, `HH`, `H`, `mm`, `ss`, `a`) and literal text in single quotes (`'...'`).
   - **Full Bangla support**:
     - Month names (জানুয়ারি, ..., ডিসেম্বর)
     - Day names (সোমবার, ..., রবিবার)
     - Digits (০–৯)
     - AM/PM options: Time of day (সকাল, দুপুর, বিকাল, সন্ধ্যা, রাত) or direct AM/PM (পূর্বাহ্ন/অপরাহ্ন).
   - Live "now" preview while editing custom formats and option to save multiple named formats.

4. **Saved Texts with Variables**
   - Variables evaluated at insertion time:
     - `{date}`, `{time}`, `{day}`: User's default formats.
     - `{date:pattern}`, `{time:pattern}`, `{day:pattern}`: Inline custom patterns.
     - `{clipboard}`: Pastes current clipboard text.
     - `{cursor}`: Places the text cursor at that position after insertion.
     - Unknown `{variables}` are safely preserved as plain text.
   - Full CRUD in Room database with JSON Export and Import.

5. **App Filtering & Privacy**
   - **Mode A**: Show in all apps.
   - **Mode B**: Show only in selected apps (searchable checklist).
   - **Mode C**: Show in all apps except selected ones.
   - **Zero idle usage**: No background threads, no wake locks, no foreground service.
   - **Privacy guarantee**: 100% on-device, no internet permission (`android.permission.INTERNET` is omitted), never logs or transmits content.

---

## How to Enable the Accessibility Service

1. Open the app. If the service is disabled, an **Accessibility Onboarding Banner** is shown.
2. Tap **"Enable in Accessibility Settings"**.
3. In Android Settings > **Accessibility** > **Downloaded apps** (or **Installed services**):
   - Locate and tap **Writing Assistant Floating Button**.
   - Turn the switch **ON** and accept the standard Android confirmation prompt.
4. Return to Writing Assistant. The banner will turn green with the status **"Floating Button is Ready"**.
5. Test it immediately by tapping into the **Interactive Playground / Test Field** right inside the app or in any other messaging/notes app!

---

## Architecture & Tech Stack

- **UI**: Jetpack Compose with Material Design 3 (M3), dynamic light/dark theming.
- **Service**: Android `AccessibilityService` drawing directly via `WindowManager` using `TYPE_ACCESSIBILITY_OVERLAY`.
- **Database**: Room with KSP (`SavedTextDao`, `AppDatabase`).
- **Preferences**: Jetpack DataStore Preferences with an in-memory cached snapshot for instant zero-latency event handling.
- **Testing**: JUnit 4 unit test suite covering `FormatEngine` and `VariableParser`.
