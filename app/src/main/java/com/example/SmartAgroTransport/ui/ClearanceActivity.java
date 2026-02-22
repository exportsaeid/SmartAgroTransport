package com.example.SmartAgroTransport.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import com.example.SmartAgroTransport.ui.Fragment.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.SmartAgroTransport.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ClearanceActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private com.example.SmartAgroTransport.ui.Fragment.InTransitFragment inTransitFragment;
    private ClearedFragment clearedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clearance);

        // Setup back button
        findViewById(R.id.backButtonClearance).setOnClickListener(v -> finish());

        // Initialize fragments
        inTransitFragment = new com.example.SmartAgroTransport.ui.Fragment.InTransitFragment();
        clearedFragment = new ClearedFragment();

        // Set default fragment
        loadFragment(inTransitFragment);

        // Setup bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // غیرفعال کردن tint پیش‌فرض Material Design
        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setItemTextColor(null);

        // تنظیم رنگ‌های اولیه برای تایتل‌ها
        setupInitialColors();

        // گوش دادن به تغییرات تب
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_in_transit) {
                loadFragment(inTransitFragment);
                updateTabColors(R.id.nav_in_transit);
                return true;
            } else if (item.getItemId() == R.id.nav_cleared) {
                loadFragment(clearedFragment);
                updateTabColors(R.id.nav_cleared);
                return true;
            }
            return false;
        });
    }

    private void setupInitialColors() {
        Menu menu = bottomNavigation.getMenu();
        MenuItem inTransitItem = menu.findItem(R.id.nav_in_transit);
        MenuItem clearedItem = menu.findItem(R.id.nav_cleared);

        if (inTransitItem != null) {
            // تایتل "در راه مرز" - نارنجی
            setMenuItemTitleColor(inTransitItem, "در راه مرز", Color.parseColor("#FF9800"));
        }

        if (clearedItem != null) {
            // تایتل "ترخیص شده" - سبز
            setMenuItemTitleColor(clearedItem, "ترخیص شده", Color.parseColor("#4CAF50"));
        }
    }

    private void updateTabColors(int selectedTabId) {
        Menu menu = bottomNavigation.getMenu();
        MenuItem inTransitItem = menu.findItem(R.id.nav_in_transit);
        MenuItem clearedItem = menu.findItem(R.id.nav_cleared);

        if (selectedTabId == R.id.nav_in_transit) {
            // تب "در راه مرز" انتخاب شده
            if (inTransitItem != null) {
                setMenuItemTitleColor(inTransitItem, "در راه مرز", Color.parseColor("#FF9800"));
            }

            if (clearedItem != null) {
                setMenuItemTitleColor(clearedItem, "ترخیص شده", Color.parseColor("#757575"));
            }
        } else if (selectedTabId == R.id.nav_cleared) {
            // تب "ترخیص شده" انتخاب شده
            if (inTransitItem != null) {
                setMenuItemTitleColor(inTransitItem, "در راه مرز", Color.parseColor("#757575"));
            }

            if (clearedItem != null) {
                setMenuItemTitleColor(clearedItem, "ترخیص شده", Color.parseColor("#4CAF50"));
            }
        }
    }

    // متد کمکی برای تنظیم رنگ تایتل
    private void setMenuItemTitleColor(MenuItem menuItem, String title, int color) {
        SpannableString spannableTitle = new SpannableString(title);
        spannableTitle.setSpan(
                new ForegroundColorSpan(color),
                0,
                spannableTitle.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
        );
        menuItem.setTitle(spannableTitle);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainerClearanceActivity, fragment);
        transaction.commit();
    }

    // متد برای رفرش تب ترخیص شده
    public void refreshClearedTab() {
        if (clearedFragment != null) {
            clearedFragment.refreshData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // رفرش فرگمنت جاری
        refreshCurrentFragment();
    }

    private void refreshCurrentFragment() {
        int selectedId = bottomNavigation.getSelectedItemId();

        if (selectedId == R.id.nav_in_transit && inTransitFragment != null) {
            inTransitFragment.refreshData();
        } else if (selectedId == R.id.nav_cleared && clearedFragment != null) {
            clearedFragment.refreshData();
        }
    }
}