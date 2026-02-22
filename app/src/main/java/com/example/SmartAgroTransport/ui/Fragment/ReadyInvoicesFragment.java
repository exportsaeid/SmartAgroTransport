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
import com.example.SmartAgroTransport.adapter.InvoiceListAdapter;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.Invoice;
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.ui.LoadInvoiceActivity;
import com.example.SmartAgroTransport.utils.NumberFormatter;
import com.example.SmartAgroTransport.utils.PersianDateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReadyInvoicesFragment extends Fragment {

    private RecyclerView recyclerReadyInvoices;
    private TextView tvEmptyReady;
    private SearchView searchViewReady;
    private InvoiceListAdapter readyAdapter;
    private DatabaseHelper dbHelper;
    private List<Invoice> allReadyInvoices = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ready_invoices, container, false);

        recyclerReadyInvoices = view.findViewById(R.id.recyclerReadyInvoices);
        tvEmptyReady = view.findViewById(R.id.tvEmptyReady);
        searchViewReady = view.findViewById(R.id.searchViewReady);

        dbHelper = new DatabaseHelper(requireContext());
        setupAdapter();
        loadData();
        setupSearchView();

        return view;
    }

    private void setupAdapter() {
        readyAdapter = new InvoiceListAdapter(
                new ArrayList<>(),
                new InvoiceListAdapter.OnInvoiceClickListener() {
                    @Override
                    public void onEdit(Invoice invoice) {
                        showLoadDialog(invoice);
                    }

                    @Override
                    public void onDelete(long invoiceId) {
                        // Handle delete if needed
                    }
                },
                dbHelper,
                1 // button_type = 1 (ثبت بارگیری)
        );

        recyclerReadyInvoices.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerReadyInvoices.setAdapter(readyAdapter);
    }

    private void loadData() {
        allReadyInvoices = dbHelper.getReadyInvoices();
        readyAdapter.setInvoices(allReadyInvoices);
        tvEmptyReady.setVisibility(allReadyInvoices.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSearchView() {
        searchViewReady.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            readyAdapter.setInvoices(allReadyInvoices);
            return;
        }

        List<Invoice> filteredReady = new ArrayList<>();
        for (Invoice invoice : allReadyInvoices) {
            if (invoice.getInvoiceNumber() != null && invoice.getInvoiceNumber().toLowerCase().contains(query)) {
                filteredReady.add(invoice);
            } else if (invoice.getCustomerName() != null && invoice.getCustomerName().toLowerCase().contains(query)) {
                filteredReady.add(invoice);
            }
        }

        readyAdapter.setInvoices(filteredReady);
    }

private void showLoadDialog(Invoice invoice) {
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

    // تنظیم شماره فاکتور در TextView
    if (tvInvoiceNumber != null) {
        tvInvoiceNumber.setText("🧾 ثبت بارگیری فاکتور #" + invoice.getInvoiceNumber());
    }

    // تنظیم تاریخ پیش‌فرض
    etLoadDate.setText(PersianDateHelper.getCurrentPersianDate());

    // اضافه کردن جداکننده هزارگان
    NumberFormatter.addThousandSeparator(etTransportCost);

    // تنظیم کلیک برای انتخاب تاریخ
    etLoadDate.setOnClickListener(v ->
            PersianDateHelper.showPersianDatePicker(requireContext(), etLoadDate)
    );
    etLoadDate.setKeyListener(null); // جلوگیری از ورود دستی

    // ایجاد دیالوگ با Dialog مستقیم (نه AlertDialog)
    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
    builder.setView(dialogView);

    androidx.appcompat.app.AlertDialog dialog = builder.create();

    // تنظیم پس‌زمینه شفاف
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    dialog.show();

    // تنظیم کلیک دکمه ذخیره (از خود layout)
    if (btnSave != null) {
        btnSave.setOnClickListener(v -> {
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
                etDriverPhone.setError("📞 شماره تلفن معتبر نیست");
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

            // ایجاد شیء Shipment
            Shipment shipment = new Shipment();
            shipment.setInvoiceId(invoice.getId());
            shipment.setInvoiceNumber(invoice.getInvoiceNumber());
            shipment.setTruckName(truckName);
            shipment.setPlateNumber(plateNumber);
            shipment.setDriverName(driverName);
            shipment.setDriverPhone(driverPhone);
            shipment.setTransportCost(transportCost);
            shipment.setLoadDate(loadDate);

            // ذخیره در دیتابیس
            long shipmentId = dbHelper.addShipment(shipment);
            if (shipmentId > 0) {
                dbHelper.updateInvoiceLoadStatus(invoice.getId(), 1);
                Toast.makeText(requireContext(), "✅ بارگیری با موفقیت ثبت شد!", Toast.LENGTH_LONG).show();
                loadData();

                // اطلاع به اکتیویتی برای رفرش تب بارگیری شده
                if (getActivity() instanceof LoadInvoiceActivity) {
                    ((LoadInvoiceActivity) getActivity()).refreshCompletedTab();
                }

                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "❌ خطا در ثبت بارگیری!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // تنظیم کلیک دکمه لغو (از خود layout)
    if (btnCancel != null) {
        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
}
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}