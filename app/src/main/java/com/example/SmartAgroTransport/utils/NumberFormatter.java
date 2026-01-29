package com.example.SmartAgroTransport.utils;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputEditText;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * کلاس کمکی برای فرمت خودکار اعداد با جداکننده هزارگان (مثل 1,234,567)
 * استفاده در تمام دیالوگ‌ها و فیلدهای عددی اپ
 */
public class NumberFormatter {

    /**
     * اعمال فرمت جداکننده ۳ رقمی روی TextInputEditText
     * @param editText فیلد ورودی که می‌خواهیم فرمت روی آن اعمال شود
     */
    public static void addThousandSeparator(TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.equals(current)) {
                    return;
                }

                editText.removeTextChangedListener(this);

                // حذف کاماهای قبلی
                String cleanString = str.replaceAll("[,]", "");

                if (cleanString.isEmpty()) {
                    current = "";
                    editText.setText("");
                    editText.setSelection(0);
                    editText.addTextChangedListener(this);
                    return;
                }

                try {
                    long value = Long.parseLong(cleanString);
                    // فرمت با جداکننده هزارگان
                    String formatted = NumberFormat.getInstance(Locale.getDefault()).format(value);
                    current = formatted;
                    editText.setText(formatted);
                    editText.setSelection(formatted.length());
                } catch (NumberFormatException e) {
                    // اگر عدد خیلی بزرگ بود یا خطا داشت، نادیده بگیر
                }

                editText.addTextChangedListener(this);
            }
        });
    }

    /**
     * برگرداندن عدد بدون فرمت (برای ذخیره در دیتابیس)
     * @param formattedText متن فرمت شده مثل "1,234,567"
     * @return عدد خام مثل 1234567 یا 0 اگر خالی باشد
     */
    public static long getRawNumber(String formattedText) {
        if (formattedText == null || formattedText.trim().isEmpty()) {
            return 0;
        }
        String clean = formattedText.replaceAll("[,]", "").trim();
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * فرمت یک عدد برای نمایش (مثلاً در TextView)
     * @param number عدد خام
     * @return رشته فرمت شده مثل "1,234,567"
     */
    public static String formatNumber(long number) {
        return NumberFormat.getInstance(Locale.getDefault()).format(number);
    }
}