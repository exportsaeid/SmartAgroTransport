//package com.example.SmartAgroTransport.utils;
//
//import android.annotation.SuppressLint;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//
//import androidx.work.OneTimeWorkRequest;
//import androidx.work.WorkManager;
//
//import java.util.Calendar;
//import java.util.concurrent.TimeUnit;
//
//public class BackupReceiver extends BroadcastReceiver {
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        OneTimeWorkRequest backupRequest = new OneTimeWorkRequest.Builder(BackupWorker.class)
//                .setInitialDelay(0, TimeUnit.MILLISECONDS)
//                .build();
//        WorkManager.getInstance(context).enqueue(backupRequest);
//    }
//
//    @SuppressLint("ScheduleExactAlarm")
//    public static void scheduleDailyBackup(Context context) {
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.HOUR_OF_DAY, 2);
//        calendar.set(Calendar.MINUTE, 30);
//        calendar.set(Calendar.SECOND, 0);
//
//        if (calendar.before(Calendar.getInstance())) {
//            calendar.add(Calendar.DAY_OF_MONTH, 1);
//        }
//
//        long triggerAtMillis = calendar.getTimeInMillis();
//
//        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(context, BackupReceiver.class);
//        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
//                context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
//
//        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
//    }
//}
package com.example.SmartAgroTransport.utils;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class BackupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        OneTimeWorkRequest backupRequest =
                new OneTimeWorkRequest.Builder(BackupWorker.class)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                10, // هر 10 ثانیه تلاش مجدد
                                TimeUnit.SECONDS
                        )
                        .setConstraints(
                                new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build()
                        )
                        .build();

        WorkManager.getInstance(context).enqueue(backupRequest);
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleDailyBackup(Context context) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        long triggerAtMillis = calendar.getTimeInMillis();

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, BackupReceiver.class);

        android.app.PendingIntent pendingIntent =
                android.app.PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                                android.app.PendingIntent.FLAG_IMMUTABLE
                );

        alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
        );
    }
}
