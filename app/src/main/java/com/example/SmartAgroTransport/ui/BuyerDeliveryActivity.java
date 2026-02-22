package com.example.SmartAgroTransport.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.adapter.BuyerDeliveryAdapter;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.BuyerDelivery;
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.utils.NumberFormatter;
import com.example.SmartAgroTransport.utils.PersianDateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuyerDeliveryActivity extends AppCompatActivity {

    private RecyclerView recyclerBuyerDelivery;
    private SearchView searchViewDelivery;
    private TextView tvEmptyBuyerDelivery;

    private BuyerDeliveryAdapter adapter;
    private DatabaseHelper dbHelper;

    private List<Shipment> allShipments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_delivery);

        ImageView backButton = findViewById(R.id.backButtonDelivery);
        backButton.setOnClickListener(v -> finish());

        recyclerBuyerDelivery = findViewById(R.id.recyclerBuyerDelivery);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerBuyerDelivery.addItemDecoration(divider);

        searchViewDelivery = findViewById(R.id.searchViewDelivery);
        tvEmptyBuyerDelivery = findViewById(R.id.tvEmptyBuyerDelivery);

        dbHelper = new DatabaseHelper(this);

        setupAdapter();
        loadAllData();

        searchViewDelivery.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        adapter = new BuyerDeliveryAdapter(
                new ArrayList<>(),
                true,
                null,
                null,
                shipment -> showBuyerDeliveryDialog(shipment, dbHelper.getBuyerDeliveryByShipmentId(shipment.getId()) != null)
        );

        recyclerBuyerDelivery.setLayoutManager(new LinearLayoutManager(this));
        recyclerBuyerDelivery.setAdapter(adapter);
    }

    private void loadAllData() {
        List<Shipment> iraqiDelivered = new ArrayList<>();
        List<Shipment> cleared = dbHelper.getClearedShipments();

        for (Shipment s : cleared) {
            if (dbHelper.getIraqiHandoverByShipmentId(s.getId()) != null) {
                iraqiDelivered.add(s);
            }
        }

        allShipments = iraqiDelivered;
        adapter.setShipments(allShipments);

        if (allShipments.isEmpty()) {
            tvEmptyBuyerDelivery.setVisibility(View.VISIBLE);
            recyclerBuyerDelivery.setVisibility(View.GONE);
        } else {
            tvEmptyBuyerDelivery.setVisibility(View.GONE);
            recyclerBuyerDelivery.setVisibility(View.VISIBLE);
        }
    }

    private void filterData(String query) {
        query = query.trim().toLowerCase(Locale.getDefault());
        if (query.isEmpty()) {
            adapter.setShipments(allShipments);
            return;
        }

        List<Shipment> filtered = new ArrayList<>();
        for (Shipment s : allShipments) {
            boolean matches = false;
            if (s.getInvoiceNumber() != null && s.getInvoiceNumber().toLowerCase().contains(query)) matches = true;
            else if (s.getTruckName() != null && s.getTruckName().toLowerCase().contains(query)) matches = true;
            else if (s.getPlateNumber() != null && s.getPlateNumber().toLowerCase().contains(query)) matches = true;
            else if (s.getDriverName() != null && s.getDriverName().toLowerCase().contains(query)) matches = true;

            BuyerDelivery delivery = dbHelper.getBuyerDeliveryByShipmentId(s.getId());
            if (delivery != null) {
                if (delivery.getBuyerName() != null && delivery.getBuyerName().toLowerCase().contains(query)) matches = true;
                else if (delivery.getHajraNumber() != null && delivery.getHajraNumber().toLowerCase().contains(query)) matches = true;
            }

            if (matches) filtered.add(s);
        }

        adapter.setShipments(filtered);

        if (filtered.isEmpty()) {
            tvEmptyBuyerDelivery.setText("📭 نتیجه‌ای برای جستجو یافت نشد");
            tvEmptyBuyerDelivery.setVisibility(View.VISIBLE);
            recyclerBuyerDelivery.setVisibility(View.GONE);
        } else {
            tvEmptyBuyerDelivery.setVisibility(View.GONE);
            recyclerBuyerDelivery.setVisibility(View.VISIBLE);
        }
    }

    // ========== متدهای کمکی برای RTL ==========
    private void setupRtlNumberField(TextInputEditText editText) {
        editText.setTextDirection(View.TEXT_DIRECTION_RTL);
        editText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        editText.setGravity(Gravity.RIGHT);
    }

    private void setupRtlPhoneField(TextInputEditText editText) {
        editText.setTextDirection(View.TEXT_DIRECTION_RTL);
        editText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        editText.setGravity(Gravity.RIGHT);
    }
    // ==========================================

    private void showBuyerDeliveryDialog(Shipment shipment, boolean isEdit) {
        BuyerDelivery delivery = isEdit ? dbHelper.getBuyerDeliveryByShipmentId(shipment.getId()) : null;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_buyer_delivery, null);

        // پیدا کردن ویوها
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvInvoiceNumber = dialogView.findViewById(R.id.tvInvoiceNumber);
        TextInputEditText etBuyerName = dialogView.findViewById(R.id.etBuyerName);
        TextInputEditText etBuyerPhone = dialogView.findViewById(R.id.etBuyerPhone);
        TextInputEditText etHajraNumber = dialogView.findViewById(R.id.etHajraNumber);
        TextInputEditText etReceivedAmount = dialogView.findViewById(R.id.etReceivedAmount);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        TextInputEditText etPersianDate = dialogView.findViewById(R.id.etPersianDate);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // تنظیم عنوان و شماره فاکتور
        if (tvDialogTitle != null) {
            tvDialogTitle.setText(isEdit ? "✏️ ویرایش تحویل به خریدار" : "📦 ثبت تحویل به خریدار");
        }

        if (tvInvoiceNumber != null) {
            tvInvoiceNumber.setText("🧾 فاکتور #" + shipment.getInvoiceNumber());
        }

        // تنظیم متن دکمه ذخیره
        if (btnSave != null) {
            btnSave.setText(isEdit ? "✏️ ویرایش" : "💾 ثبت تحویل");
        }

        // تنظیم RTL برای فیلدهای عددی
        setupRtlNumberField(etReceivedAmount);
        setupRtlNumberField(etHajraNumber);
        setupRtlPhoneField(etBuyerPhone);

        // اضافه کردن جداکننده هزارگان
        NumberFormatter.addThousandSeparator(etReceivedAmount);

        // پر کردن مقادیر
        if (delivery != null) {
            etBuyerName.setText(delivery.getBuyerName());
            etBuyerPhone.setText(delivery.getBuyerPhone());
            etHajraNumber.setText(delivery.getHajraNumber());
            etReceivedAmount.setText(delivery.getReceivedAmount() == 0 ? "" : NumberFormatter.formatNumber(delivery.getReceivedAmount()));
            etNotes.setText(delivery.getNotes());
            etPersianDate.setText(delivery.getDeliveryDate());
        } else {
            etPersianDate.setText(PersianDateHelper.getCurrentPersianDate());
        }

        // تنظیم کلیک برای انتخاب تاریخ
        etPersianDate.setOnClickListener(v ->
                PersianDateHelper.showPersianDatePicker(this, etPersianDate)
        );
        etPersianDate.setKeyListener(null); // جلوگیری از ورود دستی

        // ایجاد دیالوگ
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        // تنظیم کلیک دکمه ذخیره
        btnSave.setOnClickListener(v -> {
            String buyerName = etBuyerName.getText().toString().trim();
            String buyerPhone = etBuyerPhone.getText().toString().trim();
            String hajraNumber = etHajraNumber.getText().toString().trim();
            String amountStr = etReceivedAmount.getText().toString().replaceAll("[,]", "").trim();
            String notes = etNotes.getText().toString().trim();
            String persianDate = etPersianDate.getText().toString().trim();

            // اعتبارسنجی
            if (buyerName.isEmpty()) {
                etBuyerName.setError("👤 نام خریدار الزامی است!");
                etBuyerName.requestFocus();
                return;
            }

            if (persianDate.isEmpty()) {
                etPersianDate.setError("📅 تاریخ تحویل الزامی است!");
                etPersianDate.requestFocus();
                return;
            }

            long receivedAmount = 0;
            if (!amountStr.isEmpty()) {
                try {
                    receivedAmount = Long.parseLong(amountStr);
                } catch (NumberFormatException e) {
                    etReceivedAmount.setError("💰 مبلغ دریافتی باید عددی باشد!");
                    etReceivedAmount.requestFocus();
                    return;
                }
            }

            BuyerDelivery newDelivery = delivery != null ? delivery : new BuyerDelivery();
            newDelivery.setShipmentId(shipment.getId());
            newDelivery.setBuyerName(buyerName);
            newDelivery.setBuyerPhone(buyerPhone);
            newDelivery.setHajraNumber(hajraNumber);
            newDelivery.setDeliveryDate(persianDate);
            newDelivery.setReceivedAmount(receivedAmount);
            newDelivery.setNotes(notes);

            boolean success;
            if (isEdit) {
                success = dbHelper.updateBuyerDelivery(newDelivery);
                if (success) {
                    Toast.makeText(this, "✅ تحویل به خریدار بروزرسانی شد!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "❌ خطا در بروزرسانی!", Toast.LENGTH_SHORT).show();
                }
            } else {
                long result = dbHelper.addBuyerDelivery(newDelivery);
                success = result > 0;
                if (success) {
                    Toast.makeText(this, "✅ تحویل به خریدار با موفقیت ثبت شد!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "❌ خطا در ثبت تحویل!", Toast.LENGTH_SHORT).show();
                }
            }

            if (success) {
                loadAllData();
                dialog.dismiss();
            }
        });

        // تنظیم کلیک دکمه لغو
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllData();
    }
}