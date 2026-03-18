package com.omega.protocol.ui.sync;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.*;
import com.omega.protocol.R;
import com.omega.protocol.viewmodel.SyncViewModel;
import java.util.Locale;

public class QrSyncFragment extends Fragment {

    private SyncViewModel vm;
    private ImageView     imgQr;
    private TextView      tvQrStatus;
    private View          panelExport, panelScan;
    private DecoratedBarcodeView barcodeView;
    private boolean scanning = false;

    private ActivityResultLauncher<String> cameraPermLauncher;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_qr_sync, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        imgQr       = v.findViewById(R.id.imgQrCode);
        tvQrStatus  = v.findViewById(R.id.tvQrStatus);
        panelExport = v.findViewById(R.id.panelQrExport);
        panelScan   = v.findViewById(R.id.panelQrScan);
        barcodeView = v.findViewById(R.id.barcodeView);

        vm = new ViewModelProvider(requireActivity()).get(SyncViewModel.class);
        vm.getStatus().observe(getViewLifecycleOwner(), tvQrStatus::setText);

        cameraPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> { if (granted) startScanning(); else tvQrStatus.setText("Camera permission denied"); });

        v.findViewById(R.id.btnShowQr).setOnClickListener(x -> generateQr());
        v.findViewById(R.id.btnScanQr).setOnClickListener(x -> requestCameraAndScan());
        v.findViewById(R.id.btnCloseQr).setOnClickListener(x -> {
            panelExport.setVisibility(View.GONE);
            imgQr.setImageBitmap(null);
        });
        v.findViewById(R.id.btnStopScan).setOnClickListener(x -> stopScanning());
    }

    // ── QR Generation ────────────────────────────────────
    private void generateQr() {
        tvQrStatus.setText("Building QR…");
        vm.export(json -> {
            try {
                // Compress: keep only essential fields to fit in QR
                // Full JSON may be too large — use a compact subset
                String payload = compactPayload(json);
                if (payload.length() > 2900) {
                    tvQrStatus.setText("Data too large for QR (" + payload.length() + " chars). Use file export instead.");
                    return;
                }
                Bitmap bm = encodeQr(payload, 600);
                panelExport.setVisibility(View.VISIBLE);
                imgQr.setImageBitmap(bm);
                tvQrStatus.setText("Show this QR to the other device (" + payload.length() + " chars)");
            } catch (Exception e) {
                tvQrStatus.setText("QR error: " + e.getMessage());
            }
        });
    }

    private String compactPayload(String fullJson) {
        // Return full JSON — for very large datasets the status message handles it
        return fullJson;
    }

    private Bitmap encodeQr(String content, int size) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        com.google.zxing.common.BitMatrix matrix = writer.encode(
            content, BarcodeFormat.QR_CODE, size, size);
        Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                bm.setPixel(x, y, matrix.get(x, y) ? 0xFF0d0f1a : 0xFFe8eaff);
        return bm;
    }

    // ── QR Scanning ───────────────────────────────────────
    private void requestCameraAndScan() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            cameraPermLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void startScanning() {
        panelScan.setVisibility(View.VISIBLE);
        panelExport.setVisibility(View.GONE);
        scanning = true;
        barcodeView.decodeContinuous(result -> {
            if (!scanning) return;
            stopScanning();
            String text = result.getText();
            if (text != null && !text.isEmpty()) {
                tvQrStatus.setText("QR received — importing…");
                vm.importJson(text);
            } else {
                tvQrStatus.setText("QR scan failed — try again");
            }
        });
        barcodeView.resume();
        tvQrStatus.setText("Point camera at Device A's QR code…");
    }

    private void stopScanning() {
        scanning = false;
        barcodeView.pause();
        panelScan.setVisibility(View.GONE);
    }

    @Override public void onResume()  { super.onResume();  if (scanning) barcodeView.resume(); }
    @Override public void onPause()   { super.onPause();   barcodeView.pause(); }
    @Override public void onDestroy() { super.onDestroy(); barcodeView.pause(); }
}
