package com.example.SmartAgroTransport.model;

public class BuyerDelivery {
    private long id;
    private long shipmentId;
    private String buyerName;
    private String buyerPhone;
    private String hajraNumber;  // شماره حجره خریدار
    private String deliveryDate;
    private long receivedAmount;
    private String notes;

    // Getter و Setterها
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getShipmentId() { return shipmentId; }
    public void setShipmentId(long shipmentId) { this.shipmentId = shipmentId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }

    public String getHajraNumber() { return hajraNumber; }
    public void setHajraNumber(String hajraNumber) { this.hajraNumber = hajraNumber; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public long getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(long receivedAmount) { this.receivedAmount = receivedAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}