package com.example.banglaai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.graphics.Color;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_SPEECH = 100;
    private static final int REQ_MIC = 101;
    private EditText apiKey, question;
    private TextView answer, status;
    private Button askButton, micButton;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) { ttsReady = true; tts.setLanguage(new Locale("bn", "BD")); }
        });
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24)); root.setBackgroundColor(Color.rgb(245,248,251));
        scroll.addView(root);
        TextView title = new TextView(this); title.setText("🎙 BanglaAI"); title.setTextSize(30); title.setTextColor(Color.rgb(18,59,82)); title.setGravity(17); title.setTypeface(null, 1); root.addView(title, matchWrap());
        TextView sub = new TextView(this); sub.setText("বাংলা ও English AI Voice Assistant"); sub.setTextSize(16); sub.setGravity(17); sub.setPadding(0, dp(6),0,dp(18)); root.addView(sub, matchWrap());
        addLabel(root, "OpenAI API Key (আপনার নিজের key দিন)");
        apiKey = new EditText(this); apiKey.setSingleLine(true); apiKey.setHint("sk-..."); apiKey.setInputType(129); root.addView(apiKey, matchWrap());
        TextView warning = new TextView(this); warning.setText("নিরাপত্তা: API key কাউকে দেবেন না। এই নমুনা অ্যাপে key স্থায়ীভাবে সংরক্ষণ করা হয় না।"); warning.setTextSize(12); warning.setTextColor(Color.DKGRAY); warning.setPadding(0,dp(4),0,dp(12)); root.addView(warning, matchWrap());
        addLabel(root, "আপনার প্রশ্ন লিখুন অথবা মাইক্রোফোনে বলুন");
        question = new EditText(this); question.setHint("আপনি কী জানতে চান?"); question.setMinLines(2); question.setGravity(48); root.addView(question, matchWrap());
        micButton = new Button(this); micButton.setText("🎤 কথা বলুন / Speak"); root.addView(micButton, matchWrap());
        askButton = new Button(this); askButton.setText("AI-কে প্রশ্ন করুন"); root.addView(askButton, matchWrap());
        status = new TextView(this); status.setText("প্রস্তুত"); status.setTextSize(14); status.setPadding(0,dp(12),0,dp(6)); root.addView(status, matchWrap());
        TextView answerLabel = new TextView(this); answerLabel.setText("উত্তর"); answerLabel.setTextSize(20); answerLabel.setTypeface(null,1); answerLabel.setTextColor(Color.rgb(18,59,82)); root.addView(answerLabel, matchWrap());
        answer = new TextView(this); answer.setText("এখানে AI-এর উত্তর দেখা যাবে।"); answer.setTextSize(17); answer.setTextColor(Color.BLACK); answer.setPadding(dp(14),dp(14),dp(14),dp(14)); answer.setBackgroundColor(Color.WHITE); root.addView(answer, matchWrap());
        Button speak = new Button(this); speak.setText("🔊 উত্তর শুনুন"); root.addView(speak, matchWrap());
        setContentView(scroll);
        micButton.setOnClickListener(v -> startSpeech());
        askButton.setOnClickListener(v -> askAI());
        speak.setOnClickListener(v -> speakAnswer());
    }

    private void addLabel(LinearLayout root, String text) { TextView t = new TextView(this); t.setText(text); t.setTextSize(15); t.setTextColor(Color.rgb(40,55,65)); t.setPadding(0,dp(8),0,dp(4)); root.addView(t, matchWrap()); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }

    private void startSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { status.setText("এই ডিভাইসে Speech Recognition পাওয়া যায়নি।"); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC); return; }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "বাংলা বা ইংরেজিতে বলুন");
        try { startActivityForResult(intent, REQ_SPEECH); } catch (Exception e) { status.setText("ভয়েস ইনপুট চালু করা যায়নি। Speech Services ইনস্টল/সক্রিয় করুন।"); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spoken = results.get(0);
                question.setText(spoken);
                if (!handlePhoneCommand(spoken)) {
                    if (!apiKey.getText().toString().trim().isEmpty()) askAI();
                    else status.setText("প্রশ্ন লেখা হয়েছে। AI উত্তর পেতে API key দিন, অথবা AI-কে প্রশ্ন করুন চাপুন।");
                }
            }
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startSpeech();
        else if (requestCode == REQ_MIC) status.setText("কথা শুনতে Microphone permission দিতে হবে।");
    }


    // Basic, user-initiated phone actions. Sensitive actions open the relevant app
    // instead of silently performing them.
    private boolean handlePhoneCommand(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim();
        try {
            if (containsAny(s, "সেটিংস", "settings", "ফোন সেটিংস")) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                status.setText("ফোনের Settings খুলেছি।"); return true;
            }
            if (containsAny(s, "অ্যালার্ম", "alarm", "ঘড়ি", "ঘড়ি")) {
                Intent i = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                if (i.resolveActivity(getPackageManager()) != null) startActivity(i);
                else startActivity(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_MESSAGE, "BanglaAI alarm"));
                status.setText("Clock/Alarm অ্যাপ খুলেছি। সময়টি যাচাই করে সেট করুন।"); return true;
            }
            if (containsAny(s, "কল", "call", "ফোন কর", "ডায়াল", "ডায়াল", "dialer")) {
                startActivity(new Intent(Intent.ACTION_DIAL));
                status.setText("Dialer খুলেছি। নম্বর যাচাই করে কল করুন।"); return true;
            }
            if (containsAny(s, "ক্যামেরা", "camera", "ছবি তুল")) {
                Intent i = new Intent("android.media.action.IMAGE_CAPTURE");
                if (i.resolveActivity(getPackageManager()) != null) startActivity(i);
                else status.setText("ক্যামেরা অ্যাপ পাওয়া যায়নি।");
                return true;
            }
            if (containsAny(s, "ইউটিউব", "youtube")) {
                launchPackageOrWeb("com.google.android.youtube", "https://www.youtube.com");
                status.setText("YouTube খুলেছি।"); return true;
            }
            if (containsAny(s, "হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "whatsapp")) {
                launchPackageOrWeb("com.whatsapp", "https://www.whatsapp.com");
                status.setText("WhatsApp খুলেছি (ইনস্টল থাকলে)।"); return true;
            }
            if (containsAny(s, "ব্রাউজার", "ইন্টারনেট খোলো", "open browser", "chrome", "গুগল সার্চ", "search google")) {
                String query = raw.replaceAll("(?i)গুগল সার্চ|search google|ব্রাউজার|ইন্টারনেট খোলো|open browser|chrome", "").trim();
                String url = query.isEmpty() ? "https://www.google.com" : "https://www.google.com/search?q=" + Uri.encode(query);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                status.setText("ব্রাউজার খুলেছি।"); return true;
            }
        } catch (Exception e) {
            status.setText("কাজটি করা যায়নি: " + (e.getMessage() == null ? "অজানা সমস্যা" : e.getMessage()));
            return true;
        }
        return false;
    }

    private boolean containsAny(String s, String... terms) {
        for (String term : terms) if (s.contains(term)) return true;
        return false;
    }

    private void launchPackageOrWeb(String packageName, String fallbackUrl) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) startActivity(launch);
        else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
    }

    private void askAI() {
        String key = apiKey.getText().toString().trim(); String prompt = question.getText().toString().trim();
        if (key.isEmpty()) { status.setText("আগে আপনার API key লিখুন।"); return; }
        if (prompt.isEmpty()) { status.setText("প্রশ্ন লিখুন বা মাইক্রোফোনে বলুন।"); return; }
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(question.getWindowToken(), 0);
        askButton.setEnabled(false); micButton.setEnabled(false); status.setText("AI উত্তর তৈরি করছে…");
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                conn = (HttpURLConnection) url.openConnection(); conn.setRequestMethod("POST"); conn.setConnectTimeout(20000); conn.setReadTimeout(60000); conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + key); conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                JSONObject body = new JSONObject(); body.put("model", "gpt-4o-mini");
                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject(); system.put("role", "system"); system.put("content", "You are BanglaAI, a helpful voice assistant for Android. Reply in the same language as the user (Bangla or English). Keep spoken answers clear and concise. You cannot directly control the phone from chat; for opening apps or phone settings, the Android app handles a limited set of explicit commands. Never claim you performed an action unless it was actually done by the app."); messages.put(system);
                JSONObject user = new JSONObject(); user.put("role", "user"); user.put("content", prompt); messages.put(user); body.put("messages", messages);
                try (OutputStream os = conn.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder(); String line; while ((line = reader.readLine()) != null) response.append(line); reader.close();
                if (code < 200 || code >= 300) throw new Exception("API error " + code + ": " + response);
                JSONObject json = new JSONObject(response.toString()); String text = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                runOnUiThread(() -> { answer.setText(text); status.setText("উত্তর পাওয়া গেছে"); askButton.setEnabled(true); micButton.setEnabled(true); speakAnswer(); });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "অজানা সমস্যা" : e.getMessage();
                runOnUiThread(() -> { status.setText("সমস্যা: " + msg); askButton.setEnabled(true); micButton.setEnabled(true); });
            } finally { if (conn != null) conn.disconnect(); }
        });
    }
    private void speakAnswer() {
        if (!ttsReady) { status.setText("Text-to-Speech প্রস্তুত হচ্ছে। কিছুক্ষণ পরে চেষ্টা করুন।"); return; }
        String text = answer.getText().toString();
        if (text.isEmpty()) return;
        Locale locale = containsBangla(text) ? new Locale("bn", "BD") : Locale.US;
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US); status.setText("বাংলা TTS voice না থাকায় English voice চেষ্টা করা হচ্ছে। Voice data সেটিংস দেখুন।");
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "banglaai-answer");
    }
    private boolean containsBangla(String s) { for (int i=0;i<s.length();i++) if (s.charAt(i) >= '\u0980' && s.charAt(i) <= '\u09FF') return true; return false; }
    @Override protected void onDestroy() { super.onDestroy(); if (tts != null) { tts.stop(); tts.shutdown(); } executor.shutdownNow(); }
}
