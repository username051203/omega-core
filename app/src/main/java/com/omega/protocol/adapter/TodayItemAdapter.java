package com.omega.protocol.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.omega.protocol.R;
import com.omega.protocol.model.*;
import java.util.*;

public class TodayItemAdapter extends RecyclerView.Adapter<TodayItemAdapter.VH> {

    public interface OnTickListener {
        void onTick(String itemId, int passIdx, int spIdx, boolean done);
    }

    private final List<ScheduleItem> items = new ArrayList<>();
    private final Map<String, String> topicNames   = new HashMap<>();
    private final Map<String, String> chapterNames = new HashMap<>();
    private OnTickListener listener;

    public void setListener(OnTickListener l) { this.listener = l; }

    public void setData(List<ScheduleItem> data, Map<String,String> tNames, Map<String,String> cNames) {
        items.clear(); items.addAll(data);
        topicNames.clear();   topicNames.putAll(tNames);
        chapterNames.clear(); chapterNames.putAll(cNames);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_today_item, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ScheduleItem it   = items.get(pos);
        int done          = it.doneCount(), total = it.totalCount();
        int pct           = total > 0 ? (int)(done * 100f / total) : 0;
        String tName      = topicNames.getOrDefault(it.topicId, it.topicId);
        String cName      = chapterNames.getOrDefault(it.chapterId, it.chapterId);

        h.title.setText(tName + (it.part > 1 ? " · R" + it.part : ""));
        h.sub.setText(cName + " · " + String.format("%.1f", it.totalHrs()) + "h");
        h.passCount.setText(done + "/" + total + " passes");
        h.progress.setProgress(pct);
        h.progress.setIndicatorColor(done == total && total > 0 ? 0xFF39ffa0 : 0xFF7c6fff);

        // Build sub-pass chips inline
        h.subPassContainer.removeAllViews();
        for (int pi = 0; pi < it.passes.size(); pi++) {
            PassGroup pg = it.passes.get(pi);
            for (int si = 0; si < pg.subPasses.size(); si++) {
                SubPass sp = pg.subPasses.get(si);
                CheckBox cb = new CheckBox(h.itemView.getContext());
                cb.setText(sp.label);
                cb.setChecked(sp.done);
                cb.setTextColor(sp.done ? 0xFF39ffa0 : 0xFFe8eaff);
                cb.setTextSize(11f);
                final int fpi = pi, fsi = si;
                cb.setOnCheckedChangeListener(null);
                cb.setOnCheckedChangeListener((btn, checked) -> {
                    sp.done = checked;
                    if (listener != null) listener.onTick(it.id, fpi, fsi, checked);
                });
                h.subPassContainer.addView(cb);
            }
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub, passCount;
        com.google.android.material.progressindicator.LinearProgressIndicator progress;
        LinearLayout subPassContainer;
        VH(View v) {
            super(v);
            title            = v.findViewById(R.id.tiTitle);
            sub              = v.findViewById(R.id.tiSub);
            passCount        = v.findViewById(R.id.tiPassCount);
            progress         = v.findViewById(R.id.tiProgress);
            subPassContainer = v.findViewById(R.id.tiSubPasses);
        }
    }
}
