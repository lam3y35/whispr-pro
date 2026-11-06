# Whispr Pro (Groq Cloud API Edition)

[![Android](https://github.com/lam3y35/whispr-pro/actions/workflows/android.yml/badge.svg)](https://github.com/lam3y35/whispr-pro/actions/workflows/android.yml)

Push-to-talk dictation keyboard and smart flying button powered by **Groq Cloud API** and **Whisper Large-v3** from OpenAI.

This is the dedicated personal edition of Whispr AI, configured specifically with an API key to take full advantage of Groq's high-speed cloud infrastructure.

> **Note**: This app has package name `dev.whispr.groq`, allowing it to be installed **side-by-side** on the same Android phone with the standard on-device Whispr AI (`dev.whispr.whispr_ai`) without conflicts.

---

## Why Groq Cloud API? (المسار الأول)

- **ساعات تتجدد تلقائياً للأبد**: لا تحاسبك المنصة برصيد ينفد، بل بحدود استخدام ساعية ويومية تتجدد تلقائياً باستمرار.
- **الحد الممنوح المجاني**: تمنحك ساعتين من الصوت في كل ساعة زمنية (بحد أقصى 2,000 طلب يومياً). لن تستهلك رصيدك طالما لا تتعدى هذا الحد اللحظي.
- **الموديلات المتوفرة**:
  - `whisper-large-v3`: موديل OpenAI الأقوى عالمياً بأعلى دقة لغوية، ووضوح للعامية المصرية والعربية، وتنسيق علامات الترقيم.
  - `whisper-large-v3-turbo`: الموديل فائق السرعة (0.15 ثانية) للكلام السريع والتفريغ اللحظي.
- **السرعة**: فائق السرعة؛ يفرغ التسجيل خلال ثانيتين فقط.
- **البطاقة البنكية**: مجاني بالكامل وبدون إدخال فيزا أو بيانات بنكية.
- **الجرامر والترقيم**: يضع علامات الترقيم العربية والإنجليزية (`،` `؛` `؟` `.` `?`) ويفهم سياق وبداية ونهاية الجمل باقتدار.

---

## Features

1. **Bilingual Obsidian Voice Keyboard (IME)**:
   - Full QWERTY English and Arabic layouts.
   - Serif italics, math/currency panel, emoji picker, clipboard manager.
   - Dedicated `● Push-to-Talk` button that records in high quality and transcribes through Groq API in 1-2 seconds.

2. **Smart Flying Dictation Button (الزر العائم الذكي)**:
   - Floats over any application (WhatsApp, Telegram, Notes, Chrome, etc.).
   - **Smart Auto-Appear**: Automatically appears when an editable text field is clicked or the keyboard opens, and hides when finished.
   - Automatically pastes or types the transcribed text with native punctuation directly into the focused field.

3. **Bilingual Grammar & Question Detection**:
   - Detects Arabic and Egyptian question words (`ازاي`, `ليه`, `فين`, `إيه`, `مين`, `كام`, `هل`, `كيف`, `متى`, etc.) and automatically appends `؟`.
   - Cleans speech fillers (`um`, `uh`, `يعني`, `أمم`).
   - Removes accidental stray dots and stutter repetitions.

4. **Custom Dictionary (Bilingual)**:
   - Shared dictionary rules to autocorrect specific phrases, brand names, and names.

---

## Getting Your Free Groq API Key

1. Go to [https://console.groq.com/keys](https://console.groq.com/keys)
2. Sign in with your Google or GitHub account (no credit card requested).
3. Click **Create API Key**, give it a name (e.g. `Whispr Pro`), and copy the key starting with `gsk_...`.
4. Open Whispr Pro on your phone, paste the key into the API Key box, and tap **Save** → **Test Key**.
5. You're ready to dictate with unlimited renewing hours!

---

## Build

```bash
flutter pub get
flutter analyze --fatal-infos
flutter test
flutter build apk --release # Output: build/app/outputs/flutter-apk/app-release.apk
```
