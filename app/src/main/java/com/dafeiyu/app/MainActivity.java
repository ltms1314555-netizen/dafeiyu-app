package com.dafeiyu.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String BASE = "http://127.0.0.1:3090";

    private static final int BG = 0xFF141416;
    private static final int HEAD = 0xFF1B1B1E;
    private static final int BUBBLE_HER = 0xFF2C2C2E;
    private static final int BUBBLE_ME = 0xFF3E9C5E;
    private static final int TEXT_HER = 0xFFECECEC;
    private static final int TEXT_MUTE = 0xFF8A8A8E;

    private LinearLayout list;
    private ScrollView scroll;
    private EditText input;
    private TextView send;
    private TextView status;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private int density;
    private Bitmap avatarHer;
    private long lastStamp = 0;
    private boolean waiting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = (int) getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // ---- 顶栏 ----
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(HEAD);
        bar.setPadding(dip(16), dip(14), dip(16), dip(14));

        ImageView head = new ImageView(this);
        head.setImageBitmap(roundAvatar(loadAvatar(), dip(40)));
        bar.addView(head, new LinearLayout.LayoutParams(dip(40), dip(40)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dip(10), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("大肥鱼");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        status = new TextView(this);
        status.setText("连接中…");
        status.setTextColor(TEXT_MUTE);
        status.setTextSize(11);
        titles.addView(title);
        titles.addView(status);
        bar.addView(titles);
        root.addView(bar);

        // ---- 消息区 ----
        scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dip(10), dip(12), dip(10), dip(12));
        scroll.addView(list);
        root.addView(scroll);

        // ---- 输入区 ----
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dip(10), dip(8), dip(10), dip(10));
        bottom.setBackgroundColor(HEAD);

        input = new EditText(this);
        input.setHint("说点什么…");
        input.setHintTextColor(0xFF6E6E73);
        input.setTextColor(Color.WHITE);
        input.setTextSize(16);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(0xFF26262A);
        inBg.setCornerRadius(dip(20));
        input.setBackground(inBg);
        input.setPadding(dip(16), dip(10), dip(16), dip(10));
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        send = new TextView(this);
        send.setText("发送");
        send.setTextColor(Color.WHITE);
        send.setTextSize(15);
        send.setGravity(Gravity.CENTER);
        GradientDrawable sendBg = new GradientDrawable();
        sendBg.setColor(BUBBLE_ME);
        sendBg.setCornerRadius(dip(20));
        send.setBackground(sendBg);
        send.setPadding(dip(18), dip(10), dip(18), dip(10));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sendLp.leftMargin = dip(8);

        bottom.addView(input);
        bottom.addView(send, sendLp);
        root.addView(bottom);

        send.setOnClickListener(v -> submit());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit();
                return true;
            }
            return false;
        });

        setContentView(root);
        updateSendEnabled();
        loadHistory();
    }

    private int dip(int dp) {
        return (int) (dp * density + 0.5f);
    }

    // ---------------- 素材 ----------------

    private Bitmap loadAvatar() {
        if (avatarHer == null) {
            avatarHer = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_her);
        }
        return avatarHer;
    }

    private Bitmap roundAvatar(Bitmap src, int size) {
        if (src == null) return null;
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rect = new RectF(0, 0, size, size);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(rect, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, new Rect(0, 0, src.getWidth(), src.getHeight()), rect, paint);
        return out;
    }

    // ---------------- 消息行 ----------------

    private View herRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        ImageView av = new ImageView(this);
        av.setImageBitmap(roundAvatar(loadAvatar(), dip(38)));
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dip(38), dip(38));
        avLp.topMargin = dip(4);
        row.addView(av, avLp);

        TextView bubble = makeBubble(text, false);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.leftMargin = dip(8);
        bLp.topMargin = dip(8);
        row.addView(bubble, bLp);

        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        // 双击她的头像 = 戳她一下
        GestureDetector detector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        pokeHer();
                        return true;
                    }
                });
        av.setOnTouchListener((v, event) -> detector.onTouchEvent(event));

        return row;
    }

    private View myRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView bubble = makeBubble(text, true);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = dip(8);
        bLp.rightMargin = dip(8);
        row.addView(bubble, bLp);

        TextView av = new TextView(this);
        av.setText("我");
        av.setTextColor(Color.WHITE);
        av.setTextSize(14);
        av.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xFF44444A);
        av.setBackground(bg);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dip(38), dip(38));
        avLp.topMargin = dip(4);
        row.addView(av, avLp);

        return row;
    }

    private TextView makeBubble(String text, boolean mine) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(mine ? Color.WHITE : TEXT_HER);
        tv.setLineSpacing(dip(3), 1f);
        tv.setPadding(dip(14), dip(10), dip(14), dip(10));
        tv.setTextIsSelectable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dip(16));
        bg.setColor(mine ? BUBBLE_ME : BUBBLE_HER);
        tv.setBackground(bg);

        int max = (int) (getResources().getDisplayMetrics().widthPixels * 0.68);
        tv.setMaxWidth(max);
        return tv;
    }

    private void stamp(long at) {
        if (at - lastStamp < 5 * 60 * 1000L && lastStamp != 0) return;
        lastStamp = at;
        TextView tv = new TextView(this);
        tv.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(at)));
        tv.setTextColor(TEXT_MUTE);
        tv.setTextSize(11);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dip(14), 0, dip(2));
        list.addView(tv);
    }

    private void addMessage(String text, boolean mine, long at) {
        stamp(at);
        if (mine) list.addView(myRow(text));
        else {
            list.addView(herRow(text));
            lastHer = text;
        }
        scrollDown();
    }

    private String lastHer = "";

    private void scrollDown() {
        scroll.postDelayed(() -> scroll.fullScroll(View.FOCUS_DOWN), 60);
    }

    private void setStatus(String text) {
        ui.post(() -> status.setText(text));
    }

    private void updateSendEnabled() {
        send.setAlpha(waiting ? 0.5f : 1f);
    }

    // ---------------- 戳一戳 ----------------

    private void pokeHer() {
        Toast.makeText(this, "你戳了戳她", Toast.LENGTH_SHORT).show();
        // 温柔点的做法：把这一下也告诉她，让她自己决定回不回
        new Thread(() -> {
            try {
                post("/send", "{\"text\":" + JSONObject.quote("（你戳了戳我）") + "}");
            } catch (Exception ignored) {
            }
        }).start();
    }

    // ---------------- 发送 ----------------

    private void submit() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty() || waiting) return;
        input.setText("");
        addMessage(text, true, System.currentTimeMillis());

        waiting = true;
        updateSendEnabled();
        setStatus("她在打字…");

        final View typing = typingRow();
        list.addView(typing);
        scrollDown();

        new Thread(() -> {
            String reply;
            boolean ok = true;
            try {
                String raw = post("/send", "{\"text\":" + JSONObject.quote(text) + "}");
                JSONObject obj = new JSONObject(raw);
                ok = obj.optBoolean("ok", false);
                reply = ok ? clean(obj.optString("reply", "")) : "（" + obj.optString("message", "没回我") + "）";
            } catch (Exception e) {
                ok = false;
                reply = "连不上她：" + e.getMessage();
            }
            final String out = reply;
            final boolean good = ok;
            ui.post(() -> {
                list.removeView(typing);
                addMessage(out, false, System.currentTimeMillis());
                waiting = false;
                updateSendEnabled();
                setStatus(good ? "在呢" : "连接有问题");
            });
        }).start();
    }

    private View typingRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        ImageView av = new ImageView(this);
        av.setImageBitmap(roundAvatar(loadAvatar(), dip(38)));
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dip(38), dip(38));
        avLp.topMargin = dip(4);
        row.addView(av, avLp);

        TextView bubble = makeBubble("● ● ●", false);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.leftMargin = dip(8);
        bLp.topMargin = dip(8);
        row.addView(bubble, bLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);
        return row;
    }

    /** 去掉 QQ 用的表情 / 语音标记 */
    private static String clean(String text) {
        if (text == null) return "";
        String out = text
                .replaceAll("<face:[^>]*>", "")
                .replaceAll("<sticker:[^>]*>", "")
                .replaceAll("<[a-zA-Z]+:[^>]*>", "")
                .replace("<voice>", "")
                .replace("</voice>", "")
                .replace("[沉默]", "")
                .trim();
        return out.isEmpty() ? "（她这次只发了个表情）" : out;
    }

    // ---------------- 历史 ----------------

    private void loadHistory() {
        new Thread(() -> {
            try {
                String body = get("/history?limit=60");
                JSONObject obj = new JSONObject(body);
                JSONArray arr = obj.optJSONArray("list");
                ui.post(() -> {
                    if (arr != null && arr.length() > 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String text = clean(item.optString("text", ""));
                            boolean mine = "user".equals(item.optString("role", ""));
                            long at = item.optLong("at", System.currentTimeMillis());
                            if (!text.isEmpty()) addMessage(text, mine, at);
                        }
                    } else {
                        addMessage("我在呢。", false, System.currentTimeMillis());
                    }
                    setStatus("在呢");
                    scrollDown();
                });
            } catch (Exception e) {
                ui.post(() -> {
                    setStatus("连不上引擎");
                    addMessage("我这边没连上。可能是引擎没跑起来，去 DSH 里看一眼。", false, System.currentTimeMillis());
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
