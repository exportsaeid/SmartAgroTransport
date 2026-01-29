package com.example.SmartAgroTransport.ui;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.adapter.InvoiceItemAdapter;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.Invoice;
import com.example.SmartAgroTransport.model.InvoiceItem;
import com.example.SmartAgroTransport.presenter.InvoicePresenter;
import com.example.SmartAgroTransport.utils.BackupReceiver;
import com.example.SmartAgroTransport.utils.BackupWorker;
import com.example.SmartAgroTransport.utils.NumberFormatter;
import com.example.SmartAgroTransport.utils.PersianDateHelper;
import com.example.SmartAgroTransport.view.MainView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements MainView, NavigationView.OnNavigationItemSelectedListener {
    private static final int REQUEST_CODE_RESTORE = 101;
    private EditText etCustomerName, etCustomerPhone;
    private TextView tvInvoiceDate, tvTotalAmount;
    private RecyclerView recyclerItems;
    private Button btnAddItem, btnPreviewInvoice;

    private InvoicePresenter presenter;
    private InvoiceItemAdapter adapter;
    private long currentEditingInvoiceId = -1;

    private DrawerLayout drawerLayout;

    // مدیریت نتیجه بازگشت از پیش‌نمایش
    private final ActivityResultLauncher<Intent> previewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showToast("فاکتور نهایی و در سیستم ثبت شد.");
                    resetForm();
                }
            });

    // مدیریت نتیجه بازگشت از لیست فاکتورها (برای ویرایش فاکتور قبلی)
    private final ActivityResultLauncher<Intent> listLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Invoice editInvoice = (Invoice) result.getData().getSerializableExtra("edit_invoice");
                    if (editInvoice != null) {
                        loadInvoiceForEdit(editInvoice);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // --- فراخوانی بکاپ روزانه ساعت ۲ شب ---
        BackupReceiver.scheduleDailyBackup(this);

        // این کد را موقتاً در onCreate قرار دهید:
//        OneTimeWorkRequest testBackupRequest = new OneTimeWorkRequest.Builder(BackupWorker.class)
//                .build();
//
//        WorkManager.getInstance(this).enqueue(testBackupRequest);


//        setupAutoBackup();
//        scheduleDailyBackup();


        initViews();
        setupNavigationDrawer();

        presenter = new InvoicePresenter(this);

        // راه‌اندازی آداپتور لیست با قابلیت حذف و ویرایش
        adapter = new InvoiceItemAdapter(
                presenter.getItems(),
                position -> presenter.removeItem(position),
                this::editItem // نسخه درست و بدون کرش
        );

        recyclerItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerItems.setAdapter(adapter);

        tvInvoiceDate.setText(PersianDateHelper.getCurrentPersianDate());
        tvInvoiceDate.setOnClickListener(v -> PersianDateHelper.showPersianDatePicker(this, (TextInputEditText) tvInvoiceDate));

        btnAddItem.setOnClickListener(v -> presenter.onAddItemClicked());

        btnPreviewInvoice.setOnClickListener(v -> onPreviewClicked());
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void onPreviewClicked() {
        if (presenter.getItems().isEmpty()) {
            showToast("فاکتور خالی است! ابتدا آیتم اضافه کنید.");
            return;
        }

        Invoice invoiceToSave = new Invoice();
        invoiceToSave.setCustomerName(getCustomerName());
        invoiceToSave.setCustomerPhone(getCustomerPhone());
        invoiceToSave.setDate(getCurrentDate());
        invoiceToSave.setItems(new ArrayList<>(presenter.getItems()));

        if (currentEditingInvoiceId > 0) {
            invoiceToSave.setId(currentEditingInvoiceId);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        try {
            if (invoiceToSave.getId() <= 0) {
                long newId = dbHelper.addInvoice(invoiceToSave);
                invoiceToSave.setId(newId);
                showToast("فاکتور جدید ذخیره شد.");
            } else {
                dbHelper.updateInvoice(invoiceToSave);
                showToast("فاکتور بروزرسانی شد.");
            }

            resetForm();
            Intent intent = new Intent(this, InvoicePreviewActivity.class);
            intent.putExtra("invoice", invoiceToSave);
            previewLauncher.launch(intent);
        } catch (Exception e) {
            showToast("خطا در دیتابیس: " + e.getMessage());
        }
    }

    private void loadInvoiceForEdit(Invoice editInvoice) {
        currentEditingInvoiceId = editInvoice.getId();
        presenter.getItems().clear();
        presenter.getItems().addAll(editInvoice.getItems());
        etCustomerName.setText(editInvoice.getCustomerName());
        etCustomerPhone.setText(editInvoice.getCustomerPhone());
        tvInvoiceDate.setText(editInvoice.getDate());
        updateItemsList();
        showToast("فاکتور جهت ویرایش بارگذاری شد.");
    }

    private void resetForm() {
        currentEditingInvoiceId = -1;
        etCustomerName.setText("");
        etCustomerPhone.setText("");
        tvInvoiceDate.setText(PersianDateHelper.getCurrentPersianDate());
        presenter.getItems().clear();
        updateItemsList();
    }

    @SuppressLint("WrongViewCast")
    private void initViews() {
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        tvInvoiceDate = findViewById(R.id.tvInvoiceDate);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        recyclerItems = findViewById(R.id.recyclerItems);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnPreviewInvoice = findViewById(R.id.btnPreviewInvoice);

        // ست کردن آیکون مثبت روی دکمه افزودن
        Drawable addIcon = ContextCompat.getDrawable(this, R.drawable.ic_add_white_24dp);
        if (addIcon != null) {
            btnAddItem.setCompoundDrawablesWithIntrinsicBounds(addIcon, null, null, null);
        }
    }

    private void editItem(int position) {
        if (position < 0 || position >= presenter.getItems().size()) return;

        InvoiceItem item = presenter.getItems().get(position);
        showInvoiceItemDialog(item, position);
    }


    @Override
    public void updateTotalAmount(long total) {
        double totalWeight = 0;
        for (InvoiceItem item : presenter.getItems()) {
            totalWeight += item.getWeight();
        }
        String weightStr = (totalWeight == (long) totalWeight)
                ? String.format(Locale.US, "%,d", (long) totalWeight)
                : String.format(Locale.US, "%,.2f", totalWeight);
        tvTotalAmount.setText(String.format(Locale.getDefault(),
                "جمع کل: %,d تومان | وزن کل: %s", total, weightStr));
    }

    NumberFormat format = NumberFormat.getInstance(Locale.US);

    @Override
    public void showAddItemDialog() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);

        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etWeight = dialogView.findViewById(R.id.etWeight);
        TextInputEditText etUnitPrice = dialogView.findViewById(R.id.etUnitPrice);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etMobile = dialogView.findViewById(R.id.etMobile);
        TextInputEditText etAddress = dialogView.findViewById(R.id.etAddress);

        // برای قیمت واحد (عدد صحیح)
        NumberFormatter.addThousandSeparator(etUnitPrice);
        NumberFormatter.addThousandSeparator(etWeight);
// برای وزن (اعشاری)
//    NumberFormatter.addThousandSeparatorForDecimal(etWeight);
        // عنوان وسط‌چین
        TextView titleView = new TextView(this);
        titleView.setText("افزودن محصول");
        titleView.setPadding(32, 32, 32, 16);
        titleView.setTextSize(18);
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(titleView)
                .setView(dialogView)
                .setPositiveButton("ذخیره", (dialog, which) -> {

                    String productName = etProductName.getText().toString().trim();
                    String weightStr = etWeight.getText().toString().trim();
                    String priceStr = etUnitPrice.getText().toString().trim();

                    if (productName.isEmpty()) {
                        Toast.makeText(this, "نام محصول الزامی است", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long weight = 0;
                    if (!weightStr.isEmpty()) {
                        try {

//                        Number number = format.parse(weightStr);
//                        long result = number.longValue();
                            long result = format.parse(weightStr).longValue();
                            weight = result;
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "وزن نامعتبر است", Toast.LENGTH_SHORT).show();
                            return;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    long unitPrice = 0;
                    if (!priceStr.isEmpty()) {
                        try {
                            long result = format.parse(priceStr).longValue();
                            unitPrice = result;
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "قیمت نامعتبر است", Toast.LENGTH_SHORT).show();
                            return;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    presenter.addNewItem(
                            productName,
                            weight,
                            unitPrice,
                            etName.getText().toString().trim(),
                            etMobile.getText().toString().trim(),
                            etAddress.getText().toString().trim()
                    );
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    @Override
    public void updateItemsList() {
        adapter.notifyDataSetChanged();
        updateTotalAmount(presenter.getGrandTotal());
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getCustomerName() {
        return etCustomerName.getText().toString().trim();
    }

    @Override
    public String getCustomerPhone() {
        return etCustomerPhone.getText().toString().trim();
    }

    @Override
    public String getCurrentDate() {
        return tvInvoiceDate.getText().toString();
    }

    @Override
    public void navigateToPreview(Invoice invoice) {
        /* در PreviewLauncher مدیریت می‌شود */
    }

    // --- متدهای کمکی تاریخ و تقویم ---


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_create_invoice) {
            etCustomerName.setText("");
            etCustomerPhone.setText("");
            tvInvoiceDate.setText(PersianDateHelper.getCurrentPersianDate());
            presenter.getItems().clear();
            adapter.notifyDataSetChanged();
            updateTotalAmount(0);
            Toast.makeText(this, "فاکتور جدید آماده شد", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profit_report) {
            startActivity(new Intent(this, ProfitReportActivity.class));
        } else if (id == R.id.nav_clearance) {
            startActivity(new Intent(this, ClearanceActivity.class));
        } else if (id == R.id.nav_invoice_list) {
            Intent intent = new Intent(MainActivity.this, InvoiceListActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_backup) {
            backupInvoiceDatabase();
        } else if (id == R.id.nav_buyer_delivery) {
            Intent intent = new Intent(MainActivity.this, BuyerDeliveryActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_map) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_restore) {
            openFilePickerForRestore();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "تنظیمات (به زودی اضافه می‌شود)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_contact) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            listLauncher.launch(intent);
//            Toast.makeText(this, "تنظیمات (به زودی اضافه می‌شود)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            new AlertDialog.Builder(this)
                    .setTitle("درباره اپ")
                    .setMessage("فاکتور هوشمند کشاورزی\nنسخه 1.0")
                    .setPositiveButton("تایید", null)
                    .show();
        } else if (id == R.id.nav_load_invoice) {
            Intent intent = new Intent(MainActivity.this, LoadInvoiceActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_exit) {
            finishAffinity();
            System.exit(0);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void restoreDatabase() {
        try {
            // مسیر دیتابیس فعلی
            File dbFile = getDatabasePath("invoice_db");
            // مسیر فایل بکاپ (مثال: Download/invoice_backup.db)
            File backupFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "invoice_backup.db");

            if (!backupFile.exists()) {
                Toast.makeText(this, "فایل بکاپ پیدا نشد!", Toast.LENGTH_SHORT).show();
                return;
            }

            FileInputStream fis = new FileInputStream(backupFile);
            FileOutputStream fos = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();

            Toast.makeText(this, "بازگردانی دیتابیس با موفقیت انجام شد!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطا در بازگردانی دیتابیس: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void backupInvoiceDatabase() {
        String databaseName = "invoice_db";
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(new Date());
        String backupFileName = "invoice_db" + timeStamp + ".db";
        try {
            File currentDB = getDatabasePath(databaseName);
            if (!currentDB.exists()) {
                Toast.makeText(this, "دیتابیس invoice_db پیدا نشد!", Toast.LENGTH_LONG).show();
                return;
            }
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File backupFile = new File(downloadsDir, backupFileName);
            try (FileInputStream in = new FileInputStream(currentDB);
                 FileOutputStream out = new FileOutputStream(backupFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            String[] extensions = {"-wal", "-shm", "-journal"};
            for (String ext : extensions) {
                File companion = new File(currentDB.getAbsolutePath() + ext);
                if (companion.exists()) {
                    File companionBackup = new File(downloadsDir, backupFileName + ext);
                    try (FileInputStream in = new FileInputStream(companion);
                         FileOutputStream out = new FileOutputStream(companionBackup)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                    }
                }
            }
            Toast.makeText(this, "بکاپ با موفقیت در پوشه Downloads ذخیره شد!\nفایل: " + backupFileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطا در بکاپ:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    public void scheduleDailyBackup() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.HOUR_OF_DAY, 24);
        }

        long timeDiff = calendar.getTimeInMillis() - System.currentTimeMillis();

        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                BackupWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // فقط وقتی اینترنت دارد
                        .build())
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyBackup",
                ExistingPeriodicWorkPolicy.KEEP,
                backupRequest);
    }


    private void openFilePickerForRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // می‌توانید "application/*" یا "application/octet-stream" هم بگذارید
        startActivityForResult(intent, REQUEST_CODE_RESTORE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_RESTORE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                restoreDatabaseFromUri(uri);
            }
        }
    }

    private void restoreDatabaseFromUri(Uri uri) {
        try {
            // مسیر دیتابیس فعلی
            File dbFile = getDatabasePath("invoice_db");

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Toast.makeText(this, "بازگردانی دیتابیس با موفقیت انجام شد!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطا در بازگردانی دیتابیس: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void showInvoiceItemDialog(InvoiceItem item, int position) {

        boolean isEdit = item != null;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);

        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etWeight = dialogView.findViewById(R.id.etWeight);
        TextInputEditText etUnitPrice = dialogView.findViewById(R.id.etUnitPrice);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etMobile = dialogView.findViewById(R.id.etMobile);
        TextInputEditText etAddress = dialogView.findViewById(R.id.etAddress);
        NumberFormatter.addThousandSeparator(etUnitPrice);
        NumberFormatter.addThousandSeparator(etWeight);
        // اگر ویرایش است → پر کردن مقادیر قبلی
        long result1 = 0;
        if (isEdit) {
            etProductName.setText(item.getProductName());
            String etWeight1 = String.valueOf(item.getWeight());
            try {
                result1 = format.parse(etWeight1).longValue();
            } catch (Exception e) {

            }


            etWeight.setText(String.valueOf(result1));

            etUnitPrice.setText(String.valueOf(item.getUnitPrice()));
            etName.setText(item.getName());
            etMobile.setText(item.getMobile());
            etAddress.setText(item.getAddress());
        }

        String title = isEdit ? "ویرایش آیتم" : "افزودن آیتم";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(isEdit ? "ذخیره تغییرات" : "افزودن", (dialog, which) -> {

                    String productName = etProductName.getText().toString().trim();
                    String weightStr = etWeight.getText().toString().trim();
                    String priceStr = etUnitPrice.getText().toString().trim();

                    if (productName.isEmpty()) {
                        showToast("نام محصول الزامی است");
                        return;
                    }

                    long weight = 0;
                    long unitPrice = 0;

                    try {
                        if (!weightStr.isEmpty()) {
                            long result = format.parse(weightStr).longValue();
                            weight = result;
                        }

                        if (!priceStr.isEmpty()) {
                            long result = format.parse(priceStr).longValue();
                            unitPrice = result;
                        }

                    } catch (NumberFormatException e) {
                        showToast("وزن یا قیمت نامعتبر است");
                        return;
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                    InvoiceItem newItem = new InvoiceItem(
                            productName,
                            weight,
                            unitPrice,
                            etName.getText().toString().trim(),
                            etMobile.getText().toString().trim(),
                            etAddress.getText().toString().trim()
                    );

                    if (isEdit) {
                        presenter.getItems().set(position, newItem);
                        showToast("آیتم ویرایش شد");
                    } else {
                        presenter.getItems().add(newItem);
                        showToast("آیتم اضافه شد");
                    }

                    updateItemsList();
                })
                .setNegativeButton("لغو", null)
                .show();
    }


}