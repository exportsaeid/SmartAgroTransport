package com.example.SmartAgroTransport.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class BackupWorker extends Worker {

    private static final String DB_NAME = "invoice_db";
    private static final String CHANNEL_ID = "backup_channel";

    private static final String SENDER_EMAIL = "export.saeid@gmail.com"; // ایمیل خودت
    private static final String APP_PASSWORD = "ksfr goel btwo nxch"; // App Password 16 رقمی
    private static final String RECEIVER_EMAIL = "export.saeid@gmail.com"; // گیرنده

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        try {
            File dbFile = getApplicationContext().getDatabasePath(DB_NAME);

            if (!dbFile.exists()) {
                showNotification("خطا در بکاپ", "دیتابیس پیدا نشد، تلاش مجدد انجام می‌شود");
                return Result.retry();
            }

            File zipFile = new File(getApplicationContext().getCacheDir(), "invoice_db.zip");
            zipDatabase(dbFile, zipFile);

            sendEmail(zipFile);

            showNotification("بکاپ موفق", "دیتابیس با موفقیت ایمیل شد");
            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            showNotification("خطا در بکاپ", "ارسال ایمیل ناموفق بود، تلاش مجدد انجام می‌شود");
            return Result.retry(); // نامحدود تا موفق شود
        }
    }

//    @NonNull
//    @Override

//    public Result doWork() {
//        try {
//            File dbFile = getApplicationContext().getDatabasePath(DB_NAME);
//            if (!dbFile.exists()) {
//                showNotification("خطا در بکاپ", "دیتابیس پیدا نشد");
//                return Result.failure();
//            }
//
//            // زیپ کردن دیتابیس
//            File zipFile = new File(getApplicationContext().getCacheDir(), "invoice_db.zip");
//            zipDatabase(dbFile, zipFile);
//
//            // ارسال ایمیل
//            sendEmail(zipFile);
//
//            showNotification("بکاپ موفق", "دیتابیس با موفقیت ایمیل شد");
//            return Result.success();
//
//        } catch (Exception e) {
//            showNotification("خطا در بکاپ", e.getMessage());
//            e.printStackTrace();
//            return getRunAttemptCount() > 2 ? Result.failure() : Result.retry();
//        }
//    }

    private void zipDatabase(File dbFile, File zipFile) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
             FileInputStream fis = new FileInputStream(dbFile)) {
            ZipEntry entry = new ZipEntry(dbFile.getName());
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
            zos.closeEntry();
        }
    }

    private void sendEmail(File attachment) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.connectiontimeout", "60000");
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.writetimeout", "60000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECEIVER_EMAIL));
        message.setSubject("بکاپ خودکار دیتابیس SmartAgroTransport");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("سلام،\nفایل بکاپ دیتابیس اپلیکیشن پیوست شده است.");

        MimeBodyPart filePart = new MimeBodyPart();
        try {
            filePart.attachFile(attachment);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MessagingException("خطا در افزودن فایل به ایمیل: " + e.getMessage());
        }

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(filePart);

        message.setContent(multipart);

        Transport.send(message);
    }

    private void showNotification(String title, String message) {
        NotificationManager nm = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "بکاپ خودکار", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), b.build());
    }
}
