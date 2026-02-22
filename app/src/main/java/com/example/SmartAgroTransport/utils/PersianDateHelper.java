package com.example.SmartAgroTransport.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import com.example.SmartAgroTransport.R;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class PersianDateHelper {

    public static String getCurrentPersianDate() {
        GregorianCalendar gc = new GregorianCalendar();
        int[] p = gregorianToPersian(
                gc.get(Calendar.YEAR),
                gc.get(Calendar.MONTH) + 1,
                gc.get(Calendar.DAY_OF_MONTH)
        );
        return String.format(Locale.US, "%d/%02d/%02d", p[0], p[1], p[2]);
    }

    public static int[] gregorianToPersian(int gy, int gm, int gd) {
        int jy = (gy <= 1600) ? 0 : 979;
        gy -= (gy <= 1600) ? 621 : 1600;
        int gy2 = (gm > 2) ? (gy + 1) : gy;
        int days = (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100)
                + ((gy2 + 399) / 400) - 80 + gd
                + new int[]{0,31,59,90,120,151,181,212,243,273,304,334}[gm - 1];

        jy += 33 * (days / 12053);
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }

        int jm = (days < 186) ? 1 + days / 31 : 7 + (days - 186) / 30;
        int jd = 1 + ((days < 186) ? (days % 31) : ((days - 186) % 30));

        return new int[]{jy, jm, jd};
    }

//    public static void showPersianDatePicker(Context context, TextInputEditText target) {
//        View view = LayoutInflater.from(context).inflate(R.layout.dialog_persian_date_picker, null);
//        NumberPicker pY = view.findViewById(R.id.picker_year);
//        NumberPicker pM = view.findViewById(R.id.picker_month);
//        NumberPicker pD = view.findViewById(R.id.picker_day);
//
//        pY.setMinValue(1400);
//        pY.setMaxValue(1450);
//        pM.setMinValue(1);
//        pM.setMaxValue(12);
//        pD.setMinValue(1);
//        pD.setMaxValue(31);
//
//        // اگر EditText خالی یا نامعتبر بود، تاریخ روز جاری ست شود
//        String current = target.getText().toString();
//        int y, m, d;
//        try {
//            String[] parts = current.split("/");
//            y = Integer.parseInt(parts[0]);
//            m = Integer.parseInt(parts[1]);
//            d = Integer.parseInt(parts[2]);
//        } catch (Exception e) {
//            String today = getCurrentPersianDate();
//            String[] parts = today.split("/");
//            y = Integer.parseInt(parts[0]);
//            m = Integer.parseInt(parts[1]);
//            d = Integer.parseInt(parts[2]);
//        }
//
//        pY.setValue(y);
//        pM.setValue(m);
//        pD.setValue(d);
//
//        new AlertDialog.Builder(context)
//                .setTitle("")
//                .setView(view)
//                .setPositiveButton("تایید", (dialog, which) -> {
//                    String date = String.format(Locale.US, "%d/%02d/%02d", pY.getValue(), pM.getValue(), pD.getValue());
//                    target.setText(date);
//                })
//                .setNegativeButton("لغو", null)
//                .show();
//    }
public static void showPersianDatePicker(Context context, com.google.android.material.textfield.TextInputEditText target) {
    View view = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_persian_date_picker, null);

    NumberPicker pY = view.findViewById(R.id.picker_year);
    NumberPicker pM = view.findViewById(R.id.picker_month);
    NumberPicker pD = view.findViewById(R.id.picker_day);
    android.widget.TextView tvPreview = view.findViewById(R.id.tv_date_preview);

    pY.setMinValue(1400); pY.setMaxValue(1450);
    pM.setMinValue(1); pM.setMaxValue(12);
    pD.setMinValue(1); pD.setMaxValue(31);

    // استخراج تاریخ فعلی از فیلد
    String current = target.getText().toString();
    int y, m, d;
    try {
        String[] parts = current.split("/");
        y = Integer.parseInt(parts[0]);
        m = Integer.parseInt(parts[1]);
        d = Integer.parseInt(parts[2]);
    } catch (Exception e) {
        String today = getCurrentPersianDate();
        String[] parts = today.split("/");
        y = Integer.parseInt(parts[0]);
        m = Integer.parseInt(parts[1]);
        d = Integer.parseInt(parts[2]);
    }

    pY.setValue(y); pM.setValue(m); pD.setValue(d);

    // تابع کمکی برای آپدیت متن هدر
    Runnable updatePreview = () -> {
        String dateStr = String.format(java.util.Locale.US, "%d/%02d/%02d", pY.getValue(), pM.getValue(), pD.getValue());
        tvPreview.setText(dateStr);
    };

    // ست کردن مقدار اولیه هدر
    updatePreview.run();

    // تعریف Listener برای تغییرات لحظه‌ای
    NumberPicker.OnValueChangeListener onValueChangeListener = (picker, oldVal, newVal) -> updatePreview.run();

    pY.setOnValueChangedListener(onValueChangeListener);
    pM.setOnValueChangedListener(onValueChangeListener);
    pD.setOnValueChangedListener(onValueChangeListener);

//    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
//            .setView(view)
//            .setPositiveButton("تایید", (dialogInterface, which) -> {
//                // اینجا از مقادیر پکرها استفاده می‌کنیم
//                String finalDate = String.format(java.util.Locale.US, "%d/%02d/%02d",
//                        pY.getValue(), pM.getValue(), pD.getValue());
//                target.setText(finalDate);
//            })
//            .setNegativeButton("لغو", null)
//            .create();;
    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
            .setView(view)
            .setPositiveButton("تایید", (dialogInterface, which) -> {
                String finalDate = String.format(java.util.Locale.US, "%d/%02d/%02d",
                        pY.getValue(), pM.getValue(), pD.getValue());
                target.setText(finalDate);
            })
            .setNegativeButton("لغو", null)
            .setNeutralButton("امروز", (dialogInterface, which) -> {
                // دریافت تاریخ امروز و ست کردن مستقیم روی EditText
                target.setText(getCurrentPersianDate());
            })
            .create();

    dialog.show();

    // رنگ‌بندی دکمه‌ها برای جذابیت بیشتر
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#2E7D32")); // سبز
    dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#757575")); // خاکستری
    dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(android.graphics.Color.parseColor("#1976D2"));  // آبی
    dialog.show();

    // رنگ سبز برای دکمه تایید
    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#2E7D32"));
}
}
