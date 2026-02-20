package com.example.SmartAgroTransport.ui.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.adapter.ShipmentListAdapter;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.Clearance;
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.utils.NumberFormatter;
import com.example.SmartAgroTransport.utils.PersianDateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InTransitFragment extends Fragment {

    private RecyclerView recyclerInTransit;
    private TextView tvEmptyInTransit;
    private SearchView searchViewInTransit;
    private ShipmentListAdapter inTransitAdapter;
    private DatabaseHelper dbHelper;
    private List<Shipment> allInTransit = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_in_transit, container, false);

        recyclerInTransit = view.findViewById(R.id.recyclerInTransit);
        tvEmptyInTransit = view.findViewById(R.id.tvEmptyInTransit);
        searchViewInTransit = view.findViewById(R.id.searchViewInTransit);

        dbHelper = new DatabaseHelper(requireContext());
        setupAdapter();
        loadData();
        setupSearchView();

        return view;
    }

    private void loadData() {
        allInTransit = dbHelper.getInTransitShipments();
        inTransitAdapter.setShipments(allInTransit);
        tvEmptyInTransit.setVisibility(allInTransit.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSearchView() {
        searchViewInTransit.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

    private void filterData(String query) {
        query = query.trim().toLowerCase(Locale.getDefault());

        if (query.isEmpty()) {
            inTransitAdapter.setShipments(allInTransit);
            return;
        }

        List<Shipment> filteredInTransit = new ArrayList<>();
        for (Shipment shipment : allInTransit) {
            if (matchesShipment(shipment, query)) {
                filteredInTransit.add(shipment);
            }
        }

        inTransitAdapter.setShipments(filteredInTransit);
        tvEmptyInTransit.setVisibility(filteredInTransit.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesShipment(Shipment shipment, String query) {
        if (shipment.getInvoiceNumber() != null && shipment.getInvoiceNumber().toLowerCase().contains(query)) return true;
        if (shipment.getTruckName() != null && shipment.getTruckName().toLowerCase().contains(query)) return true;
        if (shipment.getPlateNumber() != null && shipment.getPlateNumber().toLowerCase().contains(query)) return true;
        if (shipment.getDriverName() != null && shipment.getDriverName().toLowerCase().contains(query)) return true;
        if (shipment.getLoadDate() != null && shipment.getLoadDate().toLowerCase().contains(query)) return true;
        return false;
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

    private void showClearanceDialog(Shipment shipment) {
        // اینفلیت layout دیالوگ
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_clearance, null);

        // پیدا کردن ویوها
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvInvoiceNumber = dialogView.findViewById(R.id.tvInvoiceNumber);
        TextInputEditText etName = dialogView.findViewById(R.id.etClearanceName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etClearancePhone);
        TextInputEditText etBorder = dialogView.findViewById(R.id.etBorderName);
        TextInputEditText etCost = dialogView.findViewById(R.id.etClearanceCost);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        TextInputEditText etDate = dialogView.findViewById(R.id.etClearanceDate);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // تنظیم عنوان و شماره فاکتور
        if (tvDialogTitle != null) {
            tvDialogTitle.setText("📋 ثبت ترخیص");
        }

        if (tvInvoiceNumber != null) {
            tvInvoiceNumber.setText("🧾 فاکتور #" + shipment.getInvoiceNumber());
        }

        // تنظیم RTL برای فیلدهای عددی
        setupRtlNumberField(etCost);
        setupRtlPhoneField(etPhone);

        // اضافه کردن جداکننده هزارگان
        NumberFormatter.addThousandSeparator(etCost);

        // تنظیم تاریخ پیش‌فرض
        etDate.setText(PersianDateHelper.getCurrentPersianDate());

        // تنظیم کلیک برای انتخاب تاریخ
        etDate.setOnClickListener(v ->
                PersianDateHelper.showPersianDatePicker(requireContext(), etDate)
        );
        etDate.setKeyListener(null); // جلوگیری از ورود دستی

        // ایجاد دیالوگ
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // تنظیم پس‌زمینه شفاف
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        // تنظیم کلیک دکمه ذخیره
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String border = etBorder.getText().toString().trim();
                String costStr = etCost.getText().toString().replaceAll(",", "").trim();
                String notes = etNotes.getText().toString().trim();
                String date = etDate.getText().toString().trim();

                // اعتبارسنجی
                if (name.isEmpty()) {
                    etName.setError("👤 نام ترخیصکار الزامی است");
                    etName.requestFocus();
                    return;
                }

                if (phone.isEmpty()) {
                    etPhone.setError("📞 تلفن ترخیصکار الزامی است");
                    etPhone.requestFocus();
                    return;
                }

                if (border.isEmpty()) {
                    etBorder.setError("🛂 نام مرز الزامی است");
                    etBorder.requestFocus();
                    return;
                }

                if (date.isEmpty()) {
                    etDate.setError("📅 تاریخ ترخیص الزامی است");
                    etDate.requestFocus();
                    return;
                }

                // پردازش هزینه
                long cost = 0;
                if (!costStr.isEmpty()) {
                    try {
                        cost = Long.parseLong(costStr);
                    } catch (NumberFormatException e) {
                        etCost.setError("💰 هزینه باید عددی باشد");
                        etCost.requestFocus();
                        return;
                    }
                }

                // ایجاد شیء Clearance
                Clearance clearance = new Clearance();
                clearance.setShipmentId(shipment.getId());
                clearance.setClearanceName(name);
                clearance.setClearancePhone(phone);
                clearance.setBorderName(border);
                clearance.setClearanceCost(cost);
                clearance.setNotes(notes);
                clearance.setClearanceDate(date);

                // ذخیره در دیتابیس
                long result = dbHelper.addClearance(clearance);
                if (result > 0) {
                    Toast.makeText(requireContext(), "✅ ترخیص با موفقیت ثبت شد!", Toast.LENGTH_LONG).show();
                    loadData();

                    // اطلاع به اکتیویتی برای رفرش تب ترخیص شده
                    // if (getActivity() instanceof ClearanceActivity) {
                    //     ((ClearanceActivity) getActivity()).refreshClearedTab();
                    // }

                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "❌ خطا در ثبت ترخیص!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // تنظیم کلیک دکمه لغو
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    public void refreshData() {
        loadData();
    }

    private void setupAdapter() {
        inTransitAdapter = new ShipmentListAdapter(
                new ArrayList<>(),
                false,
                shipment -> showClearanceDialog(shipment),
                null,
                null,
                true   // از تب در راه مرز هستیم
        );

        recyclerInTransit.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerInTransit.setAdapter(inTransitAdapter);
    }
}