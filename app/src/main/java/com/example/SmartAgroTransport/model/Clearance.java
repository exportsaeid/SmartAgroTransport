package com.example.SmartAgroTransport.model;

public class Clearance {
    private long id;
    private long shipmentId; // شناسه بارگیری مربوطه
    private String clearanceName; // نام ترخیصکار ایرانی
    private String clearancePhone; // تلفن ترخیصکار
    private String borderName; // نام مرز
    private String clearanceDate; // تاریخ تحویل به ترخیصکار
    private String notes; // توضیحات اختیاری
    private long clearanceCost; // هزینه ترخیص به تومان

    // سازنده خالی (برای استفاده در دیتابیس)
    public Clearance() {
    }

    // getter و setterها
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getClearanceName() {
        return clearanceName;
    }

    public void setClearanceName(String clearanceName) {
        this.clearanceName = clearanceName;
    }

    public String getClearancePhone() {
        return clearancePhone;
    }

    public void setClearancePhone(String clearancePhone) {
        this.clearancePhone = clearancePhone;
    }

    public String getBorderName() {
        return borderName;
    }

    public void setBorderName(String borderName) {
        this.borderName = borderName;
    }

    public String getClearanceDate() {
        return clearanceDate;
    }

    public void setClearanceDate(String clearanceDate) {
        this.clearanceDate = clearanceDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getClearanceCost() {
        return clearanceCost;
    }

    public void setClearanceCost(long clearanceCost) {
        this.clearanceCost = clearanceCost;
    }
}