package com.omega.protocol.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.omega.protocol.data.CosmeticsRepository;
import java.util.*;

public class SakuraView extends View {

    private static final class Petal {
        float x, y, size, speed, sway, swayOffset, swaySpeed, rotation, rotSpeed, alpha;
        int tick;
    }

    private final List<Petal> petals = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean running = false;
    private static final int[] COLOURS = {
        0xFFFFB7C5, 0xFFFFD7E0, 0xFFFF99AA, 0xFFFFEEF2, 0xFFFFCCD5, 0xFFFFAABB
    };

    public SakuraView(Context ctx) { super(ctx); }
    public SakuraView(Context ctx, AttributeSet a) { super(ctx, a); }
    public SakuraView(Context ctx, AttributeSet a, int d) { super(ctx, a, d); }

    public void startSakura(Context ctx) {
        CosmeticsRepository repo = CosmeticsRepository.get(ctx);
        if (!repo.sakuraEnabled()) { setVisibility(GONE); return; }
        setVisibility(VISIBLE);
        petals.clear();
        Random rng = new Random();
        int w = Math.max(getWidth(), 1080), h = Math.max(getHeight(), 2400);
        int count = repo.particleDensity().equals("low") ? 15
                  : repo.particleDensity().equals("high") ? 45 : 25;
        for (int i = 0; i < count; i++) petals.add(spawn(rng, w, h, true));
        running = true;
        postInvalidateOnAnimation();
    }

    public void stopSakura() { running = false; setVisibility(GONE); }

    private Petal spawn(Random rng, int w, int h, boolean rndY) {
        Petal p = new Petal();
        p.x = rng.nextFloat() * w; p.y = rndY ? rng.nextFloat() * h : -20f;
        p.size = 6f + rng.nextFloat() * 10f; p.speed = 0.8f + rng.nextFloat() * 1.2f;
        p.sway = 20f + rng.nextFloat() * 40f;
        p.swayOffset = rng.nextFloat() * 6.28f; p.swaySpeed = 0.008f + rng.nextFloat() * 0.012f;
        p.rotation = rng.nextFloat() * 360f; p.rotSpeed = (rng.nextFloat() - 0.5f) * 2f;
        p.alpha = 0.5f + rng.nextFloat() * 0.45f; p.tick = rng.nextInt(200);
        return p;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!running || petals.isEmpty()) return;
        int w = getWidth(), h = getHeight();
        Random rng = new Random();
        for (Petal p : petals) {
            p.tick++; p.y += p.speed;
            p.x += (float)(Math.sin(p.swayOffset + p.tick * p.swaySpeed) * p.sway * 0.015f);
            p.rotation += p.rotSpeed;
            if (p.y > h + 30) { p.y = -20f; p.x = rng.nextFloat() * w; }
            canvas.save(); canvas.translate(p.x, p.y); canvas.rotate(p.rotation);
            paint.setColor(COLOURS[Math.abs(p.tick) % COLOURS.length]);
            paint.setAlpha((int)(p.alpha * 200)); paint.setStyle(Paint.Style.FILL);
            canvas.drawOval(-p.size, -p.size * 0.55f, p.size, p.size * 0.55f, paint);
            canvas.restore();
        }
        paint.setAlpha(255);
        if (running) postInvalidateOnAnimation();
    }
}
