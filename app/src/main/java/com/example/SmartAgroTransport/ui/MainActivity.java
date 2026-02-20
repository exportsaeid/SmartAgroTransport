package com.example.SmartAgroTransport.ui;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity implements MainView, NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private EditText etCustomerName, etCustomerPhone;
    private TextView tvInvoiceDate, tvTotalAmount, tvAttachedFileName;
    private RecyclerView recyclerItems;
    private Button btnAddItem, btnPreviewInvoice, btnAttachFile;
    private LinearLayout attachmentContainer, attachmentsListContainer;

    private InvoicePresenter presenter;
    private InvoiceItemAdapter adapter;
    private long currentEditingInvoiceId = -1;

    private DrawerLayout drawerLayout;

    // ========== متغیرهای مدیریت فایل پیوست ==========
    private Uri selectedFileUri = null;
    private String selectedFileName = "";
    private List<AttachmentItem> attachmentsList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    // ===============================================

    // متغیر برای نگهداری پوشه موقت بکاپ
    private File currentTempBackupDir = null;

    // برای نگهداری موقت URI فایل قبل از تنظیم نام
    private Uri pendingFileUri = null;
    private String pendingOriginalFileName = "";

    // مدیریت نتیجه بازگشت از پیش‌نمایش
    private final ActivityResultLauncher<Intent> previewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showToast("✅ فاکتور نهایی و در سیستم ثبت شد.");
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

    // ========== لانچر برای انتخاب فایل پیوست ==========
    private final ActivityResultLauncher<Intent> attachmentPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pendingFileUri = result.getData().getData();
                    pendingOriginalFileName = getFileName(pendingFileUri);

                    // نمایش دیالوگ برای وارد کردن نام فایل
                    showFileNameDialog();
                }
            });

    // ========== لانچر برای ذخیره فایل ZIP بکاپ ==========
    private final ActivityResultLauncher<Intent> backupZipLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && currentTempBackupDir != null) {
                    Uri zipUri = result.getData().getData();

                    try {
                        // فشرده‌سازی پوشه موقت به فایل ZIP
                        zipFolder(currentTempBackupDir, zipUri);

                        // پاک کردن پوشه موقت
                        deleteDirectory(currentTempBackupDir);
                        currentTempBackupDir = null;

                        showToast("✅ بکاپ با موفقیت ایجاد شد!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("❌ خطا در ایجاد فایل ZIP: " + e.getMessage());
                    }
                }
            });

    // ========== لانچر برای انتخاب فایل ZIP بکاپ ==========
    private final ActivityResultLauncher<Intent> restoreZipPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri zipUri = result.getData().getData();
                    String fileName = getFileName(zipUri);

                    if (fileName != null && fileName.toLowerCase().endsWith(".zip")) {
                        new AlertDialog.Builder(this)
                                .setTitle("⚠️ تأیید بازیابی")
                                .setMessage("آیا از بازیابی اطلاعات از فایل " + fileName + " مطمئن هستید؟\n\nتمام داده‌های فعلی پاک خواهند شد.")
                                .setPositiveButton("بله، بازیابی کن", (dialog, which) -> {
                                    restoreFromZipFile(zipUri);
                                })
                                .setNegativeButton("لغو", null)
                                .show();
                    } else {
                        showToast("❌ لطفاً یک فایل ZIP معتبر انتخاب کنید");
                    }
                }
            });

    NumberFormat format = NumberFormat.getInstance(Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BackupReceiver.scheduleDailyBackup(this);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupNavigationDrawer();

        presenter = new InvoicePresenter(this);

        adapter = new InvoiceItemAdapter(
                presenter.getItems(),
                position -> presenter.removeItem(position),
                this::editItem
        );

        recyclerItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerItems.setAdapter(adapter);

        tvInvoiceDate.setText(PersianDateHelper.getCurrentPersianDate());
        tvInvoiceDate.setOnClickListener(v -> PersianDateHelper.showPersianDatePicker(this, (TextInputEditText) tvInvoiceDate));

        btnAddItem.setOnClickListener(v -> presenter.onAddItemClicked());
        btnPreviewInvoice.setOnClickListener(v -> onPreviewClicked());

        btnAttachFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {"image/*", "application/pdf", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            attachmentPickerLauncher.launch(intent);
        });

        // درخواست مجوز برای اندرویدهای جدید
        requestStoragePermissions();
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.READ_MEDIA_AUDIO
                }, 100);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }
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
            showToast("⚠️ فاکتور خالی است! ابتدا آیتم اضافه کنید.");
            return;
        }

        Invoice invoiceToSave = new Invoice();
        invoiceToSave.setCustomerName(getCustomerName());
        invoiceToSave.setCustomerPhone(getCustomerPhone());
        invoiceToSave.setDate(getCurrentDate());
        invoiceToSave.setItems(new ArrayList<>(presenter.getItems()));
        invoiceToSave.setGrandTotal(presenter.getGrandTotal());

        if (currentEditingInvoiceId > 0) {
            invoiceToSave.setId(currentEditingInvoiceId);
        }

        try {
            long finalId;
            if (invoiceToSave.getId() <= 0) {
                finalId = dbHelper.addInvoice(invoiceToSave);
                invoiceToSave.setId(finalId);
                showToast("📝 فاکتور جدید ذخیره شد.");
            } else {
                dbHelper.updateInvoice(invoiceToSave);
                finalId = invoiceToSave.getId();
                showToast("✏️ فاکتور بروزرسانی شد.");
            }


            // ذخیره فایل‌های پیوست با نام دلخواه
            for (AttachmentItem item : attachmentsList) {
                if (!item.isSaved() && item.getFileUri() != null) {
                    // ذخیره فایل با نام دلخواه (همون نامی که کاربر انتخاب کرده)
                    String savedPath = dbHelper.saveFileToInternalStorage(this, item.getFileUri(), item.getFileName(), finalId);

                    if (savedPath != null) {
                        String fileType = getContentResolver().getType(item.getFileUri());
                        // ذخیره در دیتابیس با همان نام دلخواه
                        dbHelper.addAttachment(finalId, item.getFileName(), savedPath, fileType);
                    }
                }
            }

            resetForm();
            Intent intent = new Intent(this, InvoicePreviewActivity.class);
            intent.putExtra("invoice", invoiceToSave);
            previewLauncher.launch(intent);
        } catch (Exception e) {
            showToast("❌ خطا در دیتابیس: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private String getMimeType(String filePath) {
        String lowerPath = filePath.toLowerCase();

        if (lowerPath.endsWith(".pdf")) return "application/pdf";
        else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) return "image/jpeg";
        else if (lowerPath.endsWith(".png")) return "image/png";
        else if (lowerPath.endsWith(".gif")) return "image/gif";
        else if (lowerPath.endsWith(".bmp")) return "image/bmp";
        else if (lowerPath.endsWith(".webp")) return "image/webp";
        else if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".3gp") || lowerPath.endsWith(".mkv")) return "video/*";
        else if (lowerPath.endsWith(".mp3") || lowerPath.endsWith(".wav") || lowerPath.endsWith(".aac")) return "audio/*";
        else if (lowerPath.endsWith(".doc")) return "application/msword";
        else if (lowerPath.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        else if (lowerPath.endsWith(".xls")) return "application/vnd.ms-excel";
        else if (lowerPath.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        else if (lowerPath.endsWith(".ppt") || lowerPath.endsWith(".pptx")) return "application/vnd.ms-powerpoint";
        else if (lowerPath.endsWith(".txt")) return "text/plain";
        else if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) return "text/html";
        else if (lowerPath.endsWith(".xml")) return "text/xml";
        else if (lowerPath.endsWith(".zip") || lowerPath.endsWith(".rar")) return "application/zip";
        else return "*/*";
    }

    private void openAttachment(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, "❌ فایل مورد نظر یافت نشد!", Toast.LENGTH_LONG).show();
                return;
            }

            String mimeType = getMimeType(filePath);
            String authority = getApplicationContext().getPackageName() + ".provider";
            Uri uri = FileProvider.getUriForFile(this, authority, file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

            if (activities.size() > 0) {
                startActivity(intent);
            } else {
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
                fallbackIntent.setDataAndType(uri, "*/*");
                fallbackIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                List<ResolveInfo> fallbackActivities = pm.queryIntentActivities(fallbackIntent, 0);

                if (fallbackActivities.size() > 0) {
                    startActivity(fallbackIntent);
                } else {
                    Intent chooser = Intent.createChooser(intent, "انتخاب برنامه برای باز کردن فایل");
                    startActivity(chooser);
                    Toast.makeText(this, "⚠️ هیچ برنامه پیش‌فرضی یافت نشد. لطفاً یکی را انتخاب کنید.", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ خطا در باز کردن فایل: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteAttachment(AttachmentItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ حذف فایل")
                .setMessage("آیا از حذف فایل '" + item.getFileName() + "' مطمئن هستید؟")
                .setPositiveButton("بله، حذف شود", (dialog, which) -> {
                    if (item.isSaved() && item.getId() > 0) {
                        boolean deleted = dbHelper.deleteAttachment(item.getId());
                        if (deleted) {
                            showToast("✅ فایل از دیتابیس حذف شد");
                        } else {
                            showToast("⚠️ خطا در حذف از دیتابیس");
                        }
                    }

                    attachmentsList.remove(position);
                    refreshAttachmentsList();
                    showToast("✅ فایل از لیست حذف شد");
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void refreshAttachmentsList() {
        attachmentsListContainer.removeAllViews();

        if (attachmentsList.isEmpty()) {
            attachmentsListContainer.setVisibility(View.GONE);
            return;
        }

        attachmentsListContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < attachmentsList.size(); i++) {
            AttachmentItem item = attachmentsList.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_attachment, attachmentsListContainer, false);

            TextView tvFileName = itemView.findViewById(R.id.tvFileName);
            ImageView btnView = itemView.findViewById(R.id.btnView);
            ImageView btnDelete = itemView.findViewById(R.id.btnDelete);

            if (item.isSaved()) {
                tvFileName.setText("📄 " + item.getFileName());
            } else {
                tvFileName.setText("🆕 " + item.getFileName());
            }

            final int position = i;

            // کلیک طولانی برای ویرایش نام
            tvFileName.setOnLongClickListener(v -> {
                showEditFileNameDialog(item, position);
                return true;
            });

            btnView.setOnClickListener(v -> {
                if (item.isSaved() && item.getFilePath() != null) {
                    openAttachment(item.getFilePath());
                } else if (item.getFileUri() != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(item.getFileUri(), getContentResolver().getType(item.getFileUri()));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        showToast("❌ نمی‌توان فایل را باز کرد");
                    }
                }
            });

            btnDelete.setOnClickListener(v -> deleteAttachment(item, position));

            attachmentsListContainer.addView(itemView);

            if (i < attachmentsList.size() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                );
                params.setMargins(0, 4, 0, 4);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(0xFFE0E0E0);
                attachmentsListContainer.addView(divider);
            }
        }
    }

    private void loadInvoiceForEdit(Invoice editInvoice) {
        currentEditingInvoiceId = editInvoice.getId();
        presenter.getItems().clear();
        presenter.getItems().addAll(editInvoice.getItems());
        etCustomerName.setText(editInvoice.getCustomerName());
        etCustomerPhone.setText(editInvoice.getCustomerPhone());
        tvInvoiceDate.setText(editInvoice.getDate());

        attachmentsList.clear();
        Cursor cursor = dbHelper.getAttachmentsByInvoice(editInvoice.getId());
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex("id"));
                @SuppressLint("Range") String fileName = cursor.getString(cursor.getColumnIndex("file_name"));
                @SuppressLint("Range") String filePath = cursor.getString(cursor.getColumnIndex("file_path"));

                attachmentsList.add(new AttachmentItem(id, fileName, filePath, true));
            }
            cursor.close();
        }
        refreshAttachmentsList();

        updateItemsList();
        showToast("📂 فاکتور جهت ویرایش بارگذاری شد.");
    }

    private void resetForm() {
        currentEditingInvoiceId = -1;
        etCustomerName.setText("");
        etCustomerPhone.setText("");
        tvInvoiceDate.setText(PersianDateHelper.getCurrentPersianDate());
        presenter.getItems().clear();

        attachmentsList.clear();
        refreshAttachmentsList();
        selectedFileUri = null;
        selectedFileName = "";

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

        btnAttachFile = findViewById(R.id.btnAttachFile);
        attachmentsListContainer = findViewById(R.id.attachmentsListContainer);
        attachmentContainer = findViewById(R.id.attachmentContainer);

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
                "💰 جمع کل: %,d تومان | ⚖️ وزن کل: %s کیلوگرم", total, weightStr));
    }

    private void setupRtlNumberField(TextInputEditText editText) {
        editText.setTextDirection(View.TEXT_DIRECTION_RTL);
        editText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        editText.setGravity(Gravity.RIGHT);
    }

    private void setupRtlPhoneField(TextInputEditText editText) {
        editText.setTextDirection(View.TEXT_DIRECTION_RTL);
        editText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        editText.setGravity(Gravity.RIGHT);
    }

    private void showCustomDialogTitle(AlertDialog dialog, String title) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setPadding(32, 32, 32, 16);
        titleView.setTextSize(18);
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.primary));
        dialog.setCustomTitle(titleView);
    }

    @Override
    public void showAddItemDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);

        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etWeight = dialogView.findViewById(R.id.etWeight);
        TextInputEditText etUnitPrice = dialogView.findViewById(R.id.etUnitPrice);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etMobile = dialogView.findViewById(R.id.etMobile);
        TextInputEditText etAddress = dialogView.findViewById(R.id.etAddress);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);

        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        setupRtlNumberField(etWeight);
        setupRtlNumberField(etUnitPrice);
        setupRtlPhoneField(etMobile);

        NumberFormatter.addThousandSeparator(etUnitPrice);
        NumberFormatter.addThousandSeparator(etWeight);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        showCustomDialogTitle(dialog, "➕ افزودن محصول");

        btnSave.setOnClickListener(v -> {
            String productName = etProductName.getText().toString().trim();
            String weightStr = etWeight.getText().toString().replaceAll("[,]", "").trim();
            String priceStr = etUnitPrice.getText().toString().replaceAll("[,]", "").trim();
            String name = etName.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String notesStr = etNotes.getText().toString().trim();

            if (productName.isEmpty()) {
                Toast.makeText(this, "🥬 نام محصول الزامی است", Toast.LENGTH_SHORT).show();
                return;
            }

            long weight = 0;
            if (!weightStr.isEmpty()) {
                try {
                    weight = Long.parseLong(weightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "⚖️ وزن نامعتبر است", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            long unitPrice = 0;
            if (!priceStr.isEmpty()) {
                try {
                    unitPrice = Long.parseLong(priceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "💰 قیمت نامعتبر است", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            presenter.addNewItem(
                    productName,
                    weight,
                    unitPrice,
                    name,
                    mobile,
                    address,
                    notesStr
            );

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
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
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);

        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        setupRtlNumberField(etWeight);
        setupRtlNumberField(etUnitPrice);
        setupRtlPhoneField(etMobile);

        NumberFormatter.addThousandSeparator(etUnitPrice);
        NumberFormatter.addThousandSeparator(etWeight);

        if (isEdit) {
            tvDialogTitle.setText("✏️ ویرایش محصول");
            tvDialogTitle.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            tvDialogTitle.setText("🥬 ثبت محصول جدید");
        }

        if (isEdit) {
            etProductName.setText(item.getProductName());
            etWeight.setText(NumberFormatter.formatNumber((long) item.getWeight()));
            etUnitPrice.setText(NumberFormatter.formatNumber(item.getUnitPrice()));
            etName.setText(item.getName());
            etMobile.setText(item.getMobile());
            etAddress.setText(item.getAddress());
            etNotes.setText(item.getNotes() != null ? item.getNotes() : "");
            btnSave.setText("💾 ویرایش");
        } else {
            btnSave.setText("💾 ذخیره");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String productName = etProductName.getText().toString().trim();
            String weightStr = etWeight.getText().toString().replaceAll("[,]", "").trim();
            String priceStr = etUnitPrice.getText().toString().replaceAll("[,]", "").trim();
            String name = etName.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String notesStr = etNotes.getText().toString().trim();

            if (productName.isEmpty()) {
                showToast("🥬 نام محصول الزامی است");
                return;
            }

            long weight = 0;
            long unitPrice = 0;

            try {
                if (!weightStr.isEmpty()) {
                    weight = Long.parseLong(weightStr);
                }
                if (!priceStr.isEmpty()) {
                    unitPrice = Long.parseLong(priceStr);
                }
            } catch (NumberFormatException e) {
                showToast("⚠️ وزن یا قیمت نامعتبر است");
                return;
            }

            InvoiceItem newItem = new InvoiceItem(
                    productName,
                    weight,
                    unitPrice,
                    name,
                    mobile,
                    address,
                    notesStr
            );

            if (isEdit) {
                presenter.getItems().set(position, newItem);
                showToast("✅ محصول ویرایش شد");
            } else {
                presenter.getItems().add(newItem);
                showToast("✅ محصول اضافه شد");
            }

            updateItemsList();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    // دیالوگ تنظیم نام فایل
    private void showFileNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_file_name, null);

        TextInputEditText etFileName = dialogView.findViewById(R.id.etFileName);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // تنظیم نام پیش‌فرض (بدون پسوند)
        String defaultName = pendingOriginalFileName;
        int dotIndex = defaultName.lastIndexOf('.');
        if (dotIndex > 0) {
            defaultName = defaultName.substring(0, dotIndex);
        }
        etFileName.setText(defaultName);
        etFileName.setSelection(defaultName.length());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String customName = etFileName.getText().toString().trim();

            if (customName.isEmpty()) {
                showToast("❌ لطفاً نام فایل را وارد کنید");
                return;
            }

            // استخراج پسوند
            String extension = "";
            int dotIndex2 = pendingOriginalFileName.lastIndexOf('.');
            if (dotIndex2 > 0) {
                extension = pendingOriginalFileName.substring(dotIndex2);
            }

            // نام نهایی فایل (نام دلخواه + پسوند)
            String finalFileName = customName + extension;

            // اضافه کردن فایل به لیست با نام جدید
            AttachmentItem tempItem = new AttachmentItem(
                    -1,
                    finalFileName,  // ذخیره نام دلخواه
                    pendingFileUri,
                    false
            );
            attachmentsList.add(tempItem);
            refreshAttachmentsList();

            // پاک کردن متغیرهای موقت
            pendingFileUri = null;
            pendingOriginalFileName = "";

            dialog.dismiss();
            showToast("✅ فایل با نام '" + finalFileName + "' اضافه شد");
        });

        btnCancel.setOnClickListener(v -> {
            pendingFileUri = null;
            pendingOriginalFileName = "";
            dialog.dismiss();
        });
    }

    // دیالوگ ویرایش نام فایل
    private void showEditFileNameDialog(AttachmentItem item, int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_file_name, null);

        TextInputEditText etFileName = dialogView.findViewById(R.id.etFileName);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // استخراج نام بدون پسوند
        String fileName = item.getFileName();
        String nameWithoutExt = fileName;
        String extension;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            extension = "";
        }

        etFileName.setText(nameWithoutExt);
        etFileName.setSelection(nameWithoutExt.length());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("✏️ ویرایش نام فایل");

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String newName = etFileName.getText().toString().trim();

            if (newName.isEmpty()) {
                showToast("❌ لطفاً نام فایل را وارد کنید");
                return;
            }

            String newFileName = newName + extension;

            // بررسی تکراری نبودن نام در لیست فعلی
            boolean nameExists = false;
            for (AttachmentItem otherItem : attachmentsList) {
                if (otherItem != item && otherItem.getFileName().equals(newFileName)) {
                    nameExists = true;
                    break;
                }
            }

            if (nameExists) {
                showToast("❌ این نام در فاکتور فعلی قبلاً استفاده شده");
                return;
            }

            if (item.isSaved() && item.getId() > 0) {
                // فقط نام در دیتابیس آپدیت میشه، فایل فیزیکی تغییر نمی‌کنه
                boolean updated = dbHelper.updateAttachmentFileName(item.getId(), newFileName);

                if (updated) {
                    item.setFileName(newFileName);
                    showToast("✅ نام فایل با موفقیت تغییر کرد");
                } else {
                    showToast("❌ خطا در به‌روزرسانی دیتابیس");
                }
            } else {
                // اگر هنوز ذخیره نشده
                item.setFileName(newFileName);
                showToast("✅ نام فایل تغییر کرد");
            }

            refreshAttachmentsList();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_create_invoice) {
            resetForm();
            Toast.makeText(this, "📄 فاکتور جدید آماده شد", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profit_report) {
            startActivity(new Intent(this, ProfitReportActivity.class));
        } else if (id == R.id.nav_clearance) {
            startActivity(new Intent(this, ClearanceActivity.class));
        } else if (id == R.id.nav_invoice_list) {
            Intent intent = new Intent(MainActivity.this, InvoiceListActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_backup) {
            backupInvoiceDatabaseAsZip();
        } else if (id == R.id.nav_buyer_delivery) {
            Intent intent = new Intent(MainActivity.this, BuyerDeliveryActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_map) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_restore) {
            selectBackupZipFileForRestore();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "⚙️ تنظیمات (به زودی اضافه می‌شود)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_contact) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_about) {
            new AlertDialog.Builder(this)
                    .setTitle("ℹ️ درباره اپ")
                    .setMessage("📱 فاکتور هوشمند کشاورزی\nنسخه 1.0")
                    .setPositiveButton("تایید", null)
                    .show();
        } else if (id == R.id.nav_load_invoice) {
            Intent intent = new Intent(MainActivity.this, LoadInvoiceActivity.class);
            listLauncher.launch(intent);
        } else if (id == R.id.nav_exit) {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            prefs.edit().clear().apply();

            // بستن drawer قبل از ایجاد handler
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            // استفاده از postDelayed بدون reference به drawerLayout
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finishAffinity();
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
            }, 200);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ========== متدهای بکاپ و ریستور به صورت ZIP ==========

    private void backupInvoiceDatabaseAsZip() {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(new Date());
        String backupFileName = "invoice_backup_" + timeStamp + ".zip";

        try {
            showToast("⏳ در حال آماده‌سازی بکاپ...");

            // ایجاد پوشه موقت برای فایل‌های بکاپ
            File tempBackupDir = new File(getCacheDir(), "temp_backup_" + System.currentTimeMillis());
            tempBackupDir.mkdirs();

            // کپی دیتابیس به پوشه موقت
            File currentDB = getDatabasePath("invoice_db");
            if (!currentDB.exists()) {
                showToast("❌ دیتابیس پیدا نشد!");
                return;
            }

            File dbBackupFile = new File(tempBackupDir, "invoice_db");
            copyFile(currentDB, dbBackupFile);

            // کپی فایل‌های همراه دیتابیس
            String[] extensions = {"-wal", "-shm", "-journal"};
            for (String ext : extensions) {
                File companion = new File(currentDB.getAbsolutePath() + ext);
                if (companion.exists()) {
                    File companionBackup = new File(tempBackupDir, "invoice_db" + ext);
                    copyFile(companion, companionBackup);
                }
            }

            // کپی فایل‌های پیوست
            File attachmentsDir = new File(getFilesDir(), "invoice_attachments");
            if (attachmentsDir.exists() && attachmentsDir.isDirectory()) {
                File attachmentsBackupDir = new File(tempBackupDir, "attachments");
                attachmentsBackupDir.mkdirs();

                File[] attachmentFiles = attachmentsDir.listFiles();
                if (attachmentFiles != null) {
                    for (File file : attachmentFiles) {
                        if (file.isFile()) {
                            File destFile = new File(attachmentsBackupDir, file.getName());
                            copyFile(file, destFile);
                        }
                    }
                }
            }

            // ایجاد فایل اطلاعات بکاپ
            createBackupInfoFile(tempBackupDir, timeStamp);

            // انتخاب محل ذخیره فایل ZIP
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, backupFileName);

            // ایجاد پوشه Downloads به عنوان پیش‌فرض
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Uri downloadsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Download");
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsUri);
            }

            // ذخیره مسیر پوشه موقت برای استفاده در لانچر
            currentTempBackupDir = tempBackupDir;

            backupZipLauncher.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("❌ خطا در ایجاد بکاپ: " + e.getMessage());
        }
    }

    private void selectBackupZipFileForRestore() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (intent.resolveActivity(getPackageManager()) != null) {
            restoreZipPickerLauncher.launch(intent);
        } else {
            Intent fallbackIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fallbackIntent.setType("*/*");
            fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fallbackIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/zip",
                    "application/x-zip-compressed"
            });
            fallbackIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            restoreZipPickerLauncher.launch(fallbackIntent);
        }
    }

    private void zipFolder(File sourceFolder, Uri destinationZipUri) throws IOException {
        try (OutputStream os = getContentResolver().openOutputStream(destinationZipUri);
             ZipOutputStream zos = new ZipOutputStream(os)) {

            zipFile(sourceFolder, sourceFolder.getName(), zos);
        }
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }

        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
            }
            zos.closeEntry();

            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
        }
    }

    private void restoreFromZipFile(Uri zipUri) {
        try {
            showToast("⏳ در حال استخراج و بازیابی اطلاعات...");

            // بستن دیتابیس قبلی
            SQLiteDatabase.releaseMemory();
            if (dbHelper != null) {
                dbHelper.close();
                dbHelper = null;
            }

            // ایجاد پوشه موقت با نام منحصر به فرد
            File tempDir = new File(getCacheDir(), "restore_temp_" + System.currentTimeMillis());
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
            }
            tempDir.mkdirs();

            // استخراج فایل ZIP
            boolean unzipSuccess = unzipFile(zipUri, tempDir);

            if (!unzipSuccess) {
                showToast("❌ خطا در استخراج فایل ZIP");
                deleteDirectory(tempDir);
                return;
            }

            // نمایش محتویات پوشه موقت برای دیباگ
            StringBuilder fileList = new StringBuilder("فایل‌های استخراج شده:\n");
            listAllFiles(tempDir, fileList, 0);
            Log.d(TAG, fileList.toString());

            // بررسی وجود فایل دیتابیس
            File dbBackupFile = findDatabaseFile(tempDir);

            if (dbBackupFile == null) {
                showToast("❌ فایل دیتابیس در بکاپ یافت نشد");
                showToast(fileList.toString());
                deleteDirectory(tempDir);
                return;
            }

            Log.d(TAG, "✅ فایل دیتابیس پیدا شد: " + dbBackupFile.getAbsolutePath());

            // ادامه فرآیند بازیابی
            performRestore(dbBackupFile, tempDir);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("❌ خطا در استخراج فایل: " + e.getMessage());
        }
    }

    private boolean unzipFile(Uri zipUri, File destinationDir) {
        try (InputStream is = getContentResolver().openInputStream(zipUri);
             ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            byte[] buffer = new byte[8192];
            int count = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File newFile = new File(destinationDir, entryName);

                Log.d(TAG, "استخراج: " + entryName);

                if (entry.isDirectory()) {
                    if (!newFile.mkdirs()) {
                        Log.e(TAG, "خطا در ایجاد پوشه: " + entryName);
                    }
                } else {
                    // ایجاد پوشه والد اگر وجود نداشته باشد
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    count++;
                }
                zis.closeEntry();
            }

            Log.d(TAG, "تعداد فایل‌های استخراج شده: " + count);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void performRestore(File dbBackupFile, File tempDir) {
        try {
            showToast("⏳ در حال بازیابی اطلاعات...");

            File currentDB = getDatabasePath("invoice_db");

            // کپی فایل دیتابیس
            copyFile(dbBackupFile, currentDB);
            Log.d(TAG, "✅ دیتابیس کپی شد");

            // کپی فایل‌های همراه دیتابیس
            String[] extensions = {"-wal", "-shm", "-journal"};
            for (String ext : extensions) {
                File companionBackup = findFile(tempDir, "invoice_db" + ext);
                if (companionBackup != null && companionBackup.exists()) {
                    File companion = new File(currentDB.getAbsolutePath() + ext);
                    copyFile(companionBackup, companion);
                    Log.d(TAG, "✅ فایل " + ext + " کپی شد");
                }
            }

            // جستجوی پوشه attachments
            File attachmentsBackupDir = findAttachmentsFolder(tempDir);
            if (attachmentsBackupDir != null && attachmentsBackupDir.exists()) {
                File attachmentsDir = new File(getFilesDir(), "invoice_attachments");

                if (attachmentsDir.exists()) {
                    deleteDirectory(attachmentsDir);
                }
                attachmentsDir.mkdirs();

                File[] attachmentFiles = attachmentsBackupDir.listFiles();
                if (attachmentFiles != null) {
                    int fileCount = 0;
                    for (File file : attachmentFiles) {
                        if (file.isFile()) {
                            File destFile = new File(attachmentsDir, file.getName());
                            copyFile(file, destFile);
                            fileCount++;
                        }
                    }
                    if (fileCount > 0) {
                        showToast("📎 " + fileCount + " فایل پیوست بازیابی شد");
                    }
                }
            }

            // پاکسازی
            deleteDirectory(tempDir);

            // راه‌اندازی مجدد دیتابیس
            dbHelper = new DatabaseHelper(this);
            showToast("✅ بازیابی با موفقیت انجام شد!");
            resetForm();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("❌ خطا در بازیابی: " + e.getMessage());
        }
    }

    private File findDatabaseFile(File directory) {
        if (directory == null || !directory.exists()) return null;

        File[] files = directory.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().equals("invoice_db")) {
                Log.d(TAG, "پیدا شد در: " + file.getAbsolutePath());
                return file;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findDatabaseFile(file);
                if (found != null) return found;
            }
        }

        return null;
    }

    private File findFile(File directory, String fileName) {
        if (directory == null || !directory.exists()) return null;

        File[] files = directory.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return file;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFile(file, fileName);
                if (found != null) return found;
            }
        }

        return null;
    }

    private File findAttachmentsFolder(File directory) {
        if (directory == null || !directory.exists()) return null;

        File[] files = directory.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory() && file.getName().equals("attachments")) {
                return file;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findAttachmentsFolder(file);
                if (found != null) return found;
            }
        }

        return null;
    }

    private void listAllFiles(File directory, StringBuilder output, int level) {
        if (directory == null || !directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        String indent = new String(new char[level * 2]).replace('\0', ' ');

        for (File file : files) {
            if (file.isFile()) {
                output.append(indent).append("📄 ").append(file.getName()).append("\n");
            } else if (file.isDirectory()) {
                output.append(indent).append("📁 ").append(file.getName()).append("/\n");
                listAllFiles(file, output, level + 1);
            }
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    private void deleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    private void createBackupInfoFile(File backupDir, String timeStamp) {
        try {
            File infoFile = new File(backupDir, "backup_info.txt");
            FileOutputStream fos = new FileOutputStream(infoFile);

            int attachmentCount = 0;
            File attachmentsBackupDir = new File(backupDir, "attachments");
            if (attachmentsBackupDir.exists()) {
                File[] files = attachmentsBackupDir.listFiles();
                attachmentCount = files != null ? files.length : 0;
            }

            long dbSize = new File(backupDir, "invoice_db").length();
            long attachmentsSize = getFolderSize(attachmentsBackupDir);

            String content = "========================================\n" +
                    "       اطلاعات بکاپ دیتابیس\n" +
                    "========================================\n\n" +
                    "📅 تاریخ بکاپ: " +
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                    "🆔 شناسه بکاپ: " + timeStamp + "\n" +
                    "📱 نسخه برنامه: 1.0\n\n" +
                    "========================================\n" +
                    "       مشخصات فایل‌ها\n" +
                    "========================================\n\n" +
                    "🗄️ نام دیتابیس: invoice_db\n" +
                    "📦 حجم دیتابیس: " + formatFileSize(dbSize) + "\n" +
                    "📎 تعداد فایل‌های پیوست: " + attachmentCount + "\n" +
                    "📦 حجم فایل‌های پیوست: " + formatFileSize(attachmentsSize) + "\n" +
                    "📂 مسیر فایل‌های پیوست: /attachments/\n\n" +
                    "========================================\n" +
                    "       لیست فایل‌های پیوست\n" +
                    "========================================\n\n";

            if (attachmentsBackupDir.exists() && attachmentCount > 0) {
                File[] files = attachmentsBackupDir.listFiles();
                for (File file : files) {
                    content += "📄 " + file.getName() + " (" + formatFileSize(file.length()) + ")\n";
                }
            } else {
                content += "📭 هیچ فایل پیوستی وجود ندارد\n";
            }

            content += "\n========================================\n" +
                    "       راهنمای بازیابی\n" +
                    "========================================\n\n" +
                    "1️⃣ فایل ZIP را در برنامه انتخاب کنید\n" +
                    "2️⃣ گزینه Restore را بزنید\n" +
                    "3️⃣ اطلاعات به طور خودکار بازیابی می‌شود\n\n" +
                    "========================================\n";

            fos.write(content.getBytes("UTF-8"));
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getFolderSize(File folder) {
        long size = 0;
        if (folder != null && folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
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
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyBackup",
                ExistingPeriodicWorkPolicy.KEEP,
                backupRequest);
    }

    // ========== کلاس داخلی برای مدیریت آیتم‌های پیوست ==========
    private static class AttachmentItem {
        private long id;
        private String fileName;
        private String filePath;
        private Uri fileUri;
        private boolean isSaved;

        public AttachmentItem(long id, String fileName, String filePath, boolean isSaved) {
            this.id = id;
            this.fileName = fileName;
            this.filePath = filePath;
            this.isSaved = isSaved;
        }

        public AttachmentItem(long id, String fileName, Uri fileUri, boolean isSaved) {
            this.id = id;
            this.fileName = fileName;
            this.fileUri = fileUri;
            this.isSaved = isSaved;
        }

        public long getId() { return id; }
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public Uri getFileUri() { return fileUri; }
        public boolean isSaved() { return isSaved; }

        public void setFileName(String fileName) { this.fileName = fileName; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}