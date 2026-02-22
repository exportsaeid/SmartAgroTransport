package com.example.SmartAgroTransport.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Invoice implements Serializable {

    private long id;
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private String date;
    private long totalAmount; // فیلد اصلی جمع کل (مطابق دیتابیس: total_amount)
    private List<InvoiceItem> items = new ArrayList<>();

    // اگر قبلاً متد setGrandTotal داشتی، حالا درستش می‌کنیم
    // یا اگر نداشتی، اضافه می‌کنیم تا با کدهای قبلی سازگار بشه

    // Getter و Setter برای totalAmount (نام درست فیلد در دیتابیس)
    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    // متد جدید/اصلاح‌شده: setGrandTotal (که در کدهای قبلی استفاده می‌شد)
    // این متد فقط totalAmount رو تنظیم می‌کنه تا هیچ خطایی نده
    public void setGrandTotal(long grandTotal) {
        this.totalAmount = grandTotal;
    }

    // متد getGrandTotal هم اضافه می‌کنیم تا کامل باشه
    public long getGrandTotal() {
        return totalAmount;
    }

    // محاسبه خودکار جمع کل بر اساس آیتم‌ها (اختیاری – خیلی مفید)
    public long calculateGrandTotal() {
        long total = 0;
        for (InvoiceItem item : items) {
            total += (long) (item.getWeight() * item.getUnitPrice());
        }
        this.totalAmount = total;
        return total;
    }

    // بقیه getter و setterها (بدون تغییر)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}