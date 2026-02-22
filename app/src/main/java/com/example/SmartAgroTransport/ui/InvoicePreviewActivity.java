package com.example.SmartAgroTransport.ui;

import android.graphics.pdf.PdfDocument;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

    // رنگ‌های جدید برای ظاهر زیباتر
    private static final int COLOR_PRIMARY = 0xFF2E7D32; // سبز تیره
    private static final int COLOR_SECONDARY = 0xFF4CAF50; // سبز روشن
    private static final int COLOR_ACCENT = 0xFF8BC34A; // سبز ملایم
    private static final int COLOR_HEADER_BG = 0xFF37474F; // آبی-خاکستری تیره
    private static final int COLOR_HEADER_TEXT = Color.WHITE;
    private static final int COLOR_ROW_EVEN = 0xFFF5F5F5; // خاکستری خیلی روشن
    private static final int COLOR_ROW_ODD = Color.WHITE;
    private static final int COLOR_BORDER = 0xFFBDBDBD; // خاکستری
    private static final int COLOR_TOTAL_BG = 0xFFE8F5E9; // سبز خیلی روشن
    private static final int COLOR_TOTAL_TEXT = COLOR_PRIMARY;

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
            Log.e("InvoicePreviewActivity", "backButton is null!");
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
        // استفاده از Html.fromHtml برای اطمینان از نمایش ایموجی‌ها
        String customerText = "&#128100;  " + invoice.getCustomerName() +
                (invoice.getCustomerPhone().isEmpty() ? "" : " - &#128222; " + invoice.getCustomerPhone());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvPreviewCustomer.setText(Html.fromHtml(customerText, Html.FROM_HTML_MODE_LEGACY));
            tvPreviewDate.setText(Html.fromHtml("&#128197; تاریخ: " + invoice.getDate(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvPreviewCustomer.setText(Html.fromHtml(customerText));
            tvPreviewDate.setText(Html.fromHtml("&#128197; تاریخ: " + invoice.getDate()));
        }

        // تنظیمات راست‌چین
        tvPreviewCustomer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tvPreviewCustomer.setTextDirection(View.TEXT_DIRECTION_RTL);
        tvPreviewDate.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tvPreviewDate.setTextDirection(View.TEXT_DIRECTION_RTL);

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
            addCell(row, formatWeightWithSeparator(item.getWeight()));
            addCell(row, String.format("%,d", item.getUnitPrice()));
            addCell(row, item.getName()!= null ? item.getName(): "-");
            addCell(row, item.getMobile() != null ? item.getMobile() : "-");
            addCell(row, item.getAddress() != null ? item.getAddress() : "-");
            addCell(row, String.format("%,d", itemTotal));

            tableItems.addView(row);
        }

        // ============== روش تضمینی برای راست‌چین کردن ==============
        String grandTotalFormatted = String.format("%,d", grandTotal);
        String totalWeightFormatted = formatWeightWithSeparator(totalWeight);

        String text = "💰 جمع کل قابل پرداخت: " + grandTotalFormatted + " تومان\n" +
                "⚖️ وزن کل: " + totalWeightFormatted + " کیلوگرم";

        tvPreviewTotal.setText(text);

        // تنظیمات تضمینی برای راست‌چین کردن
        tvPreviewTotal.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tvPreviewTotal.setTextDirection(View.TEXT_DIRECTION_RTL);
        tvPreviewTotal.setGravity(Gravity.RIGHT);
        tvPreviewTotal.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);

        tvPreviewTotal.setTextColor(Color.BLACK);
        tvPreviewTotal.setTextSize(16);
        tvPreviewTotal.setPadding(24, 24, 24, 24);
        tvPreviewTotal.setBackgroundColor(0xFFF5F5F5);
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

    private String formatWeightWithSeparator(double weight) {
        if (weight == 0) return "0";

        if (weight == (long) weight) {
            return String.format("%,d", (long) weight);
        } else {
            DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));
            return df.format(weight);
        }
    }

    private List<String> breakTextIntoLines(String text, float maxWidth, Paint paint) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty() || text.equals("-")) {
            lines.add("-");
            return lines;
        }

        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

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

    private float calculateTextHeight(String text, float maxWidth, Paint paint) {
        List<String> lines = breakTextIntoLines(text, maxWidth - 8, paint);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float lineHeight = fontMetrics.descent - fontMetrics.ascent;
        return Math.max(lineHeight, lines.size() * lineHeight);
    }

    private void drawMultilineText(Canvas canvas, String text, float left, float top, float width, float height, Paint paint) {
        List<String> lines = breakTextIntoLines(text, width - 8, paint);

        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float lineHeight = fontMetrics.descent - fontMetrics.ascent;
        float textHeight = lines.size() * lineHeight;

        float y = top + (height - textHeight) / 2 - fontMetrics.ascent;

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

    private void drawSingleLineText(Canvas canvas, String text, float left, float top, float width, float height, Paint paint) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float y = top + (height - textHeight) / 2 - fontMetrics.ascent;
        float centerX = left + width / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, centerX, y, paint);
    }

    private float[] calculateColumnWidths(List<InvoiceItem> items, Paint paint) {
        float[] widths = new float[8];

        widths[0] = 90;   // جمع
        widths[1] = 80;   // آدرس
        widths[2] = 70;   // موبایل
        widths[3] = 70;   // نام فروشنده
        widths[4] = 75;   // قیمت واحد
        widths[5] = 75;   // وزن
        widths[6] = 100;  // نام محصول
        widths[7] = 40;   // ردیف

        if (items == null || items.isEmpty()) return widths;

        float maxTotalWidth = 0, maxProductNameWidth = 0, maxSellerWidth = 0;
        float maxMobileWidth = 0, maxAddressWidth = 0, maxUnitPriceWidth = 0, maxWeightWidth = 0;

        for (InvoiceItem item : items) {
            long itemTotal = (long) (item.getWeight() * item.getUnitPrice());
            String totalText = String.format("%,d", itemTotal);
            maxTotalWidth = Math.max(maxTotalWidth, paint.measureText(totalText));

            if (item.getProductName() != null)
                maxProductNameWidth = Math.max(maxProductNameWidth, paint.measureText(item.getProductName()));

            if (item.getName() != null)
                maxSellerWidth = Math.max(maxSellerWidth, paint.measureText(item.getName()));

            if (item.getMobile() != null)
                maxMobileWidth = Math.max(maxMobileWidth, paint.measureText(item.getMobile()));

            if (item.getAddress() != null)
                maxAddressWidth = Math.max(maxAddressWidth, paint.measureText(item.getAddress()));

            String unitPriceText = String.format("%,d", item.getUnitPrice());
            maxUnitPriceWidth = Math.max(maxUnitPriceWidth, paint.measureText(unitPriceText));

            String weightText = formatWeightWithSeparator(item.getWeight());
            maxWeightWidth = Math.max(maxWeightWidth, paint.measureText(weightText));
        }

        widths[0] = Math.max(widths[0], maxTotalWidth + 20);
        widths[1] = Math.max(widths[1], maxAddressWidth + 20);
        widths[2] = Math.max(widths[2], maxMobileWidth + 20);
        widths[3] = Math.max(widths[3], maxSellerWidth + 20);
        widths[4] = Math.max(widths[4], maxUnitPriceWidth + 20);
        widths[5] = Math.max(widths[5], maxWeightWidth + 20);
        widths[6] = Math.max(widths[6], maxProductNameWidth + 20);

        // محدود کردن حداکثر عرض
        widths[0] = Math.min(widths[0], 110);
        widths[1] = Math.min(widths[1], 110);
        widths[2] = Math.min(widths[2], 90);
        widths[3] = Math.min(widths[3], 90);
        widths[4] = Math.min(widths[4], 100);
        widths[5] = Math.min(widths[5], 90);
        widths[6] = Math.min(widths[6], 110);

        return widths;
    }

    private float calculateRowHeight(InvoiceItem item, float[] colWidths, Paint paint,
                                     float baseRowHeight, float maxRowHeight) {
        float maxCellHeight = baseRowHeight;

        String mobile = item.getMobile() != null && !item.getMobile().trim().isEmpty() ? item.getMobile() : "-";
        String address = item.getAddress() != null && !item.getAddress().trim().isEmpty() ? item.getAddress() : "-";
        String seller = item.getName() != null ? item.getName() : "-";

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

    private void drawTableRow(Canvas canvas, InvoiceItem item, int rowNum,
                              float[] colStarts, float[] colWidths, float rowY, float rowHeight, Paint paint,
                              boolean isEvenRow, int rowIndexInPage) {

        // پس‌زمینه ردیف
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isEvenRow ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
        float tableLeft = colStarts[0];
        float tableRight = colStarts[colStarts.length - 1];
        canvas.drawRect(tableLeft, rowY, tableRight, rowY + rowHeight, paint);

        long itemTotal = (long) (item.getWeight() * item.getUnitPrice());
        String weightText = formatWeightWithSeparator(item.getWeight());
        String mobile = item.getMobile() != null && !item.getMobile().trim().isEmpty() ? item.getMobile() : "-";
        String address = item.getAddress() != null && !item.getAddress().trim().isEmpty() ? item.getAddress() : "-";
        String seller = item.getName() != null ? item.getName() : "-";

        String[] cells = {
                String.format("%,d", itemTotal),
                address,
                mobile,
                seller,
                String.format("%,d", item.getUnitPrice()),
                weightText,
                item.getProductName(),
                String.valueOf(rowNum)
        };

        paint.setColor(Color.BLACK);
        for (int i = 0; i < cells.length; i++) {
            float cellLeft = colStarts[i];
            float cellTop = rowY;
            float cellWidth = colWidths[i];

            if (i == 0 || i == 4 || i == 5 || i == 7) {
                drawSingleLineText(canvas, cells[i], cellLeft, cellTop, cellWidth, rowHeight, paint);
            } else {
                drawMultilineText(canvas, cells[i], cellLeft, cellTop, cellWidth, rowHeight, paint);
            }
        }

        // خطوط جدول
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f);
        paint.setColor(COLOR_BORDER);
        for (float x : colStarts) {
            canvas.drawLine(x, rowY, x, rowY + rowHeight, paint);
        }
    }

    private void drawFinalSummary(Canvas canvas, float pageWidth, float leftMargin, float rightMargin,
                                  float startY, float summaryRowHeight, long finalGrandTotal,
                                  double finalTotalWeight, Paint paint) {

        float tableLeft = leftMargin;
        float tableRight = pageWidth - rightMargin;
        float summaryRowY = startY;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOR_TOTAL_BG);
        RectF summaryRect = new RectF(tableLeft, summaryRowY, tableRight, summaryRowY + summaryRowHeight + 10);
        canvas.drawRoundRect(summaryRect, 8, 8, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(COLOR_PRIMARY);
        canvas.drawRoundRect(summaryRect, 8, 8, paint);

        paint.setColor(COLOR_TOTAL_TEXT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(12);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = summaryRowY + (summaryRowHeight + 10 - (fm.descent - fm.ascent)) / 2 - fm.ascent + 5;

        String weightText = formatWeightWithSeparator(finalTotalWeight);

        float summaryColWidth = (tableRight - tableLeft) / 2;
        float leftColCenterX = tableLeft + summaryColWidth / 2;
        float rightColCenterX = tableLeft + summaryColWidth + summaryColWidth / 2;

        canvas.drawText("⚖️ جمع کل وزن: " + weightText + " کیلوگرم", leftColCenterX, textY, paint);
        canvas.drawText("💰 جمع کل قابل پرداخت: " + String.format("%,d تومان", finalGrandTotal),
                rightColCenterX, textY, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(COLOR_PRIMARY);
        float verticalLineX = tableLeft + summaryColWidth;
        canvas.drawLine(verticalLineX, summaryRowY + 5, verticalLineX, summaryRowY + summaryRowHeight + 5, paint);
    }

    private boolean createPdfWithCanvas() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "فاکتور_" + invoice.getDate().replace("/", "-") + "_" + timeStamp + ".pdf";

        try {
            DocumentFile dir = DocumentFile.fromTreeUri(this, pdfFolderUri);
            DocumentFile pdfFileDoc = dir.createFile("application/pdf", fileName);
            if (pdfFileDoc == null) {
                mainHandler.post(() -> Toast.makeText(this, "خطا در ساخت فایل PDF!", Toast.LENGTH_LONG).show());
                return false;
            }

            OutputStream out = getContentResolver().openOutputStream(pdfFileDoc.getUri());
            if (out == null) {
                mainHandler.post(() -> Toast.makeText(this, "خطا در دسترسی به فایل!", Toast.LENGTH_LONG).show());
                return false;
            }

            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(1);

            // لود فونت فارسی
            try {
                Typeface vazirTypeface = Typeface.createFromAsset(getAssets(), "fonts/bnazaninr_bold.ttf");
                paint.setTypeface(vazirTypeface);
            } catch (Exception e) {
                Log.e("PDF", "خطا در لود فونت", e);
            }

            float pageWidth = 595;
            float pageHeight = 842;

            float leftMargin = 40;
            float rightMargin = 40;
            float topMargin = 30;
            float bottomMargin = 40;

            float headerHeight = 140;
            float baseRowHeight = 35;
            float maxRowHeight = 65;
            float summaryRowHeight = 45;

            paint.setTextSize(10);
            float[] colWidths = calculateColumnWidths(invoice.getItems(), paint);

            // صفحه‌بندی
            List<List<InvoiceItem>> pages = new ArrayList<>();
            List<InvoiceItem> currentPage = new ArrayList<>();

            float currentHeightFirst = 0;
            float maxHeightFirst = pageHeight - topMargin - bottomMargin - summaryRowHeight - 20 - headerHeight;

            float currentHeightOthers = 0;
            float maxHeightOthers = pageHeight - topMargin - bottomMargin - summaryRowHeight - 20;

            boolean isFirstPage = true;

            for (InvoiceItem item : invoice.getItems()) {
                float rowHeight = calculateRowHeight(item, colWidths, paint, baseRowHeight, maxRowHeight);

                if (isFirstPage) {
                    if (currentHeightFirst + rowHeight <= maxHeightFirst) {
                        currentPage.add(item);
                        currentHeightFirst += rowHeight;
                    } else {
                        if (!currentPage.isEmpty()) pages.add(new ArrayList<>(currentPage));
                        currentPage.clear();
                        currentPage.add(item);
                        currentHeightOthers = rowHeight;
                        isFirstPage = false;
                    }
                } else {
                    if (currentHeightOthers + rowHeight <= maxHeightOthers) {
                        currentPage.add(item);
                        currentHeightOthers += rowHeight;
                    } else {
                        if (!currentPage.isEmpty()) pages.add(new ArrayList<>(currentPage));
                        currentPage.clear();
                        currentPage.add(item);
                        currentHeightOthers = rowHeight;
                    }
                }
            }

            if (!currentPage.isEmpty()) pages.add(currentPage);
            if (pages.isEmpty()) pages.add(new ArrayList<>());

            long finalGrandTotal = 0;
            double finalTotalWeight = 0;
            for (InvoiceItem item : invoice.getItems()) {
                finalGrandTotal += (long) (item.getWeight() * item.getUnitPrice());
                finalTotalWeight += item.getWeight();
            }

            // رسم صفحات
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        (int) pageWidth, (int) pageHeight, pageIndex + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                float currentY = topMargin;

                if (pageIndex == 0) {
                    drawHeaderForA4(canvas, pageWidth, leftMargin, rightMargin, currentY, paint);
                    currentY += headerHeight + 10;
                }

                List<InvoiceItem> pageItems = pages.get(pageIndex);

                if (!pageItems.isEmpty()) {
                    int startRowNumber = 1;
                    for (int i = 0; i < pageIndex; i++) {
                        startRowNumber += pages.get(i).size();
                    }

                    float tableHeight;
                    if (pageIndex == 0) {
                        tableHeight = drawTableForA4(canvas, pageItems, pageIndex,
                                leftMargin, rightMargin, currentY, pageWidth,
                                colWidths, baseRowHeight, maxRowHeight, paint, startRowNumber);

                        if (pageIndex == pages.size() - 1) {
                            float summaryY = currentY + tableHeight + 10;
                            drawFinalSummary(canvas, pageWidth, leftMargin, rightMargin,
                                    summaryY, summaryRowHeight, finalGrandTotal, finalTotalWeight, paint);
                        }
                    } else {
                        tableHeight = drawTableForA4(canvas, pageItems, pageIndex,
                                leftMargin, rightMargin, topMargin, pageWidth,
                                colWidths, baseRowHeight, maxRowHeight, paint, startRowNumber);

                        if (pageIndex == pages.size() - 1) {
                            float summaryY = topMargin + tableHeight + 10;
                            drawFinalSummary(canvas, pageWidth, leftMargin, rightMargin,
                                    summaryY, summaryRowHeight, finalGrandTotal, finalTotalWeight, paint);
                        }
                    }
                } else if (pageIndex == 0) {
                    paint.setTextSize(24);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("فاکتور خالی است", pageWidth / 2, pageHeight / 2, paint);
                }

                // شماره صفحه
                paint.setTextSize(9);
                paint.setColor(Color.GRAY);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("صفحه " + (pageIndex + 1) + " از " + pages.size(),
                        pageWidth / 2, pageHeight - 20, paint);

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
                    Toast.makeText(this, "خطا در ساخت PDF: " + (errorMessage != null ? errorMessage : "نامشخص"), Toast.LENGTH_LONG).show());
            return false;
        }
    }


    private void drawHeaderForA4(Canvas canvas, float pageWidth, float leftMargin,
                                 float rightMargin, float topY, Paint paint) {

        float headerTopY = topY;
        float headerBottomY = headerTopY + 130;

        // پس‌زمینه هدر با رنگ تیره برای نمایش بهتر ایموجی‌های سفید
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOR_HEADER_BG); // رنگ تیره (آبی-خاکستری)
        RectF headerBg = new RectF(leftMargin - 5, headerTopY - 5, pageWidth - rightMargin + 5, headerBottomY);
        canvas.drawRoundRect(headerBg, 10, 10, paint);

        // خط تزئینی زیر هدر
        paint.setColor(COLOR_ACCENT);
        paint.setStrokeWidth(3);
        canvas.drawLine(leftMargin, headerBottomY - 5, pageWidth - rightMargin, headerBottomY - 5, paint);

        // عنوان فاکتور
        paint.setColor(COLOR_HEADER_TEXT);
        paint.setTextSize(28);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("فاکتور فروش", pageWidth / 2f, headerTopY + 40, paint);

        float logoInfoY = headerTopY + 80;

        // لوگو
        int logoSize = 70;
        try {
            InputStream logoStream = getAssets().open("images/logo.png");
            Bitmap logoBitmap = BitmapFactory.decodeStream(logoStream);
            logoStream.close();

            Bitmap scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true);

            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(leftMargin - 5, logoInfoY - logoSize/2 - 5,
                    leftMargin + logoSize + 5, logoInfoY + logoSize/2 + 5, 10, 10, paint);

            float logoX = leftMargin;
            float logoY = logoInfoY - (logoSize / 2);
            canvas.drawBitmap(scaledLogo, logoX, logoY, paint);

            paint.setColor(COLOR_HEADER_TEXT);
            paint.setTextSize(9);
            paint.setTextAlign(Paint.Align.LEFT);
            String website = "www.nobarexport.com";
            float websiteY = logoY + scaledLogo.getHeight() + 15;
            canvas.drawText(website, logoX, websiteY, paint);

            scaledLogo.recycle();
            logoBitmap.recycle();
        } catch (Exception e) {
            Log.w("PDF", "لوگو پیدا نشد");
            paint.setColor(COLOR_HEADER_TEXT);
            paint.setTextSize(14);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("شرکت", leftMargin, logoInfoY, paint);
        }

        // تنظیمات پایه
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float infoX = pageWidth - rightMargin - 10;
        float infoY = logoInfoY - 25;

        // ============== خط 1: نام مشتری ==============
        String customerName = invoice.getCustomerName();
        // رسم ایموجی با رنگ سفید (چون پس‌زمینه تیره است)
        paint.setColor(COLOR_HEADER_TEXT);
        canvas.drawText("👤", infoX, infoY, paint);
        float emojiWidth = paint.measureText("👤");
        canvas.drawText(" " + customerName, infoX - emojiWidth, infoY, paint);

        infoY += 22;

        // ============== خط 2: تلفن ==============
        if (!invoice.getCustomerPhone().isEmpty()) {
            String phone = invoice.getCustomerPhone();
            paint.setColor(COLOR_HEADER_TEXT);
            canvas.drawText("📞", infoX, infoY, paint);
            float emoji2Width = paint.measureText("📞");
            canvas.drawText(" " + phone, infoX - emoji2Width, infoY, paint);
            infoY += 22;
        }

        // ============== خط 3: تاریخ ==============
        String date = invoice.getDate();
        paint.setColor(COLOR_HEADER_TEXT);
        canvas.drawText("📅", infoX, infoY, paint);
        float emoji3Width = paint.measureText("📅");
        canvas.drawText(" " + date, infoX - emoji3Width, infoY, paint);
        infoY += 22;

        // ============== خط 4: شماره فاکتور ==============
        String invoiceNumber = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "پیش‌نویس";
        paint.setColor(COLOR_HEADER_TEXT);
        canvas.drawText("🔢", infoX, infoY, paint);
        float emoji4Width = paint.measureText("🔢");
        canvas.drawText(" " + invoiceNumber, infoX - emoji4Width, infoY, paint);
    }

    private float drawTableForA4(Canvas canvas, List<InvoiceItem> pageItems, int pageIndex,
                                 float leftMargin, float rightMargin, float startY, float pageWidth,
                                 float[] colWidths, float baseRowHeight, float maxRowHeight,
                                 Paint paint, int startRowNumber) {

        float tableLeft = leftMargin;
        float tableRight = pageWidth - rightMargin;
        float tableTop = startY;

        float totalColWidth = 0;
        for (float w : colWidths) totalColWidth += w;

        float availableWidth = tableRight - tableLeft;
        if (totalColWidth > availableWidth) {
            float scale = availableWidth / totalColWidth;
            for (int i = 0; i < colWidths.length; i++) {
                colWidths[i] *= scale;
            }
        }

        float[] colStarts = new float[colWidths.length + 1];
        colStarts[colWidths.length] = tableRight;
        for (int i = colWidths.length - 1; i >= 0; i--) {
            colStarts[i] = colStarts[i + 1] - colWidths[i];
        }

        // هدر جدول
        paint.setColor(COLOR_PRIMARY);
        paint.setStyle(Paint.Style.FILL);
        RectF headerRect = new RectF(tableLeft, tableTop, tableRight, tableTop + baseRowHeight + 5);
        canvas.drawRoundRect(headerRect, 8, 8, paint);

        paint.setColor(COLOR_HEADER_TEXT);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        String[] headers = {"جمع (تومان)", "آدرس فروشنده", "موبایل", "نام فروشنده",
                "قیمت واحد", "وزن (کیلوگرم)", "نام محصول", "ردیف"};

        for (int i = 0; i < headers.length; i++) {
            float centerX = colStarts[i] + colWidths[i] / 2f;
            canvas.drawText(headers[i], centerX, tableTop + 20, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(COLOR_BORDER);

        for (float x : colStarts) {
            canvas.drawLine(x, tableTop, x, tableTop + baseRowHeight + 5, paint);
        }
        canvas.drawLine(tableLeft, tableTop + baseRowHeight + 5, tableRight, tableTop + baseRowHeight + 5, paint);

        // ردیف‌های جدول
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(9);

        float rowY = tableTop + baseRowHeight + 5;

        for (int i = 0; i < pageItems.size(); i++) {
            InvoiceItem item = pageItems.get(i);
            int rowNum = startRowNumber + i;
            float rowHeight = calculateRowHeight(item, colWidths, paint, baseRowHeight, maxRowHeight);

            // اصلاح شده: استفاده از rowNum برای تشخیص زوج یا فرد بودن
            boolean isEvenRow = (rowNum % 2 != 0); // ردیف‌های فرد خاکستری، ردیف‌های زوج سفید

            drawTableRow(canvas, item, rowNum, colStarts, colWidths, rowY, rowHeight, paint,
                    isEvenRow, i);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(0.5f);
            paint.setColor(COLOR_BORDER);
            canvas.drawLine(tableLeft, rowY + rowHeight, tableRight, rowY + rowHeight, paint);

            rowY += rowHeight;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(COLOR_PRIMARY);
        canvas.drawLine(tableLeft, tableTop, tableLeft, rowY, paint);
        canvas.drawLine(tableRight, tableTop, tableRight, rowY, paint);

        return rowY - tableTop;
    }
}