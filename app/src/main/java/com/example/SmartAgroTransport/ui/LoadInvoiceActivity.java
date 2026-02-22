package com.example.SmartAgroTransport.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.ui.Fragment.CompletedShipmentsFragment;
import com.example.SmartAgroTransport.ui.Fragment.ReadyInvoicesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LoadInvoiceActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private ReadyInvoicesFragment readyFragment;
    private CompletedShipmentsFragment completedFragment;
    private static final String TAG_READY = "ready_fragment";
    private static final String TAG_COMPLETED = "completed_fragment";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_invoice);

        // Setup back button
        findViewById(R.id.backButtonShipmen).setOnClickListener(v -> finish());

        setupFragments(savedInstanceState);
        setupBottomNavigation();
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            readyFragment = new ReadyInvoicesFragment();
            completedFragment = new CompletedShipmentsFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, completedFragment, TAG_COMPLETED)
                    .hide(completedFragment)
                    .add(R.id.fragmentContainer, readyFragment, TAG_READY)
                    .commit();
        } else {
            readyFragment = (ReadyInvoicesFragment) getSupportFragmentManager()
                    .findFragmentByTag(TAG_READY);
            completedFragment = (CompletedShipmentsFragment) getSupportFragmentManager()
                    .findFragmentByTag(TAG_COMPLETED);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // استفاده از رنگ مناسب به جای null
        ColorStateList iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.bottom_nav_icon_color));
        bottomNavigation.setItemIconTintList(null);

        // تنظیم رنگ متن
        ColorStateList textColor = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.bottom_nav_text_color));
        bottomNavigation.setItemTextColor(null);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_ready) {
                loadFragment(readyFragment);
                return true;
            } else if (itemId == R.id.nav_completed) {
                loadFragment(completedFragment);
                return true;
            }
            return false;
        });

        // انتخاب پیش‌فرض
        bottomNavigation.setSelectedItemId(R.id.nav_ready);
    }

    private void loadFragment(Fragment fragment) {
        if (fragment == null) return;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (fragment == readyFragment) {
            transaction.hide(completedFragment).show(readyFragment);
        } else {
            transaction.hide(readyFragment).show(completedFragment);
        }

        transaction.commit();
    }

    public void refreshCompletedTab() {
        if (completedFragment != null) {
            completedFragment.refreshData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // به‌روزرسانی با تاخیر برای جلوگیری از لگ
        if (completedFragment != null && bottomNavigation.getSelectedItemId() == R.id.nav_completed) {
            mainHandler.postDelayed(() -> {
                if (!isFinishing() && completedFragment != null) {
                    completedFragment.refreshData();
                }
            }, 150);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null); // پاک کردن callback‌ها
    }
}