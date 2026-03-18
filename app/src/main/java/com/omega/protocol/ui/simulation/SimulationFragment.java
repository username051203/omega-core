package com.omega.protocol.ui.simulation;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.omega.protocol.R;
import com.omega.protocol.viewmodel.SimulationViewModel;
import java.util.Locale;

public class SimulationFragment extends Fragment {

    private SimulationViewModel vm;
    private TextView tvSimClock, tvSimUser, tvSimRival;
    private Button btnStart, btnStop, btnReset;
    private Spinner spinnerSpeed;
    private TimePicker timePicker;
    private ScrollView svLog;
    private TextView tvSimLog;
    private StringBuilder logBuf = new StringBuilder();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle s) {
        return i.inflate(R.layout.fragment_simulation, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        tvSimClock  = v.findViewById(R.id.tvSimClock);
        tvSimUser   = v.findViewById(R.id.tvSimUser);
        tvSimRival  = v.findViewById(R.id.tvSimRival);
        btnStart    = v.findViewById(R.id.btnSimStart);
        btnStop     = v.findViewById(R.id.btnSimStop);
        btnReset    = v.findViewById(R.id.btnSimReset);
        spinnerSpeed= v.findViewById(R.id.spinnerSimSpeed);
        timePicker  = v.findViewById(R.id.simTimePicker);
        tvSimLog    = v.findViewById(R.id.tvSimLog);
        svLog       = v.findViewById(R.id.svSimLog);
        timePicker.setIs24HourView(true);
        timePicker.setHour(8); timePicker.setMinute(0);

        // Speed options
        String[] speeds = {"1 min/sec","5 min/sec","15 min/sec","30 min/sec","1 hr/sec"};
        int[]    mults  = {60, 300, 900, 1800, 3600};
        ArrayAdapter<String> sa = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, speeds);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(sa);
        spinnerSpeed.setSelection(2); // 15 min/sec default

        vm = new ViewModelProvider(this).get(SimulationViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), state -> {
            tvSimClock.setText(state.timeLabel);
            tvSimUser.setText(String.valueOf(state.userPasses));
            tvSimRival.setText(String.format(java.util.Locale.US, "%.1f", state.rivalPasses));
            btnStart.setEnabled(!state.running);
            btnStop.setEnabled(state.running);
            if (state.anchorHit) appendLog("ANCHOR HIT " + state.timeLabel +
                " — You:" + state.userPasses + " Alex:" +
                String.format(java.util.Locale.US,"%.1f",state.rivalPasses));
        });

        btnStart.setOnClickListener(x -> {
            int sel  = spinnerSpeed.getSelectedItemPosition();
            vm.setSpeed(mults[sel]);
            vm.start(timePicker.getHour(), timePicker.getMinute());
            appendLog("SIM START → " + String.format("%02d:%02d", timePicker.getHour(), timePicker.getMinute()));
        });
        btnStop.setOnClickListener(x -> { vm.stop(); appendLog("SIM PAUSED"); });
        btnReset.setOnClickListener(x -> { vm.reset(); logBuf.setLength(0); tvSimLog.setText(""); appendLog("RESET"); });
    }

    @Override public void onDestroyView() { super.onDestroyView(); vm.stop(); }

    private void appendLog(String msg) {
        logBuf.insert(0, "› " + msg + "\n");
        if (logBuf.length() > 2000) logBuf.setLength(2000);
        tvSimLog.setText(logBuf.toString());
    }
}
