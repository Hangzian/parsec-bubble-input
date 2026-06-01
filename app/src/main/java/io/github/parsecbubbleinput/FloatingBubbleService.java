package io.github.parsecbubbleinput;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingBubbleService extends Service {
    private static final String CHANNEL_ID = "parsec_bubble_input";
    private static final int NOTIFICATION_ID = 61;
    private static final String KEY_BUBBLE_X = "bubble_x";
    private static final String KEY_BUBBLE_Y = "bubble_y";
    private static final String KEY_LAST_TEXT = "last_text";
    private static final int INK = Color.rgb(18, 24, 38);
    private static final int MUTED = Color.rgb(95, 108, 128);
    private static final int BLUE = Color.rgb(41, 99, 235);
    private static final int GREEN = Color.rgb(13, 148, 111);
    private static final int LINE = Color.rgb(218, 225, 235);

    private WindowManager windowManager;
    private SharedPreferences prefs;
    private View bubbleView;
    private View panelView;
    private EditText input;
    private TextView status;
    private Switch autoSendSwitch;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Runnable autoSendRunnable = new Runnable() {
        @Override
        public void run() {
            if (input != null && autoSendSwitch != null && autoSendSwitch.isChecked()) {
                sendCurrentText(true);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startInForeground();
        if (!PermissionHelper.hasOverlayPermission(this)) {
            Toast.makeText(this, "未授权", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubbleView == null && panelView == null) showBubble();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        removeView(bubbleView);
        removeView(panelView);
        bubbleView = null;
        panelView = null;
        executor.shutdownNow();
        super.onDestroy();
    }

    private void startInForeground() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(io.github.parsecbubbleinput.R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Parsec Bubble Input is running");
            manager.createNotificationChannel(channel);
        }

        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        Notification notification = builder
                .setSmallIcon(io.github.parsecbubbleinput.R.drawable.ic_notification)
                .setContentTitle(getString(io.github.parsecbubbleinput.R.string.app_name))
                .setContentText("悬浮输入已开启")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void showBubble() {
        if (bubbleView != null || panelView != null) return;

        final ImageView bubble = new ImageView(this);
        bubble.setImageResource(io.github.parsecbubbleinput.R.drawable.ic_bubble);
        bubble.setPadding(0, 0, 0, 0);
        bubble.setElevation(dp(8));

        final WindowManager.LayoutParams params = overlayParams(dp(54), dp(54), true);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = prefs.getInt(KEY_BUBBLE_X, dp(18));
        params.y = prefs.getInt(KEY_BUBBLE_Y, dp(120));

        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int startX;
            private int startY;
            private float downRawX;
            private float downRawY;
            private boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(event.getRawX() - downRawX);
                        int dy = Math.round(event.getRawY() - downRawY);
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) dragging = true;
                        params.x = startX + dx;
                        params.y = startY + dy;
                        windowManager.updateViewLayout(bubble, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        prefs.edit().putInt(KEY_BUBBLE_X, params.x).putInt(KEY_BUBBLE_Y, params.y).apply();
                        if (!dragging) showPanel();
                        return true;
                    default:
                        return false;
                }
            }
        });

        try {
            windowManager.addView(bubble, params);
            bubbleView = bubble;
        } catch (RuntimeException error) {
            Toast.makeText(this, "悬浮窗受限", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    private void showPanel() {
        removeView(bubbleView);
        bubbleView = null;
        mainHandler.removeCallbacks(autoSendRunnable);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = Math.min(dp(352), Math.max(dp(280), metrics.widthPixels - dp(96)));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(6), dp(6), dp(6), dp(6));
        container.setBackground(roundRect(Color.WHITE, dp(28), Color.rgb(226, 232, 241)));
        container.setElevation(dp(16));
        container.setFocusableInTouchMode(true);
        container.setOnKeyListener(backToBubbleOnBack());

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton collapse = new ImageButton(this);
        collapse.setImageResource(io.github.parsecbubbleinput.R.drawable.ic_collapse);
        collapse.setBackground(oval(Color.rgb(241, 245, 249)));
        collapse.setPadding(dp(7), dp(7), dp(7), dp(7));
        collapse.setScaleType(ImageView.ScaleType.CENTER);
        collapse.setContentDescription("收起");
        collapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePanel();
            }
        });
        topRow.addView(collapse, new LinearLayout.LayoutParams(dp(36), dp(36)));

        input = new EditText(this);
        input.setMinLines(1);
        input.setMaxLines(2);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        input.setHint("输入文字");
        input.setTextSize(15);
        input.setTextColor(INK);
        input.setHintTextColor(Color.rgb(137, 148, 166));
        input.setPadding(dp(10), 0, dp(8), 0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setOnKeyListener(backToBubbleOnBack());
        input.setText(prefs.getString(KEY_LAST_TEXT, ""));
        if (input.getText() != null && input.getText().length() > 0) {
            input.setSelection(input.getText().length());
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scheduleAutoSend();
            }
        });
        topRow.addView(input, new LinearLayout.LayoutParams(
                0,
                dp(48),
                1
        ));

        ImageButton send = new ImageButton(this);
        send.setImageResource(io.github.parsecbubbleinput.R.drawable.ic_send);
        send.setBackground(oval(BLUE));
        send.setPadding(dp(11), dp(11), dp(11), dp(11));
        send.setScaleType(ImageView.ScaleType.CENTER);
        send.setContentDescription("发送");
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCurrentText(true);
            }
        });
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        sendParams.setMargins(dp(6), 0, 0, 0);
        topRow.addView(send, sendParams);

        container.addView(topRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(dp(2), dp(8), dp(2), 0);

        autoSendSwitch = new Switch(this);
        autoSendSwitch.setText("自动发送");
        autoSendSwitch.setChecked(prefs.getBoolean(MainActivity.KEY_AUTO_SEND, false));
        autoSendSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(MainActivity.KEY_AUTO_SEND, isChecked).apply();
                if (isChecked) {
                    scheduleAutoSend();
                } else {
                    mainHandler.removeCallbacks(autoSendRunnable);
                }
            }
        });
        actionRow.addView(autoSendSwitch, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));

        Button clipboard = compactButton("剪贴板发送", Color.rgb(241, 245, 249), INK, LINE);
        clipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendClipboardText();
            }
        });
        actionRow.addView(clipboard, new LinearLayout.LayoutParams(0, dp(42), 1));

        Button resend = compactButton("重发上次", Color.rgb(241, 245, 249), INK, LINE);
        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendLast();
            }
        });
        LinearLayout.LayoutParams resendParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        resendParams.setMargins(dp(4), 0, dp(4), 0);
        actionRow.addView(resend, resendParams);

        Button clear = compactButton("清空", Color.rgb(241, 245, 249), INK, LINE);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (input != null) input.setText("");
                mainHandler.removeCallbacks(autoSendRunnable);
                setStatus("已清空");
            }
        });
        container.addView(actionRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout utilityRow = new LinearLayout(this);
        utilityRow.setOrientation(LinearLayout.HORIZONTAL);
        utilityRow.setPadding(dp(2), dp(6), dp(2), 0);

        utilityRow.addView(clear, new LinearLayout.LayoutParams(0, dp(40), 1));

        Button close = compactButton("关闭", Color.rgb(255, 247, 237), Color.rgb(180, 83, 9), Color.rgb(253, 186, 116));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, dp(40), 1);
        closeParams.setMargins(dp(6), 0, 0, 0);
        utilityRow.addView(close, closeParams);

        container.addView(utilityRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        status = new TextView(this);
        status.setText("就绪");
        status.setTextSize(12);
        status.setTextColor(MUTED);
        status.setPadding(dp(8), dp(8), dp(8), dp(2));
        container.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        WindowManager.LayoutParams params = overlayParams(width, WindowManager.LayoutParams.WRAP_CONTENT, false);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(12);
        params.y = Math.max(dp(12), prefs.getInt(KEY_BUBBLE_Y, dp(120)) - dp(22));

        try {
            windowManager.addView(container, params);
            panelView = container;
        } catch (RuntimeException error) {
            Toast.makeText(this, "悬浮窗受限", Toast.LENGTH_SHORT).show();
            showBubble();
            return;
        }

        input.requestFocus();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void hidePanel() {
        mainHandler.removeCallbacks(autoSendRunnable);
        hideKeyboard();
        removeView(panelView);
        panelView = null;
        input = null;
        status = null;
        autoSendSwitch = null;
        showBubble();
    }

    private View.OnKeyListener backToBubbleOnBack() {
        return new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    hidePanel();
                    return true;
                }
                return false;
            }
        };
    }

    private void scheduleAutoSend() {
        mainHandler.removeCallbacks(autoSendRunnable);
        if (input == null || autoSendSwitch == null || !autoSendSwitch.isChecked()) return;
        if (input.getText().toString().trim().length() == 0) return;
        mainHandler.postDelayed(autoSendRunnable, 1500);
    }

    private void sendCurrentText(final boolean clearAfter) {
        if (input == null) return;
        final String text = input.getText().toString();
        if (text.trim().length() == 0) {
            setStatus("没有可发送的文字");
            return;
        }
        setStatus("发送中");
        post("/api/paste", text, new BridgeCallback() {
            @Override
            public void success() {
                prefs.edit().putString(KEY_LAST_TEXT, text).apply();
                if (clearAfter && input != null) input.setText("");
                setStatus("已发送");
                hidePanel();
            }

            @Override
            public void failure(String message) {
                setStatus("发送失败：" + message);
            }
        });
    }

    private void resendLast() {
        String last = prefs.getString(KEY_LAST_TEXT, "");
        if (last.trim().length() == 0) {
            setStatus("还没有可重发内容");
            return;
        }
        setStatus("重发中");
        post("/api/paste", last, new BridgeCallback() {
            @Override
            public void success() {
                setStatus("已重发");
            }

            @Override
            public void failure(String message) {
                setStatus("重发失败：" + message);
            }
        });
    }

    private void sendClipboardText() {
        String text = readClipboardText();
        if (text.trim().length() == 0) {
            setStatus("剪贴板为空");
            return;
        }
        if (input != null) {
            input.setText(text);
            input.setSelection(input.getText().length());
        }
        sendCurrentText(true);
    }

    private String readClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) return "";
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        return text == null ? "" : text.toString();
    }

    private void post(final String path, final String text, final BridgeCallback callback) {
        final String baseUrl = prefs.getString(MainActivity.KEY_SERVER_URL, "");
        final String token = prefs.getString(MainActivity.KEY_TOKEN, "");

        if (baseUrl.length() == 0 || token.length() == 0) {
            callback.failure("缺少连接信息");
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    String urlText = baseUrl + path;
                    URL url = new URL(urlText);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(4000);
                    connection.setReadTimeout(6000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("content-type", "application/json; charset=utf-8");
                    connection.setRequestProperty("x-parsec-bubble-token", token);
                    connection.setDoOutput(true);

                    byte[] body = ("{\"text\":\"" + jsonEscape(text) + "\"}").getBytes(StandardCharsets.UTF_8);
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(body);
                    outputStream.close();

                    int code = connection.getResponseCode();
                    String response = readStream(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
                    if (code >= 200 && code < 300 && response.contains("\"ok\":true")) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.success();
                            }
                        });
                    } else {
                        final String message = code + " " + response;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.failure(message);
                            }
                        });
                    }
                } catch (final Exception error) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.failure(error.getMessage() == null ? "网络错误" : error.getMessage());
                        }
                    });
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        });
    }

    private WindowManager.LayoutParams overlayParams(int width, int height, boolean notFocusable) {
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        if (notFocusable) flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        else flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return params;
    }

    private Button compactButton(String text, int background, int foreground, int stroke) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTextColor(foreground);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackground(roundRect(background, dp(8), stroke));
        return button;
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        params.setMargins(dp(2), dp(10), dp(2), dp(4));
        return params;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable roundRect(int color, int radius) {
        return roundRect(color, radius, Color.rgb(210, 218, 230));
    }

    private GradientDrawable roundRect(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private void setStatus(String message) {
        if (status != null) {
            status.setText(message);
            return;
        }
        if (message.contains("失败") || message.contains("没有") || message.contains("缺少")) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        if (input == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    private void removeView(View view) {
        if (view == null || windowManager == null) return;
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString("UTF-8");
    }

    private String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i += 1) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface BridgeCallback {
        void success();

        void failure(String message);
    }
}
