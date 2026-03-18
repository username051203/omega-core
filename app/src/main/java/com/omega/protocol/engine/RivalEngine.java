package com.omega.protocol.engine;

import com.omega.protocol.db.entity.*;
import com.omega.protocol.model.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RivalEngine {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Random RNG = new Random();

    /* ── Date helpers ──────────────────────────────────── */
    public static String today() { return LocalDate.now().format(DATE_FMT); }
    public static String addDays(String d, int n) {
        return LocalDate.parse(d, DATE_FMT).plusDays(n).format(DATE_FMT);
    }
    public static long daysToExam(EngineConfig cfg) {
        if (cfg.examDate == null || cfg.examDate.isEmpty()) return -1;
        return LocalDate.now().until(LocalDate.parse(cfg.examDate, DATE_FMT), java.time.temporal.ChronoUnit.DAYS);
    }
    public static String anchorLabel(EngineConfig cfg) {
        float h = cfg.anchorHour;
        return String.format(Locale.US, "%02d:%02d", (int)h, (int)((h%1)*60));
    }
    public static String nowTimeLabel() {
        LocalTime t = LocalTime.now();
        return String.format(Locale.US, "%02d:%02d", t.getHour(), t.getMinute());
    }

    /* ── Status ────────────────────────────────────────── */
    public enum AliciaStatus { STUDYING, SLEEPING, EATING, BUFFER, NO_SCHEDULE }

    public static AliciaStatus status(List<ScheduleItem> todayItems, Routine rt) {
        if (todayItems.isEmpty()) return AliciaStatus.NO_SCHEDULE;
        float h = LocalTime.now().toSecondOfDay() / 3600f;
        if (rt == null) rt = new Routine();
        for (Routine.TimeRange r : rt.slp) if (inRange(r.s, r.e, h)) return AliciaStatus.SLEEPING;
        for (Routine.TimeRange r : rt.bf)  if (inRange(r.s, r.e, h)) return AliciaStatus.BUFFER;
        for (float m : rt.ml) if (inRange(m, m + 0.75f, h)) return AliciaStatus.EATING;
        return AliciaStatus.STUDYING;
    }

    public static int statusChibi(AliciaStatus s) {
        // Returns resource name key; actual R.drawable resolved in UI
        switch (s) {
            case SLEEPING:    return 1;
            case EATING:      return 2;
            default:          return 0; // studying
        }
    }

    /* ── Today items from DB ────────────────────────────── */
    public static List<ScheduleItem> todayItemsFrom(
            Map<String, List<String>> schedule,
            List<ScheduleItem> allItems) {
        String t = today();
        List<String> ids = schedule.getOrDefault(t, Collections.emptyList());
        List<ScheduleItem> result = new ArrayList<>();
        for (String id : ids) {
            for (ScheduleItem it : allItems) {
                if (it.id.equals(id)) { result.add(it); break; }
            }
        }
        return result;
    }

    public static ScheduleItem findById(List<ScheduleItem> items, String id) {
        for (ScheduleItem it : items) if (it.id.equals(id)) return it;
        return null;
    }

    public static int userPassesToday(List<ScheduleItem> todayItems) {
        int c = 0; for (ScheduleItem it : todayItems) c += it.doneCount(); return c;
    }

    /* ── Rival tick (call on background thread) ─────────── */
    public static void tick(RivalState rival, Routine rt,
                             List<ScheduleItem> todayItems, EngineConfig cfg) {
        float elapsedHrs = LocalTime.now().toSecondOfDay() / 3600f;
        if (rt == null) rt = new Routine();
        float blocked = 0f;
        for (Routine.TimeRange r : rt.slp) blocked += blockedSoFar(r.s, r.e, elapsedHrs);
        for (Routine.TimeRange r : rt.bf)  blocked += blockedSoFar(r.s, r.e, elapsedHrs);
        for (float m : rt.ml)              blocked += blockedSoFar(m, m + 0.75f, elapsedHrs);
        float aliciaHrs = Math.max(0f, elapsedHrs - blocked);
        rival.hrsWorked = aliciaHrs;
        int uc = userPassesToday(todayItems);
        float lead = uc - rival.cur;
        float boost = 1.0f;
        if      (lead >= 2.5f) boost = 1.30f;
        else if (lead >= 1.0f) boost = 1.15f;
        rival.boost = boost;
        rival.cur   = aliciaPassCount(todayItems, aliciaHrs * boost);
    }

    public static float aliciaPassCount(List<ScheduleItem> items, float hrsWorked) {
        float remaining = hrsWorked, passes = 0f;
        for (ScheduleItem item : items) {
            float ih = item.totalHrs(); int ip = item.totalCount();
            if (ih == 0) continue;
            if (remaining >= ih) { remaining -= ih; passes += ip; }
            else { passes += (remaining / ih) * ip; break; }
        }
        return passes;
    }

    public static float availableHrs(Routine rt) {
        if (rt == null) rt = new Routine();
        float b = 0f;
        for (Routine.TimeRange r : rt.slp) b += rangeLen(r.s, r.e);
        for (Routine.TimeRange r : rt.bf)  b += rangeLen(r.s, r.e);
        for (float m : rt.ml)              b += 0.75f;
        return Math.max(0f, 24f - b);
    }

    /* ── Schedule generator ─────────────────────────────── */
    public static Map<String, List<String>> generateSchedule(
            Map<String, List<String>> existing,
            List<ScheduleItem> allItems,
            float dailyHrs,
            String fromDay) {

        String today = today();
        Map<String, List<String>> preserved = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : existing.entrySet())
            if (e.getKey().compareTo(today) <= 0)
                preserved.put(e.getKey(), new ArrayList<>(e.getValue()));

        Set<String> todayIds = new HashSet<>(preserved.getOrDefault(today, Collections.emptyList()));
        List<ScheduleItem> incomplete = new ArrayList<>();
        for (ScheduleItem it : allItems)
            if (!it.isComplete() && !todayIds.contains(it.id)) incomplete.add(it);

        List<ScheduleItem> pinned = new ArrayList<>(), unpinned = new ArrayList<>();
        for (ScheduleItem it : incomplete) {
            if (it.pinDate != null && it.pinDate.compareTo(fromDay) >= 0) pinned.add(it);
            else if (it.pinDate == null) unpinned.add(it);
        }
        unpinned.sort(Comparator.comparingInt(a -> a.priority));

        Map<String, List<String>> schedule = new LinkedHashMap<>(preserved);
        Map<String, Float> used = new HashMap<>();

        for (ScheduleItem item : pinned) {
            String d = item.pinDate;
            float u  = used.getOrDefault(d, 0f);
            if (!schedule.containsKey(d)) schedule.put(d, new ArrayList<>());
            if (u + item.totalHrs() <= dailyHrs + 0.001f && !schedule.get(d).contains(item.id)) {
                schedule.get(d).add(item.id);
                used.put(d, u + item.totalHrs());
            }
        }

        String day = fromDay;
        List<ScheduleItem> rem = new ArrayList<>(unpinned);
        int safety = 0;
        while (!rem.isEmpty() && safety++ < 400) {
            float pinnedHrs = 0f;
            for (String id : schedule.getOrDefault(day, Collections.emptyList())) {
                ScheduleItem it = findById(allItems, id);
                if (it != null && it.pinDate != null) pinnedHrs += it.totalHrs();
            }
            float slot = dailyHrs - pinnedHrs;
            if (slot <= 0.01f) { day = addDays(day, 1); continue; }
            if (!schedule.containsKey(day)) schedule.put(day, new ArrayList<>());
            boolean placed = true;
            while (placed && slot > 0.01f && !rem.isEmpty()) {
                placed = false;
                int best = -1;
                for (int i = 0; i < rem.size(); i++) {
                    float h = rem.get(i).totalHrs();
                    if (h <= slot + 0.001f && (best == -1 || h > rem.get(best).totalHrs())) best = i;
                }
                if (best >= 0) {
                    ScheduleItem it = rem.remove(best);
                    schedule.get(day).add(it.id);
                    slot -= it.totalHrs(); placed = true;
                }
            }
            day = addDays(day, 1);
        }
        return schedule;
    }

    /* ── Streak ─────────────────────────────────────────── */
    public static int calcStreak(List<String> daysWithPasses) {
        if (daysWithPasses.isEmpty()) return 0;
        String today = today();
        int streak = 0;
        for (int i = 0; i < 365; i++) {
            String d = i == 0 ? today : addDays(today, -i);
            if (daysWithPasses.contains(d)) streak++;
            else if (i > 0) break;
        }
        return streak;
    }

    /* ── Archive ────────────────────────────────────────── */
    public static DaySnapshotEntity buildSnapshot(
            String day, int userCh, float aliciaCh,
            int totalPasses, float boost, float hrsUsed, float budget) {
        DaySnapshotEntity s = new DaySnapshotEntity();
        s.dayStr      = day;
        s.userCh      = userCh;
        s.aliciaCh    = aliciaCh;
        s.win         = (userCh == 0 && aliciaCh == 0) ? null : userCh >= aliciaCh;
        s.totalPasses = totalPasses;
        s.boost       = boost;
        s.hrsUsed     = hrsUsed;
        s.budget      = budget;
        return s;
    }

    /* ── Quips ──────────────────────────────────────────── */
    private static final Map<String, String[]> QUIPS = new HashMap<>();
    static {
        QUIPS.put("startup", new String[]{"Another day. Let's see if you can keep up.",
            "I was already studying when you opened this.",
            "Good morning. I've been at it for an hour.",
            "You showed up. That's step one."});
        QUIPS.put("winning", new String[]{"You're ahead. Enjoy it while it lasts.",
            "Nice lead. Don't let me catch up — I will.",
            "Impressive. But I'm not slowing down."});
        QUIPS.put("losing",  new String[]{"I'm ahead. That's where I belong.",
            "You're behind. You know what to do.",
            "The gap is real. Close it."});
        QUIPS.put("tie",     new String[]{"Tied. One of us blinks first.",
            "Dead even. I find that unacceptable.",
            "Same score. This gets settled today."});
        QUIPS.put("tick",    new String[]{"Logged. Now do the next one.",
            "Good. Now don't celebrate — there's more.",
            "That counted. Keep moving.", "Check. What's next?"});
        QUIPS.put("eod_win", new String[]{"You beat me today. I'll remember that.",
            "Fine. Today was yours. Tomorrow is mine.",
            "Well earned. Don't get used to it."});
        QUIPS.put("eod_loss",new String[]{"Better luck tomorrow. I'll be here.",
            "Today was mine. You already knew that.",
            "I wasn't even trying my hardest today."});
        QUIPS.put("eod_tie", new String[]{"A tie. We'll have to settle this tomorrow.",
            "Neither of us won. I find that mildly offensive."});
        QUIPS.put("eod_none",new String[]{"Neither of us did anything today. That's not great.",
            "Zero passes. The exam doesn't take days off."});
    }
    public static String quip(String ctx) {
        String[] pool = QUIPS.getOrDefault(ctx, QUIPS.get("startup"));
        return pool[RNG.nextInt(pool.length)];
    }

    /* ── Private helpers ────────────────────────────────── */
    private static boolean inRange(float s, float e, float h) {
        return e < s ? (h >= s || h < e) : (h >= s && h < e);
    }
    private static float blockedSoFar(float s, float e, float elapsed) {
        if (e < s) { return Math.max(0, Math.min(24,elapsed)-s) + Math.max(0, Math.min(e,elapsed)); }
        return Math.max(0, Math.min(e, elapsed) - s);
    }
    private static float rangeLen(float s, float e) { return e < s ? 24f-s+e : e-s; }

    /** Display hours as "45m", "1h 30m", "2h" etc — matches index-2 fmtMins() */
    public static String fmtMins(float hours) {
        int totalMins = Math.round(hours * 60);
        if (totalMins <= 0) return "0m";
        int h = totalMins / 60;
        int m = totalMins % 60;
        if (h == 0) return m + "m";
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }
}
