package com.example.SmartAgroTransport.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.SmartAgroTransport.R;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView imgWhatsapp = findViewById(R.id.imgWhatsapp);

        imgWhatsapp.setOnClickListener(v -> openWhatsapp());

        TextView imgWhatsapp1 = findViewById(R.id.imgWhatsapp1);

        imgWhatsapp1.setOnClickListener(v -> openWhatsapp());

        ImageView backButton = findViewById(R.id.backButtonSettings);
        backButton.setOnClickListener(v -> finish());
    }

    private void openWhatsapp() {

        String phoneNumber = "989010388244"; // شماره شما با کد کشور
        String message = "";

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                    "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message)
            ));
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this,
                    "واتساپ روی گوشی نصب نیست",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
