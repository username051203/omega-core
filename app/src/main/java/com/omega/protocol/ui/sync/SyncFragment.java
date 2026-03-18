package com.omega.protocol.ui.sync;

import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.omega.protocol.R;
import com.omega.protocol.viewmodel.SyncViewModel;
import java.io.*;

public class SyncFragment extends Fragment {

    private SyncViewModel vm;
    private TextView tvStatus;
    private ActivityResultLauncher<String> filePicker;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_sync, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        tvStatus = v.findViewById(R.id.tvSyncStatus);

        vm = new ViewModelProvider(this).get(SyncViewModel.class);
        vm.getStatus().observe(getViewLifecycleOwner(), tvStatus::setText);

        // File picker for import
        filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) importFromUri(uri); });

        v.findViewById(R.id.btnExport).setOnClickListener(x -> doExport());
        v.findViewById(R.id.btnQrSync).setOnClickListener(x ->
            androidx.navigation.Navigation.findNavController(v).navigate(R.id.qrSyncFragment));
        v.findViewById(R.id.btnImport).setOnClickListener(x -> filePicker.launch("*/*"));
        v.findViewById(R.id.btnHardReset).setOnClickListener(x -> confirmReset());
    }

    private void doExport() {
        vm.export(json -> {
            try {
                File dir = requireContext().getExternalFilesDir(null);
                File f   = new File(dir, "omega_backup_" + System.currentTimeMillis() + ".txt");
                try (FileWriter fw = new FileWriter(f)) { fw.write(json); }
                Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", f);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Save Backup"));
            } catch (Exception e) {
                tvStatus.setText("Export failed: " + e.getMessage());
            }
        });
    }

    private void importFromUri(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            vm.importJson(sb.toString());
        } catch (Exception e) {
            tvStatus.setText("Read failed: " + e.getMessage());
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Purge All Data")
            .setMessage("This will delete all schedules, logs, history, and syllabus changes. This cannot be undone.")
            .setPositiveButton("PURGE", (d, w) -> vm.hardReset())
            .setNegativeButton("Cancel", null).show();
    }
}
