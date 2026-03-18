package com.omega.protocol.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.omega.protocol.R;
import com.omega.protocol.model.*;
import java.util.*;

public class SyllabusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CHAPTER = 0;
    private static final int TYPE_TOPIC   = 1;

    public interface Listener {
        void onDeleteTopic(String chapterId, String topicId);
        void onAddTopic(String chapterId);
    }

    private Listener listener;
    private final List<Object> rows = new ArrayList<>();   // Chapter or Topic
    private final Map<String, Boolean> expanded = new HashMap<>();
    private final Set<String> scheduledTopics = new HashSet<>();
    private final Set<String> doneTopics      = new HashSet<>();

    // Maps topicId → chapter for callbacks
    private final Map<String, String> topicToChapter = new HashMap<>();

    public void setListener(Listener l) { this.listener = l; }

    public void setData(List<Chapter> chapters, Set<String> scheduled, Set<String> done) {
        scheduledTopics.clear(); scheduledTopics.addAll(scheduled);
        doneTopics.clear();      doneTopics.addAll(done);
        topicToChapter.clear();
        rows.clear();
        for (Chapter c : chapters) {
            rows.add(c);
            Boolean exp = expanded.getOrDefault(c.id, false);
            if (Boolean.TRUE.equals(exp)) {
                for (Topic t : c.topics) {
                    rows.add(t);
                    topicToChapter.put(t.id, c.id);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int pos) {
        return rows.get(pos) instanceof Chapter ? TYPE_CHAPTER : TYPE_TOPIC;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int type) {
        if (type == TYPE_CHAPTER)
            return new ChapterVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_chapter, p, false));
        return new TopicVH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_topic, p, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        if (h instanceof ChapterVH) {
            Chapter c   = (Chapter) rows.get(pos);
            ChapterVH ch = (ChapterVH) h;
            int total   = c.topics.size();
            int done    = (int) c.topics.stream().filter(t -> doneTopics.contains(t.id)).count();
            int sched   = (int) c.topics.stream().filter(t -> scheduledTopics.contains(t.id)).count();
            ch.name.setText(c.name);
            ch.count.setText(done + "/" + total + " done · " + sched + " scheduled");
            ch.arrow.setText(Boolean.TRUE.equals(expanded.get(c.id)) ? "▲" : "▼");
            ch.itemView.setOnClickListener(v -> {
                expanded.put(c.id, !Boolean.TRUE.equals(expanded.get(c.id)));
                // Re-build rows with same data — find the Subject from existing rows
                List<Chapter> chs = new ArrayList<>();
                for (Object r : rows) if (r instanceof Chapter) chs.add((Chapter)r);
                setData(chs, scheduledTopics, doneTopics);
            });
            ch.btnAdd.setOnClickListener(v -> { if (listener != null) listener.onAddTopic(c.id); });
        } else {
            Topic t      = (Topic) rows.get(pos);
            TopicVH tv   = (TopicVH) h;
            tv.name.setText(t.name);
            boolean sched = scheduledTopics.contains(t.id);
            boolean done2 = doneTopics.contains(t.id);
            tv.badge.setVisibility(sched ? View.VISIBLE : View.GONE);
            tv.badge.setText(done2 ? "✓ DONE" : "SCHEDULED");
            tv.badge.setTextColor(done2 ? 0xFF4ade80 : 0xFFffcb47);
            tv.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteTopic(topicToChapter.get(t.id), t.id);
            });
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    static class ChapterVH extends RecyclerView.ViewHolder {
        TextView name, count, arrow; ImageButton btnAdd;
        ChapterVH(View v) { super(v);
            name=v.findViewById(R.id.chapName); count=v.findViewById(R.id.chapCount);
            arrow=v.findViewById(R.id.chapArrow); btnAdd=v.findViewById(R.id.chapBtnAdd); }
    }
    static class TopicVH extends RecyclerView.ViewHolder {
        TextView name, badge; ImageButton btnDelete;
        TopicVH(View v) { super(v);
            name=v.findViewById(R.id.topicName); badge=v.findViewById(R.id.topicBadge);
            btnDelete=v.findViewById(R.id.topicBtnDelete); }
    }
}
