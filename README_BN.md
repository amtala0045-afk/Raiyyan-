# BanglaAI Android Voice Assistant

## কী আছে
- বাংলা/ইংরেজি টেক্সট ইনপুট
- Android Speech Recognizer দিয়ে ভয়েস ইনপুট (ডিভাইসে ভাষা/সার্ভিস সমর্থনের ওপর নির্ভরশীল)
- OpenAI Chat Completions API দিয়ে অনলাইন উত্তর
- Android TextToSpeech দিয়ে উত্তর পড়ে শোনানো (বাংলা voice ইনস্টল থাকলে)

## Android Studio-তে চালানো
1. Android Studio ইনস্টল/খুলুন।
2. File > Open নির্বাচন করে এই `BanglaAI` ফোল্ডার খুলুন।
3. Gradle Sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন। JDK 17 এবং Android SDK Platform 35 লাগতে পারে।
4. ফোনে USB debugging চালু করে Run চাপুন, অথবা emulator ব্যবহার করুন।
5. অ্যাপ খুলে নিজের OpenAI API key দিন। API key অ্যাপের ভিতরে hard-code করবেন না।
6. ইন্টারনেট চালু রেখে প্রশ্ন লিখুন অথবা Speak চাপুন।

## APK বানানো
Android Studio-তে Build > Build Bundle(s) / APK(s) > Build APK(s) নির্বাচন করুন। সফল হলে `app/build/outputs/apk/debug/app-debug.apk` ফাইল তৈরি হবে। মেনুর নাম Android Studio সংস্করণভেদে সামান্য আলাদা হতে পারে।

## গুরুত্বপূর্ণ নিরাপত্তা
এই প্রজেক্টটি ডেমো/ব্যক্তিগত পরীক্ষার জন্য। APK-এর ভিতরে সরাসরি API key রাখা নিরাপদ নয়। অন্যদের বিতরণের জন্য একটি backend server ব্যবহার করুন, যেখানে API key গোপন থাকবে এবং rate limiting/billing controls থাকবে। API ব্যবহারে খরচ হতে পারে। API key ফোনে টাইপ করলে তা অ্যাপের চলমান session-এ ব্যবহৃত হয়, স্থায়ীভাবে সংরক্ষণ করা হয় না।

## সম্ভাব্য সমস্যা
- API error 401: API key ভুল/অকার্যকর।
- API error 429: quota/rate limit/billing সমস্যা হতে পারে।
- Voice recognition ব্যর্থ: ফোনের Speech Services, internet, microphone permission এবং ভাষা সেটিংস দেখুন।
- বাংলা voice না শোনা গেলে Android Settings > Text-to-speech output-এ Bengali voice data আছে কি না দেখুন।
