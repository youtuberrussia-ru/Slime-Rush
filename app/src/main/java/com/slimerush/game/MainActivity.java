package com.slimerush.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.*;
import android.content.SharedPreferences;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.view.WindowManager;
import java.util.Random;

public class MainActivity extends Activity {

    private GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        game = new GameView();
        setContentView(game);
    }

    @Override
    public void onBackPressed() {
        if (game == null || !game.handleBack()) super.onBackPressed();
    }

    @Override
    protected void onPause() {
        if (game != null) game.pauseForLifecycle();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (game != null) game.release();
        super.onDestroy();
    }

    private class GameView extends View {
        private static final int MENU = 0, PLAY = 1, SHOP = 2,
                OVER = 3, PAUSE = 4, SETTINGS = 5;

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Typeface font = Typeface.create("sans-serif-rounded", Typeface.BOLD);
        private final Random random = new Random();
        private final SharedPreferences prefs;
        private ToneGenerator sound;

        private int state = MENU;
        private int coins, best, score, skin;
        private boolean soundOn, particlesOn;
        private final boolean[] owned = new boolean[5];

        private final int[] skins = {
                Color.rgb(66, 245, 125),
                Color.rgb(50, 174, 255),
                Color.rgb(255, 104, 185),
                Color.rgb(255, 207, 58),
                Color.rgb(177, 112, 255)
        };
        private final String[] skinNames = {
                "MINT", "OCEAN", "BUBBLEGUM", "GOLD", "GALAXY"
        };
        private final int[] prices = {0, 50, 100, 200, 350};

        private float slimeX = 83, slimeY, velocity;
        private final float ground = 650;
        private float obstacleX, obstacleW = 58;
        private float coinX, coinY;
        private float speed, animation, sceneryOffset;
        private float shake;
        private float scoreAcc;
        private int obstacleType;
        private boolean jumping, shield;
        private float shieldTime;
        private long lastFrame;
        private int combo;
        private float[] particleX = new float[28];
        private float[] particleY = new float[28];
        private float[] particleVX = new float[28];
        private float[] particleVY = new float[28];
        private int[] particleLife = new int[28];

        GameView() {
            super(MainActivity.this);
            prefs = getSharedPreferences("slime_save", MODE_PRIVATE);
            coins = prefs.getInt("coins", 50);
            best = prefs.getInt("best", 0);
            skin = prefs.getInt("skin", 0);
            soundOn = prefs.getBoolean("sound", true);
            particlesOn = prefs.getBoolean("particles", true);
            for (int i = 0; i < 5; i++) {
                owned[i] = prefs.getBoolean("owned" + i, i == 0);
            }
            if (skin < 0 || skin >= 5 || !owned[skin]) skin = 0;
            try {
                sound = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
            } catch (Exception ignored) {
                sound = null;
            }
            resetRun();
            state = MENU;
        }

        void saveData() {
            SharedPreferences.Editor e = prefs.edit();
            e.putInt("coins", coins);
            e.putInt("best", best);
            e.putInt("skin", skin);
            e.putBoolean("sound", soundOn);
            e.putBoolean("particles", particlesOn);
            for (int i = 0; i < 5; i++) e.putBoolean("owned" + i, owned[i]);
            e.apply();
        }

        void playTone(int tone, int duration) {
            if (soundOn && sound != null) {
                try { sound.startTone(tone, duration); } catch (Exception ignored) {}
            }
        }

        void release() {
            if (sound != null) {
                try { sound.release(); } catch (Exception ignored) {}
                sound = null;
            }
        }

        void pauseForLifecycle() {
            if (state == PLAY) {
                state = PAUSE;
                shake = 0;
            }
            invalidate();
        }

        boolean handleBack() {
            if (state == MENU) return false;
            if (state == PLAY) {
                state = PAUSE;
                shake = 0;
            } else {
                state = MENU;
            }
            invalidate();
            return true;
        }

        private void setColor(int color) {
            p.setColor(color);
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(1);
        }

        private void drawText(Canvas c, String s, float x, float y, float size, int color) {
            setColor(color);
            p.setTextSize(size);
            p.setTypeface(font);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, x, y, p);
        }

        private void panel(Canvas c, float l, float t, float r, float b, int color) {
            setColor(color);
            c.drawRoundRect(l, t, r, b, 20, 20, p);
        }

        private void button(Canvas c, String label, float y, int color) {
            panel(c, 62, y + 5, 338, y + 69, 0x66000000);
            panel(c, 62, y, 338, y + 61, color);
            drawText(c, label, 200, y + 39, 23, Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(0xff0d1a27);

            float scale = Math.min(getWidth() / 400f, getHeight() / 800f);
            if (scale <= 0) return;
            float dx = (getWidth() - 400 * scale) / 2f;
            float dy = (getHeight() - 800 * scale) / 2f;

            canvas.save();
            canvas.translate(dx, dy);
            canvas.scale(scale, scale);
            canvas.clipRect(0, 0, 400, 800);

            if (state == MENU) {
                animation += 0.08f;
                drawMenu(canvas);
                postInvalidateDelayed(32);
            } else if (state == SHOP) drawShop(canvas);
            else if (state == SETTINGS) drawSettings(canvas);
            else if (state == PLAY) {
                updateGame();
                drawWorld(canvas);
                postInvalidateDelayed(16);
            } else if (state == PAUSE) {
                drawWorld(canvas);
                drawPause(canvas);
            } else if (state == OVER) {
                drawWorld(canvas);
                drawGameOver(canvas);
                if (shake > 1) {
                    shake *= 0.82f;
                    postInvalidateDelayed(16);
                } else {
                    shake = 0;
                }
            }

            canvas.restore();
        }

        private void drawBackground(Canvas c) {
            p.setShader(new LinearGradient(0, 0, 0, 800,
                    new int[]{0xff142b49, 0xff376c83, 0xffa4b99e, 0xffe9d5a5},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 400, 800, p);
            p.setShader(null);

            p.setShader(new RadialGradient(315, 115, 125,
                    new int[]{0x99fff4c4, 0x33fff4c4, 0x00fff4c4},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(315, 115, 125, p);
            p.setShader(null);
            setColor(0xffffedbd);
            c.drawCircle(315, 115, 25, p);

            Path mountains = new Path();
            mountains.moveTo(0, 440);
            mountains.lineTo(55, 320);
            mountains.lineTo(115, 390);
            mountains.lineTo(195, 255);
            mountains.lineTo(285, 395);
            mountains.lineTo(350, 285);
            mountains.lineTo(400, 390);
            mountains.lineTo(400, 650);
            mountains.lineTo(0, 650);
            mountains.close();
            p.setShader(new LinearGradient(0, 255, 0, 650,
                    0xff647e83, 0xff294c4b, Shader.TileMode.CLAMP));
            c.drawPath(mountains, p);
            p.setShader(null);

            for (int i = 0; i < 18; i++) {
                float x = i * 29 - (sceneryOffset * 0.16f % 29);
                setColor(0xff315a4c);
                Path tree = new Path();
                tree.moveTo(x, 530);
                tree.lineTo(x + 17, 455);
                tree.lineTo(x + 35, 530);
                tree.close();
                c.drawPath(tree, p);
                c.drawRect(x + 15, 520, x + 21, 650, p);
            }

            p.setShader(new LinearGradient(0, 615, 0, 800,
                    new int[]{0xff597448, 0xff344b32, 0xff182b24},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 615, 400, 800, p);
            p.setShader(null);
            p.setShader(new LinearGradient(0, 630, 0, 800,
                    0xffa0925c, 0xff51482f, Shader.TileMode.CLAMP));
            c.drawRect(0, 630, 400, 800, p);
            p.setShader(null);
            setColor(0xff79904d);
            c.drawRect(0, 620, 400, 635, p);

            for (int i = 0; i < 55; i++) {
                float x = (i * 71 - sceneryOffset) % 420;
                if (x < 0) x += 420;
                float y = 642 + (i * 29) % 145;
                setColor(i % 2 == 0 ? 0x557d6b43 : 0x667b8651);
                c.drawCircle(x, y, 1 + (i % 3), p);
            }
            for (int i = 0; i < 35; i++) {
                float x = (i * 43 - sceneryOffset * 1.2f) % 420;
                if (x < 0) x += 420;
                float y = 632 + (i * 13) % 23;
                setColor(i % 2 == 0 ? 0xffb5bd77 : 0xff496c38);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                c.drawLine(x, y, x - 4, y - 9, p);
                c.drawLine(x, y, x + 5, y - 8, p);
            }
            p.setStyle(Paint.Style.FILL);
    }
                private void drawSlime(Canvas c, float x, float y, float shadowY) {
            float bounce = jumping ? 0 : (float) Math.sin(animation * 2.2f) * 2.5f;
            float squash = jumping ? 0.91f : 1f + (float) Math.sin(animation * 2f) * 0.025f;

            p.setShader(new RadialGradient(x + 34, shadowY + 5, 48,
                    new int[]{0x88000000, 0x44000000, 0x00000000},
                    null, Shader.TileMode.CLAMP));
            c.drawOval(x - 12, shadowY - 8, x + 82, shadowY + 15, p);
            p.setShader(null);

            c.save();
            c.translate(x + 34, y + 32 + bounce);
            c.scale(1f / squash, squash);
            c.translate(-x - 34, -y - 32 - bounce);

            p.setShader(new RadialGradient(x + 22, y + 15, 75,
                    new int[]{0xffffffff, skins[skin], darken(skins[skin]), darken(darken(skins[skin]))},
                    new float[]{0f, 0.35f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            c.drawOval(x, y + bounce, x + 68, y + 65 + bounce, p);
            p.setShader(null);

            p.setShader(new RadialGradient(x + 23, y + 14, 39,
                    new int[]{0x99ffffff, 0x22ffffff, 0x00ffffff},
                    null, Shader.TileMode.CLAMP));
            c.drawOval(x + 7, y + 4 + bounce, x + 59, y + 54 + bounce, p);
            p.setShader(null);

            p.setShader(new LinearGradient(x + 10, y + 3, x + 45, y + 37,
                    new int[]{0xddffffff, 0x55ffffff, 0x00ffffff},
                    null, Shader.TileMode.CLAMP));
            c.drawOval(x + 10, y + 5 + bounce, x + 49, y + 28 + bounce, p);
            p.setShader(null);

            for (int i = 0; i < 2; i++) {
                float ex = x + 22 + i * 26;
                float ey = y + 29 + bounce;
                setColor(0x55000000);
                c.drawOval(ex - 9, ey - 12, ex + 10, ey + 14, p);
                setColor(Color.WHITE);
                c.drawOval(ex - 8, ey - 13, ex + 9, ey + 12, p);
                setColor(0xff172b32);
                c.drawCircle(ex + 2, ey, 5, p);
                setColor(Color.WHITE);
                c.drawCircle(ex + 3, ey - 3, 2, p);
            }
            setColor(0xff16452b);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f);
            p.setStrokeCap(Paint.Cap.ROUND);
            c.drawArc(x + 23, y + 34 + bounce, x + 46, y + 52 + bounce,
                    15, 150, false, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.FILL);
            c.restore();

            if (shield && state == PLAY) {
                setColor(0x995eeeff);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3);
                c.drawCircle(x + 34, y + 32, 47, p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        private int darken(int color) {
            return Color.rgb((int) (Color.red(color) * 0.55f),
                    (int) (Color.green(color) * 0.55f),
                    (int) (Color.blue(color) * 0.55f));
        }

        private void drawObstacle(Canvas c) {
            p.setShader(new RadialGradient(obstacleX + 30, ground, 55,
                    new int[]{0x77000000, 0x00000000}, null, Shader.TileMode.CLAMP));
            c.drawOval(obstacleX - 15, ground - 5, obstacleX + 85, ground + 13, p);
            p.setShader(null);

            if (obstacleType == 0) {
                p.setShader(new LinearGradient(obstacleX, 0, obstacleX + obstacleW, 0,
                        new int[]{0xff56321d, 0xffc18a4e, 0xff744322},
                        null, Shader.TileMode.CLAMP));
                c.drawRoundRect(obstacleX, ground - 65, obstacleX + obstacleW, ground, 6, 6, p);
                p.setShader(null);
                for (int i = 0; i < 4; i++) {
                    float x = obstacleX + 8 + i * 14;
                    setColor(0x55804422);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1);
                    c.drawLine(x, ground - 58, x, ground - 5, p);
                }
                p.setStyle(Paint.Style.FILL);
                panel(c, obstacleX - 7, ground - 65, obstacleX + obstacleW + 7, ground - 52, 0xffb77b43);
            } else if (obstacleType == 1) {
                p.setShader(new LinearGradient(obstacleX, ground - 80, obstacleX + obstacleW, ground,
                        new int[]{0xffc0c4bf, 0xff777d79, 0xff414a49},
                        null, Shader.TileMode.CLAMP));
                c.drawRoundRect(obstacleX, ground - 80, obstacleX + obstacleW, ground, 7, 7, p);
                p.setShader(null);
                setColor(0xff454d4a);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                c.drawLine(obstacleX, ground - 53, obstacleX + obstacleW, ground - 53, p);
                c.drawLine(obstacleX + 25, ground - 80, obstacleX + 25, ground - 53, p);
                p.setStyle(Paint.Style.FILL);
            } else {
                p.setShader(new LinearGradient(obstacleX, ground - 25, obstacleX, ground,
                        0xff7c8589, 0xff242d32, Shader.TileMode.CLAMP));
                c.drawRoundRect(obstacleX, ground - 25, obstacleX + obstacleW, ground, 5, 5, p);
                p.setShader(null);
                for (int i = 0; i < 3; i++) {
                    float x = obstacleX + 5 + i * 19;
                    Path spike = new Path();
                    spike.moveTo(x, ground - 23);
                    spike.lineTo(x + 10, ground - 78);
                    spike.lineTo(x + 20, ground - 23);
                    spike.close();
                    p.setShader(new LinearGradient(x, ground - 78, x + 20, ground,
                            0xfff0f3f1, 0xff59666b, Shader.TileMode.CLAMP));
                    c.drawPath(spike, p);
                    p.setShader(null);
                }
            }
        }

        private void drawCoin(Canvas c) {
            p.setShader(new RadialGradient(coinX, coinY, 31,
                    new int[]{0x66fff3a0, 0x00fff3a0}, null, Shader.TileMode.CLAMP));
            c.drawCircle(coinX, coinY, 31, p);
            p.setShader(null);
            p.setShader(new RadialGradient(coinX - 5, coinY - 6, 24,
                    new int[]{0xfffffffd, 0xffffe34a, 0xffd58a00},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(coinX, coinY, 16, p);
            p.setShader(null);
            setColor(0xfffff4a0);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            c.drawCircle(coinX, coinY, 11, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void spawnParticles(float x, float y, int count) {
            if (!particlesOn) return;
            for (int n = 0; n < count; n++) {
                int i = random.nextInt(particleLife.length);
                particleX[i] = x;
                particleY[i] = y;
                particleVX[i] = (random.nextFloat() - 0.5f) * 7f;
                particleVY[i] = -random.nextFloat() * 6f - 1f;
                particleLife[i] = 20 + random.nextInt(18);
            }
        }

        private void updateParticles(float dt) {
            for (int i = 0; i < particleLife.length; i++) {
                if (particleLife[i] > 0) {
                    particleX[i] += particleVX[i] * dt;
                    particleY[i] += particleVY[i] * dt;
                    particleVY[i] += 0.22f * dt;
                    particleLife[i] -= (int) Math.max(1, dt);
                }
            }
        }

        private void drawParticles(Canvas c) {
            for (int i = 0; i < particleLife.length; i++) {
                if (particleLife[i] > 0) {
                    int alpha = Math.min(220, particleLife[i] * 8);
                    setColor((alpha << 24) | 0x00d9ffff);
                    c.drawCircle(particleX[i], particleY[i], 2.5f, p);
                }
            }
        }

        private void resetRun() {
            score = 0;
            scoreAcc = 0;
            slimeY = ground - 65;
            velocity = 0;
            obstacleX = 500;
            coinX = 750;
            coinY = 540;
            speed = 7;
            obstacleType = random.nextInt(3);
            jumping = false;
            shield = false;
            shieldTime = 0;
            combo = 0;
            shake = 0;
            for (int i = 0; i < particleLife.length; i++) particleLife[i] = 0;
            lastFrame = System.currentTimeMillis();
        }

        private void startGame() {
            resetRun();
            state = PLAY;
            lastFrame = System.currentTimeMillis();
            invalidate();
        }

        private void updateGame() {
            long now = System.currentTimeMillis();
            float dt = Math.min(Math.max((now - lastFrame) / 16.67f, 0.4f), 2f);
            lastFrame = now;

            animation += 0.12f * dt;
            sceneryOffset += speed * 0.35f * dt;
            updateParticles(dt);

            velocity += 0.78f * dt;
            slimeY += velocity * dt;
            if (slimeY >= ground - 65) {
                if (jumping) spawnParticles(slimeX + 34, ground - 4, 5);
                slimeY = ground - 65;
                velocity = 0;
                jumping = false;
            }

            obstacleX -= speed * dt;
            coinX -= speed * dt;

            if (obstacleX < -100) {
                obstacleX = 460 + random.nextInt(160);
                obstacleType = random.nextInt(3);
                score += 100;
                speed = Math.min(speed + 0.18f, 15f);
            }
            if (coinX < -30) {
                coinX = 480 + random.nextInt(180);
                coinY = ground - 90 - random.nextInt(100);
            }

            RectF player = new RectF(slimeX + 7, slimeY + 8, slimeX + 61, slimeY + 60);
            RectF obstacle;

            if (obstacleType == 0) {
                obstacle = new RectF(obstacleX, ground - 65,
                        obstacleX + obstacleW, ground);
            } else if (obstacleType == 1) {
                obstacle = new RectF(obstacleX, ground - 80,
                        obstacleX + obstacleW, ground);
            } else {
                obstacle = new RectF(obstacleX, ground - 78,
                        obstacleX + obstacleW, ground);
            }

            if (RectF.intersects(player, obstacle)) {
                if (shield) {
                    shield = false;
                    shake = 12;
                    obstacleX = -150;
                    spawnParticles(slimeX + 34, slimeY + 30, 18);
                    playTone(ToneGenerator.TONE_PROP_BEEP, 120);
                } else {
                    state = OVER;
                    shake = 18;

                    if (score > best) best = score;

                    saveData();
                    playTone(ToneGenerator.TONE_PROP_NACK, 300);
                    return;
                }
            }

            RectF coin = new RectF(
                    coinX - 17, coinY - 17,
                    coinX + 17, coinY + 17
            );

            if (RectF.intersects(player, coin)) {
                coins++;
                combo++;
                score += 25;

                spawnParticles(coinX, coinY, 12);
                playTone(ToneGenerator.TONE_PROP_ACK, 100);

                coinX = 500 + random.nextInt(220);
                coinY = ground - 90 - random.nextInt(100);

                saveData();
            }

            scoreAcc += dt * 2;
            int add = (int) scoreAcc;
            score += add;
            scoreAcc -= add;

            if (shield) {
                shieldTime -= dt;
                if (shieldTime <= 0) shield = false;
            }

            if (shake > 0) shake *= 0.82f;
        }
                private void drawWorld(Canvas c) {
            c.save();

            if (shake > 1 && state != PAUSE) {
                c.translate(
                        (random.nextFloat() - 0.5f) * shake,
                        (random.nextFloat() - 0.5f) * shake
                );
            }

            drawBackground(c);
            drawCoin(c);
            drawObstacle(c);
            drawSlime(c, slimeX, slimeY, ground);
            drawParticles(c);

            c.restore();

            panel(c, 12, 15, 130, 60, 0xaa102334);
            drawText(c, "SCORE", 70, 34, 12, 0xffb9d5df);
            drawText(c, String.valueOf(score), 70, 53, 20, Color.WHITE);

            panel(c, 145, 15, 255, 60, 0xaa102334);
            drawText(c, "COINS", 200, 34, 12, 0xffffdf75);
            drawText(c, String.valueOf(coins), 200, 53, 20, Color.WHITE);

            panel(c, 270, 15, 388, 60, 0xaa102334);
            drawText(c, "BEST: " + best, 329, 45, 17, Color.WHITE);

            if (state == PLAY) {
                panel(c, 345, 75, 388, 118, 0xaa102334);
                setColor(Color.WHITE);
                c.drawRoundRect(358, 87, 364, 106, 2, 2, p);
                c.drawRoundRect(369, 87, 375, 106, 2, 2, p);
            }
        }

        private void drawMenu(Canvas c) {
            drawBackground(c);

            drawText(c, "SLIME", 200, 190, 67, Color.WHITE);
            drawText(c, "RUSH", 200, 255, 67, skins[skin]);
            drawText(c, "THE LITTLE SLIME ADVENTURE",
                    200, 290, 13, 0xffd8e9e9);

            drawSlime(c, 166, 328, 400);

            button(c, "PLAY", 420, 0xff20a96b);
            button(c, "SKINS SHOP", 500, 0xff287db9);
            button(c, "SETTINGS", 580, 0xff6555a8);

            drawText(c, "BEST SCORE: " + best,
                    200, 690, 19, Color.WHITE);

            drawText(c, "TAP TO JUMP \u2022 AVOID OBSTACLES",
                    200, 755, 13, 0xffd6e4dc);
        }

        private void drawShop(Canvas c) {
            drawBackground(c);

            drawText(c, "SLIME SHOP", 200, 95, 35, Color.WHITE);

            panel(c, 100, 115, 300, 155, 0xaa102334);
            drawText(c, "YOUR COINS: " + coins,
                    200, 142, 19, 0xffffdf75);

            for (int i = 0; i < 5; i++) {
                int y = 185 + i * 88;

                panel(c, 35, y, 365, y + 72, 0xcc142b3c);

                setColor(skins[i]);
                c.drawCircle(82, y + 36, 23, p);

                drawText(c, skinNames[i],
                        190, y + 29, 17, Color.WHITE);

                String action;

                if (skin == i) {
                    action = "EQUIPPED";
                } else if (owned[i]) {
                    action = "SELECT";
                } else {
                    action = prices[i] + " COINS";
                }

                drawText(c, action,
                        260, y + 53, 13, 0xffffdf75);
            }

            button(c, "BACK", 650, 0xff596b7b);
        }

        private void drawSettings(Canvas c) {
            drawBackground(c);

            drawText(c, "SETTINGS", 200, 145, 38, Color.WHITE);

            panel(c, 35, 240, 365, 325, 0xcc142b3c);
            drawText(c, "SOUND", 200, 275, 22, Color.WHITE);
            drawText(c, soundOn ? "ON" : "OFF",
                    200, 305, 24,
                    soundOn ? 0xff63ed9c : 0xffff7777);

            panel(c, 35, 355, 365, 440, 0xcc142b3c);
            drawText(c, "PARTICLES", 200, 390, 22, Color.WHITE);
            drawText(c, particlesOn ? "ON" : "OFF",
                    200, 420, 24,
                    particlesOn ? 0xff63ed9c : 0xffff7777);

            button(c, "BACK", 580, 0xff596b7b);
        }

        private void drawPause(Canvas c) {
            setColor(0xaa071522);
            c.drawRect(0, 0, 400, 800, p);

            panel(c, 35, 245, 365, 570, 0xee142b3c);

            drawText(c, "PAUSED", 200, 310, 39, Color.WHITE);
            drawText(c, "Take a little break!",
                    200, 350, 16, 0xffc8dce5);

            button(c, "RESUME", 390, 0xff20a96b);
            button(c, "RESTART", 465, 0xff287db9);
            button(c, "MENU", 540, 0xff596b7b);
        }

        private void drawGameOver(Canvas c) {
            setColor(0xbb071522);
            c.drawRect(0, 0, 400, 800, p);

            panel(c, 30, 210, 370, 610, 0xee142b3c);

            drawText(c, "GAME OVER", 200, 285, 37,
                    0xffff7777);

            drawText(c, "YOUR SCORE",
                    200, 345, 17, 0xffb9d5df);

            drawText(c, String.valueOf(score),
                    200, 405, 55, Color.WHITE);

            drawText(c, "BEST: " + best,
                    200, 450, 19, 0xffffdf75);

            drawText(c, "COINS: " + coins,
                    200, 485, 18, 0xffffdf75);

            button(c, "TRY AGAIN", 510, 0xff20a96b);
            button(c, "MAIN MENU", 585, 0xff596b7b);
        }

        private void jump() {
            if (state != PLAY || jumping) return;

            velocity = -17.5f;
            jumping = true;

            spawnParticles(slimeX + 34, ground - 5, 5);
            playTone(ToneGenerator.TONE_PROP_BEEP, 70);
        }

        private void handleShopTap(float x, float y) {
            if (y >= 650 && y <= 720) {
                state = MENU;
                return;
            }

            for (int i = 0; i < 5; i++) {
                float top = 185 + i * 88;

                if (y >= top && y <= top + 72) {
                    if (owned[i]) {
                        skin = i;
                        saveData();
                        playTone(ToneGenerator.TONE_PROP_ACK, 100);
                    } else if (coins >= prices[i]) {
                        coins -= prices[i];
                        owned[i] = true;
                        skin = i;
                        saveData();
                        playTone(ToneGenerator.TONE_PROP_ACK, 150);
                    } else {
                        playTone(ToneGenerator.TONE_PROP_NACK, 150);
                    }

                    return;
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN
                    && event.getActionMasked() != MotionEvent.ACTION_POINTER_DOWN) {
                return true;
            }

            float scale = Math.min(
                    getWidth() / 400f,
                    getHeight() / 800f
            );

            if (scale <= 0) return true;

            float dx = (getWidth() - 400 * scale) / 2f;
            float dy = (getHeight() - 800 * scale) / 2f;

            int idx = event.getActionIndex();
            float x = (event.getX(idx) - dx) / scale;
            float y = (event.getY(idx) - dy) / scale;

            if (state == MENU) {
                if (y >= 420 && y <= 481) {
                    startGame();
                } else if (y >= 500 && y <= 561) {
                    state = SHOP;
                } else if (y >= 580 && y <= 641) {
                    state = SETTINGS;
                }
            } else if (state == PLAY) {
                if (x >= 335 && y <= 130) {
                    state = PAUSE;
                    shake = 0;
                } else {
                    jump();
                }
            } else if (state == SHOP) {
                handleShopTap(x, y);
            } else if (state == SETTINGS) {
                if (y >= 240 && y <= 325) {
                    soundOn = !soundOn;
                    saveData();
                } else if (y >= 355 && y <= 440) {
                    particlesOn = !particlesOn;
                    saveData();
                } else if (y >= 580 && y <= 650) {
                    state = MENU;
                }
            } else if (state == PAUSE) {
                if (y >= 390 && y <= 451) {
                    state = PLAY;
                    lastFrame = System.currentTimeMillis();
                } else if (y >= 465 && y <= 526) {
                    startGame();
                } else if (y >= 540 && y <= 601) {
                    state = MENU;
                }
            } else if (state == OVER) {
                if (y >= 510 && y <= 571) {
                    startGame();
                } else if (y >= 585 && y <= 646) {
                    state = MENU;
                }
            }

            invalidate();
            return true;
        }
    }
        }
