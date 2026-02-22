package com.example.SmartAgroTransport.model;

public class ProfitItem {
    private String invoiceNumber;
    private long invoiceAmount; // مبلغ فاکتور
    private long iranianTransport;
    private long iranianClearance;
    private long iraqiTotalCost;
    private long receivedAmount;
    private long profit;
    private String buyerName;

    public ProfitItem() {}

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public long getInvoiceAmount() { return invoiceAmount; }
    public void setInvoiceAmount(long invoiceAmount) { this.invoiceAmount = invoiceAmount; }

    public long getIranianTransport() { return iranianTransport; }
    public void setIranianTransport(long iranianTransport) { this.iranianTransport = iranianTransport; }

    public long getIranianClearance() { return iranianClearance; }
    public void setIranianClearance(long iranianClearance) { this.iranianClearance = iranianClearance; }

    public long getIraqiTotalCost() { return iraqiTotalCost; }
    public void setIraqiTotalCost(long iraqiTotalCost) { this.iraqiTotalCost = iraqiTotalCost; }

    public long getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(long receivedAmount) { this.receivedAmount = receivedAmount; }

    public long getProfit() { return profit; }
    public void setProfit(long profit) { this.profit = profit; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
}