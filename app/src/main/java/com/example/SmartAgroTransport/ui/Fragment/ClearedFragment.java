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
import com.example.SmartAgroTransport.model.Clearance;
import com.example.SmartAgroTransport.model.IraqiHandover;
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.utils.NumberFormatter;
import com.example.SmartAgroTransport.utils.PersianDateHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClearedFragment extends Fragment {

    private RecyclerView recyclerCleared;
    private TextView tvEmptyCleared;
    private SearchView searchViewCleared;
    private ShipmentListAdapter clearedAdapter;
    private DatabaseHelper dbHelper;
    private List<Shipment> allCleared = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cleared, container, false);

        recyclerCleared = view.findViewById(R.id.recyclerCleared);
        tvEmptyCleared = view.findViewById(R.id.tvEmptyCleared);
        searchViewCleared = view.findViewById(R.id.searchViewCleared);

        dbHelper = new DatabaseHelper(requireContext());
        setupAdapter();
        loadData();
        setupSearchView();

        return view;
    }

    private void setupAdapter() {
        clearedAdapter = new ShipmentListAdapter(
                new ArrayList<>(),
                true,
                shipment -> showClearanceDialog(shipment, true),
                shipment -> showIraqiHandoverDialog(
                        shipment,
                        dbHelper.getIraqiHandoverByShipmentId(shipment.getId()) != null
                )
        );

        recyclerCleared.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerCleared.setAdapter(clearedAdapter);
    }

    private void loadData() {
        allCleared = dbHelper.getClearedShipments();
        clearedAdapter.setShipments(allCleared);
        tvEmptyCleared.setVisibility(allCleared.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSearchView() {
        searchViewCleared.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            clearedAdapter.setShipments(allCleared);
            return;
        }

        List<Shipment> filteredCleared = new ArrayList<>();
        for (Shipment shipment : allCleared) {
            if (matchesShipment(shipment, query)) {
                filteredCleared.add(shipment);
            }
        }

        clearedAdapter.setShipments(filteredCleared);
        tvEmptyCleared.setVisibility(filteredCleared.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesShipment(Shipment shipment, String query) {
        if (shipment.getInvoiceNumber() != null && shipment.getInvoiceNumber().toLowerCase().contains(query)) return true;
        if (shipment.getTruckName() != null && shipment.getTruckName().toLowerCase().contains(query)) return true;
        if (shipment.getPlateNumber() != null && shipment.getPlateNumber().toLowerCase().contains(query)) return true;

        Clearance clearance = dbHelper.getClearanceByShipmentId(shipment.getId());
        if (clearance != null) {
            if (clearance.getClearanceName() != null && clearance.getClearanceName().toLowerCase().contains(query)) return true;
            if (clearance.getBorderName() != null && clearance.getBorderName().toLowerCase().contains(query)) return true;
        }

        IraqiHandover iraqi = dbHelper.getIraqiHandoverByShipmentId(shipment.getId());
        if (iraqi != null) {
            if (iraqi.getClearanceName() != null && iraqi.getClearanceName().toLowerCase().contains(query)) return true;
            if (iraqi.getBorderName() != null && iraqi.getBorderName().toLowerCase().contains(query)) return true;
        }

        return false;
    }

    private void showClearanceDialog(Shipment shipment, boolean isEdit) {
        Clearance clearance = isEdit ? dbHelper.getClearanceByShipmentId(shipment.getId()) : null;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_clearance, null);

        TextInputEditText etName = dialogView.findViewById(R.id.etClearanceName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etClearancePhone);
        TextInputEditText etBorder = dialogView.findViewById(R.id.etBorderName);
        TextInputEditText etCost = dialogView.findViewById(R.id.etClearanceCost);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        TextInputEditText etDate = dialogView.findViewById(R.id.etClearanceDate);

        NumberFormatter.addThousandSeparator(etCost);
        etDate.setOnClickListener(v -> PersianDateHelper.showPersianDatePicker(requireContext(), etDate));

        if (clearance != null) {
            etName.setText(clearance.getClearanceName());
            etPhone.setText(clearance.getClearancePhone());
            etBorder.setText(clearance.getBorderName());
            etCost.setText(clearance.getClearanceCost() == 0 ? "" : NumberFormatter.formatNumber(clearance.getClearanceCost()));
            etNotes.setText(clearance.getNotes());
            etDate.setText(clearance.getClearanceDate());
        } else {
            etDate.setText(PersianDateHelper.getCurrentPersianDate());
        }

        String title = isEdit ? "ویرایش ترخیص" : "ثبت ترخیص";

        new AlertDialog.Builder(requireContext())
                .setTitle(title + " - فاکتور #" + shipment.getInvoiceNumber())
                .setView(dialogView)
                .setPositiveButton(isEdit ? "ذخیره تغییرات" : "ثبت ترخیص", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String border = etBorder.getText().toString().trim();
                    String costStr = etCost.getText().toString().replaceAll(",", "").trim();
                    String notes = etNotes.getText().toString().trim();
                    String date = etDate.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty() || border.isEmpty()) {
                        Toast.makeText(requireContext(), "نام، تلفن و مرز الزامی هستند!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long cost = 0;
                    if (!costStr.isEmpty()) {
                        try {
                            cost = Long.parseLong(costStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "هزینه باید عددی باشد", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    Clearance newClearance = clearance != null ? clearance : new Clearance();
                    newClearance.setShipmentId(shipment.getId());
                    newClearance.setClearanceName(name);
                    newClearance.setClearancePhone(phone);
                    newClearance.setBorderName(border);
                    newClearance.setClearanceCost(cost);
                    newClearance.setNotes(notes);
                    newClearance.setClearanceDate(date);

                    if (isEdit) {
                        if (dbHelper.updateClearance(newClearance)) {
                            Toast.makeText(requireContext(), "ترخیص بروزرسانی شد!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "خطا در بروزرسانی ترخیص!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        long result = dbHelper.addClearance(newClearance);
                        if (result > 0) {
                            Toast.makeText(requireContext(), "ترخیص با موفقیت ثبت شد!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "خطا در ثبت ترخیص!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    loadData();
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void showIraqiHandoverDialog(Shipment shipment, boolean isEdit) {
        IraqiHandover handover = isEdit ? dbHelper.getIraqiHandoverByShipmentId(shipment.getId()) : null;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_clearance, null);

        TextInputEditText etName = dialogView.findViewById(R.id.etClearanceName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etClearancePhone);
        TextInputEditText etBorder = dialogView.findViewById(R.id.etBorderName);
        TextInputEditText etCost = dialogView.findViewById(R.id.etClearanceCost);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        TextInputEditText etDate = dialogView.findViewById(R.id.etClearanceDate);

        if (etName.getParent() instanceof TextInputLayout) ((TextInputLayout) etName.getParent()).setHint("نام ترخیص‌کار عراقی");
        if (etPhone.getParent() instanceof TextInputLayout) ((TextInputLayout) etPhone.getParent()).setHint("تلفن ترخیص‌کار عراقی (+964)");
        if (etBorder.getParent() instanceof TextInputLayout) ((TextInputLayout) etBorder.getParent()).setHint("نام مرز");
        if (etCost.getParent() instanceof TextInputLayout) ((TextInputLayout) etCost.getParent()).setHint("هزینه اضافی (تومان)");
        if (etNotes.getParent() instanceof TextInputLayout) ((TextInputLayout) etNotes.getParent()).setHint("توضیحات و شماره پلاک عراقی");
        if (etDate.getParent() instanceof TextInputLayout) ((TextInputLayout) etDate.getParent()).setHint("تاریخ ترخیص");

        NumberFormatter.addThousandSeparator(etCost);

        etDate.setOnClickListener(v -> PersianDateHelper.showPersianDatePicker(requireContext(), etDate));

        if (handover != null) {
            etName.setText(handover.getClearanceName());
            etPhone.setText(handover.getClearancePhone());
            etBorder.setText(handover.getBorderName());
            etCost.setText(handover.getClearanceCost() == 0 ? "" : NumberFormatter.formatNumber(handover.getClearanceCost()));
            etNotes.setText(handover.getNotes());
            etDate.setText(handover.getClearanceDate());
        } else {
            etDate.setText(PersianDateHelper.getCurrentPersianDate());
        }

        String title = isEdit ? "ویرایش تحویل به طرف عراقی" : "ثبت تحویل به ترخیص‌کار عراقی";

        new AlertDialog.Builder(requireContext())
                .setTitle(title + " - فاکتور #" + shipment.getInvoiceNumber())
                .setView(dialogView)
                .setPositiveButton(isEdit ? "ذخیره تغییرات" : "ثبت تحویل", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String border = etBorder.getText().toString().trim();
                    String costStr = etCost.getText().toString().replaceAll(",", "").trim();
                    String notes = etNotes.getText().toString().trim();
                    String date = etDate.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty() || border.isEmpty()) {
                        Toast.makeText(requireContext(), "نام، تلفن و مرز الزامی هستند!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long cost = 0;
                    if (!costStr.isEmpty()) {
                        try {
                            cost = Long.parseLong(costStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "هزینه باید عددی باشد", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    IraqiHandover newHandover = handover != null ? handover : new IraqiHandover();
                    newHandover.setShipmentId(shipment.getId());
                    newHandover.setClearanceName(name);
                    newHandover.setClearancePhone(phone);
                    newHandover.setBorderName(border);
                    newHandover.setClearanceCost(cost);
                    newHandover.setNotes(notes);
                    newHandover.setClearanceDate(date);

                    if (isEdit) {
                        if (dbHelper.updateIraqiHandover(newHandover)) {
                            Toast.makeText(requireContext(), "تحویل به عراقی بروزرسانی شد!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "خطا در بروزرسانی تحویل!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        long result = dbHelper.addIraqiHandover(newHandover);
                        if (result > 0) {
                            Toast.makeText(requireContext(), "تحویل با موفقیت ثبت شد!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "خطا در ثبت تحویل!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    loadData();
                })
                .setNegativeButton("لغو", null)
                .show();
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