package com.omega.protocol.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.omega.protocol.data.CosmeticsRepository;
import java.util.*;

public class ParticleView extends View {

    private static final class Particle {
        float x, y, r, vx, vy, alpha;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int     particleColor = 0xFF7C6FFF;
    private boolean running       = false;

    public ParticleView(Context ctx) { super(ctx); init(); }
    public ParticleView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public ParticleView(Context ctx, AttributeSet a, int d) { super(ctx, a, d); init(); }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void startParticles(Context ctx) {
        CosmeticsRepository repo = CosmeticsRepository.get(ctx);
        if (!repo.particlesEnabled()) { setVisibility(GONE); return; }
        setVisibility(VISIBLE);
        int count = densityCount(repo.particleDensity());
        particles.clear();
        Random rng = new Random();
        int w = getWidth()  > 0 ? getWidth()  : 1080;
        int h = getHeight() > 0 ? getHeight() : 2400;
        for (int i = 0; i < count; i++) {
            Particle p  = new Particle();
            p.x         = rng.nextFloat() * w;
            p.y         = rng.nextFloat() * h;
            p.r         = 1.5f + rng.nextFloat() * 2.5f;
            p.vx        = (rng.nextFloat() - 0.5f) * 0.5f;
            p.vy        = (rng.nextFloat() - 0.5f) * 0.5f;
            p.alpha     = 0.2f + rng.nextFloat() * 0.5f;
            particles.add(p);
        }
        running = true;
        postInvalidateOnAnimation();
    }

    public void stopParticles() {
        running = false;
        setVisibility(GONE);
    }

    public void setScoreState(int userPasses, float rivalPasses) {
        if      (userPasses   > rivalPasses + 0.5f) particleColor = 0xFF39ffa0;
        else if (rivalPasses  > userPasses   + 0.5f) particleColor = 0xFFff6b6b;
        else                                          particleColor = 0xFF7C6FFF;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!running || particles.isEmpty()) return;
        int w = getWidth(), h = getHeight();
        for (Particle p : particles) {
            p.x += p.vx; p.y += p.vy;
            if (p.x < 0) p.x = w; else if (p.x > w) p.x = 0;
            if (p.y < 0) p.y = h; else if (p.y > h) p.y = 0;
            paint.setColor(particleColor);
            paint.setAlpha((int)(p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.r, paint);
        }
        paint.setAlpha(255);
        if (running) postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (running && !particles.isEmpty()) {
            for (Particle p : particles) {
                p.x *= (float) w / Math.max(ow, 1);
                p.y *= (float) h / Math.max(oh, 1);
            }
        }
    }

    private int densityCount(String d) {
        switch (d) {
            case "low":  return 25;
            case "high": return 100;
            default:     return 55;
        }
    }
}
