package com.example.SmartAgroTransport.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.Invoice;
import com.example.SmartAgroTransport.model.InvoiceItem;

import java.util.ArrayList;
import java.util.List;

public class InvoiceListAdapter extends RecyclerView.Adapter<InvoiceListAdapter.ViewHolder> {

    private List<Invoice> invoices = new ArrayList<>();
    private OnInvoiceClickListener listener;
    private DatabaseHelper dbHelper;
    private int buttonType; // 1=ثبت بارگیری, 2=ویرایش, 3=ثبت ترخیص, 4=ویرایش ترخیص

    public interface OnInvoiceClickListener {
        void onEdit(Invoice invoice);
        void onDelete(long invoiceId);
    }

    // سازنده جدید با پارامتر buttonType
    public InvoiceListAdapter(List<Invoice> invoices, OnInvoiceClickListener listener,
                              DatabaseHelper dbHelper, int buttonType) {
        this.invoices = invoices != null ? new ArrayList<>(invoices) : new ArrayList<>();
        this.listener = listener;
        this.dbHelper = dbHelper;
        this.buttonType = buttonType;
    }

    // سازنده قدیمی برای سازگاری
    public InvoiceListAdapter(List<Invoice> invoices, OnInvoiceClickListener listener, DatabaseHelper dbHelper) {
        this(invoices, listener, dbHelper, 1); // پیش‌فرض: ثبت بارگیری
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices.clear();
        if (invoices != null) {
            this.invoices.addAll(invoices);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice invoice = invoices.get(position);

        String customerName = invoice.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty()) {
            customerName = "مشتری نامشخص";
        }

        String invoiceDate = invoice.getDate() != null ? invoice.getDate() : "نامشخص";
        long total = 0;

        holder.tvTitle.setText("فاکتور #" + invoice.getInvoiceNumber() + " - " + customerName);
        holder.tvCustomer.setText("مشتری: " + customerName);
        holder.tvDate.setText("تاریخ: " + invoiceDate);

        for (InvoiceItem item : invoice.getItems()) {
            total += item.getWeight() * item.getUnitPrice();
        }

        holder.tvTotal.setText(String.format("جمع کل: %,d تومان", total));
        holder.tvItemCount.setText("تعداد آیتم: " + invoice.getItems().size());

        // ======== راه حل قطعی برای نمایش متن دکمه ========
        // اگر buttonType = 2 باشد، "ویرایش" نمایش داده شود
        // در غیر این صورت متن پیش‌فرض آداپتر استفاده شود
        if (buttonType == 2) {
            holder.btnEdit.setText("ویرایش");
        } else {
            // برای سایر حالت‌ها از switch-case استفاده می‌کنیم
            switch (buttonType) {
                case 1:
                    holder.btnEdit.setText("ثبت بارگیری");
                    break;
                case 3:
                    holder.btnEdit.setText("ثبت ترخیص");
                    break;
                case 4:
                    holder.btnEdit.setText("ویرایش ترخیص");
                    break;
                default:
                    holder.btnEdit.setText("ویرایش");
            }
        }
        // ===============================================

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(invoice);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            final String currentInvoiceNumber = invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().trim().isEmpty()
                    ? invoice.getInvoiceNumber()
                    : "F" + invoice.getId();

            if (dbHelper.hasShipment(invoice.getId())) {
                Toast.makeText(holder.itemView.getContext(),
                        "این فاکتور بارگیری شده است و قابل حذف نیست!",
                        Toast.LENGTH_LONG).show();
            } else {
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("حذف فاکتور")
                        .setMessage("آیا مطمئن هستید که می‌خواهید فاکتور #" + currentInvoiceNumber + " را حذف کنید؟")
                        .setPositiveButton("بله", (dialog, which) -> {
                            dbHelper.deleteInvoice(invoice.getId());

                            int pos = holder.getAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                invoices.remove(pos);
                                notifyItemRemoved(pos);
                                notifyItemRangeChanged(pos, invoices.size());
                            }

                            Toast.makeText(holder.itemView.getContext(),
                                    "فاکتور با موفقیت حذف شد",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("خیر", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCustomer, tvDate, tvTotal, tvItemCount, tvTransportCost;
        Button btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCustomer = itemView.findViewById(R.id.tvCustomer);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvTransportCost = itemView.findViewById(R.id.tvTransportCost);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}