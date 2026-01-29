package com.example.SmartAgroTransport.ui;

import android.graphics.pdf.PdfDocument;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.example.SmartAgroTransport.R;
import com.example.SmartAgroTransport.database.DatabaseHelper;
import com.example.SmartAgroTransport.model.Invoice;
import com.example.SmartAgroTransport.model.InvoiceItem;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InvoicePreviewActivity extends AppCompatActivity {

    private TextView tvPreviewCustomer, tvPreviewDate, tvPreviewTotal;
    private TableLayout tableItems;
    private Invoice invoice;
    private Uri pdfFolderUri = null;
    private ImageView backButton;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> selectPdfFolderLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        pdfFolderUri = uri;
                        Toast.makeText(this, "پوشه انتخاب شد. در حال ساخت PDF...", Toast.LENGTH_LONG).show();
                        executor.execute(() -> {
                            boolean success = createPdfWithCanvas();
                            mainHandler.post(() -> {
                                if (success) {
                                    Toast.makeText(InvoicePreviewActivity.this,
                                            "PDF با موفقیت ساخته شد!", Toast.LENGTH_LONG).show();
                                }
                            });
                        });
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_preview);

        backButton = findViewById(R.id.backButtonPish);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.e("LoadInvoiceActivity", "backButton is null!");
        }

        invoice = (Invoice) getIntent().getSerializableExtra("invoice");
        if (invoice == null) {
            Toast.makeText(this, "خطا در بارگذاری فاکتور!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        if (invoice.getId() > 0) {
            Invoice fullInvoice = dbHelper.getInvoiceById(invoice.getId());
            if (fullInvoice != null && fullInvoice.getInvoiceNumber() != null && !fullInvoice.getInvoiceNumber().trim().isEmpty()) {
                invoice.setInvoiceNumber(fullInvoice.getInvoiceNumber());
            }
        } else if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            String tempNumber = dbHelper.generateInvoiceNumber();
            invoice.setInvoiceNumber(tempNumber + " (پیش‌نویس)");
            Toast.makeText(this, "فاکتور هنوز ذخیره نشده. شماره موقت: " + invoice.getInvoiceNumber(), Toast.LENGTH_LONG).show();
        }

        initViews();
        displayInvoice();

        findViewById(R.id.button_export_pdf).setOnClickListener(v -> {
            if (invoice.getItems().isEmpty()) {
                Toast.makeText(this, "فاکتور خالی است!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pdfFolderUri == null) {
                chooseFolder();
            } else {
                executor.execute(() -> {
                    boolean success = createPdfWithCanvas();
                    mainHandler.post(() -> {
                        if (success) {
                            Toast.makeText(InvoicePreviewActivity.this,
                                    "PDF با موفقیت ساخته شد!", Toast.LENGTH_LONG).show();
                        }
                    });
                });
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void initViews() {
        tvPreviewCustomer = findViewById(R.id.tvPreviewCustomer);
        tvPreviewDate = findViewById(R.id.tvPreviewDate);
        tvPreviewTotal = findViewById(R.id.tvPreviewTotal);
        tableItems = findViewById(R.id.tableItems);
    }

    private void displayInvoice() {
        tvPreviewCustomer.setText("مشتری: " + invoice.getCustomerName() +
                (invoice.getCustomerPhone().isEmpty() ? "" : " - تماس: " + invoice.getCustomerPhone()));
        tvPreviewDate.setText("تاریخ: " + invoice.getDate());

        tableItems.removeAllViews();
        long grandTotal = 0;
        long totalWeight = 0;
        for (int i = 0; i < invoice.getItems().size(); i++) {
            InvoiceItem item = invoice.getItems().get(i);
            long itemTotal = (long) (item.getWeight() * item.getUnitPrice());
            grandTotal += itemTotal;
            totalWeight += item.getWeight();
            TableRow row = new TableRow(this);
            row.setPadding(8, 16, 8, 16);
            if (i % 2 == 0) row.setBackgroundColor(0xFFF5F5F5);

            addCell(row, String.valueOf(i + 1));
            addCell(row, item.getProductName());
            addCell(row, formatWeightWithSeparator(item.getWeight())); // با جداکننده و بدون .00
            addCell(row, String.format("%,d", item.getUnitPrice()));
            addCell(row, item.getName()!= null ? item.getName(): "-");
            addCell(row, item.getMobile() != null ? item.getMobile() : "-");
            addCell(row, item.getAddress() != null ? item.getAddress() : "-");
            addCell(row, String.format("%,d", itemTotal));

            tableItems.addView(row);
        }

        tvPreviewTotal.setText(
                "جمع کل قابل پرداخت: "
                        + String.format("%,d", grandTotal)
                        + " تومان\n"
                        + "وزن کل: " + formatWeightWithSeparator(totalWeight) + " کیلوگرم"
        );

    }

    private void addCell(TableRow row, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(8, 8, 8, 8);
        row.addView(tv);
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra("android.provider.extra.INITIAL_URI", Uri.parse("/storage/emulated/0/Download"));
        selectPdfFolderLauncher.launch(intent);
    }

    /**
     * تابع برای فرمت کردن وزن برای نمایش (هم در پیش‌نمایش و هم در PDF)
     * - بدون .00 برای اعداد صحیح
     * - با جداکننده 3 رقمی
     */
    private String formatWeightWithSeparator(double weight) {
        if (weight == 0) {
            return "0";
        }

        // بررسی آیا وزن عدد صحیح است
        if (weight == (long) weight) {
            // عدد صحیح - با جداکننده و بدون اعشار
            return String.format("%,d", (long) weight);
        } else {
            // عدد اعشاری - با جداکننده و با اعشار
            // فقط تا دو رقم اعشار نمایش داده می‌شود
            DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));
            return df.format(weight);
        }
    }

    /**
     * تابع برای شکستن متن به خطوط مختلف با توجه به عرض مشخص
     */
    private List<String> breakTextIntoLines(String text, float maxWidth, Paint paint) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty() || text.equals("-")) {
            lines.add("-");
            return lines;
        }

        // اگر متن کوتاه است و در یک خط جا می‌شود
        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        // شکستن متن بر اساس کاراکتر (برای فارسی بهتر است)
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String testLine = currentLine.toString() + c;

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(c);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(String.valueOf(c));
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * تابع برای محاسبه ارتفاع مورد نیاز برای متن چند خطی
     */
    private float calculateTextHeight(String text, float maxWidth, Paint paint) {
        List<String> lines = breakTextIntoLines(text, maxWidth - 8, paint);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float lineHeight = fontMetrics.descent - fontMetrics.ascent;
        float textHeight = lines.size() * lineHeight;

        // حداقل ارتفاع یک خط
        return Math.max(lineHeight, textHeight);
    }

    /**
     * تابع برای رسم متن چند خطی در یک مستطیل مشخص
     */
    private void drawMultilineText(Canvas canvas, String text, float left, float top, float width, float height, Paint paint) {
        List<String> lines = breakTextIntoLines(text, width - 8, paint);

        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float lineHeight = fontMetrics.descent - fontMetrics.ascent;
        float textHeight = lines.size() * lineHeight;

        // محاسبه موقعیت شروع متن (وسط عمودی)
        float y = top + (height - textHeight) / 2 - fontMetrics.ascent;

        // تنظیم تراز متن به مرکز
        paint.setTextAlign(Paint.Align.CENTER);

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                y += lineHeight;
                continue;
            }

            float centerX = left + width / 2;
            canvas.drawText(line, centerX, y, paint);
            y += lineHeight;
        }
    }

    /**
     * تابع برای رسم متن در یک خط (برای ستون‌های وزن، قیمت و جمع)
     */
    private void drawSingleLineText(Canvas canvas, String text, float left, float top, float width, float height, Paint paint) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float y = top + (height - textHeight) / 2 - fontMetrics.ascent;
        float centerX = left + width / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, centerX, y, paint);
    }

    /**
     * محاسبه عرض ستون‌ها با توجه به محتوا برای A4
     */
    private float[] calculateColumnWidths(List<InvoiceItem> items, Paint paint) {
        // عرض‌های پیش‌فرض برای A4
        float[] widths = new float[8];

        // مقادیر پیش‌فرض برای A4
        widths[0] = 90;   // جمع
        widths[1] = 80;   // آدرس
        widths[2] = 60;   // موبایل
        widths[3] = 60;   // نام فروشنده
        widths[4] = 70;   // قیمت واحد
        widths[5] = 70;   // وزن (افزایش عرض برای جداکننده)
        widths[6] = 90;   // نام محصول
        widths[7] = 35;   // ردیف

        // اگر آیتمی وجود ندارد، عرض پیش‌فرض را برگردان
        if (items == null || items.isEmpty()) {
            return widths;
        }

        // محاسبه حداکثر عرض مورد نیاز برای هر ستون
        float maxTotalWidth = 0;
        float maxProductNameWidth = 0;
        float maxSellerWidth = 0;
        float maxMobileWidth = 0;
        float maxAddressWidth = 0;
        float maxUnitPriceWidth = 0;
        float maxWeightWidth = 0;

        for (InvoiceItem item : items) {
            long itemTotal = (long) (item.getWeight() * item.getUnitPrice());
            String totalText = String.format("%,d", itemTotal);
            float totalWidth = paint.measureText(totalText);
            if (totalWidth > maxTotalWidth) {
                maxTotalWidth = totalWidth;
            }

            // نام محصول
            if (item.getProductName() != null) {
                float productWidth = paint.measureText(item.getProductName());
                if (productWidth > maxProductNameWidth) {
                    maxProductNameWidth = productWidth;
                }
            }

            // نام فروشنده
            if (item.getName() != null) {
                float sellerWidth = paint.measureText(item.getName());
                if (sellerWidth > maxSellerWidth) {
                    maxSellerWidth = sellerWidth;
                }
            }

            // موبایل
            if (item.getMobile() != null) {
                float mobileWidth = paint.measureText(item.getMobile());
                if (mobileWidth > maxMobileWidth) {
                    maxMobileWidth = mobileWidth;
                }
            }

            // آدرس
            if (item.getAddress() != null) {
                float addressWidth = paint.measureText(item.getAddress());
                if (addressWidth > maxAddressWidth) {
                    maxAddressWidth = addressWidth;
                }
            }

            // قیمت واحد
            String unitPriceText = String.format("%,d", item.getUnitPrice());
            float unitPriceWidth = paint.measureText(unitPriceText);
            if (unitPriceWidth > maxUnitPriceWidth) {
                maxUnitPriceWidth = unitPriceWidth;
            }

            // وزن (با جداکننده)
            String weightText = formatWeightWithSeparator(item.getWeight());
            float weightWidth = paint.measureText(weightText);
            if (weightWidth > maxWeightWidth) {
                maxWeightWidth = weightWidth;
            }
        }

        // تنظیم عرض‌ها با اضافه کردن padding (8 پیکسل در هر طرف برای A4)
        widths[0] = Math.max(widths[0], maxTotalWidth + 16);
        widths[1] = Math.max(widths[1], maxAddressWidth + 16);
        widths[2] = Math.max(widths[2], maxMobileWidth + 16);
        widths[3] = Math.max(widths[3], maxSellerWidth + 16);
        widths[4] = Math.max(widths[4], maxUnitPriceWidth + 16);
        widths[5] = Math.max(widths[5], maxWeightWidth + 16);
        widths[6] = Math.max(widths[6], maxProductNameWidth + 16);

        // محدود کردن عرض‌ها برای A4
        widths[0] = Math.min(widths[0], 100); // جمع
        widths[1] = Math.min(widths[1], 100); // آدرس
        widths[2] = Math.min(widths[2], 80);  // موبایل
        widths[3] = Math.min(widths[3], 80);  // نام فروشنده
        widths[4] = Math.min(widths[4], 90);  // قیمت واحد
        widths[5] = Math.min(widths[5], 80);  // وزن (افزایش برای جداکننده)
        widths[6] = Math.min(widths[6], 100); // نام محصول

        return widths;
    }

    /**
     * محاسبه ارتفاع یک ردیف از جدول
     */
    private float calculateRowHeight(InvoiceItem item, float[] colWidths, Paint paint,
                                     float baseRowHeight, float maxRowHeight) {
        float maxCellHeight = baseRowHeight;

        String mobile = item.getMobile() != null && !item.getMobile().trim().isEmpty() ? item.getMobile() : "-";
        String address = item.getAddress() != null && !item.getAddress().trim().isEmpty() ? item.getAddress() : "-";
        String seller = item.getName() != null ? item.getName() : "-";

        // محاسبه ارتفاع برای ستون‌های متنی چند خطی
        float addressHeight = calculateTextHeight(address, colWidths[1] - 4, paint);
        float mobileHeight = calculateTextHeight(mobile, colWidths[2] - 4, paint);
        float sellerHeight = calculateTextHeight(seller, colWidths[3] - 4, paint);
        float productHeight = calculateTextHeight(item.getProductName(), colWidths[6] - 4, paint);

        maxCellHeight = Math.max(maxCellHeight, addressHeight);
        maxCellHeight = Math.max(maxCellHeight, mobileHeight);
        maxCellHeight = Math.max(maxCellHeight, sellerHeight);
        maxCellHeight = Math.max(maxCellHeight, productHeight);

        return Math.min(maxCellHeight, maxRowHeight);
    }

    /**
     * رسم یک ردیف از جدول با رنگ‌آمیزی یک در میان
     */
    private void drawTableRow(Canvas canvas, InvoiceItem item, int rowNum,
                              float[] colStarts, float[] colWidths, float rowY, float rowHeight, Paint paint,
                              boolean isEvenRow, int rowIndexInPage) {

        // رنگ پس‌زمینه ردیف (یک در میان)
        paint.setStyle(Paint.Style.FILL);
        if (rowIndexInPage % 2 == 0) {
            // ردیف زوج - خاکستری روشن
            paint.setColor(0xFFF5F5F5); // رنگ خاکستری روشن
        } else {
            // ردیف فرد - سفید
            paint.setColor(Color.WHITE);
        }

        // رسم پس‌زمینه ردیف
        float tableLeft = colStarts[0];
        float tableRight = colStarts[colStarts.length - 1];
        canvas.drawRect(tableLeft, rowY, tableRight, rowY + rowHeight, paint);

        // محتویات سلول‌ها
        long itemTotal = (long) (item.getWeight() * item.getUnitPrice());

        // وزن برای PDF (با جداکننده)
        String weightText = formatWeightWithSeparator(item.getWeight());

        String mobile = item.getMobile() != null && !item.getMobile().trim().isEmpty() ? item.getMobile() : "-";
        String address = item.getAddress() != null && !item.getAddress().trim().isEmpty() ? item.getAddress() : "-";
        String seller = item.getName() != null ? item.getName() : "-";

        String[] cells = {
                String.format("%,d", itemTotal), // جمع
                address,
                mobile,
                seller,
                String.format("%,d", item.getUnitPrice()), // قیمت واحد
                weightText, // وزن برای PDF (با جداکننده)
                item.getProductName(),
                String.valueOf(rowNum) // ردیف
        };

        // رسم متن در هر سلول
        paint.setColor(Color.BLACK); // رنگ متن سیاه
        for (int i = 0; i < cells.length; i++) {
            float cellLeft = colStarts[i];
            float cellTop = rowY;
            float cellWidth = colWidths[i];

            if (i == 0 || i == 4 || i == 5 || i == 7) {
                // ستون‌های تک خطی
                drawSingleLineText(canvas, cells[i], cellLeft, cellTop, cellWidth, rowHeight, paint);
            } else {
                // ستون‌های چند خطی
                drawMultilineText(canvas, cells[i], cellLeft, cellTop, cellWidth, rowHeight, paint);
            }
        }

        // رسم خطوط عمودی بین ستون‌ها
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f);
        paint.setColor(0xFFDDDDDD); // رنگ خطوط خاکستری روشن

        for (float x : colStarts) {
            canvas.drawLine(x, rowY, x, rowY + rowHeight, paint);
        }
    }

    private boolean createPdfWithCanvas() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "فاکتور_" + invoice.getDate().replace("/", "-") + "_" + timeStamp + ".pdf";

        try {
            DocumentFile dir = DocumentFile.fromTreeUri(this, pdfFolderUri);
            DocumentFile pdfFileDoc = dir.createFile("application/pdf", fileName);
            if (pdfFileDoc == null) {
                mainHandler.post(() ->
                        Toast.makeText(this, "خطا در ساخت فایل PDF!", Toast.LENGTH_LONG).show());
                return false;
            }

            OutputStream out = getContentResolver().openOutputStream(pdfFileDoc.getUri());
            if (out == null) {
                mainHandler.post(() ->
                        Toast.makeText(this, "خطا در دسترسی به فایل!", Toast.LENGTH_LONG).show());
                return false;
            }

            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(1);

            // ابعاد صفحه A4 (595 x 842 پیکسل)
            float pageWidth = 595;
            float pageHeight = 842;

            // افزایش حاشیه‌های چپ و راست به دو برابر (از 20 به 40 پیکسل)
            float leftMargin = 40; // دو برابر شده
            float rightMargin = 40; // دو برابر شده
            float topMargin = 20;
            float bottomMargin = 40;

            // ارتفاع هدر (لوگو و اطلاعات) - فقط در صفحه اول
            float headerHeight = 150;

            // ارتفاع ردیف‌ها
            float baseRowHeight = 30;
            float maxRowHeight = 60;
            float summaryRowHeight = baseRowHeight;

            paint.setTextSize(10);
            float[] colWidths = calculateColumnWidths(invoice.getItems(), paint);

            // تقسیم آیتم‌ها به صفحات
            List<List<InvoiceItem>> pages = new ArrayList<>();
            List<InvoiceItem> currentPage = new ArrayList<>();

            // برای صفحه اول: هدر + جدول
            float currentHeightForFirstPage = headerHeight;
            float maxHeightForFirstPage = pageHeight - topMargin - bottomMargin - summaryRowHeight;

            // برای صفحات بعدی: فقط جدول
            float currentHeightForOtherPages = 0;
            float maxHeightForOtherPages = pageHeight - topMargin - bottomMargin - summaryRowHeight;

            boolean isFirstPage = true;

            for (InvoiceItem item : invoice.getItems()) {
                float rowHeight = calculateRowHeight(item, colWidths, paint, baseRowHeight, maxRowHeight);

                if (isFirstPage) {
                    if (currentHeightForFirstPage + rowHeight <= maxHeightForFirstPage) {
                        currentPage.add(item);
                        currentHeightForFirstPage += rowHeight;
                    } else {
                        // صفحه اول پر شد
                        if (!currentPage.isEmpty()) {
                            pages.add(new ArrayList<>(currentPage));
                        }
                        currentPage.clear();
                        currentPage.add(item);
                        currentHeightForOtherPages = rowHeight;
                        isFirstPage = false;
                    }
                } else {
                    if (currentHeightForOtherPages + rowHeight <= maxHeightForOtherPages) {
                        currentPage.add(item);
                        currentHeightForOtherPages += rowHeight;
                    } else {
                        // صفحه جدید
                        if (!currentPage.isEmpty()) {
                            pages.add(new ArrayList<>(currentPage));
                        }
                        currentPage.clear();
                        currentPage.add(item);
                        currentHeightForOtherPages = rowHeight;
                    }
                }
            }

            if (!currentPage.isEmpty()) {
                pages.add(currentPage);
            }

            if (pages.isEmpty()) {
                pages.add(new ArrayList<>());
            }

            // محاسبه جمع کل
            long finalGrandTotal = 0;
            double finalTotalWeight = 0;
            for (InvoiceItem item : invoice.getItems()) {
                finalGrandTotal += (long) (item.getWeight() * item.getUnitPrice());
                finalTotalWeight += item.getWeight();
            }

            // ایجاد صفحات
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        (int) pageWidth, (int) pageHeight, pageIndex + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float currentY = topMargin;

                // فقط در صفحه اول هدر را رسم کن
                if (pageIndex == 0) {
                    drawHeaderForA4(canvas, pageWidth, leftMargin, rightMargin, currentY, paint);
                    currentY += headerHeight;
                }

                List<InvoiceItem> pageItems = pages.get(pageIndex);

                if (!pageItems.isEmpty()) {
                    // محاسبه شماره ردیف شروع برای این صفحه (ادامه از صفحه قبلی)
                    int startRowNumber = 1; // شروع از 1 برای صفحه اول
                    for (int i = 0; i < pageIndex; i++) {
                        startRowNumber += pages.get(i).size();
                    }

                    // رسم جدول با شماره ردیف‌های متوالی
                    float tableHeight;
                    if (pageIndex == 0) {
                        // صفحه اول با هدر
                        tableHeight = drawTableForA4(canvas, pageItems, pageIndex,
                                leftMargin, rightMargin, currentY, pageWidth,
                                colWidths, baseRowHeight, maxRowHeight, paint, startRowNumber);
                    } else {
                        // صفحات بعدی بدون هدر
                        tableHeight = drawTableForA4(canvas, pageItems, pageIndex,
                                leftMargin, rightMargin, topMargin, pageWidth,
                                colWidths, baseRowHeight, maxRowHeight, paint, startRowNumber);
                    }

                    // اگر آخرین صفحه است، جمع کل اضافه شود
                    if (pageIndex == pages.size() - 1) {
                        float summaryY;
                        if (pageIndex == 0) {
                            // اگر فقط یک صفحه وجود دارد
                            summaryY = currentY + tableHeight;
                        } else {
                            // اگر چند صفحه وجود دارد
                            summaryY = topMargin + tableHeight;
                        }
                        drawFinalSummary(canvas, pageWidth, leftMargin, rightMargin,
                                summaryY, summaryRowHeight, finalGrandTotal, finalTotalWeight, paint);
                    }
                } else {
                    // اگر صفحه خالی است (فقط در صفحه اول ممکن است)
                    if (pageIndex == 0) {
                        paint.setTextSize(24);
                        paint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("فاکتور خالی است", pageWidth / 2, pageHeight / 2, paint);
                    }
                }

                // شماره صفحه
                paint.setTextSize(9);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("صفحه " + (pageIndex + 1) + " از " + pages.size(),
                        pageWidth / 2, pageHeight - 15, paint);

                pdfDocument.finishPage(page);
            }

            pdfDocument.writeTo(out);
            pdfDocument.close();
            out.close();

            final String finalFileName = fileName;
            mainHandler.post(() ->
                    Toast.makeText(this, "PDF با موفقیت ساخته شد!\n" + finalFileName, Toast.LENGTH_LONG).show());

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            final String errorMessage = e.getMessage();
            mainHandler.post(() ->
                    Toast.makeText(this, "خطا در ساخت PDF: " + errorMessage, Toast.LENGTH_LONG).show());
            return false;
        }
    }

    /**
     * هدر مخصوص A4 (فقط صفحه اول)
     * لوگو و اطلاعات مشتری در یک سطح زیر عنوان "فاکتور فروش"
     */
    private void drawHeaderForA4(Canvas canvas, float pageWidth, float leftMargin,
                                 float rightMargin, float topY, Paint paint) {

        // عنوان فاکتور در بالای صفحه
        paint.setTextSize(24);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("فاکتور فروش", pageWidth / 2f, topY + 35, paint);

        // ارتفاع برای لوگو و اطلاعات (در یک سطح)
        float logoInfoY = topY + 75;

        // اندازه لوگو
        int logoSize = 70;

        // لوگو در سمت چپ
        try {
            InputStream logoStream = getAssets().open("images/logo.png");
            Bitmap logoBitmap = BitmapFactory.decodeStream(logoStream);
            logoStream.close();

            Bitmap scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true);

            // موقعیت لوگو: چپ، هم‌سطح با اطلاعات
            float logoX = leftMargin;
            float logoY = logoInfoY - (logoSize / 2);

            canvas.drawBitmap(scaledLogo, logoX, logoY, paint);

            // وب‌سایت زیر لوگو
            paint.setTextSize(8);
            paint.setTextAlign(Paint.Align.LEFT);
            String website = "www.nobarexport.com";
            float websiteY = logoY + scaledLogo.getHeight() + 8;
            canvas.drawText(website, logoX, websiteY, paint);

            scaledLogo.recycle();
            logoBitmap.recycle();
        } catch (Exception e) {
            Log.w("PDF", "لوگو پیدا نشد");
        }

        // اطلاعات مشتری در سمت راست (هم‌سطح با لوگو)
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.RIGHT);

        // موقعیت شروع اطلاعات در سمت راست
        float infoX = pageWidth - rightMargin;
        float infoY = logoInfoY;

        // نام مشتری
        String customerName = "نام مشتری: " + invoice.getCustomerName();
        canvas.drawText(customerName, infoX, infoY, paint);
        infoY += 22;

        // تلفن تماس (اگر وجود دارد)
        if (!invoice.getCustomerPhone().isEmpty()) {
            String phoneText = "تلفن تماس: " + invoice.getCustomerPhone();
            canvas.drawText(phoneText, infoX, infoY, paint);
            infoY += 22;
        }

        // تاریخ فاکتور
        String dateText = "تاریخ فاکتور: " + invoice.getDate();
        canvas.drawText(dateText, infoX, infoY, paint);
        infoY += 22;

        // شماره فاکتور
        String invoiceNumber = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "پیش‌نویس";
        String invoiceNumberText = "شماره فاکتور: " + invoiceNumber;
        canvas.drawText(invoiceNumberText, infoX, infoY, paint);

        // خط جداکننده زیر هدر
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(Color.BLACK);
        float lineY = infoY + 15;
        canvas.drawLine(leftMargin, lineY, pageWidth - rightMargin, lineY, paint);
    }

    /**
     * رسم جدول برای A4 (بدون ردیف خلاصه) با سطرهای یک در میان رنگی و شماره ردیف‌های متوالی
     */
    private float drawTableForA4(Canvas canvas, List<InvoiceItem> pageItems, int pageIndex,
                                 float leftMargin, float rightMargin, float startY, float pageWidth,
                                 float[] colWidths, float baseRowHeight, float maxRowHeight,
                                 Paint paint, int startRowNumber) {

        float tableLeft = leftMargin;
        float tableRight = pageWidth - rightMargin;
        float tableTop = startY;

        // تنظیم عرض ستون‌ها
        float totalColWidth = 0;
        for (float width : colWidths) {
            totalColWidth += width;
        }

        float availableWidth = tableRight - tableLeft;
        if (totalColWidth > availableWidth) {
            float scale = availableWidth / totalColWidth;
            for (int i = 0; i < colWidths.length; i++) {
                colWidths[i] *= scale;
            }
        }

        // موقعیت ستون‌ها
        float[] colStarts = new float[colWidths.length + 1];
        colStarts[colWidths.length] = tableRight;
        for (int i = colWidths.length - 1; i >= 0; i--) {
            colStarts[i] = colStarts[i + 1] - colWidths[i];
        }

        // هدر جدول - خاکستری
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(tableLeft, tableTop, tableRight, tableTop + baseRowHeight, paint);

        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        String[] headers = {"جمع", "آدرس فروشنده", "موبایل فروشنده", "نام فروشنده",
                "قیمت واحد", "وزن", "نام محصول", "ردیف"};

        for (int i = 0; i < headers.length; i++) {
            float centerX = colStarts[i] + colWidths[i] / 2f;
            canvas.drawText(headers[i], centerX, tableTop + 18, paint);
        }

        // خطوط هدر
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(Color.BLACK);

        for (float x : colStarts) {
            canvas.drawLine(x, tableTop, x, tableTop + baseRowHeight, paint);
        }
        canvas.drawLine(tableLeft, tableTop, tableRight, tableTop, paint);
        canvas.drawLine(tableLeft, tableTop + baseRowHeight, tableRight, tableTop + baseRowHeight, paint);

        // محتوای جدول
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(9);

        float rowY = tableTop + baseRowHeight;

        // رسم ردیف‌ها با شماره‌های متوالی
        for (int i = 0; i < pageItems.size(); i++) {
            InvoiceItem item = pageItems.get(i);
            int rowNum = startRowNumber + i; // شماره ردیف واقعی با احتساب صفحات قبلی

            float rowHeight = calculateRowHeight(item, colWidths, paint, baseRowHeight, maxRowHeight);

            // رسم ردیف با رنگ‌آمیزی یک در میان
            drawTableRow(canvas, item, rowNum, colStarts, colWidths, rowY, rowHeight, paint,
                    i % 2 == 0, i);

            // خط پایین ردیف
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(0.5f);
            paint.setColor(0xFFDDDDDD); // رنگ خطوط خاکستری روشن
            canvas.drawLine(tableLeft, rowY + rowHeight, tableRight, rowY + rowHeight, paint);

            rowY += rowHeight;
        }

        // خطوط عمودی خارجی جدول
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(Color.BLACK);
        canvas.drawLine(tableLeft, tableTop + baseRowHeight, tableLeft, rowY, paint);
        canvas.drawLine(tableRight, tableTop + baseRowHeight, tableRight, rowY, paint);

        // بازگرداندن ارتفاع کل جدول
        return rowY - tableTop;
    }

    /**
     * رسم جمع کل فقط در آخرین صفحه
     */
    private void drawFinalSummary(Canvas canvas, float pageWidth, float leftMargin, float rightMargin,
                                  float startY, float summaryRowHeight, long finalGrandTotal, double finalTotalWeight, Paint paint) {

        float tableLeft = leftMargin;
        float tableRight = pageWidth - rightMargin;
        float summaryRowY = startY;

        // پس‌زمینه خاکستری تیره‌تر
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFE8E8E8); // خاکستری کمی تیره‌تر از سطرهای جدول
        canvas.drawRect(tableLeft, summaryRowY, tableRight, summaryRowY + summaryRowHeight, paint);

        // متن
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(11);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float textY = summaryRowY + (summaryRowHeight - textHeight) / 2 - fontMetrics.ascent;

        // فرمت وزن برای خلاصه PDF (با جداکننده)
        String weightText = formatWeightWithSeparator(finalTotalWeight);

        float summaryColWidth = (tableRight - tableLeft) / 2;
        float leftColCenterX = tableLeft + summaryColWidth / 2;
        float rightColCenterX = tableLeft + summaryColWidth + summaryColWidth / 2;

        canvas.drawText("جمع کل وزن: " + weightText + " کیلوگرم", leftColCenterX, textY, paint);
        canvas.drawText("جمع کل قابل پرداخت: " + String.format("%,d تومان", finalGrandTotal),
                rightColCenterX, textY, paint);

        // خطوط
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(Color.BLACK);

        canvas.drawLine(tableLeft, summaryRowY, tableRight, summaryRowY, paint);
        float verticalLineX = tableLeft + summaryColWidth;
        canvas.drawLine(verticalLineX, summaryRowY, verticalLineX, summaryRowY + summaryRowHeight, paint);
        canvas.drawLine(tableLeft, summaryRowY + summaryRowHeight, tableRight, summaryRowY + summaryRowHeight, paint);
        canvas.drawLine(tableLeft, summaryRowY, tableLeft, summaryRowY + summaryRowHeight, paint);
        canvas.drawLine(tableRight, summaryRowY, tableRight, summaryRowY + summaryRowHeight, paint);
    }
}