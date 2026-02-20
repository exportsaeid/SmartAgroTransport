// فایل: src/main/java/com/example/SmartAgroTransport/model/IraqiHandover.java

package com.example.SmartAgroTransport.model;

public class IraqiHandover {
    private long id;
    private long shipmentId;           // کلید خارجی به جدول shipments (بارگیری ایرانی)

    private String clearanceName;      // نام ترخیص‌کار عراقی
    private String clearancePhone;     // تلفن ترخیص‌کار عراقی
    private String borderName;         // نام مرز
    private String clearanceDate;      // تاریخ تحویل به طرف عراقی
    private String notes;              // توضیحات (مثل شماره پلاک عراقی، مشکلات و ...)
    private long clearanceCost;        // هزینه اضافی (تخلیه، بارگیری مجدد، انبارداری و ...)

    public IraqiHandover() {}

    // Getter و Setterها
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getShipmentId() { return shipmentId; }
    public void setShipmentId(long shipmentId) { this.shipmentId = shipmentId; }

    public String getClearanceName() { return clearanceName; }
    public void setClearanceName(String clearanceName) { this.clearanceName = clearanceName; }

    public String getClearancePhone() { return clearancePhone; }
    public void setClearancePhone(String clearancePhone) { this.clearancePhone = clearancePhone; }

    public String getBorderName() { return borderName; }
    public void setBorderName(String borderName) { this.borderName = borderName; }

    public String getClearanceDate() { return clearanceDate; }
    public void setClearanceDate(String clearanceDate) { this.clearanceDate = clearanceDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getClearanceCost() { return clearanceCost; }
    public void setClearanceCost(long clearanceCost) { this.clearanceCost = clearanceCost; }
}