package com.example.SmartAgroTransport.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.SmartAgroTransport.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String PHONE_NUMBER = "989010388244"; // شماره پشتیبانی (بدون صفر اول)
    private static final String WHATSAPP_NUMBER = "989010388244"; // شماره واتساپ
    private static final String EMAIL_ADDRESS = "nobarexport@gmail.com"; // ایمیل پشتیبانی

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
    }

    private void initViews() {
        // آیکون واتساپ
        ImageView imgWhatsapp = findViewById(R.id.imgWhatsapp);
        if (imgWhatsapp != null) {
            imgWhatsapp.setOnClickListener(v -> openWhatsapp());
        }

        // متن "ارتباط با پشتیبانی"
        TextView tvSupportText = findViewById(R.id.tvSupportText);
        if (tvSupportText != null) {
            tvSupportText.setOnClickListener(v -> openWhatsapp());
        }

        // شماره تلفن (برای تماس)
        TextView tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        if (tvPhoneNumber != null) {
            tvPhoneNumber.setOnClickListener(v -> makePhoneCall());
        }

        // ایمیل پشتیبانی
        TextView tvEmail = findViewById(R.id.tvEmail);
        if (tvEmail != null) {
            tvEmail.setOnClickListener(v -> sendEmail());
        }

        // دکمه برگشت
        ImageView backButton = findViewById(R.id.backButtonSettings);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void openWhatsapp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + WHATSAPP_NUMBER + "?text=" + Uri.encode("")));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "❌ واتساپ روی گوشی نصب نیست یا خطایی رخ داد",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void makePhoneCall() {
        // استفاده از ACTION_DIAL (فقط شماره‌گیری بدون برقراری خودکار تماس)
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + PHONE_NUMBER));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "📞 امکان برقراری تماس وجود ندارد",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + EMAIL_ADDRESS)); // مستقیم به آدرس ایمیل
        intent.putExtra(Intent.EXTRA_SUBJECT, "پیام از طرف کاربر اپلیکیشن");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "✉️ اپلیکیشن ایمیل یافت نشد",
                    Toast.LENGTH_SHORT).show();
        }
    }
}