package com.dafeiyu.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final String BASE = "http://127.0.0.1:3090";

    private LinearLayout list;
    private ScrollView scroll;
    private EditText input;
    private Button send;
    private TextView status;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private int density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = (int) getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F6F4F1"));

        // 顶部标题条
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#2B2B2B"));
        bar.setPadding(dip(16), dip(12), dip(16), dip(10));
        TextView title = new TextView(this);
        title.setText("大肥鱼");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        status = new TextView(this);
        status.setText("连接中…");
        status.setTextColor(Color.parseColor("#B9B9B9"));
        status.setTextSize(11);
        bar.addView(title);
        bar.addView(status);
        root.addView(bar);

        // 消息区
        scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dip(10), dip(10), dip(10), dip(10));
        scroll.addView(list);
        root.addView(scroll);

        // 输入区
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(dip(8), dip(8), dip(8), dip(8));
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setBackgroundColor(Color.parseColor("#FFFFFF"));

        input = new EditText(this);
        input.setHint("跟她说点什么…");
        input.setTextSize(16);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setBackgroundColor(Color.parseColor("#F1EFEC"));
        input.setPadding(dip(12), dip(10), dip(12), dip(10));
        input.setMaxLines(4);
        input.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        send = new Button(this);
        send.setText("发送");
        send.setAllCaps(false);
        send.setOnClickListener(v -> submit());

        bottom.addView(input);
        bottom.addView(send, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(bottom);

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit();
                return true;
            }
            return false;
        });

        setContentView(root);
        loadHistory();
    }

    private int dip(int dp) {
        return (int) (dp * density + 0.5f);
    }

    // ---------------- UI ----------------

    private TextView bubble(String text, boolean mine, boolean placeholder) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(mine ? Color.WHITE : Color.parseColor("#222222"));
        tv.setPadding(dip(12), dip(9), dip(12), dip(9));
        tv.setTextIsSelectable(!placeholder);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dip(14));
        bg.setColor(mine ? Color.parseColor("#3D7BF7")
                : placeholder ? Color.parseColor("#EDEAE6") : Color.WHITE);
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        lp.topMargin = dip(8);
        lp.leftMargin = mine ? dip(48) : 0;
        lp.rightMargin = mine ? 0 : dip(48);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void addMessage(String text, boolean mine) {
        list.addView(bubble(text, mine, false));
        scrollDown();
    }

    private void scrollDown() {
        scroll.postDelayed(() -> scroll.fullScroll(View.FOCUS_DOWN), 60);
    }

    private void setStatus(String text) {
        ui.post(() -> status.setText(text));
    }

    // ---------------- 发送 ----------------

    private void submit() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        addMessage(text, true);

        final TextView typing = bubble("…", false, true);
        list.addView(typing);
        scrollDown();
        send.setEnabled(false);
        setStatus("她在打字…");

        new Thread(() -> {
            String reply;
            boolean ok = true;
            try {
                reply = post("/send", "{\"text\":" + JSONObject.quote(text) + "}");
                JSONObject obj = new JSONObject(reply);
                ok = obj.optBoolean("ok", false);
                reply = ok ? obj.optString("reply", "") : "（" + obj.optString("message", "没回我") + "）";
                if (reply.trim().isEmpty()) reply = "（她这次没说话）";
            } catch (Exception e) {
                ok = false;
                reply = "连不上她：" + e.getMessage();
            }
            final String out = reply;
            final boolean good = ok;
            ui.post(() -> {
                list.removeView(typing);
                addMessage(out, false);
                send.setEnabled(true);
                setStatus(good ? "在呢" : "连接有问题");
            });
        }).start();
    }

    // ---------------- 历史 ----------------

    private void loadHistory() {
        new Thread(() -> {
            try {
                String body = get("/history?limit=80");
                JSONObject obj = new JSONObject(body);
                JSONArray arr = obj.optJSONArray("list");
                ui.post(() -> {
                    if (arr != null && arr.length() > 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String text = item.optString("text", "");
                            boolean mine = "user".equals(item.optString("role", ""));
                            if (!text.isEmpty()) addMessage(text, mine);
                        }
                    } else {
                        addMessage("我在呢。", false);
                    }
                    setStatus("在呢");
                    scrollDown();
                });
            } catch (Exception e) {
                ui.post(() -> {
                    setStatus("连不上引擎（" + e.getMessage() + "）");
                    addMessage("我这边没连上。可能是引擎没跑起来，去 DSH 里看一眼。", false);
                });
            }
        }).start();
    }

    // ---------------- HTTP ----------------

    private String get(String path) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE + path).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(20000);
        conn.setRequestMethod("GET");
        return read(conn);
    }

    private String post(String path, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE + path).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(310000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return read(conn);
    }

    private String read(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) throw new Exception("HTTP " + code);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
