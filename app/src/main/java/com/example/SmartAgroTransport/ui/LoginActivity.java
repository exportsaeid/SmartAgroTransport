package com.example.SmartAgroTransport.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout layoutUsername, layoutPassword;
    private TextInputEditText etUsername, etPassword;
    private TextView tvError;
    private Button btnLogin;

    private DatabaseHelper dbHelper;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // ویوها
//        layoutUsername = findViewById(R.id.layoutUsername);
//        layoutPassword = findViewById(R.id.layoutPassword);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);

        dbHelper = new DatabaseHelper(this);

        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                // اگر کاربر دکمه "استفاده از نام کاربری و رمز عبور" رو زد
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    showManualLoginFields();
                    return;
                }

                // برای خطاهای دیگر هم ورود دستی نشون بده
                showError("خطا در اثر انگشت: " + errString);
                showManualLoginFields();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(LoginActivity.this, "ورود با اثر انگشت موفق بود!", Toast.LENGTH_SHORT).show();
                goToMainScreen();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                showError("اثر انگشت شناخته نشد. دوباره امتحان کنید");
            }
        });

        // <<< مهم: دکمه منفی داریم + فقط اثر انگشت (نه قفل گوشی) >>>
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("ورود با اثر انگشت")
                .setSubtitle("انگشت خود را روی حسگر قرار دهید")
                .setNegativeButtonText("استفاده از نام کاربری و رمز عبور")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG) // فقط اثر انگشت
                .build();

        // چک کن آیا اثر انگشت موجوده
        checkBiometricAvailability();

        // دکمه ورود دستی
        btnLogin.setOnClickListener(v -> attemptManualLogin());
    }

    private void checkBiometricAvailability() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            // اثر انگشت موجوده — پنجره رو نشون بده
            biometricPrompt.authenticate(promptInfo);
        } else {
            // اثر انگشت موجود نیست — مستقیم ورود دستی
            showManualLoginFields();
            Toast.makeText(this, "اثر انگشت در دسترس نیست. از نام کاربری و رمز عبور استفاده کنید", Toast.LENGTH_LONG).show();
        }
    }

    private void showManualLoginFields() {
//        layoutUsername.setVisibility(View.VISIBLE);
//        layoutPassword.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE); // خطا رو پاک کن
    }

    private void attemptManualLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("نام کاربری و رمز عبور را وارد کنید");
            return;
        }

        String role = dbHelper.login(username, password);

        if (role != null) {
            Toast.makeText(this, "ورود موفق! خوش آمدید (" + role + ")", Toast.LENGTH_SHORT).show();
            goToMainScreen();
        } else {
            showError("نام کاربری یا رمز عبور اشتباه است");
        }
    }

    private void goToMainScreen() {
        Intent intent = new Intent(this, MainActivity.class); // یا InvoicePreviewActivity یا هر صفحه اصلی که داری
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}