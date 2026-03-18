package com.omega.protocol.ui.schedule;

import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.widget.GridLayout;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.omega.protocol.R;
import com.omega.protocol.adapter.ScheduleItemAdapter;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import com.omega.protocol.viewmodel.ScheduleViewModel;
import com.google.android.material.tabs.TabLayout;
import androidx.recyclerview.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleFragment extends Fragment {

    private ScheduleViewModel vm;
    private Spinner spAddSubject, spAddChapter, spAddTopic;
    private java.util.List<com.omega.protocol.model.Subject> allSubjects = new java.util.ArrayList<>();
    private java.util.List<com.omega.protocol.model.Chapter> allChapters = new java.util.ArrayList<>();
    private java.util.List<com.omega.protocol.model.Topic>   allTopics   = new java.util.ArrayList<>();
    private ScheduleItemAdapter poolAdapter;
    private RecyclerView rvPool;
    private TextView tvMissionDate, tvMissionSummary, tvCalMonth, tvSelectedDay;
    private GridLayout calGrid;
    private TabLayout tabs;
    private View panelMission, panelPool;
    private Button btnAutoGen, btnAssignSelected;

    private String selectedDay = null;
    private String calYear;
    private int calMonth;
    private ScheduleViewModel.ScheduleState currentState;

    private static final DateTimeFormatter FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISP = DateTimeFormatter.ofPattern("MMM yyyy");

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_schedule, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        bindViews(v);
        setupTabs();
        setupPool();

        LocalDate now = LocalDate.now();
        calYear  = String.valueOf(now.getYear());
        calMonth = now.getMonthValue();

        vm = new ViewModelProvider(this).get(ScheduleViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), state -> {
            currentState = state;
            renderCalendar(state);
            renderMission(state);
            renderPool(state);
        });
        vm.load();

        v.findViewById(R.id.btnCalPrev).setOnClickListener(x -> { prevMonth(); renderCalendar(currentState); });
        v.findViewById(R.id.btnCalNext).setOnClickListener(x -> { nextMonth(); renderCalendar(currentState); });
        btnAutoGen.setOnClickListener(x -> vm.autoGenerate(() ->
            Toast.makeText(requireContext(), "Schedule regenerated", Toast.LENGTH_SHORT).show()));
        btnAssignSelected.setOnClickListener(x -> assignPoolToSelected());
    }

    @Override public void onResume() { super.onResume(); vm.load(); }

    private void bindViews(View v) {
        tvMissionDate    = v.findViewById(R.id.tvMissionDate);
        tvMissionSummary = v.findViewById(R.id.tvMissionSummary);
        tvCalMonth       = v.findViewById(R.id.tvCalMonth);
        tvSelectedDay    = v.findViewById(R.id.tvSelectedDay);
        calGrid          = v.findViewById(R.id.calGrid);
        tabs             = v.findViewById(R.id.schedTabs);
        panelMission     = v.findViewById(R.id.panelMission);
        panelPool        = v.findViewById(R.id.panelPool);
        rvPool           = v.findViewById(R.id.rvPool);
        btnAutoGen       = v.findViewById(R.id.btnAutoGen);
        btnAssignSelected= v.findViewById(R.id.btnAssignSelected);
    }

    private void setupTabs() {
        tabs.addTab(tabs.newTab().setText("Daily Mission"));
        tabs.addTab(tabs.newTab().setText("Pool / Assign"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) {
                panelMission.setVisibility(t.getPosition() == 0 ? View.VISIBLE : View.GONE);
                panelPool.setVisibility(t.getPosition() == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
    }

    private void setupPool() {
        poolAdapter = new ScheduleItemAdapter();
        rvPool.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPool.setAdapter(poolAdapter);
        poolAdapter.setListener(new ScheduleItemAdapter.Listener() {
            @Override public void onAssign(ScheduleItem item) {
                if (selectedDay == null) {
                    Toast.makeText(requireContext(), "Tap a day on the calendar first", Toast.LENGTH_SHORT).show();
                    return;
                }
                vm.assignToDay(item.id, selectedDay, () ->
                    Toast.makeText(requireContext(), "Added to " + selectedDay, Toast.LENGTH_SHORT).show());
            }
            @Override public void onDelete(ScheduleItem item) { vm.removeItem(item.id, null); }
        });
    }

    // ── Calendar ──────────────────────────────────────────
    private void renderCalendar(ScheduleViewModel.ScheduleState state) {
        if (state == null || !isAdded()) return;
        LocalDate first = LocalDate.of(Integer.parseInt(calYear), calMonth, 1);
        tvCalMonth.setText(first.format(DISP));
        calGrid.removeAllViews();
        calGrid.setColumnCount(7);

        // Colored day-of-week headers matching index-2
        String[] dowLabels = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        int[]    dowColors = {0xFFf87171, 0xFFa78bfa, 0xFF60a5fa,
                              0xFF34d399, 0xFFfb923c, 0xFFf472b6, 0xFFf87171};
        for (int i = 0; i < 7; i++) {
            TextView hdr = new TextView(requireContext());
            hdr.setText(dowLabels[i]);
            hdr.setTextColor(dowColors[i]);
            hdr.setTextSize(9);
            hdr.setGravity(Gravity.CENTER);
            hdr.setTypeface(null, android.graphics.Typeface.BOLD);
            hdr.setPadding(0, 6, 0, 6);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            hdr.setLayoutParams(lp);
            calGrid.addView(hdr);
        }

        // Blank slots for first day offset
        int firstDow = first.getDayOfWeek().getValue() % 7; // Sun=0
        for (int i = 0; i < firstDow; i++) {
            View blank = new View(requireContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            blank.setLayoutParams(lp);
            calGrid.addView(blank);
        }

        String todayStr = RivalEngine.today();
        int daysInMonth = first.lengthOfMonth();
        float dailyHrs  = state != null ? state.dailyHrs : 8f;

        for (int d = 1; d <= daysInMonth; d++) {
            String ds = String.format(Locale.US, "%s-%02d-%02d", calYear, calMonth, d);
            boolean isToday  = ds.equals(todayStr);
            boolean isSel    = ds.equals(selectedDay);
            boolean isPast   = ds.compareTo(todayStr) < 0;
            List<String> ids = state != null
                ? state.scheduleMap.getOrDefault(ds, java.util.Collections.emptyList())
                : java.util.Collections.emptyList();
            boolean hasItems = !ids.isEmpty();
            float usedHrs = 0f;
            if (state != null) {
                for (String id : ids) {
                    for (ScheduleItem it : state.allItems)
                        if (it.id.equals(id)) { usedHrs += it.totalHrs(); break; }
                }
            }
            boolean isFull = hasItems && usedHrs >= dailyHrs - 0.74f;

            TextView cell = new TextView(requireContext());
            cell.setText(String.valueOf(d));
            cell.setTextSize(11);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, 14, 0, 14);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);

            // Apply styling matching index-2 CSS classes
            if (isSel) {
                // selected — accent gradient, white text, scale up
                cell.setBackgroundColor(0xFF7c6fff);
                cell.setTextColor(Color.WHITE);
                cell.setScaleX(1.08f);
                cell.setScaleY(1.08f);
            } else if (isFull) {
                // full — green gradient
                cell.setBackgroundColor(0x3839FFA0);
                cell.setTextColor(0xFF39ffa0);
                cell.setScaleX(1f); cell.setScaleY(1f);
            } else if (hasItems) {
                // has items — purple tint
                cell.setBackgroundColor(0x387C6FFF);
                cell.setTextColor(0xFFe8eaff);
                cell.setScaleX(1f); cell.setScaleY(1f);
            } else if (!isPast) {
                // empty future — teal hint
                cell.setBackgroundColor(0x1400E5C8);
                cell.setTextColor(0xFF8890c0);
                cell.setScaleX(1f); cell.setScaleY(1f);
            } else {
                // past
                cell.setBackgroundColor(Color.TRANSPARENT);
                cell.setTextColor(0xFF8890c0);
                cell.setAlpha(0.35f);
                cell.setScaleX(1f); cell.setScaleY(1f);
            }

            if (isToday && !isSel) {
                // Today — golden ring (simulate with text colour + border)
                cell.setTextColor(0xFFffcb47);
                cell.setTextSize(12);
            }

            final String fds = ds;
            cell.setOnClickListener(vv -> {
                selectedDay = fds;
                tvSelectedDay.setText("Selected: " + fds);
                renderCalendar(currentState);
            });
            calGrid.addView(cell);
        }
    }

    private void prevMonth() { calMonth--; if (calMonth < 1) { calMonth = 12; calYear = String.valueOf(Integer.parseInt(calYear)-1); } }
    private void nextMonth() { calMonth++; if (calMonth > 12){ calMonth = 1;  calYear = String.valueOf(Integer.parseInt(calYear)+1); } }

    // ── Mission ───────────────────────────────────────────
    private void renderMission(ScheduleViewModel.ScheduleState state) {
        if (state == null || !isAdded()) return;
        String today = state.today;
        tvMissionDate.setText(today);
        List<String> ids = state.scheduleMap.getOrDefault(today, Collections.emptyList());
        int total = 0, done = 0;
        for (String id : ids) {
            ScheduleItem it = null;
            for (ScheduleItem x : state.allItems) if (x.id.equals(id)) { it=x; break; }
            if (it != null) { total += it.totalCount(); done += it.doneCount(); }
        }
        tvMissionSummary.setText(done + " / " + total + " passes done · " + ids.size() + " topics");
    }

    // ── Pool ──────────────────────────────────────────────
    private void populateSubjectSpinner(java.util.List<com.omega.protocol.model.Subject> subjects) {
        if (spAddSubject == null) return;
        allSubjects.clear();
        allSubjects.addAll(subjects);
        android.widget.ArrayAdapter<String> a = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (com.omega.protocol.model.Subject s : allSubjects) a.add(s.name);
        spAddSubject.setAdapter(a);
    }

    private void renderPool(ScheduleViewModel.ScheduleState state) {
        populateSubjectSpinner(state != null ? state.subjects : new java.util.ArrayList<>());
        if (state == null || !isAdded()) return;
        // Items not yet assigned to any future day
        Set<String> futureAssigned = new HashSet<>();
        String today = state.today;
        for (Map.Entry<String,List<String>> e : state.scheduleMap.entrySet())
            if (e.getKey().compareTo(today) >= 0)
                futureAssigned.addAll(e.getValue());

        List<ScheduleItem> pool = new ArrayList<>();
        for (ScheduleItem it : state.allItems)
            if (!it.isComplete() && !futureAssigned.contains(it.id)) pool.add(it);

        // Build name maps from subjects
        Map<String,String> tNames = new HashMap<>(), cNames = new HashMap<>();
        for (Subject s : state.subjects)
            for (Chapter c : s.chapters) {
                cNames.put(c.id, c.name);
                for (Topic t : c.topics) tNames.put(t.id, t.name);
            }
        poolAdapter.setData(pool, tNames, cNames);
    }

    private void assignPoolToSelected() {
        if (selectedDay == null) {
            Toast.makeText(requireContext(), "Select a day on the calendar first", Toast.LENGTH_SHORT).show();
            return;
        }
        vm.autoGenerate(() -> Toast.makeText(requireContext(), "Auto-filled from selected day", Toast.LENGTH_SHORT).show());
    }
    private void doAddToPool() {
        int si = spAddSubject.getSelectedItemPosition();
        int ci = spAddChapter.getSelectedItemPosition();
        int ti = spAddTopic.getSelectedItemPosition();
        if (si < 0 || si >= allSubjects.size()) { android.widget.Toast.makeText(requireContext(), "Pick a subject", android.widget.Toast.LENGTH_SHORT).show(); return; }
        if (ci < 0 || ci >= allChapters.size()) { android.widget.Toast.makeText(requireContext(), "Pick a chapter", android.widget.Toast.LENGTH_SHORT).show(); return; }
        if (ti < 0 || ti >= allTopics.size())   { android.widget.Toast.makeText(requireContext(), "Pick a topic",   android.widget.Toast.LENGTH_SHORT).show(); return; }
        com.omega.protocol.model.Subject  subj = allSubjects.get(si);
        com.omega.protocol.model.Chapter  chap = allChapters.get(ci);
        com.omega.protocol.model.Topic    top  = allTopics.get(ti);
        com.omega.protocol.model.ScheduleItem item = new com.omega.protocol.model.ScheduleItem();
        item.id        = "item_" + System.currentTimeMillis();
        item.subjectId = subj.id;
        item.chapterId = chap.id;
        item.topicId   = top.id;
        item.priority  = 50;
        item.part      = 1;
        com.omega.protocol.model.PassGroup pg = new com.omega.protocol.model.PassGroup("Lecture");
        com.omega.protocol.model.SubPass   sp = new com.omega.protocol.model.SubPass(
            top.id + "_L1_" + System.currentTimeMillis(), top.name + " — Lecture", 1.5f);
        pg.subPasses.add(sp);
        item.passes.add(pg);
        vm.addItemToPool(item, () -> android.widget.Toast.makeText(
            requireContext(), top.name + " added to pool", android.widget.Toast.LENGTH_SHORT).show());
    }

}