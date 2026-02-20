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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private static final String ATTACHMENTS_FOLDER = "invoice_attachments";
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

            // ایجاد فایل زیپ کامل شامل دیتابیس و attachments
            File zipFile = new File(getApplicationContext().getCacheDir(),
                    "backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".zip");

            createFullBackupZip(dbFile, zipFile);

            sendEmail(zipFile);

            showNotification("بکاپ موفق", "بکاپ کامل با موفقیت ایمیل شد");
            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            showNotification("خطا در بکاپ", "ارسال ایمیل ناموفق بود: " + e.getMessage());
            return Result.retry();
        }
    }

    /**
     * ایجاد فایل زیپ کامل شامل دیتابیس و تمام فایل‌های پیوست
     */
    private void createFullBackupZip(File dbFile, File zipFile) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

            // 1. اضافه کردن فایل دیتابیس
            addFileToZip(zos, dbFile, "database/");

            // 2. اضافه کردن فایل‌های همراه دیتابیس (wal, shm, journal)
            String[] extensions = {"-wal", "-shm", "-journal"};
            for (String ext : extensions) {
                File companionFile = new File(dbFile.getAbsolutePath() + ext);
                if (companionFile.exists()) {
                    addFileToZip(zos, companionFile, "database/");
                }
            }

            // 3. اضافه کردن فایل‌های پیوست
            File attachmentsDir = new File(getApplicationContext().getFilesDir(), ATTACHMENTS_FOLDER);
            if (attachmentsDir.exists() && attachmentsDir.isDirectory()) {
                addDirectoryToZip(zos, attachmentsDir, "attachments/");
            }

            // 4. اضافه کردن فایل اطلاعات بکاپ
            addBackupInfoFileToZip(zos);
        }
    }

    /**
     * اضافه کردن یک فایل به زیپ
     */
    private void addFileToZip(ZipOutputStream zos, File file, String basePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            String entryName = basePath + file.getName();
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    /**
     * اضافه کردن یک پوشه به زیپ (به صورت بازگشتی)
     */
    private void addDirectoryToZip(ZipOutputStream zos, File dir, String basePath) throws Exception {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        String entryName = basePath + file.getName();
                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);

                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    /**
     * اضافه کردن فایل اطلاعات بکاپ به زیپ
     */
    private void addBackupInfoFileToZip(ZipOutputStream zos) throws Exception {
        StringBuilder info = new StringBuilder();
        info.append("========================================\n");
        info.append("       اطلاعات بکاپ دیتابیس\n");
        info.append("========================================\n\n");
        info.append("📅 تاریخ بکاپ: ").append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        info.append("📱 نسخه برنامه: 1.0\n\n");
        info.append("========================================\n");
        info.append("       ساختار فایل‌ها\n");
        info.append("========================================\n\n");
        info.append("📁 database/ - پوشه دیتابیس\n");
        info.append("   ├── invoice_db (فایل اصلی دیتابیس)\n");
        info.append("   ├── invoice_db-wal (فایل WAL)\n");
        info.append("   ├── invoice_db-shm (فایل SHM)\n");
        info.append("   └── invoice_db-journal (فایل Journal)\n\n");
        info.append("📁 attachments/ - پوشه فایل‌های پیوست\n");
        info.append("   └── فایل‌های ضمیمه شده به فاکتورها\n\n");
        info.append("========================================\n");
        info.append("       راهنمای بازیابی\n");
        info.append("========================================\n\n");
        info.append("1️⃣ فایل زیپ را Extract کنید\n");
        info.append("2️⃣ فایل دیتابیس را در مسیر /databases/ کپی کنید\n");
        info.append("3️⃣ پوشه attachments را به مسیر /files/invoice_attachments/ کپی کنید\n\n");
        info.append("========================================\n");

        ZipEntry entry = new ZipEntry("backup_info.txt");
        zos.putNextEntry(entry);
        zos.write(info.toString().getBytes("UTF-8"));
        zos.closeEntry();
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

        String fileName = attachment.getName();
        message.setSubject("📦 بکاپ خودکار - " + fileName);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("سلام،\n\n" +
                "فایل بکاپ کامل اپلیکیشن SmartAgroTransport پیوست شده است.\n" +
                "این بکاپ شامل:\n" +
                "✅ فایل دیتابیس (invoice_db)\n" +
                "✅ فایل‌های همراه دیتابیس\n" +
                "✅ تمام فایل‌های پیوست فاکتورها\n\n" +
                "تاریخ بکاپ: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                "نام فایل: " + fileName + "\n\n" +
                "با تشکر");

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