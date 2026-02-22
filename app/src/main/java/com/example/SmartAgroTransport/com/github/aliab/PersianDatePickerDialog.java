package com.example.SmartAgroTransport.com.github.aliab;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import java.util.GregorianCalendar;

public class PersianDatePickerDialog extends AlertDialog {

    private NumberPicker yearPicker;
    private NumberPicker monthPicker;
    private NumberPicker dayPicker;
    private OnDateSetListener listener;

    public interface OnDateSetListener {
        void onDateSet(PersianDate persianDate);
    }

    public PersianDatePickerDialog(Context context) {
        super(context);

        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null); // فقط برای تست — می‌تونی layout سفارشی بسازی
        setView(view);

        PersianDate today = new PersianDate();

        yearPicker = new NumberPicker(context);
        monthPicker = new NumberPicker(context);
        dayPicker = new NumberPicker(context);

        yearPicker.setMinValue(today.getYear() - 50);
        yearPicker.setMaxValue(today.getYear() + 10);
        yearPicker.setValue(today.getYear());

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(today.getMonth());

        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(31);
        dayPicker.setValue(today.getDay());

        setButton(BUTTON_POSITIVE, "تایید", (dialog, which) -> {
            PersianDate selected = new PersianDate();
            selected = new PersianDate(); // برای تنظیم دستی
            // تنظیم دستی
            GregorianCalendar gc = new GregorianCalendar(yearPicker.getValue(), monthPicker.getValue() - 1, dayPicker.getValue());
            selected = new PersianDate(); // دوباره برای بروزرسانی
            if (listener != null) {
                listener.onDateSet(selected);
            }
        });

        setButton(BUTTON_NEGATIVE, "لغو", (DialogInterface.OnClickListener) null);
    }

    public void setListener(OnDateSetListener listener) {
        this.listener = listener;
    }
}