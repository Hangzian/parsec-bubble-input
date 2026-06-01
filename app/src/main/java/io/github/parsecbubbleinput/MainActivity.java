package io.github.parsecbubbleinput;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    static final String PREFS = "parsec_bubble_input";
    static final String KEY_SERVER_URL = "server_url";
    static final String KEY_TOKEN = "token";
    static final String KEY_AUTO_SEND = "auto_send";

    private static final int BG = Color.rgb(246, 248, 252);
    private static final int INK = Color.rgb(18, 24, 38);
    private static final int MUTED = Color.rgb(95, 108, 128);
    private static final int BLUE = Color.rgb(41, 99, 235);
    private static final int GREEN = Color.rgb(13, 148, 111);
    private static final int LINE = Color.rgb(218, 225, 235);

    private EditText serverUrlInput;
    private EditText tokenInput;
    private Switch autoSendSwitch;
    private TextView statusText;
    private TextView permissionBadge;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOverlayStatus();
    }

    private void buildUi() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
            if (Build.VERSION.SDK_INT >= 23) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(22));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        root.addView(topBar());
        root.addView(space(18));
        root.addView(introPanel());
        root.addView(space(12));
        root.addView(connectionPanel());
        root.addView(space(12));
        root.addView(actionPanel());

        setContentView(scrollView);
        updateOverlayStatus();
    }

    private View topBar() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(io.github.parsecbubbleinput.R.drawable.ic_launcher);
        row.addView(appIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText(getString(io.github.parsecbubbleinput.R.string.app_name));
        title.setTextSize(24);
        title.setTextColor(INK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(title);

        row.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        permissionBadge = pill("检测中", Color.rgb(235, 240, 248), MUTED);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.setMargins(dp(12), 0, 0, 0);
        row.addView(permissionBadge, badgeParams);

        return row;
    }

    private View introPanel() {
        LinearLayout card = panel();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = sectionTitle("用于 Parsec 的远程输入助手");
        title.setTextSize(14);
        title.setTextColor(INK);
        card.addView(title);

        TextView body = new TextView(this);
        body.setText("填入 Mac bridge 的地址和口令，授权悬浮窗后即可在 Parsec 上方输入并发送文字。");
        body.setTextSize(13);
        body.setTextColor(MUTED);
        body.setLineSpacing(dp(2), 1.0f);
        card.addView(body);

        return card;
    }

    private View connectionPanel() {
        LinearLayout card = panel();
        card.setPadding(dp(16), dp(14), dp(16), dp(16));

        card.addView(sectionTitle("连接设置"));

        serverUrlInput = input("http://192.168.1.10:8765");
        serverUrlInput.setSingleLine(true);
        serverUrlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        serverUrlInput.setText(prefs.getString(KEY_SERVER_URL, getString(io.github.parsecbubbleinput.R.string.default_server_url)));
        card.addView(labeled("Mac / Relay 地址", serverUrlInput));

        tokenInput = input("Mac bridge 生成的访问口令");
        tokenInput.setSingleLine(true);
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        tokenInput.setText(prefs.getString(KEY_TOKEN, getString(io.github.parsecbubbleinput.R.string.default_token)));
        card.addView(labeled("访问口令", tokenInput));

        LinearLayout autoRow = new LinearLayout(this);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        autoRow.setGravity(Gravity.CENTER_VERTICAL);
        autoRow.setPadding(0, dp(6), 0, 0);

        TextView autoText = new TextView(this);
        autoText.setText("自动发送");
        autoText.setTextSize(16);
        autoText.setTextColor(INK);
        autoRow.addView(autoText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        autoSendSwitch = new Switch(this);
        autoSendSwitch.setChecked(prefs.getBoolean(KEY_AUTO_SEND, false));
        autoSendSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveSettings();
            }
        });
        autoRow.addView(autoSendSwitch);
        card.addView(autoRow);

        return card;
    }

    private View actionPanel() {
        LinearLayout card = panel();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        Button start = button("启动悬浮球", BLUE, Color.WHITE);
        start.setTextSize(17);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBubble();
            }
        });
        card.addView(start, fullWidth(dp(52), 0, 0, 0, dp(10)));

        LinearLayout secondary = new LinearLayout(this);
        secondary.setOrientation(LinearLayout.HORIZONTAL);

        Button overlayPermission = button("悬浮窗授权", Color.WHITE, BLUE);
        overlayPermission.setBackground(roundRect(Color.WHITE, dp(8), BLUE));
        overlayPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionHelper.hasOverlayPermission(MainActivity.this)) {
                    setStatus("已授权", true);
                } else {
                    requestOverlayPermission();
                }
            }
        });
        secondary.addView(overlayPermission, weightedButton(0, dp(6), dp(6), 0));

        Button stop = button("停止悬浮球", Color.WHITE, Color.rgb(190, 45, 45));
        stop.setBackground(roundRect(Color.WHITE, dp(8), Color.rgb(230, 184, 184)));
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, FloatingBubbleService.class));
                setStatus("已停止", false);
            }
        });
        secondary.addView(stop, weightedButton(dp(6), dp(6), 0, 0));
        card.addView(secondary);

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setTextColor(MUTED);
        statusText.setPadding(0, dp(14), 0, 0);
        card.addView(statusText);

        return card;
    }

    private LinearLayout panel() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundRect(Color.WHITE, dp(8), LINE));
        card.setElevation(dp(1));
        return card;
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(13);
        title.setTextColor(MUTED);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(8));
        return title;
    }

    private LinearLayout labeled(String label, View child) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, dp(8));
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(12);
        text.setTextColor(MUTED);
        text.setPadding(dp(2), 0, 0, dp(5));
        box.addView(text);
        box.addView(child, fullWidth(dp(48), 0, 0, 0, 0));
        return box;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextSize(15);
        editText.setTextColor(INK);
        editText.setHintTextColor(Color.rgb(145, 155, 171));
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(roundRect(Color.rgb(250, 252, 255), dp(8), LINE));
        return editText;
    }

    private Button button(String text, int background, int foreground) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(foreground);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(roundRect(background, dp(8), background));
        return button;
    }

    private TextView pill(String text, int background, int foreground) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(foreground);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(10), dp(5), dp(10), dp(5));
        view.setBackground(roundRect(background, dp(999), background));
        return view;
    }

    private View space(int height) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return space;
    }

    private LinearLayout.LayoutParams fullWidth(int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private LinearLayout.LayoutParams weightedButton(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private void startBubble() {
        saveSettings();
        if (!hasConnectionInfo()) {
            setStatus("请先填写地址和访问口令", false);
            return;
        }
        if (!PermissionHelper.hasOverlayPermission(this)) {
            setStatus("请先授权悬浮窗", false);
            return;
        }

        try {
            Intent intent = new Intent(this, FloatingBubbleService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            setStatus("运行中", true);
        } catch (Exception error) {
            setStatus("启动失败：" + safeMessage(error), false);
        }
    }

    private void saveSettings() {
        String rawUrl = serverUrlInput.getText().toString().trim();
        String token = tokenInput.getText().toString().trim();

        if (rawUrl.length() == 0) {
            prefs.edit()
                    .putString(KEY_SERVER_URL, "")
                    .putString(KEY_TOKEN, token)
                    .putBoolean(KEY_AUTO_SEND, autoSendSwitch.isChecked())
                    .apply();
            return;
        }

        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "http://" + rawUrl;
        }

        try {
            Uri uri = Uri.parse(rawUrl);
            if (uri.getScheme() != null && uri.getEncodedAuthority() != null) {
                rawUrl = uri.getScheme() + "://" + uri.getEncodedAuthority();
            }
        } catch (Exception ignored) {
        }

        while (rawUrl.endsWith("/")) rawUrl = rawUrl.substring(0, rawUrl.length() - 1);
        serverUrlInput.setText(rawUrl);
        tokenInput.setText(token);

        prefs.edit()
                .putString(KEY_SERVER_URL, rawUrl)
                .putString(KEY_TOKEN, token)
                .putBoolean(KEY_AUTO_SEND, autoSendSwitch.isChecked())
                .apply();
    }

    private boolean hasConnectionInfo() {
        return prefs.getString(KEY_SERVER_URL, "").trim().length() > 0
                && prefs.getString(KEY_TOKEN, "").trim().length() > 0;
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void updateOverlayStatus() {
        boolean granted = PermissionHelper.hasOverlayPermission(this);
        if (permissionBadge != null) {
            permissionBadge.setText(granted ? "已授权" : "未授权");
            permissionBadge.setTextColor(granted ? GREEN : Color.rgb(174, 89, 0));
            permissionBadge.setBackground(roundRect(
                    granted ? Color.rgb(224, 246, 239) : Color.rgb(255, 239, 214),
                    dp(999),
                    granted ? Color.rgb(224, 246, 239) : Color.rgb(255, 239, 214)
            ));
        }
        if (statusText == null) return;
        if (!granted) {
            setStatus("请授权悬浮窗", false);
        } else if (!hasConnectionInfo()) {
            setStatus("请填写 Mac / Relay 地址和访问口令", false);
        } else {
            setStatus("已就绪", true);
        }
    }

    private void setStatus(String message, boolean positive) {
        if (statusText == null) return;
        statusText.setText(message);
        statusText.setTextColor(positive ? GREEN : MUTED);
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.length() == 0 ? error.getClass().getSimpleName() : message;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable roundRect(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
