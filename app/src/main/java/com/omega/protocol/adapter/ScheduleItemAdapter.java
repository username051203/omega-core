package com.omega.protocol.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.omega.protocol.R;
import com.omega.protocol.model.ScheduleItem;
import java.util.*;

public class ScheduleItemAdapter extends RecyclerView.Adapter<ScheduleItemAdapter.VH> {

    public interface Listener {
        void onAssign(ScheduleItem item);
        void onDelete(ScheduleItem item);
    }

    private final List<ScheduleItem> items = new ArrayList<>();
    private final Map<String, String> topicNames    = new HashMap<>();
    private final Map<String, String> chapterNames  = new HashMap<>();
    private Listener listener;
    private boolean showAssign = true;

    public void setListener(Listener l)     { this.listener = l; }
    public void setShowAssign(boolean show) { this.showAssign = show; notifyDataSetChanged(); }

    public void setData(List<ScheduleItem> data,
                        Map<String,String> tNames,
                        Map<String,String> cNames) {
        items.clear(); items.addAll(data);
        topicNames.clear();   topicNames.putAll(tNames);
        chapterNames.clear(); chapterNames.putAll(cNames);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_schedule_item, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ScheduleItem it  = items.get(pos);
        int done         = it.doneCount();
        int total        = it.totalCount();
        float pct        = total > 0 ? done / (float) total : 0f;
        String tName     = topicNames.getOrDefault(it.topicId, it.topicId);
        String cName     = chapterNames.getOrDefault(it.chapterId, it.chapterId);

        h.title.setText((it.part > 1 ? "R" + it.part + " — " : "") + tName);
        h.sub.setText(cName + " · " + String.format("%.1f", it.totalHrs()) + "h");
        h.progress.setProgress((int)(pct * 100));
        h.passCount.setText(done + "/" + total);

        h.btnAssign.setVisibility(showAssign ? View.VISIBLE : View.GONE);
        h.btnAssign.setOnClickListener(v -> { if (listener != null) listener.onAssign(it); });
        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(it); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, sub, passCount;
        ProgressBar progress;
        ImageButton btnAssign, btnDelete;
        VH(View v) {
            super(v);
            title     = v.findViewById(R.id.siTitle);
            sub       = v.findViewById(R.id.siSub);
            passCount = v.findViewById(R.id.siPassCount);
            progress  = v.findViewById(R.id.siProgress);
            btnAssign = v.findViewById(R.id.siBtnAssign);
            btnDelete = v.findViewById(R.id.siBtnDelete);
        }
    }
}
