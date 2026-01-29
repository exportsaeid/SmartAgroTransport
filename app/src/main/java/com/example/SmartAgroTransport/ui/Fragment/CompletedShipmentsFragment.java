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

//    private void setupAdapter() {
//        completedAdapter = new ShipmentListAdapter(
//                new ArrayList<>(),
//                false,
//                shipment -> showEditDialog(shipment)
//        );
//
//        recyclerCompletedShipments.setLayoutManager(new LinearLayoutManager(getContext()));
//        recyclerCompletedShipments.setAdapter(completedAdapter);
//    }
private void setupAdapter() {
    completedAdapter = new ShipmentListAdapter(
            new ArrayList<>(),
            false,
            shipment -> showEditDialog(shipment),
            false   // ← مهم: از تب بارگیری شده هستیم
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_load_invoice, null);

        TextInputEditText etTruckName = dialogView.findViewById(R.id.etTruckName);
        TextInputEditText etPlateNumber = dialogView.findViewById(R.id.etPlateNumber);
        TextInputEditText etDriverName = dialogView.findViewById(R.id.etDriverName);
        TextInputEditText etDriverPhone = dialogView.findViewById(R.id.etDriverPhone);
        TextInputEditText etTransportCost = dialogView.findViewById(R.id.etTransportCost);
        TextInputEditText etLoadDate = dialogView.findViewById(R.id.etLoadDate);

        etTruckName.setText(shipment.getTruckName());
        etPlateNumber.setText(shipment.getPlateNumber());
        etDriverName.setText(shipment.getDriverName());
        etDriverPhone.setText(shipment.getDriverPhone());
        etTransportCost.setText(shipment.getTransportCost() == 0 ? "" : NumberFormatter.formatNumber(shipment.getTransportCost()));
        etLoadDate.setText(shipment.getLoadDate());

        NumberFormatter.addThousandSeparator(etTransportCost);
        etLoadDate.setOnClickListener(v -> PersianDateHelper.showPersianDatePicker(requireContext(), etLoadDate));

        new AlertDialog.Builder(requireContext())
                .setTitle("ویرایش بارگیری فاکتور #" + shipment.getInvoiceNumber())
                .setView(dialogView)
                .setPositiveButton("ذخیره تغییرات", (dialog, which) -> {
                    String truckName = etTruckName.getText().toString().trim();
                    String plateNumber = etPlateNumber.getText().toString().trim();
                    String driverName = etDriverName.getText().toString().trim();
                    String driverPhone = etDriverPhone.getText().toString().trim();
                    String costStr = etTransportCost.getText().toString().replaceAll("[,]", "").trim();
                    String loadDate = etLoadDate.getText().toString().trim();

                    if (truckName.isEmpty() || plateNumber.isEmpty() || driverName.isEmpty() || driverPhone.isEmpty()) {
                        Toast.makeText(requireContext(), "همه فیلدها الزامی هستند!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long transportCost = 0;
                    if (!costStr.isEmpty()) {
                        try {
                            transportCost = Long.parseLong(costStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "هزینه حمل باید عددی باشد", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    shipment.setTruckName(truckName);
                    shipment.setPlateNumber(plateNumber);
                    shipment.setDriverName(driverName);
                    shipment.setDriverPhone(driverPhone);
                    shipment.setTransportCost(transportCost);
                    shipment.setLoadDate(loadDate);

                    if (dbHelper.updateShipment(shipment)) {
                        Toast.makeText(requireContext(), "بارگیری بروزرسانی شد!", Toast.LENGTH_LONG).show();
                        loadData();
                    } else {
                        Toast.makeText(requireContext(), "خطا در بروزرسانی!", Toast.LENGTH_SHORT).show();
                    }
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