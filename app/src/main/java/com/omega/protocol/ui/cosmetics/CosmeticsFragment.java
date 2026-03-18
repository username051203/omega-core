package com.omega.protocol.ui.cosmetics;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.omega.protocol.R;
import com.omega.protocol.data.CosmeticsRepository;
import java.util.Locale;

public class CosmeticsFragment extends Fragment {

    private CosmeticsRepository repo;
    private Button btnNeon, btnOcean, btnSunset, btnForest;
    private com.skydoves.colorpickerview.ColorPickerView colorPicker;
    private TextView tvAccentHex;
    private Button btnApplyAccent;
    private int lastPickedColor = 0xFF7C6FFF;
    private Switch swParticles, swStreak, swSmoothAnim;
    private RadioGroup rgDensity;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_cosmetics, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        repo = CosmeticsRepository.get(requireContext());
        bindViews(v);
        loadCurrentSettings();
        setupListeners();
    }

    private void bindViews(View v) {
        btnNeon        = v.findViewById(R.id.btnThemeNeon);
        btnOcean       = v.findViewById(R.id.btnThemeOcean);
        btnSunset      = v.findViewById(R.id.btnThemeSunset);
        btnForest      = v.findViewById(R.id.btnThemeForest);
        colorPicker    = v.findViewById(R.id.colorPicker);
        tvAccentHex    = v.findViewById(R.id.tvAccentHex);
        btnApplyAccent = v.findViewById(R.id.btnApplyAccent);
        swParticles    = v.findViewById(R.id.swParticles);
        swStreak       = v.findViewById(R.id.swStreak);
        swSmoothAnim   = v.findViewById(R.id.swSmoothAnim);
        rgDensity      = v.findViewById(R.id.rgDensity);
    }

    private void loadCurrentSettings() {
        swParticles.setChecked(repo.particlesEnabled());
        swStreak.setChecked(repo.streakFxEnabled());
        swSmoothAnim.setChecked(repo.smoothAnimEnabled());
        int id;
        switch (repo.particleDensity()) {
            case "low":  id = R.id.rbDensityLow;  break;
            case "high": id = R.id.rbDensityHigh; break;
            default:     id = R.id.rbDensityMed;  break;
        }
        rgDensity.check(id);
        if (repo.hasCustomAccent()) {
            int c = repo.getAccentColor();
            tvAccentHex.setText(String.format("#%06X", 0xFFFFFF & c));
        }
        highlightActiveTheme();
    }

    private void highlightActiveTheme() {
        String theme = repo.getTheme();
        int active = 0xFFffffff, inactive = 0xFF444466;
        btnNeon.setTextColor(theme.equals(CosmeticsRepository.THEME_NEON)    ? active : inactive);
        btnOcean.setTextColor(theme.equals(CosmeticsRepository.THEME_OCEAN)  ? active : inactive);
        btnSunset.setTextColor(theme.equals(CosmeticsRepository.THEME_SUNSET)? active : inactive);
        btnForest.setTextColor(theme.equals(CosmeticsRepository.THEME_FOREST)? active : inactive);
    }

    private void setupListeners() {
        btnNeon.setOnClickListener(v   -> applyTheme(CosmeticsRepository.THEME_NEON));
        btnOcean.setOnClickListener(v  -> applyTheme(CosmeticsRepository.THEME_OCEAN));
        btnSunset.setOnClickListener(v -> applyTheme(CosmeticsRepository.THEME_SUNSET));
        btnForest.setOnClickListener(v -> applyTheme(CosmeticsRepository.THEME_FOREST));

        colorPicker.setColorListener(new com.skydoves.colorpickerview.listeners.ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                lastPickedColor = color;
                tvAccentHex.setText(String.format("#%06X", 0xFFFFFF & color));
            }
        });

        btnApplyAccent.setOnClickListener(v -> {
            int color = lastPickedColor;
            repo.setAccentColor(color);
            // Apply to current activity theme overlay
            requireActivity().getTheme().applyStyle(R.style.CustomAccentOverlay, true);
            Toast.makeText(requireContext(), "Accent applied — restart app to see full effect", Toast.LENGTH_SHORT).show();
        });

        swParticles.setOnCheckedChangeListener((btn, checked) -> {
            repo.setParticles(checked);
            // Notify MainActivity to toggle particle canvas
            if (getActivity() instanceof com.omega.protocol.ui.MainActivity) {
                ((com.omega.protocol.ui.MainActivity)getActivity()).setParticlesEnabled(checked);
            }
        });

        swStreak.setOnCheckedChangeListener((btn, checked) -> repo.setStreakFx(checked));
        swSmoothAnim.setOnCheckedChangeListener((btn, checked) -> repo.setSmoothAnim(checked));

        // Sakura
        Switch swSakura = requireView().findViewById(R.id.swSakura);
        if (swSakura != null) {
            swSakura.setChecked(repo.sakuraEnabled());
            swSakura.setOnCheckedChangeListener((btn, checked) -> {
                repo.setSakura(checked);
                if (getActivity() instanceof com.omega.protocol.ui.MainActivity) {
                    ((com.omega.protocol.ui.MainActivity) getActivity()).setSakuraEnabled(checked);
                }
            });
        }

        rgDensity.setOnCheckedChangeListener((grp, id) -> {
            if (id == R.id.rbDensityLow)       repo.setParticleDensity("low");
            else if (id == R.id.rbDensityHigh) repo.setParticleDensity("high");
            else                               repo.setParticleDensity("medium");
            if (getActivity() instanceof com.omega.protocol.ui.MainActivity) {
                ((com.omega.protocol.ui.MainActivity)getActivity()).restartParticles();
            }
        });
    }

    private void applyTheme(String theme) {
        repo.setTheme(theme);
        highlightActiveTheme();
        Toast.makeText(requireContext(), "Theme saved — restart app to apply fully", Toast.LENGTH_SHORT).show();
    }
}
