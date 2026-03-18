package com.omega.protocol.adapter;

import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.omega.protocol.R;
import com.omega.protocol.db.entity.DaySnapshotEntity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.VH> {

    public interface OnDayClick { void onClick(DaySnapshotEntity snap); }
    public interface OnDayDelete { void onDelete(DaySnapshotEntity snap); }

    private final List<DaySnapshotEntity> items = new ArrayList<>();
    private OnDayClick  clickListener;
    private OnDayDelete deleteListener;
    private static final DateTimeFormatter FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISP = DateTimeFormatter.ofPattern("EEE, MMM d");

    public void setClickListener(OnDayClick l)   { clickListener  = l; }
    public void setDeleteListener(OnDayDelete l) { deleteListener = l; }

    public void setData(List<DaySnapshotEntity> data) {
        items.clear(); items.addAll(data); notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_archive_day, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        DaySnapshotEntity s = items.get(pos);
        String disp;
        try { disp = LocalDate.parse(s.dayStr, FMT).format(DISP); }
        catch (Exception e) { disp = s.dayStr; }

        h.date.setText(disp);
        h.userScore.setText(String.valueOf(s.userCh));
        h.rivalScore.setText(String.format(Locale.US, "%.1f", s.aliciaCh));
        float gap = s.userCh - s.aliciaCh;
        h.gap.setText((gap >= 0 ? "+" : "") + String.format(Locale.US, "%.1f", gap));

        if (s.win == null) {
            h.resultBadge.setText("NO DATA");
            h.resultBadge.setBackgroundColor(0xFF252a45);
            h.resultBadge.setTextColor(0xFF8890c0);
            h.gap.setTextColor(0xFF8890c0);
        } else if (s.win) {
            h.resultBadge.setText("WIN");
            h.resultBadge.setBackgroundColor(0xFF14532d);
            h.resultBadge.setTextColor(0xFF4ade80);
            h.gap.setTextColor(0xFF39ffa0);
        } else {
            h.resultBadge.setText("LOSS");
            h.resultBadge.setBackgroundColor(0xFF7f1d1d);
            h.resultBadge.setTextColor(0xFFfca5a5);
            h.gap.setTextColor(0xFFff6b6b);
        }

        h.hrsUsed.setText(String.format(Locale.US, "%.1fh / %.1fh", s.hrsUsed, s.budget));
        h.boost.setText(s.boost >= 1.15f ? String.format(Locale.US, "%.2f×", s.boost) : "—");
        h.boost.setTextColor(s.boost >= 1.15f ? 0xFFfb923c : 0xFF8890c0);

        h.itemView.setOnClickListener(v -> { if (clickListener  != null) clickListener.onClick(s); });
        h.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(s);
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView date, userScore, rivalScore, gap, resultBadge, hrsUsed, boost;
        VH(View v) {
            super(v);
            date        = v.findViewById(R.id.archDate);
            userScore   = v.findViewById(R.id.archUser);
            rivalScore  = v.findViewById(R.id.archRival);
            gap         = v.findViewById(R.id.archGap);
            resultBadge = v.findViewById(R.id.archResult);
            hrsUsed     = v.findViewById(R.id.archHrs);
            boost       = v.findViewById(R.id.archBoost);
        }
    }
}
