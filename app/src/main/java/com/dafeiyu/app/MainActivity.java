package com.dafeiyu.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String[] LINES = {
        "哦鲸鲸在呢。",
        "今天也好好吃饭了吗？",
        "别熬太晚。",
        "有事就叫我。"
    };
    private int idx = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        final TextView tv = new TextView(this);
        tv.setText(LINES[0]);
        tv.setTextSize(22);
        tv.setGravity(Gravity.CENTER);

        Button btn = new Button(this);
        btn.setText("再说一句");
        btn.setOnClickListener(v -> {
            idx = (idx + 1) % LINES.length;
            tv.setText(LINES[idx]);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad;

        root.addView(tv);
        root.addView(btn, lp);
        setContentView(root);
    }
}
