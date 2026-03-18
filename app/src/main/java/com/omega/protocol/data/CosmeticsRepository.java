package com.omega.protocol.data;

import android.content.Context;
import android.content.SharedPreferences;

public class CosmeticsRepository {
    private static final String PREFS = "omega_cosmetics";
    private static CosmeticsRepository sInstance;
    private final SharedPreferences prefs;

    public static final String THEME_NEON   = "neon_pulse";
    public static final String THEME_OCEAN  = "midnight_ocean";
    public static final String THEME_SUNSET = "sunset_glow";
    public static final String THEME_FOREST = "forest_dew";

    private CosmeticsRepository(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public static CosmeticsRepository get(Context ctx) {
        if (sInstance == null) sInstance = new CosmeticsRepository(ctx);
        return sInstance;
    }
    public String  getTheme()              { return prefs.getString("theme", THEME_NEON); }
    public void    setTheme(String t)      { prefs.edit().putString("theme", t).apply(); }
    public int     getAccentColor()        { return prefs.getInt("accent_color", 0xFF7C6FFF); }
    public void    setAccentColor(int c)   { prefs.edit().putInt("accent_color", c).apply(); }
    public boolean hasCustomAccent()       { return prefs.contains("accent_color"); }
    public boolean particlesEnabled()      { return prefs.getBoolean("particles", true); }
    public void    setParticles(boolean v) { prefs.edit().putBoolean("particles", v).apply(); }
    public String  particleDensity()       { return prefs.getString("particle_density","medium"); }
    public void    setParticleDensity(String d) { prefs.edit().putString("particle_density",d).apply(); }
    public boolean streakFxEnabled()       { return prefs.getBoolean("streak_fx", true); }
    public void    setStreakFx(boolean v)  { prefs.edit().putBoolean("streak_fx", v).apply(); }
    public boolean smoothAnimEnabled()     { return prefs.getBoolean("smooth_anim", true); }
    public void    setSmoothAnim(boolean v){ prefs.edit().putBoolean("smooth_anim", v).apply(); }
    public boolean sakuraEnabled()         { return prefs.getBoolean("sakura", false); }
    public void    setSakura(boolean v)    { prefs.edit().putBoolean("sakura", v).apply(); }
}
