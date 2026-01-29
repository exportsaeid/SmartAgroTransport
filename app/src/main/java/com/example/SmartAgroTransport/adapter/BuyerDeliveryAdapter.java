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

public class BuyerDeliveryAdapter extends RecyclerView.Adapter<BuyerDeliveryAdapter.ViewHolder> {

    private List<Shipment> shipments = new ArrayList<>();
    private final boolean isClearedPage;
    private final OnEditClickListener editListener;
    private final OnIraqiHandoverClickListener iraqiListener;
    private final OnBuyerDeliveryClickListener buyerListener;
    private DatabaseHelper dbHelper;

    // اینترفیس برای ویرایش بارگیری یا ترخیص ایرانی
    public interface OnEditClickListener {
        void onEdit(Shipment shipment);
    }

    // اینترفیس برای تحویل به طرف عراقی
    public interface OnIraqiHandoverClickListener {
        void onIraqiHandover(Shipment shipment);
    }

    // اینترفیس جدید برای تحویل به خریدار
    public interface OnBuyerDeliveryClickListener {
        void onBuyerDelivery(Shipment shipment);
    }

    // سازنده کامل (برای صفحه ترخیص و تحویل به خریدار)
    public BuyerDeliveryAdapter(List<Shipment> shipments, boolean isClearedPage,
                                OnEditClickListener editListener,
                                OnIraqiHandoverClickListener iraqiListener,
                                OnBuyerDeliveryClickListener buyerListener) {
        this.shipments = shipments != null ? new ArrayList<>(shipments) : new ArrayList<>();
        this.isClearedPage = isClearedPage;
        this.editListener = editListener;
        this.iraqiListener = iraqiListener;
        this.buyerListener = buyerListener;
    }

    // سازنده برای صفحه بارگیری (فقط ویرایش بارگیری)
    public BuyerDeliveryAdapter(List<Shipment> shipments, boolean isClearedPage, OnEditClickListener listener) {
        this(shipments, isClearedPage, listener, null, null);
    }

    // سازنده برای صفحه ترخیص (ویرایش ترخیص + تحویل عراقی)
    public BuyerDeliveryAdapter(List<Shipment> shipments, boolean isClearedPage,
                                OnEditClickListener editListener, OnIraqiHandoverClickListener iraqiListener) {
        this(shipments, isClearedPage, editListener, iraqiListener, null);
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
        int layoutId = isClearedPage ? R.layout.item_buyer_delivery : R.layout.item_buyer_delivery;
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
//        holder.tvTruck.setText("ماشین: " + (shipment.getTruckName() != null ? shipment.getTruckName() : "-"));
//        holder.tvDriver.setText("راننده: " + (shipment.getDriverName() != null ? shipment.getDriverName() : "-"));
//        holder.tvDate.setText("تاریخ بارگیری: " + (shipment.getLoadDate() != null ? shipment.getLoadDate() : "-"));
        BuyerDelivery buyerDelivery = dbHelper.getBuyerDeliveryByShipmentId(shipment.getId());
        if (buyerDelivery != null && holder.tvBuyerDeliveryInfo != null) {
            String text = "تحویل به خریدار:\n" +
                    "نام: " + buyerDelivery.getBuyerName() + "\n" +
                    "تلفن: " + (buyerDelivery.getBuyerPhone() != null ? buyerDelivery.getBuyerPhone() : "-") + "\n" +
                    "حجره: " + (buyerDelivery.getHajraNumber() != null ? buyerDelivery.getHajraNumber() : "-") + "\n" +
                    "مبلغ دریافتی: " + NumberFormatter.formatNumber(buyerDelivery.getReceivedAmount()) + " تومان" + "\n" +
                    "تاریخ: " + buyerDelivery.getDeliveryDate();
            holder.tvBuyerDeliveryInfo.setText(text);
            holder.tvBuyerDeliveryInfo.setVisibility(View.VISIBLE);

        } else if (holder.tvBuyerDeliveryInfo != null) {
            holder.tvBuyerDeliveryInfo.setVisibility(View.GONE);
        }
        if (holder.tvTransportCost != null) {
            long cost = shipment.getTransportCost();
//            String costText = cost == 0 ? "هزینه حمل: تعیین نشده" : "هزینه حمل: " + NumberFormatter.formatNumber(cost) + " تومان";
//            holder.tvTransportCost.setText(costText);
        }

        // فقط در صفحه ترخیص شده
        if (isClearedPage) {
            // تعریف متغیر clearance — اینجا باید باشد!
            Clearance clearance = dbHelper.getClearanceByShipmentId(shipment.getId());

            if (clearance != null) {
//                if (holder.tvClearanceName != null)
//                    holder.tvClearanceName.setText("ترخیص‌کار ایرانی: " + clearance.getClearanceName());
//                if (holder.tvClearancePhone != null)
//                    holder.tvClearancePhone.setText("تلفن: " + clearance.getClearancePhone());
//                if (holder.tvBorderName != null)
//                    holder.tvBorderName.setText("مرز: " + clearance.getBorderName());
//                if (holder.tvClearanceCost != null)
//                    holder.tvClearanceCost.setText("هزینه ترخیص: " + NumberFormatter.formatNumber(clearance.getClearanceCost()) + " تومان");
            }
            IraqiHandover iraqi = dbHelper.getIraqiHandoverByShipmentId(shipment.getId());
            if (iraqi != null && holder.tvIraqiInfo != null) {
//                String text = "تحویل به ترخیص‌کار عراقی:\n" +
//                        "نام: " + iraqi.getClearanceName() + "\n" +
//                        "تلفن: " + iraqi.getClearancePhone() + "\n" +
//                        "مرز: " + iraqi.getBorderName() + "\n" +
//                        "هزینه : " + NumberFormatter.formatNumber(iraqi.getClearanceCost()) + " تومان";
//                holder.tvIraqiInfo.setText(text);
//                holder.tvIraqiInfo.setVisibility(View.VISIBLE);
////                holder.tvIraqiInfo.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_light));
//                holder.tvIraqiInfo.setBackgroundColor(Color.parseColor("#DBDBDB"));
            } else if (holder.tvIraqiInfo != null) {
                holder.tvIraqiInfo.setVisibility(View.GONE);
            }

            // دکمه ویرایش ترخیص ایرانی — حالا clearance تعریف شده و خطا رفع می‌شه
            if (holder.btnEditShipment != null) {
                if (editListener != null) {
                    holder.btnEditShipment.setVisibility(View.VISIBLE);
                    holder.btnEditShipment.setText(clearance == null ? "ثبت ترخیص" : "ویرایش ترخیص");
                    holder.btnEditShipment.setOnClickListener(v -> editListener.onEdit(shipment));
                } else {
                    holder.btnEditShipment.setVisibility(View.GONE);
                }
            }


            // دکمه‌های دیگر
            if (holder.btnIraqiHandover != null) {
                if (iraqiListener != null) {
                    holder.btnIraqiHandover.setVisibility(View.VISIBLE);
                    holder.btnIraqiHandover.setOnClickListener(v -> iraqiListener.onIraqiHandover(shipment));
                } else {
                    holder.btnIraqiHandover.setVisibility(View.GONE);
                }
            }

            if (holder.btnBuyerDelivery != null) {
                if (buyerListener != null) {
                    holder.btnBuyerDelivery.setVisibility(View.VISIBLE);
                    holder.btnBuyerDelivery.setOnClickListener(v -> buyerListener.onBuyerDelivery(shipment));
                } else {
                    holder.btnBuyerDelivery.setVisibility(View.GONE);
                }
            }
        } else {
            // صفحه بارگیری
            if (holder.btnEditShipment != null && editListener != null) {
                holder.btnEditShipment.setVisibility(View.VISIBLE);
                holder.btnEditShipment.setText("ویرایش بارگیری");
                holder.btnEditShipment.setOnClickListener(v -> editListener.onEdit(shipment));
            }
        }
    }


    @Override
    public int getItemCount() {
        return shipments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInvoiceNumber, tvTruck, tvDriver, tvDate, tvTransportCost;
        TextView tvClearanceName, tvClearancePhone, tvBorderName, tvClearanceCost;
        TextView tvIraqiInfo, tvBuyerDeliveryInfo;
        Button btnEditShipment, btnIraqiHandover, btnBuyerDelivery;

        ViewHolder(View itemView) {
            super(itemView);
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
