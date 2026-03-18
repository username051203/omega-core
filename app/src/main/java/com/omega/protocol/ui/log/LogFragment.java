package com.omega.protocol.ui.log;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.omega.protocol.R;
import com.omega.protocol.adapter.TodayItemAdapter;
import com.omega.protocol.model.*;
import com.omega.protocol.viewmodel.DashboardViewModel;
import com.omega.protocol.viewmodel.ScheduleViewModel;
import java.util.*;

public class LogFragment extends Fragment {

    private DashboardViewModel dashVm;
    private ScheduleViewModel  schedVm;
    private TodayItemAdapter   adapter;
    private TextView tvSummary;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_log, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        tvSummary = v.findViewById(R.id.tvLogSummary);

        RecyclerView rv = v.findViewById(R.id.rvLogItems);
        adapter = new TodayItemAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        dashVm  = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        schedVm = new ViewModelProvider(requireActivity()).get(ScheduleViewModel.class);

        dashVm.getDashState().observe(getViewLifecycleOwner(), ds -> {
            if (ds == null) return;
            // Build name maps from schedule state
            Map<String,String> tNames = new HashMap<>(), cNames = new HashMap<>();
            ScheduleViewModel.ScheduleState ss = schedVm.getState().getValue();
            if (ss != null) {
                for (Subject sub : ss.subjects)
                    for (Chapter ch : sub.chapters) {
                        cNames.put(ch.id, ch.name);
                        for (Topic t : ch.topics) tNames.put(t.id, t.name);
                    }
            }
            adapter.setData(ds.todayItems, tNames, cNames);
            int done = ds.userPasses, total = ds.totalPasses;
            tvSummary.setText(done + " / " + total + " passes done today");
            tvSummary.setTextColor(done >= total && total > 0 ? 0xFF39ffa0 : 0xFFffcb47);
        });

        adapter.setListener((itemId, passIdx, spIdx, done) ->
            dashVm.tickSubPass(itemId, passIdx, spIdx, done, null));

        dashVm.refresh();
        schedVm.load();
    }

    @Override public void onResume() { super.onResume(); dashVm.refresh(); }
}
