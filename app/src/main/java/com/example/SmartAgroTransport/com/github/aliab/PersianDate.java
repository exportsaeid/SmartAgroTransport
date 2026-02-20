package com.example.SmartAgroTransport.com.github.aliab;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class PersianDate {
    private int year;
    private int month;
    private int day;

    public PersianDate() {
        GregorianCalendar gc = new GregorianCalendar();
        setPersianDate(gc.get(Calendar.YEAR), gc.get(Calendar.MONTH) + 1, gc.get(Calendar.DAY_OF_MONTH));
    }

    private void setPersianDate(int gregorianYear, int gregorianMonth, int gregorianDay) {
        int[] persian = gregorianToPersian(gregorianYear, gregorianMonth, gregorianDay);
        this.year = persian[0];
        this.month = persian[1];
        this.day = persian[2];
    }

    private int[] gregorianToPersian(int gy, int gm, int gd) {
        int[] g_d_m = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int jy;
        if (gy <= 1600) {
            jy = 979;
            gy -= 621;
        } else {
            jy = 1600;
            gy -= 1600;
        }
        int gy2 = (gm > 2) ? (gy + 1) : gy;
        int days = (365 * gy) + ((int)((gy2 + 3) / 4)) - ((int)((gy2 + 99) / 100)) + ((int)((gy2 + 399) / 400)) - 80 + gd + g_d_m[gm - 1];
        jy += 33 * ((int)(days / 12053));
        days %= 12053;
        jy += 4 * ((int)(days / 1461));
        days %= 1461;
        if (days > 365) {
            jy += (int)((days - 1) / 365);
            days = (days - 1) % 365;
        }
        int jm = (days < 186) ? 1 + (int)(days / 31) : 7 + (int)((days - 186) / 30);
        int jd = 1 + ((days < 186) ? (days % 31) : ((days - 186) % 30));
        return new int[]{jy, jm, jd};
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public String format(String pattern) {
        return pattern.replace("Y", String.valueOf(year))
                .replace("m", String.format("%02d", month))
                .replace("d", String.format("%02d", day));
    }
}