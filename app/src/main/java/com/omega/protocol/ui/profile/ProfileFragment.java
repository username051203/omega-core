package com.omega.protocol.ui.profile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.omega.protocol.R;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import com.omega.protocol.viewmodel.ProfileViewModel;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ProfileViewModel vm;

    // User
    private EditText etUserName, etUserTagline, etUserPhotoDefault, etUserPhotoWin, etUserPhotoLoss;
    private ImageView imgUserPreview;

    // Alexandrius
    private EditText etAlexName, etAlexTagline, etAlexPhotoDefault, etAlexPhotoWin, etAlexPhotoLoss;
    private ImageView imgAlexPreview;

    // Engine
    private EditText etExamDate, etAnchorHour, etDailyHrs;

    // Routine
    private EditText etSleep, etMeals, etBuffer;
    private TextView tvAvailHrs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_profile, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        bindViews(v);

        vm = new ViewModelProvider(this).get(ProfileViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            populateUser(state.profiles.user);
            populateAlex(state.profiles.alicia);
            populateEngine(state.engine);
            populateRoutine(state.routine);
        });
        vm.load();

        v.findViewById(R.id.btnSaveUser).setOnClickListener(x -> saveUser());
        v.findViewById(R.id.btnSaveAlex).setOnClickListener(x -> saveAlex());
        v.findViewById(R.id.btnSaveEngine).setOnClickListener(x -> saveEngine());
        v.findViewById(R.id.btnSaveRoutine).setOnClickListener(x -> saveRoutine());

        // Live photo preview
        etUserPhotoDefault.addTextChangedListener(new SimpleWatcher(url ->
            Glide.with(this).load(url).circleCrop().into(imgUserPreview)));
        etAlexPhotoDefault.addTextChangedListener(new SimpleWatcher(url ->
            Glide.with(this).load(url).circleCrop().into(imgAlexPreview)));

        // Live available-hours preview
        android.text.TextWatcher routineWatcher = new SimpleWatcher(x -> updateAvailPreview());
        etSleep.addTextChangedListener(routineWatcher);
        etMeals.addTextChangedListener(routineWatcher);
        etBuffer.addTextChangedListener(routineWatcher);
    }

    @Override public void onResume() { super.onResume(); vm.load(); }

    // ── Bind ──────────────────────────────────────────────
    private void bindViews(View v) {
        etUserName         = v.findViewById(R.id.etUserName);
        etUserTagline      = v.findViewById(R.id.etUserTagline);
        etUserPhotoDefault = v.findViewById(R.id.etUserPhotoDefault);
        etUserPhotoWin     = v.findViewById(R.id.etUserPhotoWin);
        etUserPhotoLoss    = v.findViewById(R.id.etUserPhotoLoss);
        imgUserPreview     = v.findViewById(R.id.imgUserPreview);
        etAlexName         = v.findViewById(R.id.etAlexName);
        etAlexTagline      = v.findViewById(R.id.etAlexTagline);
        etAlexPhotoDefault = v.findViewById(R.id.etAlexPhotoDefault);
        etAlexPhotoWin     = v.findViewById(R.id.etAlexPhotoWin);
        etAlexPhotoLoss    = v.findViewById(R.id.etAlexPhotoLoss);
        imgAlexPreview     = v.findViewById(R.id.imgAlexPreview);
        etExamDate         = v.findViewById(R.id.etExamDate);
        etAnchorHour       = v.findViewById(R.id.etAnchorHour);
        etDailyHrs         = v.findViewById(R.id.etDailyHrs);
        etSleep            = v.findViewById(R.id.etSleep);
        etMeals            = v.findViewById(R.id.etMeals);
        etBuffer           = v.findViewById(R.id.etBuffer);
        tvAvailHrs         = v.findViewById(R.id.tvAvailHrs);
    }

    // ── Populate ──────────────────────────────────────────
    private void populateUser(Profile p) {
        etUserName.setText(p.name);
        etUserTagline.setText(p.tagline);
        etUserPhotoDefault.setText(p.photoDefault);
        etUserPhotoWin.setText(p.photoWin);
        etUserPhotoLoss.setText(p.photoLoss);
        Glide.with(this).load(p.photoDefault).circleCrop()
             .placeholder(R.drawable.ic_avatar_placeholder).into(imgUserPreview);
    }
    private void populateAlex(Profile p) {
        etAlexName.setText(p.name);
        etAlexTagline.setText(p.tagline);
        etAlexPhotoDefault.setText(p.photoDefault);
        etAlexPhotoWin.setText(p.photoWin);
        etAlexPhotoLoss.setText(p.photoLoss);
        Glide.with(this).load(p.photoDefault).circleCrop()
             .placeholder(R.drawable.ic_avatar_placeholder).into(imgAlexPreview);
    }
    private void populateEngine(EngineConfig ec) {
        etExamDate.setText(ec.examDate != null ? ec.examDate : "");
        etAnchorHour.setText(String.valueOf(ec.anchorHour));
        etDailyHrs.setText(String.valueOf(ec.dailyHrs));
    }
    private void populateRoutine(Routine rt) {
        // Serialize sleep ranges to "23:00-06:30, 14:00-15:00" format
        StringBuilder slp = new StringBuilder();
        for (Routine.TimeRange r : rt.slp) {
            if (slp.length() > 0) slp.append(", ");
            slp.append(fmtTime(r.s)).append("-").append(fmtTime(r.e));
        }
        StringBuilder ml = new StringBuilder();
        for (float m : rt.ml) { if (ml.length()>0) ml.append(", "); ml.append(fmtTime(m)); }
        StringBuilder bf = new StringBuilder();
        for (Routine.TimeRange r : rt.bf) {
            if (bf.length()>0) bf.append(", ");
            bf.append(fmtTime(r.s)).append("-").append(fmtTime(r.e));
        }
        etSleep.setText(slp.toString());
        etMeals.setText(ml.toString());
        etBuffer.setText(bf.toString());
        updateAvailPreview();
    }

    // ── Save ──────────────────────────────────────────────
    private void saveUser() {
        ProfileViewModel.ProfileState st = vm.getState().getValue();
        if (st == null) return;
        st.profiles.user.name         = etUserName.getText().toString().trim();
        st.profiles.user.tagline      = etUserTagline.getText().toString().trim();
        st.profiles.user.photoDefault = etUserPhotoDefault.getText().toString().trim();
        st.profiles.user.photoWin     = etUserPhotoWin.getText().toString().trim();
        st.profiles.user.photoLoss    = etUserPhotoLoss.getText().toString().trim();
        vm.saveProfiles(st.profiles, () ->
            Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show());
    }
    private void saveAlex() {
        ProfileViewModel.ProfileState st = vm.getState().getValue();
        if (st == null) return;
        st.profiles.alicia.name         = etAlexName.getText().toString().trim();
        st.profiles.alicia.tagline      = etAlexTagline.getText().toString().trim();
        st.profiles.alicia.photoDefault = etAlexPhotoDefault.getText().toString().trim();
        st.profiles.alicia.photoWin     = etAlexPhotoWin.getText().toString().trim();
        st.profiles.alicia.photoLoss    = etAlexPhotoLoss.getText().toString().trim();
        vm.saveProfiles(st.profiles, () ->
            Toast.makeText(requireContext(), "ALEXANDRIUS profile saved", Toast.LENGTH_SHORT).show());
    }
    private void saveEngine() {
        try {
            EngineConfig ec = new EngineConfig();
            ec.examDate    = etExamDate.getText().toString().trim();
            ec.anchorHour  = Float.parseFloat(etAnchorHour.getText().toString().trim());
            ec.dailyHrs    = Float.parseFloat(etDailyHrs.getText().toString().trim());
            vm.saveEngine(ec, () ->
                Toast.makeText(requireContext(), "Engine config saved", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveRoutine() {
        Routine rt = parseRoutine();
        vm.saveRoutine(rt, () -> {
            updateAvailPreview();
            Toast.makeText(requireContext(), "Constraints saved", Toast.LENGTH_SHORT).show();
        });
    }

    // ── Routine parser ────────────────────────────────────
    private Routine parseRoutine() {
        Routine rt = new Routine();
        rt.slp.clear(); rt.ml.clear(); rt.bf.clear();
        for (String part : etSleep.getText().toString().split(",")) {
            Routine.TimeRange r = parseRange(part.trim()); if (r != null) rt.slp.add(r);
        }
        for (String part : etMeals.getText().toString().split(",")) {
            float t = parseHour(part.trim()); if (t >= 0) rt.ml.add(t);
        }
        for (String part : etBuffer.getText().toString().split(",")) {
            Routine.TimeRange r = parseRange(part.trim()); if (r != null) rt.bf.add(r);
        }
        if (rt.slp.isEmpty()) rt.slp.add(new Routine.TimeRange(23f, 6.5f));
        return rt;
    }
    private Routine.TimeRange parseRange(String s) {
        String[] p = s.split("-"); if (p.length < 2) return null;
        float a = parseHour(p[0]), b = parseHour(p[1]);
        return (a >= 0 && b >= 0) ? new Routine.TimeRange(a, b) : null;
    }
    private float parseHour(String s) {
        try {
            String[] p = s.trim().split(":");
            return p.length == 2 ? Integer.parseInt(p[0]) + Integer.parseInt(p[1])/60f : Float.parseFloat(s);
        } catch (Exception e) { return -1; }
    }
    private String fmtTime(float h) {
        return String.format(Locale.US, "%02d:%02d", (int)h, (int)((h%1)*60));
    }
    private void updateAvailPreview() {
        try {
            Routine rt = parseRoutine();
            float avail = com.omega.protocol.engine.RivalEngine.availableHrs(rt);
            tvAvailHrs.setText(RivalEngine.fmtMins(avail) + " available per day for studying");
        } catch (Exception ignored) {}
    }

    // ── Simple TextWatcher helper ─────────────────────────
    static class SimpleWatcher implements android.text.TextWatcher {
        interface Action { void run(String s); }
        private final Action action;
        SimpleWatcher(Action a) { action = a; }
        @Override public void beforeTextChanged(CharSequence s,int i,int c,int a2){}
        @Override public void onTextChanged(CharSequence s,int i,int b,int c){}
        @Override public void afterTextChanged(android.text.Editable e) { action.run(e.toString()); }
    }
}
