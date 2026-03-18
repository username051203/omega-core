package com.omega.protocol.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.omega.protocol.R;
import com.omega.protocol.model.*;
import java.util.*;

public class SubPassAdapter extends RecyclerView.Adapter<SubPassAdapter.VH> {

    public interface OnTickListener { void onTick(int passIdx, int spIdx, boolean checked); }

    private final List<SubPassAdapter.Row> rows = new ArrayList<>();
    private OnTickListener listener;

    public void setListener(OnTickListener l) { this.listener = l; }

    public void setData(ScheduleItem item) {
        rows.clear();
        for (int pi = 0; pi < item.passes.size(); pi++) {
            PassGroup pg = item.passes.get(pi);
            for (int si = 0; si < pg.subPasses.size(); si++) {
                rows.add(new Row(pg.type, pg.subPasses.get(si), pi, si));
            }
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                     .inflate(R.layout.item_subpass, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Row row = rows.get(pos);
        h.label.setText(row.sp.label);
        h.hrs.setText(row.sp.hrs + "h");
        h.typeTag.setText(typeIcon(row.type) + " " + row.type);
        h.typeTag.setBackgroundColor(typeBg(row.type));
        h.check.setChecked(row.sp.done);
        h.check.setOnCheckedChangeListener(null);
        h.check.setOnCheckedChangeListener((btn, checked) -> {
            row.sp.done = checked;
            if (listener != null) listener.onTick(row.passIdx, row.spIdx, checked);
        });
        // Dim completed rows
        h.itemView.setAlpha(row.sp.done ? 0.55f : 1f);
    }

    @Override public int getItemCount() { return rows.size(); }

    private String typeIcon(String type) {
        switch (type) {
            case "Lecture":       return "📹";
            case "Reading":       return "📖";
            case "Question Bank": return "❓";
            case "Revision":      return "🔁";
            case "Notes":         return "✍️";
            default:              return "•";
        }
    }
    private int typeBg(String type) {
        switch (type) {
            case "Lecture":       return 0xFF1e3a5f;
            case "Reading":       return 0xFF3b2a1a;
            case "Question Bank": return 0xFF14532d;
            case "Revision":      return 0xFF2d1b69;
            case "Notes":         return 0xFF1f2937;
            default:              return 0xFF1a1a2e;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox check; TextView label, hrs, typeTag;
        VH(View v) {
            super(v);
            check   = v.findViewById(R.id.spCheck);
            label   = v.findViewById(R.id.spLabel);
            hrs     = v.findViewById(R.id.spHrs);
            typeTag = v.findViewById(R.id.spType);
        }
    }

    static class Row {
        String type; SubPass sp; int passIdx, spIdx;
        Row(String type, SubPass sp, int pi, int si) {
            this.type=type; this.sp=sp; passIdx=pi; spIdx=si;
        }
    }
}
