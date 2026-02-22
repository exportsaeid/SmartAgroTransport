package com.example.SmartAgroTransport.presenter;

import com.example.SmartAgroTransport.model.Invoice;
import com.example.SmartAgroTransport.model.InvoiceItem;
import com.example.SmartAgroTransport.view.MainView;

import java.util.ArrayList;
import java.util.List;

public class InvoicePresenter {
    private MainView view;
    private List<InvoiceItem> items = new ArrayList<>();

    public InvoicePresenter(MainView view) {
        this.view = view;
    }

    public void onAddItemClicked() {
        if (view != null) {
            view.showAddItemDialog();
        }
    }

    public void addNewItem(String productName, long weight, long unitPrice,
                           String name, String mobile, String address,String notes) {
        if (productName == null || productName.trim().isEmpty()) {
            if (view != null) view.showToast("نام محصول را وارد کنید");
            return;
        }
        if (weight <= 0 || unitPrice <= 0) {
            if (view != null) view.showToast("وزن و قیمت باید بزرگتر از صفر باشند");
            return;
        }

        InvoiceItem item = new InvoiceItem(productName, weight, unitPrice, name, mobile, address,notes);
        items.add(item);
        if (view != null) {
            view.updateItemsList();
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            if (view != null) {
                view.updateItemsList();
            }
        }
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    // متد جدید: جمع کل مبلغ
    public long getGrandTotal() {
        long total = 0;
        for (InvoiceItem item : items) {
            total += item.getRowTotal();
        }
        return total;
    }

    public void onPreviewClicked() {
        if (items.isEmpty()) {
            if (view != null) view.showToast("حداقل یک آیتم اضافه کنید");
            return;
        }

        Invoice invoice = new Invoice();
        if (view != null) {
            invoice.setCustomerName(view.getCustomerName());
            invoice.setCustomerPhone(view.getCustomerPhone());
            invoice.setDate(view.getCurrentDate());
        }
        invoice.setItems(new ArrayList<>(items));

        if (view != null) {
            view.navigateToPreview(invoice);
        }
    }

    public void detachView() {
        this.view = null;
    }
}