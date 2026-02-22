package com.example.SmartAgroTransport.view;

import com.example.SmartAgroTransport.model.Invoice;

public interface MainView {
    void updateItemsList();
    void updateTotalAmount(long total);
    void showToast(String message);
    void showAddItemDialog();
    void navigateToPreview(Invoice invoice);

    String getCustomerName();
    String getCustomerPhone();
    String getCurrentDate();
}