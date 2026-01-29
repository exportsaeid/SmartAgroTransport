package com.example.SmartAgroTransport.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.SmartAgroTransport.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;

public class MapActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 1001;

    private MapView mapView;
    private Marker locationMarker;
    private PulseOverlay pulseOverlay;
    private  ImageView backButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
         backButton = findViewById(R.id.backButton);
//        ImageView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.e("MapActivity", "backButton is null!");
        }



        Configuration.getInstance().setUserAgentValue(getPackageName());


        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

        if (hasPermission()) {
            setupLocation();
        } else {
            requestPermission();
        }
    }

    private void setupLocation() {
        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        provider.setLocationUpdateMinDistance(3);
        provider.setLocationUpdateMinTime(1000);

        // Consumer برای دریافت موقعیت زنده
        IMyLocationConsumer consumer = new IMyLocationConsumer() {
            @Override
            public void onLocationChanged(@NonNull Location location, @NonNull org.osmdroid.views.overlay.mylocation.IMyLocationProvider source) {
                if (location == null) return;
                GeoPoint p = new GeoPoint(location.getLatitude(), location.getLongitude());

                runOnUiThread(() -> {
                    if (locationMarker == null) {
                        initMarker(p);
                        initPulse(p);
                        mapView.getController().setCenter(p);
                    } else {
                        updateLocation(p);
                    }
                });
            }
        };

        // شروع دریافت موقعیت
        provider.startLocationProvider(consumer);
    }

    private void initMarker(GeoPoint p) {
        if (locationMarker == null) {
            locationMarker = new Marker(mapView);
            locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            // تبدیل Vector Drawable به Bitmap با اندازه بزرگ (96x96)

            Bitmap bitmap = vectorToBitmap(org.osmdroid.wms.R.drawable.marker_default, 81, 144);
            locationMarker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), bitmap));

            mapView.getOverlays().add(locationMarker);
        }
        locationMarker.setPosition(p);
        mapView.invalidate();
    }

    private Bitmap vectorToBitmap(int drawableId, int width, int height) {
        android.graphics.drawable.Drawable vectorDrawable = ContextCompat.getDrawable(this, drawableId);
        if (vectorDrawable == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private void initPulse(GeoPoint p) {
        pulseOverlay = new PulseOverlay(p);
        mapView.getOverlays().add(pulseOverlay);
        pulseOverlay.start();
    }

    private void updateLocation(GeoPoint newPoint) {
        if (locationMarker != null) {
            locationMarker.setPosition(newPoint);
        }
        if (pulseOverlay != null) {
            pulseOverlay.updateCenter(newPoint);
        }
        mapView.getController().setCenter(newPoint);
        mapView.invalidate();
    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] results) {
        if (requestCode == REQ_LOCATION && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    // کلاس داخلی PulseOverlay برای موج سبز اطراف Marker
    private class PulseOverlay extends Overlay {
        private GeoPoint center;
        private Paint paint;
        private float radius = 0;

        PulseOverlay(GeoPoint p) {
            center = p;
            paint = new Paint();
            paint.setColor(0x332E7D32); // سبز شفاف
            paint.setAntiAlias(true);
        }

        void updateCenter(GeoPoint p) {
            center = p;
        }

        void start() {
            android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(2000);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.addUpdateListener(a -> {
                float v = (float) a.getAnimatedValue();
                radius = 70 * v;
                paint.setAlpha((int) (120 * (1f - v)));
                mapView.invalidate();
            });
            animator.start();
        }

        @Override
        public void draw(Canvas c, MapView mapView, boolean shadow) {
            if (center == null) return;
            android.graphics.Point pt = new android.graphics.Point();
            mapView.getProjection().toPixels(center, pt);
            c.drawCircle(pt.x, pt.y, radius, paint);
        }
    }
}
