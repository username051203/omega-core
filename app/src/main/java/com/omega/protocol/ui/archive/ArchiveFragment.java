package com.omega.protocol.ui.archive;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.omega.protocol.R;
import com.omega.protocol.adapter.ArchiveAdapter;
import com.omega.protocol.db.entity.DaySnapshotEntity;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.viewmodel.ArchiveViewModel;
import java.util.*;

public class ArchiveFragment extends Fragment {

    private ArchiveViewModel vm;
    private ArchiveAdapter   adapter;
    private TextView tvStats;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_archive, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        tvStats = v.findViewById(R.id.tvArchiveStats);
        RecyclerView rv = v.findViewById(R.id.rvArchive);
        adapter = new ArchiveAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(ArchiveViewModel.class);
        vm.getSnapshots().observe(getViewLifecycleOwner(), snaps -> {
            adapter.setData(snaps);
            renderStats(snaps);
        });
        vm.load();

        adapter.setClickListener(snap -> showDetail(snap));
        adapter.setDeleteListener(snap -> showConfirmDelete(snap));
    }

    @Override public void onResume() { super.onResume(); vm.load(); }

    private void renderStats(List<DaySnapshotEntity> snaps) {
        if (snaps == null || snaps.isEmpty()) {
            tvStats.setText("No archive data yet");
            return;
        }
        int wins = 0, losses = 0, ties = 0;
        for (DaySnapshotEntity s : snaps) {
            if (s.win == null) continue;
            if (s.win) wins++; else losses++;
        }
        ties = snaps.size() - wins - losses;
        tvStats.setText(snaps.size() + " days archived  ·  " + wins + "W  " + losses + "L  " + ties + "T");
    }

    private void showDetail(DaySnapshotEntity snap) {
        String msg = "Date: " + snap.dayStr +
            "\nYour passes: " + snap.userCh +
            "\nAlexandrius: " + String.format(Locale.US, "%.1f", snap.aliciaCh) +
            "\nResult: " + (snap.win == null ? "No data" : snap.win ? "WIN 🎯" : "LOSS ⚠️") +
            "\nHours: " + RivalEngine.fmtMins(snap.hrsUsed) + " / " + RivalEngine.fmtMins(snap.budget) +
            "\nBoost: " + String.format(Locale.US, "%.2f×", snap.boost);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(snap.dayStr).setMessage(msg)
            .setPositiveButton("OK", null)
            .setNegativeButton("🗑 Delete", (d, w) -> showConfirmDelete(snap))
            .show();
    }

    private void showConfirmDelete(DaySnapshotEntity snap) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete " + snap.dayStr + "?")
            .setMessage("Remove this day from the chronicle. Cannot be undone.")
            .setPositiveButton("Delete", (d, w) ->
                vm.deleteSnapshot(snap.dayStr, () ->
                    Toast.makeText(requireContext(), "Day deleted", Toast.LENGTH_SHORT).show()))
            .setNegativeButton("Cancel", null)
            .show();
    }
}
