package com.example.SmartAgroTransport.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.model.InvoiceItem;

import java.util.List;
import java.util.Locale;

public class InvoiceItemAdapter extends RecyclerView.Adapter<InvoiceItemAdapter.ViewHolder> {

    private final List<InvoiceItem> items;
    private final OnRemoveClickListener removeListener;
    private final OnEditClickListener editListener;

    public interface OnRemoveClickListener {
        void onRemove(int position);
    }

    public interface OnEditClickListener {
        void onEdit(int position);
    }

    public InvoiceItemAdapter(List<InvoiceItem> items,
                              OnRemoveClickListener removeListener,
                              OnEditClickListener editListener) {
        this.items = items;
        this.removeListener = removeListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvoiceItem item = items.get(position);

        holder.tvRow.setText(String.valueOf(position + 1));
        holder.tvProductName.setText(item.getProductName());
        holder.tvWeight.setText(item.getWeightDisplay());
        holder.tvUnitPrice.setText(String.format(Locale.getDefault(), "%,d", item.getUnitPrice()));
        holder.tvName.setText(item.getName());
        holder.tvMobile.setText(item.getMobile());
        holder.tvAddress.setText(item.getAddress());
        holder.tvNotes.setText(item.getNotes() != null && !item.getNotes().trim().isEmpty()
                ? item.getNotes()
                : "—");   // نمایش توضیحات یا خط تیره
        holder.tvRowTotal.setText(String.format(Locale.getDefault(), "%,d", item.getRowTotal()));

        // دکمه حذف
        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("حذف محصول")
                        .setMessage("آیا مطمئن هستید که می‌خواهید این ردیف را حذف کنید؟")
                        .setPositiveButton("بله", (dialog, which) -> {
                            removeListener.onRemove(position);
                            Toast.makeText(holder.itemView.getContext(),
                                    "ردیف حذف شد",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("خیر", null)
                        .show();
            }
        });

        // دکمه ویرایش
        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEdit(position);
            }
        });

        // رنگ‌بندی سطرها
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F8F9FA"));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRow, tvProductName, tvWeight, tvUnitPrice;
        TextView tvName, tvMobile, tvAddress, tvNotes, tvRowTotal;
        ImageButton btnRemove, btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            tvRow = itemView.findViewById(R.id.tvRow);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvName = itemView.findViewById(R.id.tvName);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvAddress = itemView.findViewById(R.id.tvAddress);     // توجه: قبلاً tvَAddress بود (با فتحه)
            tvNotes = itemView.findViewById(R.id.tvNotes);         // جدید
            tvRowTotal = itemView.findViewById(R.id.tvRowTotal);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}