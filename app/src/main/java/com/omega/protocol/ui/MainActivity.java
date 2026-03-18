package com.omega.protocol.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.*;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.omega.protocol.R;
import com.omega.protocol.data.CosmeticsRepository;
import com.omega.protocol.db.OmegaRepository;
import com.omega.protocol.worker.RivalTickWorker;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout   drawer;
    private NavController  navController;
    private ParticleView   particleView;
    private SakuraView     sakuraView;
    private AppBarConfiguration appBarConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyThemeFromPrefs();
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Navigation
        drawer = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navView);
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        navController = navHost.getNavController();

        appBarConfig = new AppBarConfiguration.Builder(
            R.id.dashboardFragment, R.id.scheduleFragment, R.id.logFragment,
            R.id.syllabusFragment, R.id.archiveFragment, R.id.profileFragment,
            R.id.cosmeticsFragment, R.id.simulationFragment, R.id.syncFragment)
            .setOpenableLayout(drawer).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig);
        NavigationUI.setupWithNavController(navView, navController);

        // Particle view
        particleView = findViewById(R.id.particleView);
        sakuraView   = findViewById(R.id.sakuraView);
        particleView.post(() -> startParticlesIfEnabled());
        sakuraView.post(()   -> startSakuraIfEnabled());

        // Header avatar update
        View header = navView.getHeaderView(0);
        updateDrawerHeader(header);

        // Background rival tick
        RivalTickWorker.schedule(this);
    }

    @Override public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfig) || super.onSupportNavigateUp();
    }

    @Override public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    // ── Particle helpers (called from CosmeticsFragment) ──
    public void setParticlesEnabled(boolean enabled) {
        if (enabled) particleView.startParticles(this);
        else         particleView.stopParticles();
    }
    public void restartParticles() {
        particleView.stopParticles();
        particleView.startParticles(this);
    }
    public void setSakuraEnabled(boolean enabled) {
        if (enabled) sakuraView.startSakura(this);
        else         sakuraView.stopSakura();
    }
    public void updateParticleScore(int userPasses, float rivalPasses) {
        particleView.setScoreState(userPasses, rivalPasses);
    }

    private void startParticlesIfEnabled() {
        CosmeticsRepository repo = CosmeticsRepository.get(this);
        if (repo.particlesEnabled()) particleView.startParticles(this);
        else particleView.setVisibility(android.view.View.GONE);
    }

    private void startSakuraIfEnabled() {
        CosmeticsRepository repo = CosmeticsRepository.get(this);
        if (repo.sakuraEnabled()) sakuraView.startSakura(this);
        else sakuraView.setVisibility(android.view.View.GONE);
    }

    private void applyThemeFromPrefs() {
        CosmeticsRepository repo = CosmeticsRepository.get(this);
        int style;
        switch (repo.getTheme()) {
            case CosmeticsRepository.THEME_OCEAN:  style = R.style.Theme_Omega_Ocean;  break;
            case CosmeticsRepository.THEME_SUNSET: style = R.style.Theme_Omega_Sunset; break;
            case CosmeticsRepository.THEME_FOREST: style = R.style.Theme_Omega_Forest; break;
            default:                                style = R.style.Theme_Omega;        break;
        }
        setTheme(style);
    }

    private void updateDrawerHeader(View header) {
        // Async profile load
        new Thread(() -> {
            OmegaRepository repo = OmegaRepository.get(this);
            com.omega.protocol.model.Profiles p = repo.getProfilesSync();
            runOnUiThread(() -> {
                TextView tvUserName  = header.findViewById(R.id.navHeaderUserName);
                TextView tvRivalName = header.findViewById(R.id.navHeaderRivalName);
                if (tvUserName  != null) tvUserName.setText(p.user.name);
                if (tvRivalName != null) tvRivalName.setText(p.alicia.name);
                de.hdodenhof.circleimageview.CircleImageView ivUser  = header.findViewById(R.id.navHeaderUserAvatar);
                de.hdodenhof.circleimageview.CircleImageView ivRival = header.findViewById(R.id.navHeaderRivalAvatar);
                if (ivUser  != null) Glide.with(MainActivity.this).load(p.user.photoDefault).circleCrop().placeholder(R.drawable.ic_avatar_placeholder).into(ivUser);
                if (ivRival != null) Glide.with(MainActivity.this).load(p.alicia.photoDefault).circleCrop().placeholder(R.drawable.ic_avatar_placeholder).into(ivRival);
            });
        }).start();
    }
}
