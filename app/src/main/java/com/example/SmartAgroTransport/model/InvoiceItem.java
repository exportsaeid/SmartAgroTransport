package com.example.SmartAgroTransport.model;
import java.io.Serializable;

public class InvoiceItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productName;
    private long weight;
    private long unitPrice;
    private String name;
    private String mobile;
    private String address;
    private String notes;  // فیلد جدید: توضیحات

    public InvoiceItem(String productName, long weight, long unitPrice,
                       String name, String mobile, String address, String notes) {
        this.productName = productName != null ? productName.trim() : "";
        this.weight = weight > 0 ? weight : 0;
        this.unitPrice = unitPrice;
        this.name = name != null ? name.trim() : "";
        this.mobile = mobile != null ? mobile.trim() : "";
        this.address = address != null ? address.trim() : "";
        this.notes = notes != null ? notes.trim() : "";  // تنظیم notes
    }

    public long getRowTotal() {
        return Math.round(weight * unitPrice);
    }

    // اصلاح شده: اگر وزن عدد صحیح باشه، بدون اعشار نمایش بده
    public String getWeightDisplay() {
        if (weight == Math.floor(weight)) {
            // عدد صحیح — فقط عدد بدون .00
            return String.format("%,d", (long) weight);
        } else {
            // عدد اعشاری — با دو رقم اعشار
            return String.format("%,.2f", weight);
        }
    }

    // گترها
    public String getProductName() { return productName; }
    public double getWeight() { return weight; }
    public long getUnitPrice() { return unitPrice; }
    public String getName() { return name; }
    public String getMobile() { return mobile; }
    public String getAddress() { return address; }
    public String getNotes() { return notes; }  // getter جدید
    // Setter برای notes (در صورت نیاز برای ویرایش)
    public void setNotes(String notes) { this.notes = notes != null ? notes.trim() : ""; }
}