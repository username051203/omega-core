package com.omega.protocol.ui.dashboard;

import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.omega.protocol.R;
import com.omega.protocol.adapter.TodayItemAdapter;
import com.omega.protocol.engine.RivalEngine;
import com.omega.protocol.model.*;
import com.omega.protocol.viewmodel.DashboardViewModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.*;

public class DashboardFragment extends Fragment {

    private DashboardViewModel vm;
    private TodayItemAdapter   todayAdapter;
    private Handler tickHandler;
    private Runnable tickRunnable;

    // Views
    private TextView  tvClock, tvStatus, tvAnchor, tvBadge, tvLead, tvDeficit;
    private TextView  tvUserName, tvUserPasses, tvRivalName, tvRivalPasses;
    private TextView  tvAliciaHrs, tvAliciaPasses, tvBoost, tvBoostLabel, tvQuip;
    private de.hdodenhof.circleimageview.CircleImageView imgRivalPfpSmall;
    private TextView  tvStreakOverlay;
    private ImageView imgUserPfp, imgRivalPfp, imgChibi;
    private LinearProgressIndicator barLoad, barUser, barRival;
    private RecyclerView rvToday;
    private View        cardEod;
    private TextView    tvEodDate, tvEodUserCh, tvEodRivalCh, tvEodResult, tvEodGap, tvEodBoost, tvEodBudget;
    private Button      btnCloseEod, btnShareEod;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        return inf.inflate(R.layout.fragment_dashboard, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        bindViews(v);
        setupRecycler();

        vm = new ViewModelProvider(this).get(DashboardViewModel.class);
        vm.getDashState().observe(getViewLifecycleOwner(), this::render);
        vm.refresh();

        // Tick every 60 s
        tickHandler  = new Handler(Looper.getMainLooper());
        tickRunnable = () -> { vm.refresh(); vm.checkEndOfDay(this::showEod); tickHandler.postDelayed(tickRunnable, 60_000); };
        tickHandler.postDelayed(tickRunnable, 60_000);

        btnCloseEod.setOnClickListener(x -> cardEod.setVisibility(View.GONE));
        btnShareEod.setOnClickListener(x -> shareReport());
    }

    @Override public void onResume() { super.onResume(); vm.refresh(); }

    @Override public void onDestroyView() {
        super.onDestroyView();
        tickHandler.removeCallbacks(tickRunnable);
    }

    // ─────────────────────────────────────────────────────
    private void bindViews(View v) {
        tvClock        = v.findViewById(R.id.tvClock);
        tvStatus       = v.findViewById(R.id.tvStatus);
        tvAnchor       = v.findViewById(R.id.tvAnchor);
        tvBadge        = v.findViewById(R.id.tvBadge);
        tvLead         = v.findViewById(R.id.tvLead);
        tvDeficit      = v.findViewById(R.id.tvDeficit);
        tvUserName     = v.findViewById(R.id.tvUserName);
        tvUserPasses   = v.findViewById(R.id.tvUserPasses);
        tvRivalName    = v.findViewById(R.id.tvRivalName);
        tvRivalPasses  = v.findViewById(R.id.tvRivalPasses);
        tvAliciaHrs    = v.findViewById(R.id.tvAliciaHrs);
        tvAliciaPasses = v.findViewById(R.id.tvAliciaPasses);
        tvBoost        = v.findViewById(R.id.tvBoost);
        tvBoostLabel   = v.findViewById(R.id.tvBoostLabel);
        tvQuip         = v.findViewById(R.id.tvQuip);
        imgRivalPfpSmall = v.findViewById(R.id.imgRivalPfpSmall);
        tvStreakOverlay= v.findViewById(R.id.tvStreakOverlay);
        imgUserPfp     = v.findViewById(R.id.imgUserPfp);
        imgRivalPfp    = v.findViewById(R.id.imgRivalPfp);
        imgChibi       = v.findViewById(R.id.imgChibi);
        barLoad        = v.findViewById(R.id.barLoad);
        barUser        = v.findViewById(R.id.barUser);
        barRival       = v.findViewById(R.id.barRival);
        rvToday        = v.findViewById(R.id.rvToday);
        cardEod        = v.findViewById(R.id.cardEod);
        tvEodDate      = v.findViewById(R.id.tvEodDate);
        tvEodUserCh    = v.findViewById(R.id.tvEodUserCh);
        tvEodRivalCh   = v.findViewById(R.id.tvEodRivalCh);
        tvEodResult    = v.findViewById(R.id.tvEodResult);
        tvEodGap       = v.findViewById(R.id.tvEodGap);
        tvEodBoost     = v.findViewById(R.id.tvEodBoost);
        tvEodBudget    = v.findViewById(R.id.tvEodBudget);
        btnCloseEod    = v.findViewById(R.id.btnCloseEod);
        btnShareEod    = v.findViewById(R.id.btnShareEod);
    }

    private void setupRecycler() {
        todayAdapter = new TodayItemAdapter();
        rvToday.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvToday.setAdapter(todayAdapter);
        rvToday.setNestedScrollingEnabled(false);
        todayAdapter.setListener((itemId, passIdx, spIdx, done) ->
            vm.tickSubPass(itemId, passIdx, spIdx, done, null));
    }

    // ─────────────────────────────────────────────────────
    private void render(DashboardViewModel.DashState ds) {
        if (ds == null || !isAdded()) return;

        tvClock.setText(ds.timeLabel);
        tvAnchor.setText("ANCHOR: " + ds.anchorLabel);

        // Badge + chibi
        String statusLabel = statusString(ds.status);
        tvBadge.setText("ALEXANDRIUS: " + statusLabel);
        setBadgeStyle(ds.status);
        int chibiRes = chibiRes(ds.status);
        Glide.with(this).load(chibiRes).into(imgChibi);

        // Status sub
        tvStatus.setText(ds.dailyBudget > 0
            ? RivalEngine.fmtMins(ds.rivalHrs) + " / " + RivalEngine.fmtMins(ds.dailyBudget)
            : "System Standby");

        // Lead text
        String diffAbs = String.format(Locale.US, "%.1f", Math.abs(ds.diff));
        if (ds.todayItems.isEmpty()) {
            tvLead.setText("NO SCHEDULE — ADD TOPICS");
            tvLead.setTextColor(0xFF8890c0);
        } else if (ds.diff > 0.05f) {
            tvLead.setText("LEADING BY " + diffAbs + " PASSES ▲");
            tvLead.setTextColor(0xFF39ffa0);
        } else if (ds.diff < -0.05f) {
            tvLead.setText("BEHIND BY " + diffAbs + " PASSES ▼");
            tvLead.setTextColor(0xFFff6b6b);
        } else {
            tvLead.setText("DEAD EVEN — TIED");
            tvLead.setTextColor(0xFFffcb47);
        }

        tvDeficit.setText("TODAY: " + ds.totalPasses + " PASSES SCHEDULED" +
            (ds.daysToExam >= 0 ? "  ·  " + ds.daysToExam + " DAYS TO EXAM" : ""));

        // Profiles
        Profiles p = ds.profiles;
        tvUserName.setText(p.user.name);
        tvRivalName.setText(p.alicia.name);
        tvUserPasses.setText(ds.userPasses + " / " + ds.totalPasses);
        tvRivalPasses.setText(String.format(Locale.US, "%.1f", ds.rivalPasses) + " / " + ds.totalPasses);

        // Avatars
        Glide.with(this).load(ds.diff > 0 ? p.user.photoWin : ds.diff < 0 ? p.user.photoLoss : p.user.photoDefault)
             .circleCrop().placeholder(R.drawable.ic_avatar_placeholder).into(imgUserPfp);
        Glide.with(this).load(ds.diff > 0 ? p.alicia.photoLoss : ds.diff < 0 ? p.alicia.photoWin : p.alicia.photoDefault)
             .circleCrop().placeholder(R.drawable.ic_avatar_placeholder).into(imgRivalPfp);

        // Progress bars
        int userPct  = ds.totalPasses > 0 ? (int)(ds.userPasses * 100f / ds.totalPasses) : 0;
        int rivalPct = ds.totalPasses > 0 ? (int)(ds.rivalPasses * 100f / ds.totalPasses) : 0;
        int timePct  = ds.dailyBudget > 0 ? (int)(Math.min(ds.rivalHrs / ds.dailyBudget, 1f) * 100) : 0;
        barUser.setProgressCompat(userPct, true);
        barRival.setProgressCompat(rivalPct, true);
        barLoad.setProgressCompat(timePct, true);

        // Mini stats
        tvAliciaHrs.setText(RivalEngine.fmtMins(ds.rivalHrs));
        tvAliciaPasses.setText(String.format(Locale.US, "%.1f", ds.rivalPasses));
        float boost = ds.rivalBoost;
        tvBoost.setText(String.format(Locale.US, "%.2f×", boost));
        tvBoost.setTextColor(boost >= 1.30f ? 0xFFf87171 : boost >= 1.15f ? 0xFFfb923c : 0xFF8890c0);
        tvBoostLabel.setText(boost >= 1.30f ? "MAX BOOST" : boost >= 1.15f ? "+1 LEAD" : "NO BOOST");

        // Quip
        tvQuip.setText("\u201c" + ds.quip + "\u201d");
        if (imgRivalPfpSmall != null) {
            Glide.with(this).load(ds.profiles.alicia.photoDefault)
                 .circleCrop().placeholder(R.drawable.ic_avatar_placeholder)
                 .into(imgRivalPfpSmall);
        }

        // Streak
        int streak = ds.streak;
        if (streak > 0) {
            StringBuilder flames = new StringBuilder();
            int flameCount = streak >= 100 ? 5 : streak >= 30 ? 4 : streak >= 14 ? 3 : streak >= 7 ? 2 : 1;
            for (int i = 0; i < flameCount; i++) flames.append("🔥");
            tvStreakOverlay.setText(flames + "\n" + streak + "d");
            tvStreakOverlay.setVisibility(View.VISIBLE);
        } else {
            tvStreakOverlay.setVisibility(View.GONE);
        }

        // Today's items
        Map<String,String> tNames = buildTopicNames(ds);
        Map<String,String> cNames = buildChapterNames(ds);
        todayAdapter.setData(ds.todayItems, tNames, cNames);
    }

    private void setBadgeStyle(RivalEngine.AliciaStatus s) {
        int bg, fg, border;
        switch (s) {
            case SLEEPING:    bg=0xFF0d1b2e; fg=0xFF60a5fa; border=0xFF3b82f6; break;
            case EATING:      bg=0xFF2a1a0a; fg=0xFFfb923c; border=0xFFea580c; break;
            case BUFFER:      bg=0xFF1e1a2e; fg=0xFFa78bfa; border=0xFF7c3aed; break;
            case NO_SCHEDULE: bg=0xFF252a45;  fg=0xFF8890c0; border=0xFF252a45; break;
            default:          bg=0xFF0a2a0a; fg=0xFF4ade80; border=0xFF22c55e; break;
        }
        tvBadge.setBackgroundColor(bg);
        tvBadge.setTextColor(fg);
    }

    private String statusString(RivalEngine.AliciaStatus s) {
        switch (s) {
            case SLEEPING:    return "SLEEPING";
            case EATING:      return "EATING";
            case BUFFER:      return "BUFFER";
            case NO_SCHEDULE: return "NO SCHEDULE";
            default:          return "STUDYING";
        }
    }

    private int chibiRes(RivalEngine.AliciaStatus s) {
        switch (s) {
            case SLEEPING:    return R.drawable.chibi_sleeping;
            case EATING:      return R.drawable.chibi_eating;
            default:          return R.drawable.chibi_studying;
        }
    }

    private Map<String,String> buildTopicNames(DashboardViewModel.DashState ds) {
        Map<String,String> m = new HashMap<>();
        // Topics come from profiles subject data — we use item's topicId as key
        // The ViewModel doesn't carry subjects here; best effort from item data
        for (ScheduleItem it : ds.todayItems) m.put(it.topicId, it.topicId);
        return m;
    }
    private Map<String,String> buildChapterNames(DashboardViewModel.DashState ds) {
        Map<String,String> m = new HashMap<>();
        for (ScheduleItem it : ds.todayItems) m.put(it.chapterId, it.chapterId);
        return m;
    }

    // ── EOD Report ────────────────────────────────────────
    private void showEod() {
        DashboardViewModel.DashState ds = vm.getDashState().getValue();
        if (ds == null) return;
        float gap = ds.userPasses - ds.rivalPasses;

        tvEodDate.setText(RivalEngine.today());
        tvEodUserCh.setText(String.valueOf(ds.userPasses));
        tvEodRivalCh.setText(String.format(Locale.US, "%.1f", ds.rivalPasses));
        tvEodGap.setText((gap >= 0 ? "+" : "") + String.format(Locale.US, "%.1f", gap) + " passes");
        tvEodGap.setTextColor(gap >= 0 ? 0xFF39ffa0 : 0xFFff6b6b);
        tvEodBoost.setText(ds.rivalBoost >= 1.15f
            ? String.format(Locale.US, "%.2f× (ACTIVE)", ds.rivalBoost) : "None");
        tvEodBudget.setText(RivalEngine.fmtMins(ds.rivalHrs) + " / " + RivalEngine.fmtMins(ds.dailyBudget));

        boolean noData = ds.userPasses == 0 && ds.rivalPasses == 0;
        if (noData) {
            tvEodResult.setText("— NO ACTIVITY —");
            tvEodResult.setBackgroundColor(0xFF1a1a2e);
        } else if (ds.diff > 0) {
            tvEodResult.setText("🎯 VICTORY ACHIEVED");
            tvEodResult.setBackgroundColor(0xFF14532d);
            tvEodResult.setTextColor(0xFF86efac);
        } else if (ds.diff < 0) {
            tvEodResult.setText("⚠️ DEFEAT ACCEPTED");
            tvEodResult.setBackgroundColor(0xFF7f1d1d);
            tvEodResult.setTextColor(0xFFfca5a5);
        } else {
            tvEodResult.setText("⚖️ PERFECT TIE");
            tvEodResult.setBackgroundColor(0xFF1e3a8a);
            tvEodResult.setTextColor(0xFF93c5fd);
        }
        cardEod.setVisibility(View.VISIBLE);
    }

    private void shareReport() {
        DashboardViewModel.DashState ds = vm.getDashState().getValue();
        if (ds == null) return;
        // Build canvas PNG
        int W = 700, H = 400;
        Bitmap bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Background
        p.setColor(0xFF0d0f1a); canvas.drawRect(0,0,W,H,p);
        p.setColor(0xFF7c6fff); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
        canvas.drawRect(6,6,W-6,H-6,p); p.setStyle(Paint.Style.FILL);

        // Title
        p.setColor(0xFF8890c0); p.setTextSize(14); p.setFakeBoldText(true);
        canvas.drawText("OMEGA PROTOCOL — END OF DAY REPORT", 30, 50, p);
        p.setColor(0xFFffcb47); p.setTextSize(22);
        canvas.drawText(RivalEngine.today(), 30, 85, p);

        // Stats
        drawStat(canvas, p, "YOUR PASSES",   String.valueOf(ds.userPasses),           0xFF39ffa0, 30,  125);
        drawStat(canvas, p, "ALEXANDRIUS",   String.format(Locale.US,"%.1f",ds.rivalPasses), 0xFF7c6fff, 240, 125);
        float gap = ds.userPasses - ds.rivalPasses;
        drawStat(canvas, p, "GAP",
            (gap>=0?"+":"")+String.format(Locale.US,"%.1f",gap),
            gap>=0?0xFF39ffa0:0xFFff6b6b, 450, 125);

        // Divider
        p.setColor(0x33ffffff); p.setStrokeWidth(1); p.setStyle(Paint.Style.STROKE);
        canvas.drawLine(30,210,W-30,210,p); p.setStyle(Paint.Style.FILL);

        // Quip
        p.setColor(0xFFe8eaff); p.setTextSize(13); p.setFakeBoldText(false);
        canvas.drawText(ds.quip.length() > 80 ? ds.quip.substring(0,80)+"…" : ds.quip, 40, 245, p);

        // Footer
        p.setColor(0x558890c0); p.setTextSize(10);
        canvas.drawText("Protocol Omega — StudyRival", 30, H-20, p);

        // Save to Downloads
        try {
            java.io.File dir = requireContext().getExternalFilesDir(null);
            java.io.File f   = new java.io.File(dir, "omega_report_" + System.currentTimeMillis() + ".png");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
                bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            // Share
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), requireContext().getPackageName() + ".fileprovider", f);
            android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(share, "Share Report"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void drawStat(Canvas canvas, Paint p, String label, String val, int color, float x, float y) {
        p.setColor(0xFF8890c0); p.setTextSize(10); p.setFakeBoldText(true);
        canvas.drawText(label, x, y, p);
        p.setColor(color); p.setTextSize(36);
        canvas.drawText(val, x, y + 42, p);
    }
}
