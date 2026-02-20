package com.example.SmartAgroTransport.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_invoice);

        // Setup back button
        findViewById(R.id.backButtonShipmen).setOnClickListener(v -> finish());

        // Initialize fragments
        readyFragment = new ReadyInvoicesFragment();
        completedFragment = new CompletedShipmentsFragment();

        // Set default fragment
        loadFragment(readyFragment);

        // Setup bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // ============ حل مشکل رنگ آیکن ============
        // این خط تینت پیشفرض Material Design را غیرفعال می‌کند
        bottomNavigation.setItemIconTintList(null);
        // این خط هم برای تایتل‌ها (اگر لازم باشد)
        bottomNavigation.setItemTextColor(null);
        // ==========================================

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_ready) {
                loadFragment(readyFragment);
                return true;
            } else if (item.getItemId() == R.id.nav_completed) {
                loadFragment(completedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    // Method to refresh completed tab from ready fragment
    public void refreshCompletedTab() {
        if (completedFragment != null) {
            completedFragment.refreshData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh both fragments when activity resumes
        if (readyFragment != null) {
            // readyFragment will refresh automatically in onResume
        }
        if (completedFragment != null && bottomNavigation.getSelectedItemId() == R.id.nav_completed) {
            completedFragment.refreshData();
        }
    }
}