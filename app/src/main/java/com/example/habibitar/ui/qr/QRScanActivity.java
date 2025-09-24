package com.example.habibitar.ui.qr;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habibitar.R;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QRScanActivity extends AppCompatActivity {
    public static final String EXTRA_QR_PAYLOAD = "qr_payload";

    private DecoratedBarcodeView barcodeView;
    private boolean handled = false;

    private final ActivityResultLauncher<String> cameraPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startScanning();
                else finish(); // no permission, just close
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        barcodeView = findViewById(R.id.barcodeScanner);
        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        // Ask for camera permission and then scan
        cameraPerm.launch(Manifest.permission.CAMERA);
    }

    private void startScanning() {
        // Continuous mode -> we stop on the first result
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (handled || result.getText() == null) return;
                handled = true;
                barcodeView.pause();

                Intent data = new Intent();
                data.putExtra(EXTRA_QR_PAYLOAD, result.getText());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        barcodeView.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!handled && barcodeView != null) barcodeView.resume();
    }

    @Override
    protected void onPause() {
        if (barcodeView != null) barcodeView.pause();
        super.onPause();
    }
}
