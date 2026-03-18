package com.omega.protocol.ui.syllabus;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.omega.protocol.R;
import com.omega.protocol.adapter.SyllabusAdapter;
import com.omega.protocol.model.*;
import com.omega.protocol.viewmodel.ScheduleViewModel;
import com.omega.protocol.viewmodel.SyllabusViewModel;
import java.util.*;

public class SyllabusFragment extends Fragment {

    private SyllabusViewModel vm;
    private ScheduleViewModel schedVm;
    private SyllabusAdapter   adapter;
    private Spinner           subjectSpinner;
    private List<Subject>     subjects = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_syllabus, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        subjectSpinner = v.findViewById(R.id.subjectSpinner);
        RecyclerView rv = v.findViewById(R.id.rvSyllabus);
        adapter = new SyllabusAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        vm      = new ViewModelProvider(this).get(SyllabusViewModel.class);
        schedVm = new ViewModelProvider(requireActivity()).get(ScheduleViewModel.class);

        vm.getSubjects().observe(getViewLifecycleOwner(), list -> {
            subjects = list;
            List<String> names = new ArrayList<>();
            for (Subject s2 : list) names.add(s2.name);
            ArrayAdapter<String> sa = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, names);
            sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            subjectSpinner.setAdapter(sa);
            renderCurrentSubject();
        });

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View vv, int pos, long id) { renderCurrentSubject(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        adapter.setListener(new SyllabusAdapter.Listener() {
            @Override public void onDeleteTopic(String chapterId, String topicId) {
                Subject cur = currentSubject();
                if (cur == null) return;
                showConfirm("Delete this topic?", () -> vm.deleteTopic(cur.id, chapterId, topicId, null));
            }
            @Override public void onAddTopic(String chapterId) {
                Subject cur = currentSubject();
                if (cur == null) return;
                showAddTopicDialog(cur.id, chapterId);
            }
        });

        v.findViewById(R.id.btnAddSubject).setOnClickListener(x -> showAddSubjectDialog());
        v.findViewById(R.id.btnAddChapter).setOnClickListener(x -> showAddChapterDialog());
        v.findViewById(R.id.btnBatchImport).setOnClickListener(x -> showBatchImportDialog());

        vm.load();
        schedVm.load();
    }

    @Override public void onResume() { super.onResume(); vm.load(); }

    private Subject currentSubject() {
        int pos = subjectSpinner.getSelectedItemPosition();
        return (pos >= 0 && pos < subjects.size()) ? subjects.get(pos) : null;
    }

    private void renderCurrentSubject() {
        Subject cur = currentSubject();
        if (cur == null) { adapter.setData(Collections.emptyList(), Collections.emptySet(), Collections.emptySet()); return; }
        ScheduleViewModel.ScheduleState ss = schedVm.getState().getValue();
        Set<String> scheduled = new HashSet<>(), done = new HashSet<>();
        if (ss != null) {
            for (ScheduleItem it : ss.allItems) {
                scheduled.add(it.topicId);
                if (it.isComplete()) done.add(it.topicId);
            }
        }
        adapter.setData(cur.chapters, scheduled, done);
    }

    private void showAddSubjectDialog() {
        EditText et = new EditText(requireContext());
        et.setHint("Subject name");
        new AlertDialog.Builder(requireContext())
            .setTitle("Add Subject").setView(et)
            .setPositiveButton("Add", (d, w) -> {
                String name = et.getText().toString().trim();
                if (!name.isEmpty()) vm.addSubject(name, null);
            }).setNegativeButton("Cancel", null).show();
    }

    private void showAddChapterDialog() {
        Subject cur = currentSubject(); if (cur == null) return;
        EditText et = new EditText(requireContext()); et.setHint("Chapter name");
        new AlertDialog.Builder(requireContext())
            .setTitle("Add Chapter to " + cur.name).setView(et)
            .setPositiveButton("Add", (d, w) -> {
                String name = et.getText().toString().trim();
                if (!name.isEmpty()) vm.addChapter(cur.id, name, null);
            }).setNegativeButton("Cancel", null).show();
    }

    private void showAddTopicDialog(String subjId, String chapId) {
        EditText et = new EditText(requireContext()); et.setHint("Topic name");
        new AlertDialog.Builder(requireContext())
            .setTitle("Add Topic").setView(et)
            .setPositiveButton("Add", (d, w) -> {
                String name = et.getText().toString().trim();
                if (!name.isEmpty()) vm.addTopic(subjId, chapId, name, null);
            }).setNegativeButton("Cancel", null).show();
    }

    private void showBatchImportDialog() {
        Subject cur = currentSubject(); if (cur == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_batch_import, null);
        EditText etInput = dialogView.findViewById(R.id.etBatchInput);
        new AlertDialog.Builder(requireContext())
            .setTitle("Batch Import — " + cur.name)
            .setMessage("One entry per line:\nChapter Name, Topic Name")
            .setView(dialogView)
            .setPositiveButton("Import", (d, w) -> {
                String raw = etInput.getText().toString();
                List<String[]> pairs = new ArrayList<>();
                for (String line : raw.split("\n")) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) pairs.add(new String[]{parts[0].trim(), parts[1].trim()});
                }
                if (!pairs.isEmpty()) vm.batchImport(cur.id, pairs, null);
            }).setNegativeButton("Cancel", null).show();
    }

    private void showConfirm(String msg, Runnable onYes) {
        new AlertDialog.Builder(requireContext())
            .setMessage(msg)
            .setPositiveButton("Yes", (d,w) -> onYes.run())
            .setNegativeButton("No", null).show();
    }
}
