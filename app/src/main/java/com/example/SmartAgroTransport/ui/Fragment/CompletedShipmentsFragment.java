package com.example.SmartAgroTransport.ui.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.utils.NumberFormatter;
import com.example.SmartAgroTransport.utils.PersianDateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CompletedShipmentsFragment extends Fragment {

    private RecyclerView recyclerCompletedShipments;
    private TextView tvEmptyCompleted;
    private SearchView searchViewCompleted;
    private ShipmentListAdapter completedAdapter;
    private DatabaseHelper dbHelper;
    private List<Shipment> allCompletedShipments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_completed_shipments, container, false);

        recyclerCompletedShipments = view.findViewById(R.id.recyclerCompletedShipments);
        tvEmptyCompleted = view.findViewById(R.id.tvEmptyCompleted);
        searchViewCompleted = view.findViewById(R.id.searchViewCompleted);

        dbHelper = new DatabaseHelper(requireContext());
        setupAdapter();
        loadData();
        setupSearchView();

        return view;
    }

    private void setupAdapter() {
        completedAdapter = new ShipmentListAdapter(
                new ArrayList<>(),
                false,
                shipment -> showEditDialog(shipment),
                false
        );

        recyclerCompletedShipments.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCompletedShipments.setAdapter(completedAdapter);
    }

    private void loadData() {
        allCompletedShipments = dbHelper.getAllShipments();
        completedAdapter.setShipments(allCompletedShipments);
        tvEmptyCompleted.setVisibility(allCompletedShipments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSearchView() {
        searchViewCompleted.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            completedAdapter.setShipments(allCompletedShipments);
            return;
        }

        List<Shipment> filteredCompleted = new ArrayList<>();
        for (Shipment shipment : allCompletedShipments) {
            if (shipment.getInvoiceNumber() != null && shipment.getInvoiceNumber().toLowerCase().contains(query)) {
                filteredCompleted.add(shipment);
            } else if (shipment.getTruckName() != null && shipment.getTruckName().toLowerCase().contains(query)) {
                filteredCompleted.add(shipment);
            } else if (shipment.getPlateNumber() != null && shipment.getPlateNumber().toLowerCase().contains(query)) {
                filteredCompleted.add(shipment);
            } else if (shipment.getDriverName() != null && shipment.getDriverName().toLowerCase().contains(query)) {
                filteredCompleted.add(shipment);
            }
        }

        completedAdapter.setShipments(filteredCompleted);
    }

    private void showEditDialog(Shipment shipment) {
        // اینفلیت layout دیالوگ
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_load_invoice, null);

        // پیدا کردن ویوها
        TextView tvInvoiceNumber = dialogView.findViewById(R.id.tvInvoiceNumber);
        TextInputEditText etTruckName = dialogView.findViewById(R.id.etTruckName);
        TextInputEditText etPlateNumber = dialogView.findViewById(R.id.etPlateNumber);
        TextInputEditText etDriverName = dialogView.findViewById(R.id.etDriverName);
        TextInputEditText etDriverPhone = dialogView.findViewById(R.id.etDriverPhone);
        TextInputEditText etTransportCost = dialogView.findViewById(R.id.etTransportCost);
        TextInputEditText etLoadDate = dialogView.findViewById(R.id.etLoadDate);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // تنظیم شماره فاکتور
        tvInvoiceNumber.setText("🧾 ویرایش بارگیری فاکتور #" + shipment.getInvoiceNumber());
// تنظیم متن دکمه ذخیره به "ویرایش"
        if (btnSave != null) {
            btnSave.setText("✏️ ویرایش");
        }
        // تنظیم مقادیر موجود
        etTruckName.setText(shipment.getTruckName());
        etPlateNumber.setText(shipment.getPlateNumber());
        etDriverName.setText(shipment.getDriverName());
        etDriverPhone.setText(shipment.getDriverPhone());

        if (shipment.getTransportCost() > 0) {
            etTransportCost.setText(NumberFormatter.formatNumber(shipment.getTransportCost()));
        } else {
            etTransportCost.setText("");
        }

        etLoadDate.setText(shipment.getLoadDate());

        // اضافه کردن جداکننده هزارگان به فیلد هزینه
        NumberFormatter.addThousandSeparator(etTransportCost);

        // تنظیم کلیک برای انتخاب تاریخ
        etLoadDate.setOnClickListener(v ->
                PersianDateHelper.showPersianDatePicker(requireContext(), etLoadDate)
        );
        etLoadDate.setKeyListener(null); // جلوگیری از ورود دستی

        // ساخت دیالوگ
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        // ایجاد دیالوگ
        AlertDialog dialog = builder.create();

        // تنظیم رنگ‌بندی دیالوگ (اختیاری)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        // کلیک دکمه ذخیره
        btnSave.setOnClickListener(v -> {
            // دریافت مقادیر
            String truckName = etTruckName.getText().toString().trim();
            String plateNumber = etPlateNumber.getText().toString().trim();
            String driverName = etDriverName.getText().toString().trim();
            String driverPhone = etDriverPhone.getText().toString().trim();
            String costStr = etTransportCost.getText().toString().replaceAll("[,]", "").trim();
            String loadDate = etLoadDate.getText().toString().trim();

            // اعتبارسنجی
            if (truckName.isEmpty()) {
                etTruckName.setError("🚛 نام ماشین الزامی است");
                etTruckName.requestFocus();
                return;
            }

            if (plateNumber.isEmpty()) {
                etPlateNumber.setError("🔢 پلاک ماشین الزامی است");
                etPlateNumber.requestFocus();
                return;
            }

            if (driverName.isEmpty()) {
                etDriverName.setError("👤 نام راننده الزامی است");
                etDriverName.requestFocus();
                return;
            }

            if (driverPhone.isEmpty()) {
                etDriverPhone.setError("📞 شماره تلفن الزامی است");
                etDriverPhone.requestFocus();
                return;
            }

            if (driverPhone.length() < 10) {
                etDriverPhone.setError("📞 شماره تلفن معتبر نیست (حداقل ۱۰ رقم)");
                etDriverPhone.requestFocus();
                return;
            }

            if (loadDate.isEmpty()) {
                etLoadDate.setError("📅 تاریخ بارگیری الزامی است");
                etLoadDate.requestFocus();
                return;
            }

            // پردازش هزینه
            long transportCost = 0;
            if (!costStr.isEmpty()) {
                try {
                    transportCost = Long.parseLong(costStr);
                } catch (NumberFormatException e) {
                    etTransportCost.setError("💰 هزینه حمل باید عددی باشد");
                    etTransportCost.requestFocus();
                    return;
                }
            }

            // بروزرسانی مدل
            shipment.setTruckName(truckName);
            shipment.setPlateNumber(plateNumber);
            shipment.setDriverName(driverName);
            shipment.setDriverPhone(driverPhone);
            shipment.setTransportCost(transportCost);
            shipment.setLoadDate(loadDate);

            // ذخیره در دیتابیس
            if (dbHelper.updateShipment(shipment)) {
                Toast.makeText(requireContext(), "✅ بارگیری با موفقیت بروزرسانی شد!", Toast.LENGTH_LONG).show();
                loadData(); // رفرش لیست
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "❌ خطا در بروزرسانی! دوباره تلاش کنید", Toast.LENGTH_SHORT).show();
            }
        });

        // کلیک دکمه لغو
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    public void refreshData() {
        loadData();
    }
}