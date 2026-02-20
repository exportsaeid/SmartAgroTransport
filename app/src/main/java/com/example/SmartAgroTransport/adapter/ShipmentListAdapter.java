package com.example.SmartAgroTransport.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.BuyerDelivery;
import com.example.SmartAgroTransport.model.Clearance;
import com.example.SmartAgroTransport.model.IraqiHandover;
import com.example.SmartAgroTransport.model.Shipment;
import com.example.SmartAgroTransport.utils.NumberFormatter;

import java.util.ArrayList;
import java.util.List;

public class ShipmentListAdapter extends RecyclerView.Adapter<ShipmentListAdapter.ViewHolder> {

    private List<Shipment> shipments = new ArrayList<>();
    private final boolean isClearedPage;
    private final boolean isFromInTransitTab;   // ← فیلد جدید اضافه شد
    private final OnEditClickListener editListener;
    private final OnIraqiHandoverClickListener iraqiListener;
    private final OnBuyerDeliveryClickListener buyerListener;
    private DatabaseHelper dbHelper;

    public interface OnEditClickListener {
        void onEdit(Shipment shipment);
    }

    public interface OnIraqiHandoverClickListener {
        void onIraqiHandover(Shipment shipment);
    }

    public interface OnBuyerDeliveryClickListener {
        void onBuyerDelivery(Shipment shipment);
    }

    // سازنده کامل (با پارامتر جدید)
    public ShipmentListAdapter(
            List<Shipment> shipments,
            boolean isClearedPage,
            OnEditClickListener editListener,
            OnIraqiHandoverClickListener iraqiListener,
            OnBuyerDeliveryClickListener buyerListener,
            boolean isFromInTransitTab) {   // ← پارامتر جدید

        this.shipments = shipments != null ? new ArrayList<>(shipments) : new ArrayList<>();
        this.isClearedPage = isClearedPage;
        this.editListener = editListener;
        this.iraqiListener = iraqiListener;
        this.buyerListener = buyerListener;
        this.isFromInTransitTab = isFromInTransitTab;
    }

    // سازنده ساده برای تب در راه مرز / بارگیری
    public ShipmentListAdapter(
            List<Shipment> shipments,
            boolean isClearedPage,
            OnEditClickListener listener,
            boolean isFromInTransitTab) {

        this(shipments, isClearedPage, listener, null, null, isFromInTransitTab);
    }

    // سازنده برای صفحه ترخیص شده (بدون تغییر زیاد)
    public ShipmentListAdapter(
            List<Shipment> shipments,
            boolean isClearedPage,
            OnEditClickListener editListener,
            OnIraqiHandoverClickListener iraqiListener) {

        this(shipments, isClearedPage, editListener, iraqiListener, null, false);
        // در صفحه ترخیص شده معمولاً نیازی به این فلگ نیست → false پیش‌فرض
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments.clear();
        if (shipments != null) {
            this.shipments.addAll(shipments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(parent.getContext());
        }
        int layoutId = isClearedPage ? R.layout.item_shipment_cleared : R.layout.item_shipment_list;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shipment shipment = shipments.get(position);

        String invNum = (shipment.getInvoiceNumber() == null || shipment.getInvoiceNumber().isEmpty())
                ? "F" + shipment.getInvoiceId()
                : shipment.getInvoiceNumber();

        holder.tvInvoiceNumber.setText("فاکتور: " + invNum);
        holder.tvTruck.setText("ماشین: " + nullToDash(shipment.getTruckName()));
        holder.tvDriver.setText("راننده: " + nullToDash(shipment.getDriverName()));
        holder.tvDate.setText("تاریخ بارگیری: " + nullToDash(shipment.getLoadDate()));

        if (holder.tvTransportCost != null) {
            long cost = shipment.getTransportCost();
            String costText = cost == 0 ? "هزینه حمل: تعیین نشده" : "هزینه حمل: " + NumberFormatter.formatNumber(cost) + " تومان";
            holder.tvTransportCost.setText(costText);
        }

        if (isClearedPage) {
            // صفحه ترخیص شده - بدون تغییر در این بخش
            Clearance clearance = dbHelper.getClearanceByShipmentId(shipment.getId());

            if (clearance != null) {
                if (holder.tvClearanceName != null) holder.tvClearanceName.setText("ترخیص‌کار ایرانی: " + clearance.getClearanceName());
                if (holder.tvClearancePhone != null) holder.tvClearancePhone.setText("تلفن: " + clearance.getClearancePhone());
                if (holder.tvBorderName != null) holder.tvBorderName.setText("مرز: " + clearance.getBorderName());
                if (holder.tvClearanceCost != null) holder.tvClearanceCost.setText("هزینه ترخیص: " + NumberFormatter.formatNumber(clearance.getClearanceCost()) + " تومان");
            }

            IraqiHandover iraqi = dbHelper.getIraqiHandoverByShipmentId(shipment.getId());
            if (iraqi != null && holder.tvIraqiInfo != null) {
                String text = "تحویل به ترخیص‌کار عراقی:\n" +
                        "نام: " + iraqi.getClearanceName() + "\n" +
                        "تلفن: " + iraqi.getClearancePhone() + "\n" +
                        "مرز: " + iraqi.getBorderName() + "\n" +
                        "هزینه: " + NumberFormatter.formatNumber(iraqi.getClearanceCost()) + " تومان";
                holder.tvIraqiInfo.setText(text);
                holder.tvIraqiInfo.setVisibility(View.VISIBLE);
                holder.tvIraqiInfo.setBackgroundColor(Color.TRANSPARENT);
            } else if (holder.tvIraqiInfo != null) {
                holder.tvIraqiInfo.setVisibility(View.GONE);
            }

            if (holder.btnEditShipment != null) {
                if (editListener != null) {
                    holder.btnEditShipment.setVisibility(View.VISIBLE);
                    holder.btnEditShipment.setText(clearance == null ? "ثبت ترخیص" : "ترخیصکار ایران");
                    holder.btnEditShipment.setOnClickListener(v -> editListener.onEdit(shipment));
                } else {
                    holder.btnEditShipment.setVisibility(View.GONE);
                }
            }

            // دکمه‌های دیگر بدون تغییر
            if (holder.btnIraqiHandover != null) {
                holder.btnIraqiHandover.setVisibility(iraqiListener != null ? View.VISIBLE : View.GONE);
                if (iraqiListener != null) {
                    holder.btnIraqiHandover.setOnClickListener(v -> iraqiListener.onIraqiHandover(shipment));
                }
            }

            if (holder.btnBuyerDelivery != null) {
                holder.btnBuyerDelivery.setVisibility(buyerListener != null ? View.VISIBLE : View.GONE);
                if (buyerListener != null) {
                    holder.btnBuyerDelivery.setOnClickListener(v -> buyerListener.onBuyerDelivery(shipment));
                }
            }

        } else {
            // تب در راه مرز یا بارگیری شده
            if (holder.btnEditShipment != null && editListener != null) {
                holder.btnEditShipment.setVisibility(View.VISIBLE);

                // تصمیم‌گیری متن دکمه بر اساس تب
                if (isFromInTransitTab) {
                    holder.btnEditShipment.setText("ثبت ترخیص");
                } else {
                    holder.btnEditShipment.setText("ویرایش بارگیری");   // یا فقط "ویرایش"
                }

                holder.btnEditShipment.setOnClickListener(v -> editListener.onEdit(shipment));
            }
        }
    }

    private String nullToDash(String value) {
        return value != null ? value : "-";
    }

    @Override
    public int getItemCount() {
        return shipments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // ... بدون تغییر
        TextView tvInvoiceNumber, tvTruck, tvDriver, tvDate, tvTransportCost;
        TextView tvClearanceName, tvClearancePhone, tvBorderName, tvClearanceCost;
        TextView tvIraqiInfo, tvBuyerDeliveryInfo;
        Button btnEditShipment, btnIraqiHandover, btnBuyerDelivery;

        ViewHolder(View itemView) {
            super(itemView);
            // ... بدون تغییر در findViewById ها
            tvInvoiceNumber = itemView.findViewById(R.id.tvInvoiceNumber);
            tvTruck = itemView.findViewById(R.id.tvTruck);
            tvDriver = itemView.findViewById(R.id.tvDriver);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTransportCost = itemView.findViewById(R.id.tvTransportCost);
            tvClearanceName = itemView.findViewById(R.id.tvClearanceName);
            tvClearancePhone = itemView.findViewById(R.id.tvClearancePhone);
            tvBorderName = itemView.findViewById(R.id.tvBorderName);
            tvClearanceCost = itemView.findViewById(R.id.tvClearanceCost);
            tvIraqiInfo = itemView.findViewById(R.id.tvIraqiInfo);
            tvBuyerDeliveryInfo = itemView.findViewById(R.id.tvBuyerDeliveryInfo);
            btnEditShipment = itemView.findViewById(R.id.btnEditShipment);
            btnIraqiHandover = itemView.findViewById(R.id.btnIraqiHandover);
            btnBuyerDelivery = itemView.findViewById(R.id.btnBuyerDelivery);
        }
    }
}