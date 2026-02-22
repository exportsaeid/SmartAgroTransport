package com.example.SmartAgroTransport.model;

public class Shipment {
    private long id;
    private long invoiceId;
    private String invoiceNumber; // ذخیره مستقیم شماره فاکتور
    private String truckName;
    private String plateNumber;
    private String driverName;
    private String driverPhone;
    private String loadDate;
    private long transportCost = 0; // فیلد جدید: هزینه حمل به تومان (پیش‌فرض 0)

    public Shipment() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getTruckName() {
        return truckName;
    }

    public void setTruckName(String truckName) {
        this.truckName = truckName;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(String loadDate) {
        this.loadDate = loadDate;
    }

    // getter و setter برای هزینه حمل
    public long getTransportCost() {
        return transportCost;
    }

    public void setTransportCost(long transportCost) {
        this.transportCost = transportCost;
    }

    // اختیاری: برای دیباگ بهتر
    @Override
    public String toString() {
        return "Shipment{" +
                "id=" + id +
                ", invoiceId=" + invoiceId +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", truckName='" + truckName + '\'' +
                ", plateNumber='" + plateNumber + '\'' +
                ", driverName='" + driverName + '\'' +
                ", driverPhone='" + driverPhone + '\'' +
                ", loadDate='" + loadDate + '\'' +
                ", transportCost=" + transportCost +
                '}';
    }
}