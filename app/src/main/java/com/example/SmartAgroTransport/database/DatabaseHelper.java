package com.example.SmartAgroTransport.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.example.SmartAgroTransport.model.BuyerDelivery;
import com.example.SmartAgroTransport.model.Clearance;
import com.example.SmartAgroTransport.model.Invoice;
import com.example.SmartAgroTransport.model.InvoiceItem;
import com.example.SmartAgroTransport.model.IraqiHandover;
import com.example.SmartAgroTransport.model.Shipment;
import com.ibm.icu.util.PersianCalendar;

import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "invoice_db";
    private static final int DATABASE_VERSION = 18; // افزایش به 18 برای جدول attachments

    // جدول فاکتورها
    private static final String TABLE_INVOICES = "invoices";
    private static final String KEY_ID = "id";
    private static final String KEY_INVOICE_NUMBER = "invoice_number";
    private static final String KEY_CUSTOMER_NAME = "customer_name";
    private static final String KEY_CUSTOMER_PHONE = "customer_phone";
    private static final String KEY_DATE = "date";
    private static final String KEY_TOTAL_AMOUNT = "total_amount";
    private static final String KEY_LOAD_STATUS = "load_status";

    // جدول آیتم‌ها
    private static final String TABLE_ITEMS = "invoice_items";
    private static final String KEY_INVOICE_ID = "invoice_id";
    private static final String KEY_PRODUCT_NAME = "product_name";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_UNIT_PRICE = "unit_price";
    private static final String KEY_NAME = "name";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_NOTES = "notes";

    // جدول کاربران
    private static final String TABLE_USERS = "users";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ROLE = "role";

    // جدول بارگیری
    private static final String TABLE_SHIPMENTS = "shipments";
    private static final String KEY_SHIPMENT_ID = "id";
    private static final String KEY_SHIPMENT_INVOICE_ID = "invoice_id";
    private static final String KEY_SHIPMENT_INVOICE_NUMBER = "invoice_number";
    private static final String KEY_SHIPMENT_TRUCK_NAME = "truck_name";
    private static final String KEY_SHIPMENT_PLATE_NUMBER = "plate_number";
    private static final String KEY_SHIPMENT_DRIVER_NAME = "driver_name";
    private static final String KEY_SHIPMENT_DRIVER_PHONE = "driver_phone";
    private static final String KEY_SHIPMENT_LOAD_DATE = "load_date";
    private static final String KEY_SHIPMENT_CLEARANCE_STATUS = "clearance_status";
    private static final String KEY_TRANSPORT_COST = "transport_cost";

    // جدول ترخیص ایرانی
    private static final String TABLE_CLEARANCES = "clearances";
    private static final String KEY_CLEARANCE_ID = "id";
    private static final String KEY_CLEARANCE_SHIPMENT_ID = "shipment_id";
    private static final String KEY_CLEARANCE_NAME = "clearance_name";
    private static final String KEY_CLEARANCE_PHONE = "clearance_phone";
    private static final String KEY_BORDER_NAME = "border_name";
    private static final String KEY_CLEARANCE_DATE = "clearance_date";
    private static final String KEY_CLEARANCE_NOTES = "notes";
    private static final String KEY_CLEARANCE_COST = "clearance_cost";

    // جدول تحویل به ترخیص‌کار عراقی
    private static final String TABLE_IRAQI_HANDOVERS = "iraqi_handovers";
    private static final String KEY_IRAQI_ID = "id";
    private static final String KEY_IRAQI_SHIPMENT_ID = "shipment_id";

    // جدول تحویل نهایی به خریدار
    private static final String TABLE_BUYER_DELIVERY = "buyer_delivery";
    private static final String KEY_BUYER_ID = "id";
    private static final String KEY_BUYER_SHIPMENT_ID = "shipment_id";
    private static final String KEY_BUYER_NAME = "buyer_name";
    private static final String KEY_BUYER_PHONE = "buyer_phone";
    private static final String KEY_HAJRA_NUMBER = "hajra_number";
    private static final String KEY_DELIVERY_DATE = "delivery_date";
    private static final String KEY_RECEIVED_AMOUNT = "received_amount";
    private static final String KEY_BUYER_NOTES = "notes";

    // ========== جدول جدید: پیوست‌ها ==========
    private static final String TABLE_ATTACHMENTS = "attachments";
    private static final String KEY_ATTACH_ID = "id";
    private static final String KEY_ATTACH_INVOICE_ID = "invoice_id";
    private static final String KEY_ATTACH_FILE_NAME = "file_name";
    private static final String KEY_ATTACH_FILE_PATH = "file_path";
    private static final String KEY_ATTACH_FILE_TYPE = "file_type";
    // =========================================

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + "(" +
                KEY_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_USERNAME + " TEXT UNIQUE NOT NULL," +
                KEY_PASSWORD + " TEXT NOT NULL," +
                KEY_ROLE + " TEXT NOT NULL" + ")");

        db.execSQL("CREATE TABLE " + TABLE_INVOICES + "(" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_INVOICE_NUMBER + " TEXT UNIQUE," +
                KEY_CUSTOMER_NAME + " TEXT," +
                KEY_CUSTOMER_PHONE + " TEXT," +
                KEY_DATE + " TEXT," +
                KEY_TOTAL_AMOUNT + " INTEGER," +
                KEY_LOAD_STATUS + " INTEGER DEFAULT 0" + ")");

        db.execSQL("CREATE TABLE " + TABLE_ITEMS + "(" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_INVOICE_ID + " INTEGER," +
                KEY_PRODUCT_NAME + " TEXT," +
                KEY_WEIGHT + " REAL," +
                KEY_UNIT_PRICE + " INTEGER," +
                KEY_NAME + " TEXT," +
                KEY_MOBILE + " TEXT," +
                KEY_ADDRESS + " TEXT," +
                KEY_NOTES + " TEXT," +
                "FOREIGN KEY(" + KEY_INVOICE_ID + ") REFERENCES " + TABLE_INVOICES + "(" + KEY_ID + ") ON DELETE CASCADE" + ")");

        db.execSQL("CREATE TABLE " + TABLE_SHIPMENTS + "(" +
                KEY_SHIPMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_SHIPMENT_INVOICE_ID + " INTEGER," +
                KEY_SHIPMENT_INVOICE_NUMBER + " TEXT," +
                KEY_SHIPMENT_TRUCK_NAME + " TEXT," +
                KEY_SHIPMENT_PLATE_NUMBER + " TEXT," +
                KEY_SHIPMENT_DRIVER_NAME + " TEXT," +
                KEY_SHIPMENT_DRIVER_PHONE + " TEXT," +
                KEY_SHIPMENT_LOAD_DATE + " TEXT," +
                KEY_SHIPMENT_CLEARANCE_STATUS + " INTEGER DEFAULT 0," +
                KEY_TRANSPORT_COST + " INTEGER DEFAULT 0" + ")");

        db.execSQL("CREATE TABLE " + TABLE_CLEARANCES + "(" +
                KEY_CLEARANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_CLEARANCE_SHIPMENT_ID + " INTEGER," +
                KEY_CLEARANCE_NAME + " TEXT," +
                KEY_CLEARANCE_PHONE + " TEXT," +
                KEY_BORDER_NAME + " TEXT," +
                KEY_CLEARANCE_DATE + " TEXT," +
                KEY_CLEARANCE_NOTES + " TEXT," +
                KEY_CLEARANCE_COST + " INTEGER DEFAULT 0," +
                "FOREIGN KEY(" + KEY_CLEARANCE_SHIPMENT_ID + ") REFERENCES " + TABLE_SHIPMENTS + "(" + KEY_SHIPMENT_ID + ") ON DELETE CASCADE" + ")");

        db.execSQL("CREATE TABLE " + TABLE_IRAQI_HANDOVERS + "(" +
                KEY_IRAQI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_IRAQI_SHIPMENT_ID + " INTEGER," +
                KEY_CLEARANCE_NAME + " TEXT," +
                KEY_CLEARANCE_PHONE + " TEXT," +
                KEY_BORDER_NAME + " TEXT," +
                KEY_CLEARANCE_DATE + " TEXT," +
                KEY_CLEARANCE_NOTES + " TEXT," +
                KEY_CLEARANCE_COST + " INTEGER DEFAULT 0," +
                "FOREIGN KEY(" + KEY_IRAQI_SHIPMENT_ID + ") REFERENCES " + TABLE_SHIPMENTS + "(" + KEY_SHIPMENT_ID + ") ON DELETE CASCADE" + ")");

        // جدول تحویل به خریدار
        db.execSQL("CREATE TABLE " + TABLE_BUYER_DELIVERY + "(" +
                KEY_BUYER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_BUYER_SHIPMENT_ID + " INTEGER," +
                KEY_BUYER_NAME + " TEXT," +
                KEY_BUYER_PHONE + " TEXT," +
                KEY_HAJRA_NUMBER + " TEXT," +
                KEY_DELIVERY_DATE + " TEXT," +
                KEY_RECEIVED_AMOUNT + " INTEGER DEFAULT 0," +
                KEY_BUYER_NOTES + " TEXT," +
                "FOREIGN KEY(" + KEY_BUYER_SHIPMENT_ID + ") REFERENCES " + TABLE_SHIPMENTS + "(" + KEY_SHIPMENT_ID + ") ON DELETE CASCADE" +
                ")");

        // ========== جدول جدید: پیوست‌ها ==========
        db.execSQL("CREATE TABLE " + TABLE_ATTACHMENTS + "(" +
                KEY_ATTACH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_ATTACH_INVOICE_ID + " INTEGER," +
                KEY_ATTACH_FILE_NAME + " TEXT," +
                KEY_ATTACH_FILE_PATH + " TEXT," +
                KEY_ATTACH_FILE_TYPE + " TEXT," +
                "FOREIGN KEY(" + KEY_ATTACH_INVOICE_ID + ") REFERENCES " + TABLE_INVOICES + "(" + KEY_ID + ") ON DELETE CASCADE" + ")");
        // =========================================

        // کاربر پیش‌فرض admin
        String defaultPassword = BCrypt.hashpw("123", BCrypt.gensalt());
        ContentValues adminValues = new ContentValues();
        adminValues.put(KEY_USERNAME, "admin");
        adminValues.put(KEY_PASSWORD, defaultPassword);
        adminValues.put(KEY_ROLE, "admin");
        db.insert(TABLE_USERS, null, adminValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 17) {
            db.execSQL("ALTER TABLE " + TABLE_ITEMS + " ADD COLUMN " + KEY_NOTES + " TEXT");
        }
        if (oldVersion < 18) {
            // ایجاد جدول پیوست‌ها در هنگام آپدیت
            db.execSQL("CREATE TABLE " + TABLE_ATTACHMENTS + "(" +
                    KEY_ATTACH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    KEY_ATTACH_INVOICE_ID + " INTEGER," +
                    KEY_ATTACH_FILE_NAME + " TEXT," +
                    KEY_ATTACH_FILE_PATH + " TEXT," +
                    KEY_ATTACH_FILE_TYPE + " TEXT," +
                    "FOREIGN KEY(" + KEY_ATTACH_INVOICE_ID + ") REFERENCES " + TABLE_INVOICES + "(" + KEY_ID + ") ON DELETE CASCADE" + ")");
        }
    }

    // ==================== توابع فاکتور ====================
    public boolean hasShipment(long invoiceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHIPMENTS, new String[]{KEY_SHIPMENT_ID}, KEY_SHIPMENT_INVOICE_ID + "=?", new String[]{String.valueOf(invoiceId)}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String generateInvoiceNumber() {
        PersianCalendar persianCalendar = new PersianCalendar();
        int persianYear = persianCalendar.get(PersianCalendar.YEAR);

        String prefix = String.valueOf(persianYear);

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT invoice_number FROM invoices " +
                "WHERE invoice_number LIKE ? " +
                "ORDER BY id DESC LIMIT 1";

        Cursor cursor = db.rawQuery(query, new String[]{prefix + "%"});

        int nextSeq = 1;

        if (cursor.moveToFirst()) {
            String lastNumber = cursor.getString(0);
            String lastSeqStr = lastNumber.substring(prefix.length());
            nextSeq = Integer.parseInt(lastSeqStr) + 1;
        }

        cursor.close();

        return prefix + String.format("%04d", nextSeq);
    }

    public long addInvoice(Invoice invoice) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CUSTOMER_NAME, invoice.getCustomerName());
            values.put(KEY_CUSTOMER_PHONE, invoice.getCustomerPhone());
            values.put(KEY_DATE, invoice.getDate());
            values.put(KEY_TOTAL_AMOUNT, invoice.getGrandTotal());
            values.put(KEY_LOAD_STATUS, 0);
            String invoiceNumber = generateInvoiceNumber();
            invoice.setInvoiceNumber(invoiceNumber);
            values.put(KEY_INVOICE_NUMBER, invoiceNumber);
            long invoiceId = db.insert(TABLE_INVOICES, null, values);
            invoice.setId(invoiceId);
            for (InvoiceItem item : invoice.getItems()) {
                ContentValues itemValues = new ContentValues();
                itemValues.put(KEY_INVOICE_ID, invoiceId);
                itemValues.put(KEY_PRODUCT_NAME, item.getProductName());
                itemValues.put(KEY_WEIGHT, item.getWeight());
                itemValues.put(KEY_UNIT_PRICE, item.getUnitPrice());
                itemValues.put(KEY_NAME, item.getName());
                itemValues.put(KEY_MOBILE, item.getMobile());
                itemValues.put(KEY_ADDRESS, item.getAddress());
                itemValues.put(KEY_NOTES, item.getNotes());
                db.insert(TABLE_ITEMS, null, itemValues);
            }
            db.setTransactionSuccessful();
            return invoiceId;
        } finally {
            db.endTransaction();
        }
    }

    public List<Invoice> getAllInvoices() {
        List<Invoice> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_INVOICES, null, null, null, null, null, KEY_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Invoice invoice = new Invoice();
                invoice.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
                invoice.setInvoiceNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INVOICE_NUMBER)));
                invoice.setCustomerName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CUSTOMER_NAME)));
                invoice.setCustomerPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CUSTOMER_PHONE)));
                invoice.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
                invoice.setGrandTotal(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TOTAL_AMOUNT)));
                invoice.setItems(getItemsByInvoiceId(invoice.getId()));
                list.add(invoice);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private List<InvoiceItem> getItemsByInvoiceId(long invoiceId) {
        List<InvoiceItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEMS, null, KEY_INVOICE_ID + "=?", new String[]{String.valueOf(invoiceId)}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                InvoiceItem item = new InvoiceItem(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRODUCT_NAME)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_WEIGHT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_UNIT_PRICE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_MOBILE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTES))
                );
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    public void updateInvoice(Invoice invoice) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CUSTOMER_NAME, invoice.getCustomerName());
            values.put(KEY_CUSTOMER_PHONE, invoice.getCustomerPhone());
            values.put(KEY_DATE, invoice.getDate());
            values.put(KEY_TOTAL_AMOUNT, invoice.getGrandTotal());
            db.update(TABLE_INVOICES, values, KEY_ID + "=?", new String[]{String.valueOf(invoice.getId())});
            db.delete(TABLE_ITEMS, KEY_INVOICE_ID + "=?", new String[]{String.valueOf(invoice.getId())});
            for (InvoiceItem item : invoice.getItems()) {
                ContentValues itemValues = new ContentValues();
                itemValues.put(KEY_INVOICE_ID, invoice.getId());
                itemValues.put(KEY_PRODUCT_NAME, item.getProductName());
                itemValues.put(KEY_WEIGHT, item.getWeight());
                itemValues.put(KEY_UNIT_PRICE, item.getUnitPrice());
                itemValues.put(KEY_NAME, item.getName());
                itemValues.put(KEY_MOBILE, item.getMobile());
                itemValues.put(KEY_ADDRESS, item.getAddress());
                itemValues.put(KEY_NOTES, item.getNotes());
                db.insert(TABLE_ITEMS, null, itemValues);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteInvoice(long invoiceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITEMS, KEY_INVOICE_ID + "=?", new String[]{String.valueOf(invoiceId)});
        db.delete(TABLE_INVOICES, KEY_ID + "=?", new String[]{String.valueOf(invoiceId)});
    }

    public Invoice getInvoiceById(long invoiceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_INVOICES, null, KEY_ID + "=?", new String[]{String.valueOf(invoiceId)}, null, null, null);
        Invoice invoice = null;
        if (cursor.moveToFirst()) {
            invoice = new Invoice();
            invoice.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
            invoice.setInvoiceNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INVOICE_NUMBER)));
            invoice.setCustomerName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CUSTOMER_NAME)));
            invoice.setCustomerPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CUSTOMER_PHONE)));
            invoice.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
            invoice.setGrandTotal(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TOTAL_AMOUNT)));
            invoice.setItems(getItemsByInvoiceId(invoiceId));
        }
        if (cursor != null) cursor.close();
        return invoice;
    }

    public List<Invoice> getReadyInvoices() {
        List<Invoice> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_INVOICES, null, KEY_LOAD_STATUS + " = 0 OR " + KEY_LOAD_STATUS + " IS NULL", null, null, null, KEY_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Invoice invoice = new Invoice();
                invoice.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)));
                invoice.setInvoiceNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INVOICE_NUMBER)));
                invoice.setCustomerName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CUSTOMER_NAME)));
                invoice.setCustomerPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CUSTOMER_PHONE)));
                invoice.setDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)));
                invoice.setGrandTotal(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TOTAL_AMOUNT)));
                invoice.setItems(getItemsByInvoiceId(invoice.getId()));
                list.add(invoice);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // ==================== توابع بارگیری ====================
    public long addShipment(Shipment shipment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SHIPMENT_INVOICE_ID, shipment.getInvoiceId());
        values.put(KEY_SHIPMENT_INVOICE_NUMBER, shipment.getInvoiceNumber());
        values.put(KEY_SHIPMENT_TRUCK_NAME, shipment.getTruckName());
        values.put(KEY_SHIPMENT_PLATE_NUMBER, shipment.getPlateNumber());
        values.put(KEY_SHIPMENT_DRIVER_NAME, shipment.getDriverName());
        values.put(KEY_SHIPMENT_DRIVER_PHONE, shipment.getDriverPhone());
        values.put(KEY_SHIPMENT_LOAD_DATE, shipment.getLoadDate());
        values.put(KEY_TRANSPORT_COST, shipment.getTransportCost());
        long id = db.insert(TABLE_SHIPMENTS, null, values);
        db.close();
        return id;
    }

    public boolean updateShipment(Shipment shipment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SHIPMENT_TRUCK_NAME, shipment.getTruckName());
        values.put(KEY_SHIPMENT_PLATE_NUMBER, shipment.getPlateNumber());
        values.put(KEY_SHIPMENT_DRIVER_NAME, shipment.getDriverName());
        values.put(KEY_SHIPMENT_DRIVER_PHONE, shipment.getDriverPhone());
        values.put(KEY_SHIPMENT_LOAD_DATE, shipment.getLoadDate());
        values.put(KEY_TRANSPORT_COST, shipment.getTransportCost());
        int rows = db.update(TABLE_SHIPMENTS, values, KEY_SHIPMENT_ID + "=?", new String[]{String.valueOf(shipment.getId())});
        db.close();
        return rows > 0;
    }

    public List<Shipment> getAllShipments() {
        List<Shipment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHIPMENTS, null, null, null, null, null, KEY_SHIPMENT_LOAD_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Shipment shipment = new Shipment();
                shipment.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_ID)));
                shipment.setInvoiceId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_INVOICE_ID)));
                shipment.setInvoiceNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_INVOICE_NUMBER)));
                shipment.setTruckName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_TRUCK_NAME)));
                shipment.setPlateNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_PLATE_NUMBER)));
                shipment.setDriverName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_DRIVER_NAME)));
                shipment.setDriverPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_DRIVER_PHONE)));
                shipment.setLoadDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_LOAD_DATE)));
                shipment.setTransportCost(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRANSPORT_COST)));
                list.add(shipment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Shipment> getInTransitShipments() {
        List<Shipment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHIPMENTS, null, KEY_SHIPMENT_CLEARANCE_STATUS + " = 0", null, null, null, KEY_SHIPMENT_LOAD_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Shipment shipment = new Shipment();
                shipment.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_ID)));
                shipment.setInvoiceId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_INVOICE_ID)));
                shipment.setInvoiceNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_INVOICE_NUMBER)));
                shipment.setTruckName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_TRUCK_NAME)));
                shipment.setPlateNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_PLATE_NUMBER)));
                shipment.setDriverName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_DRIVER_NAME)));
                shipment.setDriverPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_DRIVER_PHONE)));
                shipment.setLoadDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_LOAD_DATE)));
                shipment.setTransportCost(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRANSPORT_COST)));
                list.add(shipment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Shipment> getClearedShipments() {
        List<Shipment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHIPMENTS, null, KEY_SHIPMENT_CLEARANCE_STATUS + " = 1", null, null, null, KEY_SHIPMENT_LOAD_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                Shipment shipment = new Shipment();
                shipment.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_ID)));
                shipment.setInvoiceId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_INVOICE_ID)));
                shipment.setInvoiceNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_INVOICE_NUMBER)));
                shipment.setTruckName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_TRUCK_NAME)));
                shipment.setPlateNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_PLATE_NUMBER)));
                shipment.setDriverName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_DRIVER_NAME)));
                shipment.setDriverPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_DRIVER_PHONE)));
                shipment.setLoadDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SHIPMENT_LOAD_DATE)));
                shipment.setTransportCost(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TRANSPORT_COST)));
                list.add(shipment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void updateInvoiceLoadStatus(long invoiceId, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LOAD_STATUS, status);
        db.update(TABLE_INVOICES, values, KEY_ID + "=?", new String[]{String.valueOf(invoiceId)});
        db.close();
    }

    public void updateShipmentClearanceStatus(long shipmentId, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SHIPMENT_CLEARANCE_STATUS, status);
        db.update(TABLE_SHIPMENTS, values, KEY_SHIPMENT_ID + "=?", new String[]{String.valueOf(shipmentId)});
        db.close();
    }

    // ==================== توابع ترخیص ایرانی ====================
    public long addClearance(Clearance clearance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CLEARANCE_SHIPMENT_ID, clearance.getShipmentId());
        values.put(KEY_CLEARANCE_NAME, clearance.getClearanceName());
        values.put(KEY_CLEARANCE_PHONE, clearance.getClearancePhone());
        values.put(KEY_BORDER_NAME, clearance.getBorderName());
        values.put(KEY_CLEARANCE_DATE, clearance.getClearanceDate());
        values.put(KEY_CLEARANCE_NOTES, clearance.getNotes());
        values.put(KEY_CLEARANCE_COST, clearance.getClearanceCost());
        long id = db.insert(TABLE_CLEARANCES, null, values);
        updateShipmentClearanceStatus(clearance.getShipmentId(), 1);
        db.close();
        return id;
    }

    public boolean updateClearance(Clearance clearance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CLEARANCE_NAME, clearance.getClearanceName());
        values.put(KEY_CLEARANCE_PHONE, clearance.getClearancePhone());
        values.put(KEY_BORDER_NAME, clearance.getBorderName());
        values.put(KEY_CLEARANCE_NOTES, clearance.getNotes());
        values.put(KEY_CLEARANCE_COST, clearance.getClearanceCost());
        values.put(KEY_CLEARANCE_DATE, clearance.getClearanceDate());
        int rows = db.update(TABLE_CLEARANCES, values, KEY_CLEARANCE_ID + "=?", new String[]{String.valueOf(clearance.getId())});
        db.close();
        return rows > 0;
    }

    public Clearance getClearanceByShipmentId(long shipmentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CLEARANCES, null, KEY_CLEARANCE_SHIPMENT_ID + "=?", new String[]{String.valueOf(shipmentId)}, null, null, null);
        Clearance clearance = null;
        if (cursor.moveToFirst()) {
            clearance = new Clearance();
            clearance.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_ID)));
            clearance.setShipmentId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_SHIPMENT_ID)));
            clearance.setClearanceName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_NAME)));
            clearance.setClearancePhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_PHONE)));
            clearance.setBorderName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BORDER_NAME)));
            clearance.setClearanceDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_DATE)));
            clearance.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_NOTES)));
            clearance.setClearanceCost(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_COST)));
        }
        cursor.close();
        db.close();
        return clearance;
    }

    // ==================== توابع تحویل به ترخیص‌کار عراقی ====================
    public long addIraqiHandover(IraqiHandover handover) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IRAQI_SHIPMENT_ID, handover.getShipmentId());
        values.put(KEY_CLEARANCE_NAME, handover.getClearanceName());
        values.put(KEY_CLEARANCE_PHONE, handover.getClearancePhone());
        values.put(KEY_BORDER_NAME, handover.getBorderName());
        values.put(KEY_CLEARANCE_DATE, handover.getClearanceDate());
        values.put(KEY_CLEARANCE_NOTES, handover.getNotes());
        values.put(KEY_CLEARANCE_COST, handover.getClearanceCost());
        long id = db.insert(TABLE_IRAQI_HANDOVERS, null, values);
        db.close();
        return id;
    }

    public boolean updateIraqiHandover(IraqiHandover handover) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CLEARANCE_NAME, handover.getClearanceName());
        values.put(KEY_CLEARANCE_PHONE, handover.getClearancePhone());
        values.put(KEY_BORDER_NAME, handover.getBorderName());
        values.put(KEY_CLEARANCE_NOTES, handover.getNotes());
        values.put(KEY_CLEARANCE_COST, handover.getClearanceCost());
        values.put(KEY_CLEARANCE_DATE, handover.getClearanceDate());
        int rows = db.update(TABLE_IRAQI_HANDOVERS, values, KEY_IRAQI_ID + "=?", new String[]{String.valueOf(handover.getId())});
        db.close();
        return rows > 0;
    }

    public IraqiHandover getIraqiHandoverByShipmentId(long shipmentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_IRAQI_HANDOVERS, null, KEY_IRAQI_SHIPMENT_ID + "=?", new String[]{String.valueOf(shipmentId)}, null, null, null);
        IraqiHandover handover = null;
        if (cursor.moveToFirst()) {
            handover = new IraqiHandover();
            handover.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_IRAQI_ID)));
            handover.setShipmentId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_IRAQI_SHIPMENT_ID)));
            handover.setClearanceName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_NAME)));
            handover.setClearancePhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_PHONE)));
            handover.setBorderName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BORDER_NAME)));
            handover.setClearanceDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_DATE)));
            handover.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_NOTES)));
            handover.setClearanceCost(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CLEARANCE_COST)));
        }
        cursor.close();
        db.close();
        return handover;
    }

    public boolean hasClearance(long shipmentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CLEARANCES, new String[]{KEY_CLEARANCE_ID}, KEY_CLEARANCE_SHIPMENT_ID + "=?", new String[]{String.valueOf(shipmentId)}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // ==================== توابع کاربر ====================
    public boolean registerUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, KEY_USERNAME + "=?", new String[]{username}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, hashedPassword);
        values.put(KEY_ROLE, role);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public String login(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_PASSWORD, KEY_ROLE},
                KEY_USERNAME + "=?", new String[]{username}, null, null, null);
        String role = null;
        if (cursor != null && cursor.moveToFirst()) {
            int passwordIndex = cursor.getColumnIndex(KEY_PASSWORD);
            int roleIndex = cursor.getColumnIndex(KEY_ROLE);
            if (passwordIndex != -1 && roleIndex != -1) {
                String storedHash = cursor.getString(passwordIndex);
                role = cursor.getString(roleIndex);
                cursor.close();
                if (BCrypt.checkpw(password, storedHash)) {
                    return role;
                }
            }
            cursor.close();
        }
        return null;
    }

    // ==================== توابع تحویل به خریدار ====================
    public long addBuyerDelivery(BuyerDelivery delivery) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BUYER_SHIPMENT_ID, delivery.getShipmentId());
        values.put(KEY_BUYER_NAME, delivery.getBuyerName());
        values.put(KEY_BUYER_PHONE, delivery.getBuyerPhone());
        values.put(KEY_HAJRA_NUMBER, delivery.getHajraNumber());
        values.put(KEY_DELIVERY_DATE, delivery.getDeliveryDate());
        values.put(KEY_RECEIVED_AMOUNT, delivery.getReceivedAmount());
        values.put(KEY_BUYER_NOTES, delivery.getNotes());

        long id = db.insert(TABLE_BUYER_DELIVERY, null, values);
        db.close();
        return id;
    }

    public boolean updateBuyerDelivery(BuyerDelivery delivery) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BUYER_NAME, delivery.getBuyerName());
        values.put(KEY_BUYER_PHONE, delivery.getBuyerPhone());
        values.put(KEY_HAJRA_NUMBER, delivery.getHajraNumber());
        values.put(KEY_DELIVERY_DATE, delivery.getDeliveryDate());
        values.put(KEY_RECEIVED_AMOUNT, delivery.getReceivedAmount());
        values.put(KEY_BUYER_NOTES, delivery.getNotes());

        int rows = db.update(TABLE_BUYER_DELIVERY, values, KEY_BUYER_ID + "=?", new String[]{String.valueOf(delivery.getId())});
        db.close();
        return rows > 0;
    }

    public BuyerDelivery getBuyerDeliveryByShipmentId(long shipmentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUYER_DELIVERY, null, KEY_BUYER_SHIPMENT_ID + "=?", new String[]{String.valueOf(shipmentId)}, null, null, null);
        BuyerDelivery delivery = null;
        if (cursor.moveToFirst()) {
            delivery = new BuyerDelivery();
            delivery.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BUYER_ID)));
            delivery.setShipmentId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BUYER_SHIPMENT_ID)));
            delivery.setBuyerName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUYER_NAME)));
            delivery.setBuyerPhone(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUYER_PHONE)));
            delivery.setHajraNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_HAJRA_NUMBER)));
            delivery.setDeliveryDate(cursor.getString(cursor.getColumnIndexOrThrow(KEY_DELIVERY_DATE)));
            delivery.setReceivedAmount(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_RECEIVED_AMOUNT)));
            delivery.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BUYER_NOTES)));
        }
        cursor.close();
        db.close();
        return delivery;
    }

    public long getInvoiceTotalById(long invoiceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        long total = 0;
        String query = "SELECT " + KEY_TOTAL_AMOUNT + " FROM " + TABLE_INVOICES + " WHERE " + KEY_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(invoiceId)});
        if (cursor.moveToFirst()) {
            total = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    // متد جدید: اضافه کردن آیتم به صورت جداگانه
    public long addInvoiceItem(InvoiceItem item, long invoiceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_INVOICE_ID, invoiceId);
        values.put(KEY_PRODUCT_NAME, item.getProductName());
        values.put(KEY_WEIGHT, item.getWeight());
        values.put(KEY_UNIT_PRICE, item.getUnitPrice());
        values.put(KEY_NAME, item.getName());
        values.put(KEY_MOBILE, item.getMobile());
        values.put(KEY_ADDRESS, item.getAddress());
        values.put(KEY_NOTES, item.getNotes());
        long rowId = db.insert(TABLE_ITEMS, null, values);
        db.close();
        return rowId;
    }
    // ذخیره فایل در حافظه داخلی با UUID
    public String saveFileToInternalStorage(Context context, Uri uri, String customName, long invoiceId) {
        try {
            File dir = new File(context.getFilesDir(), "invoice_attachments");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // استخراج پسوند فایل
            String extension = "";
            int dotIndex = customName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = customName.substring(dotIndex);
            }

            // ایجاد نام یکتا با UUID
            String uniqueId = UUID.randomUUID().toString();
            String uniqueFileName = uniqueId + extension;

            File destinationFile = new File(dir, uniqueFileName);

            // کپی فایل
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ثبت اطلاعات فایل در دیتابیس
    public long addAttachment(long invoiceId, String fileName, String filePath, String fileType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ATTACH_INVOICE_ID, invoiceId);
        values.put(KEY_ATTACH_FILE_NAME, fileName);
        values.put(KEY_ATTACH_FILE_PATH, filePath);
        values.put(KEY_ATTACH_FILE_TYPE, fileType);
        return db.insert(TABLE_ATTACHMENTS, null, values);
    }

    // دریافت لیست فایل‌های یک فاکتور
    public Cursor getAttachmentsByInvoice(long invoiceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ATTACHMENTS, null, KEY_ATTACH_INVOICE_ID + "=?",
                new String[]{String.valueOf(invoiceId)}, null, null, KEY_ATTACH_ID + " DESC");
    }

    // دریافت مسیر آخرین فایل پیوست شده
    public String getLatestAttachmentPath(long invoiceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTACHMENTS, new String[]{KEY_ATTACH_FILE_PATH},
                KEY_ATTACH_INVOICE_ID + "=?", new String[]{String.valueOf(invoiceId)},
                null, null, KEY_ATTACH_ID + " DESC", "1");

        String path = null;
        if (cursor.moveToFirst()) {
            path = cursor.getString(0);
        }
        cursor.close();
        return path;
    }

    // ========== متد جدید: به‌روزرسانی نام فایل در دیتابیس ==========
//    public boolean updateAttachmentFileName(long attachmentId, String newFileName, String newFilePath) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(KEY_ATTACH_FILE_NAME, newFileName);
//        values.put(KEY_ATTACH_FILE_PATH, newFilePath);
//
//        int rows = db.update(TABLE_ATTACHMENTS, values, KEY_ATTACH_ID + "=?",
//                new String[]{String.valueOf(attachmentId)});
//        return rows > 0;
//    }
// به‌روزرسانی نام فایل در دیتابیس (فقط نام دلخواه)
    public boolean updateAttachmentFileName(long attachmentId, String newFileName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ATTACH_FILE_NAME, newFileName); // فقط نام دلخواه تغییر می‌کنه

        int rows = db.update(TABLE_ATTACHMENTS, values, KEY_ATTACH_ID + "=?",
                new String[]{String.valueOf(attachmentId)});
        return rows > 0;
    }
    // ========== متد جدید: حذف پیوست ==========
    public boolean deleteAttachment(long attachmentId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // ابتدا مسیر فایل را بگیریم
        String filePath = null;
        Cursor cursor = db.query(TABLE_ATTACHMENTS, new String[]{KEY_ATTACH_FILE_PATH},
                KEY_ATTACH_ID + "=?", new String[]{String.valueOf(attachmentId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(0);
        }
        cursor.close();

        // حذف از دیتابیس
        int rowsDeleted = db.delete(TABLE_ATTACHMENTS, KEY_ATTACH_ID + "=?",
                new String[]{String.valueOf(attachmentId)});

        // اگر فایل فیزیکی وجود داشت، آن را هم حذف کن
        if (rowsDeleted > 0 && filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }

        return rowsDeleted > 0;
    }

    // متد جدید: حذف همه پیوست‌های یک فاکتور
    public boolean deleteAllAttachments(long invoiceId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // دریافت لیست فایل‌ها برای حذف فیزیکی
        Cursor cursor = db.query(TABLE_ATTACHMENTS, new String[]{KEY_ATTACH_FILE_PATH},
                KEY_ATTACH_INVOICE_ID + "=?", new String[]{String.valueOf(invoiceId)},
                null, null, null);

        while (cursor.moveToNext()) {
            String filePath = cursor.getString(0);
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        cursor.close();

        // حذف از دیتابیس
        int rowsDeleted = db.delete(TABLE_ATTACHMENTS, KEY_ATTACH_INVOICE_ID + "=?",
                new String[]{String.valueOf(invoiceId)});

        return rowsDeleted > 0;
    }
    // ==========================================
}