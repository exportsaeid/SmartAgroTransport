package com.example.SmartAgroTransport.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.model.ProfitItem;
import com.example.SmartAgroTransport.utils.NumberFormatter;

import java.util.ArrayList;
import java.util.List;

public class ProfitReportAdapter extends RecyclerView.Adapter<ProfitReportAdapter.ViewHolder> {

    private List<ProfitItem> data = new ArrayList<>();

    public ProfitReportAdapter() {
        this.data = new ArrayList<>();
    }

    public void setData(List<ProfitItem> data) {
        this.data.clear();
        if (data != null) {
            this.data.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profit_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfitItem item = data.get(position);

        holder.tvInvoice.setText("فاکتور: " + item.getInvoiceNumber());
        holder.tvInvoiceAmount.setText("مبلغ فاکتور: " + NumberFormatter.formatNumber(item.getInvoiceAmount()) + " تومان");
        holder.tvIranianTransport.setText("کرایه ایرانی: " + NumberFormatter.formatNumber(item.getIranianTransport()) + " تومان");
        holder.tvIranianClearance.setText("ترخیص ایرانی: " + NumberFormatter.formatNumber(item.getIranianClearance()) + " تومان");
        holder.tvIraqiCost.setText(" ترخیص عراقی: " + NumberFormatter.formatNumber(item.getIraqiTotalCost()) + " تومان");
        holder.tvReceived.setText("واریزی خریدار: " + NumberFormatter.formatNumber(item.getReceivedAmount()) + " تومان");

        long profit = item.getProfit();
        holder.tvProfit.setText("سود/ضرر: " + NumberFormatter.formatNumber(profit) + " تومان");
        holder.tvProfit.setTextColor(profit >= 0 ?
                holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark) :
                holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInvoice, tvInvoiceAmount, tvIranianTransport, tvIranianClearance, tvIraqiCost, tvReceived, tvProfit;

        ViewHolder(View itemView) {
            super(itemView);
            tvInvoice = itemView.findViewById(R.id.tvInvoice);
            tvInvoiceAmount = itemView.findViewById(R.id.tvInvoiceAmount);
            tvIranianTransport = itemView.findViewById(R.id.tvIranianTransport);
            tvIranianClearance = itemView.findViewById(R.id.tvIranianClearance);
            tvIraqiCost = itemView.findViewById(R.id.tvIraqiCost);
            tvReceived = itemView.findViewById(R.id.tvReceived);
            tvProfit = itemView.findViewById(R.id.tvProfit);
        }
    }
}