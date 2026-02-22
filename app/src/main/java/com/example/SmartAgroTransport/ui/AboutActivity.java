package com.example.SmartAgroTransport.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.SmartAgroTransport.R;

import org.osmdroid.wms.BuildConfig;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // تنظیم تولبار
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("درباره ما");

        // نمایش اطلاعات
        setupAboutInfo();
    }

    private void setupAboutInfo() {
        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("نسخه " + BuildConfig.VERSION_NAME);

        // کلیک روی لوگو
        ImageView ivLogo = findViewById(R.id.iv_logo);
        ivLogo.setOnClickListener(v -> {
            // انیمیشن یا افکت خاص
            Toast.makeText(this, "فاکتور هوشمند کشاورزی", Toast.LENGTH_SHORT).show();
        });

        // دکمه‌های اشتراک‌گذاری
        findViewById(R.id.btn_share).setOnClickListener(v -> shareApp());

        // لینک‌های شبکه‌های اجتماعی
        findViewById(R.id.btn_instagram).setOnClickListener(v ->
                openSocialMedia("https://instagram.com/..."));
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "فاکتور هوشمند کشاورزی");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "اپلیکیشن فاکتور هوشمند کشاورزی رو نصب کن! \n" +
                        "https://play.google.com/store/apps/details?id=" + getPackageName());
        startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری"));
    }

    private void openSocialMedia(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "مشکل در باز کردن لینک", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}