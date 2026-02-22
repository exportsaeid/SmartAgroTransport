package com.example.SmartAgroTransport.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.adapter.InvoiceListAdapter;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.Invoice;

import java.util.ArrayList;
import java.util.List;

public class InvoiceListActivity extends AppCompatActivity {

    private RecyclerView recyclerInvoices;
    private TextView tvEmpty;
    private SearchView searchView;

    private InvoiceListAdapter adapter;
    private DatabaseHelper dbHelper;

    private List<Invoice> allInvoices = new ArrayList<>();
    private List<Invoice> filteredInvoices = new ArrayList<>();
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_list);

        // ============ تغییر قطعی ============
        // برای لیست فاکتورهای ذخیره شده، همیشه buttonType = 2 (ویرایش)
        final int buttonType = 2;
        // ====================================

        backButton = findViewById(R.id.backButtonInvoice);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.e("InvoiceListActivity", "backButton is null!");
        }

        recyclerInvoices = findViewById(R.id.recyclerInvoices);
        tvEmpty = findViewById(R.id.tvEmpty);
        searchView = findViewById(R.id.searchView);

        dbHelper = new DatabaseHelper(this);
        recyclerInvoices.setLayoutManager(new LinearLayoutManager(this));

        // ایجاد آداپتر با buttonType = 2 (ویرایش)
        adapter = new InvoiceListAdapter(filteredInvoices, new InvoiceListAdapter.OnInvoiceClickListener() {
            @Override
            public void onEdit(Invoice invoice) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("edit_invoice", invoice);
                // برای سازگاری با کدهای قدیمی، buttonType = 2 هم می‌فرستیم
                resultIntent.putExtra("button_type", 2);
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onDelete(long invoiceId) {
                // حذف در آداپتر انجام می‌شود
            }
        }, dbHelper, buttonType); // buttonType = 2

        recyclerInvoices.setAdapter(adapter);

        loadInvoices();

        // جستجو
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterInvoices(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterInvoices(newText);
                return true;
            }
        });

        // لاگ برای اطمینان
        Log.d("InvoiceListActivity", "آداپتر با buttonType = " + buttonType + " ایجاد شد");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void loadInvoices() {
        allInvoices = dbHelper.getAllInvoices();
        filteredInvoices.clear();
        filteredInvoices.addAll(allInvoices);
        adapter.setInvoices(filteredInvoices);
        updateEmptyView();
    }

    private void filterInvoices(String query) {
        query = query.trim().toLowerCase();
        filteredInvoices.clear();

        if (query.isEmpty()) {
            filteredInvoices.addAll(allInvoices);
        } else {
            for (Invoice invoice : allInvoices) {
                boolean matches = false;

                String customerName = invoice.getCustomerName();
                if (customerName != null && customerName.toLowerCase().contains(query)) {
                    matches = true;
                }

                String date = invoice.getDate();
                if (date != null && date.contains(query)) {
                    matches = true;
                }

                String invoiceNumber = invoice.getInvoiceNumber();
                if (invoiceNumber != null && invoiceNumber.toLowerCase().contains(query)) {
                    matches = true;
                }

                if (String.valueOf(invoice.getId()).contains(query)) {
                    matches = true;
                }

                if (matches) {
                    filteredInvoices.add(invoice);
                }
            }
        }

        adapter.setInvoices(filteredInvoices);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredInvoices.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerInvoices.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerInvoices.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvoices();
    }
}