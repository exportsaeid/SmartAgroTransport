package com.example.SmartAgroTransport.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.adapter.ProfitReportAdapter;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.BuyerDelivery;
import com.example.SmartAgroTransport.model.Clearance;
import com.example.SmartAgroTransport.model.IraqiHandover;
import com.example.SmartAgroTransport.model.ProfitItem;
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.utils.NumberFormatter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfitReportActivity extends AppCompatActivity {

    private RecyclerView recyclerProfit;
    private SearchView searchViewProfit;
    private TextView tvTotalProfit, tvEmpty;

    private ProfitReportAdapter adapter;
    private DatabaseHelper dbHelper;
    private ImageView backButtonProfit; // اضافه شد
    private List<ProfitItem> allProfitItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profit_report);
        // دکمه برگشت
        backButtonProfit = findViewById(R.id.backButtonProfit);
        backButtonProfit.setOnClickListener(v -> finish()); // کار می‌کنه!

        recyclerProfit = findViewById(R.id.recyclerProfit);
        searchViewProfit = findViewById(R.id.searchViewProfit);
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        tvEmpty = findViewById(R.id.tvEmptyProfit);

        dbHelper = new DatabaseHelper(this);

        setupAdapter();
        loadProfitData();

        searchViewProfit.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterData(newText);
                return true;
            }
        });
    }

    private void setupAdapter() {
        adapter = new ProfitReportAdapter();
        recyclerProfit.setLayoutManager(new LinearLayoutManager(this));
        recyclerProfit.setAdapter(adapter);
    }

    private long calculateInvoiceTotalFromItems(long invoiceId) {
        long total = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT weight * unit_price FROM invoice_items WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        if (cursor.moveToFirst()) {
            do {
                total += cursor.getLong(0);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return total;
    }

    private void loadProfitData() {
        allProfitItems.clear();
        long grandTotalProfit = 0;

        List<Shipment> allShipments = dbHelper.getAllShipments();

        for (Shipment shipment : allShipments) {
            long invoiceId = shipment.getInvoiceId();

            // محاسبه مبلغ فاکتور از آیتم‌ها
            long invoiceAmount = calculateInvoiceTotalFromItems(invoiceId);
            if (invoiceAmount == 0) {
                continue;
            }

            long iranianTransport = shipment.getTransportCost();

            long iranianClearance = 0;
            Clearance clearance = dbHelper.getClearanceByShipmentId(shipment.getId());
            if (clearance != null) {
                iranianClearance = clearance.getClearanceCost();
            }

            long iraqiTotalCost = 0;
            IraqiHandover iraqi = dbHelper.getIraqiHandoverByShipmentId(shipment.getId());
            if (iraqi != null) {
                iraqiTotalCost = iraqi.getClearanceCost();
            }

            BuyerDelivery buyer = dbHelper.getBuyerDeliveryByShipmentId(shipment.getId());
            if (buyer == null) {
                continue;
            }

            long receivedFromBuyer = buyer.getReceivedAmount();

            long totalCosts = invoiceAmount + iranianTransport + iranianClearance + iraqiTotalCost;

            long profit = receivedFromBuyer - totalCosts;
            grandTotalProfit += profit;

            ProfitItem item = new ProfitItem();
            item.setInvoiceNumber(shipment.getInvoiceNumber() != null ? shipment.getInvoiceNumber() : "F" + invoiceId);
            item.setInvoiceAmount(invoiceAmount);
            item.setIranianTransport(iranianTransport);
            item.setIranianClearance(iranianClearance);
            item.setIraqiTotalCost(iraqiTotalCost);
            item.setReceivedAmount(receivedFromBuyer);
            item.setProfit(profit);
            item.setBuyerName(buyer.getBuyerName() != null ? buyer.getBuyerName() : "-");

            allProfitItems.add(item);
        }

        adapter.setData(allProfitItems);
        updateTotalProfit(grandTotalProfit);

        if (allProfitItems.isEmpty()) {
            tvEmpty.setText("هیچ فاکتور تکمیل شده‌ای (تحویل به خریدار) وجود ندارد");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerProfit.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerProfit.setVisibility(View.VISIBLE);
        }
    }

    private void updateTotalProfit(long total) {
        String totalText = "جمع کل سود/ضرر: " + NumberFormatter.formatNumber(total) + " تومان";
        tvTotalProfit.setText(totalText);
        tvTotalProfit.setTextColor(total >= 0 ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));
    }

    private void filterData(String query) {
        query = query.trim().toLowerCase(Locale.getDefault());

        if (query.isEmpty()) {
            adapter.setData(allProfitItems);
            calculateTotalFromList(allProfitItems);
            return;
        }

        List<ProfitItem> filtered = new ArrayList<>();
        for (ProfitItem item : allProfitItems) {
            if (item.getInvoiceNumber() != null && item.getInvoiceNumber().toLowerCase().contains(query)) {
                filtered.add(item);
            } else if (item.getBuyerName() != null && item.getBuyerName().toLowerCase().contains(query)) {
                filtered.add(item);
            }
        }

        adapter.setData(filtered);
        calculateTotalFromList(filtered);

        if (filtered.isEmpty()) {
            tvEmpty.setText("نتیجه‌ای برای جستجو یافت نشد");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerProfit.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerProfit.setVisibility(View.VISIBLE);
        }
    }

    private void calculateTotalFromList(List<ProfitItem> list) {
        long total = 0;
        for (ProfitItem item : list) {
            total += item.getProfit();
        }
        updateTotalProfit(total);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfitData();
    }
}