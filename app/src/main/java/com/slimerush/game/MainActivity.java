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
import java.util.Locale;
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
                OVER = 3, PAUSE = 4, SETTINGS = 5, LEVELS = 6;
        private static final int NSKIN = 10, NACC = 11, NLEV = 5;

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Typeface font = Typeface.DEFAULT_BOLD;
        private final Path tmp = new Path();
        private final Path mountains = new Path();
        private final float[] hsv = new float[3];
        private final Random random = new Random();
        private final SharedPreferences prefs;
        private ToneGenerator sound;

        private int state = MENU;
        private int coins, score, skin, acc, lang, level;
        private final int[] bestL = new int[NLEV];
        private boolean soundOn, particlesOn, newUnlock;
        private final boolean[] skinOwn = new boolean[NSKIN];
        private final boolean[] accOwn = new boolean[NACC];
        private int shopTab, shopPage;

        // ---------- ЯЗЫКИ ----------
        private final String[] langNames = {"ENGLISH", "РУССКИЙ", "DEUTSCH"};

        private String tr(String en, String ru, String de) {
            return lang == 1 ? ru : (lang == 2 ? de : en);
        }

        // ---------- СКИНЫ ----------
        private final int[] skinCols = {
                Color.rgb(66, 245, 125), Color.rgb(50, 174, 255),
                Color.rgb(255, 104, 185), Color.rgb(255, 207, 58),
                Color.rgb(177, 112, 255), Color.rgb(255, 86, 40),
                Color.rgb(150, 230, 255), Color.rgb(170, 255, 40),
                Color.rgb(70, 70, 95), Color.WHITE /* радуга */
        };
        private final int[] skinPrices = {0, 50, 100, 200, 350, 400, 450, 500, 600, 1000};
        private final String[][] skinNames = {
                {"MINT", "МЯТА", "MINZE"},
                {"OCEAN", "ОКЕАН", "OZEAN"},
                {"BUBBLEGUM", "ЖВАЧКА", "KAUGUMMI"},
                {"GOLD", "ЗОЛОТО", "GOLD"},
                {"GALAXY", "ГАЛАКТИКА", "GALAXIE"},
                {"LAVA", "ЛАВА", "LAVA"},
                {"ICE", "ЛЁД", "EIS"},
                {"TOXIC", "ЯД", "GIFT"},
                {"SHADOW", "ТЕНЬ", "SCHATTEN"},
                {"RAINBOW", "РАДУГА", "REGENBOGEN"}
        };

        // ---------- АКСЕССУАРЫ ----------
        private final int[] accPrices = {0, 60, 80, 100, 150, 200, 250, 300, 450, 700, 800};
        private final String[][] accNames = {
                {"NONE", "НЕТ", "KEINE"},
                {"SUNGLASSES", "ОЧКИ", "SONNENBRILLE"},
                {"PARTY HAT", "КОЛПАК", "PARTYHUT"},
                {"CAP", "КЕПКА", "KAPPE"},
                {"HEADPHONES", "НАУШНИКИ", "KOPFHÖRER"},
                {"CAT EARS", "УШКИ КОТА", "KATZENOHREN"},
                {"FLOWER", "ЦВЕТОК", "BLUME"},
                {"TOP HAT", "ЦИЛИНДР", "ZYLINDER"},
                {"WIZARD HAT", "ШЛЯПА МАГА", "ZAUBERHUT"},
                {"CROWN", "КОРОНА", "KRONE"},
                {"HALO", "НИМБ", "HEILIGENSCHEIN"}
        };

        // ---------- УРОВНИ ----------
        private final String[][] levelNames = {
                {"SUNNY FOREST", "СОЛНЕЧНЫЙ ЛЕС", "SONNIGER WALD"},
                {"AUTUMN FOREST", "ОСЕННИЙ ЛЕС", "HERBSTWALD"},
                {"NIGHT FOREST", "НОЧНОЙ ЛЕС", "NACHTWALD"},
                {"SNOWY FOREST", "ЗАСНЕЖЕННЫЙ ЛЕС", "VERSCHNEITER WALD"},
                {"MAGIC FOREST", "ВОЛШЕБНЫЙ ЛЕС", "ZAUBERWALD"}
        };
        // сколько очков нужно набрать на ПРЕДЫДУЩЕМ уровне, чтобы открыть этот
        private final int[] levelNeed = {0, 2000, 3000, 4000, 5000};
        private final float[] levelStart = {7f, 8f, 8.5f, 9f, 10f};
        private final float[] levelMax = {15f, 16f, 17f, 18f, 19.5f};

        private final int[][] skyCols = {
                {0xff3f95d6, 0xff7cc4ee, 0xffcdeedd, 0xfff7f2bd},
                {0xff5f7fa8, 0xffd9a07c, 0xfff0c27c, 0xfffadf9f},
                {0xff040a1c, 0xff0d1f45, 0xff1b3962, 0xff2a5570},
                {0xff6f93b8, 0xffa9c6dc, 0xffd6e6ef, 0xfff2f7fa},
                {0xff180a38, 0xff3a1a6a, 0xff7b3fa2, 0xffe08fc8}
        };
        private final int[] hillTop = {0xff6fae9a, 0xff9a6a52, 0xff1b3550, 0xff93b0c4, 0xff4f2a85};
        private final int[] hillBot = {0xff3f7f66, 0xff5e3a2e, 0xff0d1c2c, 0xff5e7f93, 0xff2a1450};
        private final int[] farC = {0xff4d8f78, 0xff9c5c3a, 0xff17344a, 0xff7ea0b2, 0xff4a2a7d};
        private final int[] midC = {0xff2f7a4f, 0xffb4552a, 0xff0f2a3d, 0xff5a8590, 0xff351b66};
        private final int[] nearC = {0xff1b5a36, 0xff7e3220, 0xff08171f, 0xff3b6165, 0xff22104a};
        private final int[] trunkC = {0xff4a3220, 0xff3d2818, 0xff1a1410, 0xff4a3a30, 0xff2a1a3a};
        private final int[] groundTop = {0xff6aa84a, 0xffb98a3e, 0xff2b4a34, 0xfff4f8fb, 0xff4a3a7a};
        private final int[] groundMid = {0xff8a7a45, 0xff7a5430, 0xff3a3a2a, 0xffc9d8e4, 0xff342a5a};
        private final int[] groundBot = {0xff4a3b24, 0xff3d2a1c, 0xff1a1d17, 0xff8aa0b3, 0xff1d1538};
        private final int[] grassA = {0xff86c45a, 0xffd9a441, 0xff3f7a4c, 0xffffffff, 0xff5de0d0};
        private final int[] grassB = {0xff4a8a3a, 0xff9a5a26, 0xff25503a, 0xffd5e4ee, 0xffb070e8};
        private final int[] haze = {0xffd6f0e0, 0xffffd9a8, 0xff3a5a80, 0xffeaf4fa, 0xffb070d8};
        private final float[] celX = {315, 300, 310, 320, 300};
        private final float[] celY = {115, 210, 110, 120, 150};
        private final float[] celR = {25, 38, 24, 20, 34};
        private final int[] celCol = {0xfffff2b0, 0xffffb86b, 0xfff0f4ff, 0xfffff6dd, 0xffffd6f6};
        private final boolean[] hasStars = {false, false, true, false, true};
        private final boolean[] hasClouds = {true, true, false, true, false};
        private final boolean[] hasRays = {true, true, false, false, false};
        private final boolean[] hasFireflies = {false, false, true, false, true};
        private final int[] weatherCount = {12, 30, 0, 36, 24};

        private final Shader[] skyS = new Shader[NLEV];
        private final Shader[] hillS = new Shader[NLEV];
        private final Shader[] groundS = new Shader[NLEV];
        private final Shader[] fogS = new Shader[NLEV];
        private final Shader[] glowS = new Shader[NLEV];

        // ---------- ИГРОВОЕ СОСТОЯНИЕ ----------
        private float slimeX = 83, slimeY, velocity;
        private final float ground = 650;
        private float obstacleX, obstacleW = 58;
        private float coinX, coinY;
        private float speed = 7, animation, sceneryOffset;
        private float shake, scoreAcc;
        private int obstacleType;
        private boolean jumping, shield;
        private float shieldTime;
        private long lastFrame;
        private int combo;
        private final float[] particleX = new float[28];
        private final float[] particleY = new float[28];
        private final float[] particleVX = new float[28];
        private final float[] particleVY = new float[28];
        private final int[] particleLife = new int[28];
        private final float[] wx = new float[36], wy = new float[36],
                wvx = new float[36], wvy = new float[36],
                wph = new float[36], wsz = new float[36];

        GameView() {
            super(MainActivity.this);
            prefs = getSharedPreferences("slime_save", MODE_PRIVATE);
            coins = prefs.getInt("coins", 50);
            skin = prefs.getInt("skin", 0);
            acc = prefs.getInt("acc", 0);
            soundOn = prefs.getBoolean("sound", true);
            particlesOn = prefs.getBoolean("particles", true);
            String lc = Locale.getDefault().getLanguage();
            lang = prefs.getInt("lang", lc.equals("ru") ? 1 : (lc.equals("de") ? 2 : 0));
            if (lang < 0 || lang > 2) lang = 0;
            for (int i = 0; i < NSKIN; i++) {
                skinOwn[i] = prefs.getBoolean("skinOwn" + i, prefs.getBoolean("owned" + i, i == 0));
            }
            for (int i = 0; i < NACC; i++) accOwn[i] = prefs.getBoolean("accOwn" + i, i == 0);
            accOwn[0] = true;
            skinOwn[0] = true;
            for (int i = 0; i < NLEV; i++) {
                bestL[i] = prefs.getInt("bestL" + i, i == 0 ? prefs.getInt("best", 0) : 0);
            }
            if (skin < 0 || skin >= NSKIN || !skinOwn[skin]) skin = 0;
            if (acc < 0 || acc >= NACC || !accOwn[acc]) acc = 0;
            level = prefs.getInt("level", 0);
            if (level < 0 || level >= NLEV || !levelUnlocked(level)) level = 0;
            try {
                sound = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
            } catch (Exception ignored) {
                sound = null;
            }
            buildShaders();
            initWeather();
            resetRun();
            state = MENU;
        }

        private boolean levelUnlocked(int i) {
            return i == 0 || bestL[i - 1] >= levelNeed[i];
        }

        private void buildShaders() {
            for (int i = 0; i < NLEV; i++) {
                skyS[i] = new LinearGradient(0, 0, 0, 800, skyCols[i], null, Shader.TileMode.CLAMP);
                hillS[i] = new LinearGradient(0, 255, 0, 650, hillTop[i], hillBot[i], Shader.TileMode.CLAMP);
                groundS[i] = new LinearGradient(0, 620, 0, 800,
                        new int[]{groundTop[i], groundMid[i], groundBot[i]}, null, Shader.TileMode.CLAMP);
                int hz = haze[i] & 0x00ffffff;
                fogS[i] = new LinearGradient(0, 430, 0, 650, hz, 0x88000000 | hz, Shader.TileMode.CLAMP);
                int gc = celCol[i] & 0x00ffffff;
                glowS[i] = new RadialGradient(celX[i], celY[i], celR[i] * 4.5f,
                        new int[]{0x99000000 | gc, 0x33000000 | gc, gc}, null, Shader.TileMode.CLAMP);
            }
            mountains.reset();
            mountains.moveTo(0, 440);
            mountains.lineTo(55, 330);
            mountains.lineTo(115, 395);
            mountains.lineTo(195, 265);
            mountains.lineTo(285, 400);
            mountains.lineTo(350, 295);
            mountains.lineTo(400, 395);
            mountains.lineTo(400, 650);
            mountains.lineTo(0, 650);
            mountains.close();
        }

        void saveData() {
            SharedPreferences.Editor e = prefs.edit();
            e.putInt("coins", coins);
            e.putInt("skin", skin);
            e.putInt("acc", acc);
            e.putInt("lang", lang);
            e.putInt("level", level);
            e.putBoolean("sound", soundOn);
            e.putBoolean("particles", particlesOn);
            for (int i = 0; i < NSKIN; i++) e.putBoolean("skinOwn" + i, skinOwn[i]);
            for (int i = 0; i < NACC; i++) e.putBoolean("accOwn" + i, accOwn[i]);
            for (int i = 0; i < NLEV; i++) e.putInt("bestL" + i, bestL[i]);
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

        private float rnd(int idx, int seed) {
            int h = idx * 374761393 + seed * 668265263;
            h = (h ^ (h >>> 13)) * 1274126177;
            h ^= (h >>> 16);
            return (h & 0xffff) / 65535f;
        }

        // ---------- ПРИМИТИВЫ UI ----------
        private void setColor(int color) {
            p.setColor(color);
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(1);
        }

        private void drawTextA(Canvas c, String s, float x, float y, float size,
                               int color, float maxW, Paint.Align align) {
            setColor(color);
            p.setTypeface(font);
            p.setTextSize(size);
            float w = p.measureText(s);
            if (w > maxW) p.setTextSize(size * maxW / w);
            p.setTextAlign(align);
            c.drawText(s, x, y, p);
        }

        private void drawText(Canvas c, String s, float x, float y, float size, int color) {
            drawTextA(c, s, x, y, size, color, 360, Paint.Align.CENTER);
        }

        private void panel(Canvas c, float l, float t, float r, float b, int color) {
            setColor(color);
            c.drawRoundRect(l, t, r, b, 20, 20, p);
        }

        private void button(Canvas c, String label, float y, int color) {
            panel(c, 62, y + 5, 338, y + 69, 0x66000000);
            panel(c, 62, y, 338, y + 61, color);
            drawTextA(c, label, 200, y + 39, 23, Color.WHITE, 230, Paint.Align.CENTER);
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

            if (state == PLAY) {
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
            } else {
                // меню, магазин, уровни, настройки: фон плавно живёт
                animation += 0.08f;
                sceneryOffset += 1.2f;
                updateWeather(0.6f);
                if (state == MENU) drawMenu(canvas);
                else if (state == SHOP) drawShop(canvas);
                else if (state == LEVELS) drawLevels(canvas);
                else drawSettings(canvas);
                postInvalidateDelayed(32);
            }

            canvas.restore();
            }
                // ---------- ФОН: НАСТОЯЩИЙ ЛЕС ----------
        private void drawBackground(Canvas c) {
            int L = level;
            p.setStyle(Paint.Style.FILL);
            p.setShader(skyS[L]);
            c.drawRect(0, 0, 400, 800, p);
            p.setShader(null);

            if (hasStars[L]) {
                for (int i = 0; i < 45; i++) {
                    float sx = rnd(i, 11) * 400;
                    float sy = rnd(i, 12) * 330;
                    float tw = 0.5f + 0.5f * (float) Math.sin(animation * 0.8f + i);
                    setColor(((int) (70 + 150 * tw) << 24) | 0x00ffffff);
                    c.drawCircle(sx, sy, 0.8f + rnd(i, 13) * 1.4f, p);
                }
            }

            p.setShader(glowS[L]);
            c.drawCircle(celX[L], celY[L], celR[L] * 4.5f, p);
            p.setShader(null);
            setColor(celCol[L]);
            c.drawCircle(celX[L], celY[L], celR[L], p);
            if (L == 2 || L == 4) {
                setColor(0x225a6a90);
                c.drawCircle(celX[L] - 8, celY[L] - 5, celR[L] * 0.28f, p);
                c.drawCircle(celX[L] + 9, celY[L] + 7, celR[L] * 0.2f, p);
                c.drawCircle(celX[L] + 4, celY[L] - 11, celR[L] * 0.14f, p);
            }

            if (hasClouds[L]) {
                for (int k = 0; k < 4; k++) {
                    float cx = (k * 150f - sceneryOffset * 0.03f) % 560f;
                    if (cx < 0) cx += 560f;
                    cx -= 80;
                    float cy = 55 + (k * 53) % 110;
                    setColor(L == 1 ? 0x88ffe2c0 : 0xaaffffff);
                    c.drawOval(cx - 38, cy - 12, cx + 38, cy + 14, p);
                    c.drawOval(cx - 20, cy - 26, cx + 26, cy + 6, p);
                    c.drawOval(cx + 8, cy - 18, cx + 52, cy + 10, p);
                }
            }

            p.setShader(hillS[L]);
            c.drawPath(mountains, p);
            p.setShader(null);

            drawTreeLayer(c, 0.04f, 36, 565, 150, 44, farC[L], 1, false);
            if (hasRays[L]) drawRays(c);
            drawTreeLayer(c, 0.10f, 50, 605, 215, 64, midC[L], 2, true);

            p.setShader(fogS[L]);
            c.drawRect(0, 430, 400, 650, p);
            p.setShader(null);

            drawTreeLayer(c, 0.22f, 74, 642, 300, 92, nearC[L], 3, true);
            drawGround(c);
        }

        private void drawRays(Canvas c) {
            for (int k = 0; k < 4; k++) {
                float a = 22 + 10 * (float) Math.sin(animation * 0.4f + k);
                setColor(((int) a << 24) | 0x00fff4c8);
                float x0 = 190 + k * 52;
                tmp.reset();
                tmp.moveTo(x0, 0);
                tmp.lineTo(x0 + 34, 0);
                tmp.lineTo(x0 - 150, 640);
                tmp.lineTo(x0 - 230, 640);
                tmp.close();
                c.drawPath(tmp, p);
            }
        }

        private void drawTreeLayer(Canvas c, float factor, float spacing, float baseY,
                                   float hBase, float w, int color, int seed, boolean details) {
            float shift = sceneryOffset * factor;
            int first = (int) Math.floor(shift / spacing);
            float frac = shift - first * spacing;
            int count = (int) (440 / spacing) + 3;
            for (int i = -1; i < count; i++) {
                int idx = first + i;
                float r = rnd(idx, seed);
                float x = i * spacing - frac;
                float h = hBase * (0.72f + 0.55f * rnd(idx, seed + 50));
                float ww = w * (0.85f + 0.3f * rnd(idx, seed + 90));
                boolean oak = (level == 1 && r > 0.35f) || (level == 0 && r > 0.8f);
                drawTree(c, x, baseY, h, ww, color, oak, details);
            }
        }

        private void drawTree(Canvas c, float x, float baseY, float h, float w,
                              int color, boolean oak, boolean details) {
            float th = h * 0.2f;
            setColor(details ? trunkC[level] : color);
            c.drawRect(x - w * 0.07f, baseY - th, x + w * 0.07f, baseY + 8, p);
            if (oak) {
                float cy = baseY - th - h * 0.25f;
                setColor(color);
                c.drawCircle(x, cy, w * 0.52f, p);
                c.drawCircle(x - w * 0.32f, cy + h * 0.1f, w * 0.4f, p);
                c.drawCircle(x + w * 0.32f, cy + h * 0.1f, w * 0.4f, p);
                c.drawCircle(x, cy - h * 0.15f, w * 0.4f, p);
                if (details) {
                    setColor(0x22ffffff);
                    c.drawCircle(x - w * 0.12f, cy - h * 0.08f, w * 0.3f, p);
                }
            } else {
                float foliage = h - th;
                float top = baseY - h;
                for (int k = 0; k < 4; k++) {
                    float apex = top + k * foliage * 0.22f;
                    float tierH = foliage * 0.45f;
                    float hw = w * 0.5f * (0.45f + 0.18f * k);
                    setColor(color);
                    tmp.reset();
                    tmp.moveTo(x, apex);
                    tmp.lineTo(x - hw, apex + tierH);
                    tmp.lineTo(x + hw, apex + tierH);
                    tmp.close();
                    c.drawPath(tmp, p);
                    if (details) {
                        setColor(0x18ffffff);
                        tmp.reset();
                        tmp.moveTo(x, apex);
                        tmp.lineTo(x - hw, apex + tierH);
                        tmp.lineTo(x - hw * 0.1f, apex + tierH);
                        tmp.close();
                        c.drawPath(tmp, p);
                        if (level == 3) {
                            setColor(0xeeffffff);
                            tmp.reset();
                            tmp.moveTo(x, apex);
                            tmp.lineTo(x - hw * 0.55f, apex + tierH * 0.42f);
                            tmp.lineTo(x + hw * 0.55f, apex + tierH * 0.42f);
                            tmp.close();
                            c.drawPath(tmp, p);
                        }
                    }
                }
            }
        }

        private void drawBlades(Canvas c, float spacing, float baseY, float maxH,
                                float factor, int colA, int colB, int seed) {
            float shift = sceneryOffset * factor;
            int first = (int) Math.floor(shift / spacing);
            float frac = shift - first * spacing;
            int count = (int) (410 / spacing) + 2;
            for (int i = -1; i < count; i++) {
                int idx = first + i;
                float x = i * spacing - frac;
                float h = maxH * (0.4f + 0.6f * rnd(idx, seed));
                setColor((idx & 1) == 0 ? colA : colB);
                tmp.reset();
                tmp.moveTo(x - spacing * 0.3f, baseY);
                tmp.lineTo(x + spacing * 0.05f, baseY - h);
                tmp.lineTo(x + spacing * 0.35f, baseY);
                tmp.close();
                c.drawPath(tmp, p);
            }
        }

        private void drawGround(Canvas c) {
            p.setShader(groundS[level]);
            c.drawRect(0, 620, 400, 800, p);
            p.setShader(null);
            setColor(grassA[level]);
            c.drawRect(0, 620, 400, 636, p);
            setColor(grassB[level]);
            c.drawRect(0, 632, 400, 640, p);

            for (int i = 0; i < 50; i++) {
                float x = (i * 71 - sceneryOffset) % 420;
                if (x < 0) x += 420;
                float y = 650 + (i * 29) % 140;
                setColor(i % 2 == 0 ? 0x44000000 : 0x33ffffff);
                c.drawCircle(x, y, 1 + (i % 3), p);
            }
            drawBlades(c, 10, 636, 16, 1.0f, grassA[level], grassB[level], 5);
            drawProps(c);
        }

        private void drawProps(Canvas c) {
            float sp = 110;
            int first = (int) Math.floor(sceneryOffset / sp);
            float frac = sceneryOffset - first * sp;
            for (int i = -1; i < 5; i++) {
                int idx = first + i;
                float r = rnd(idx, 7);
                if (r < 0.4f) continue;
                float x = i * sp - frac + rnd(idx, 8) * 60;
                float y = 662 + rnd(idx, 9) * 70;
                switch (level) {
                    case 0: {
                        setColor(0xff3f8a3a);
                        c.drawRect(x - 1, y - 14, x + 1, y, p);
                        setColor(r > 0.7f ? 0xffffe14a : 0xffff7eb6);
                        for (int j = 0; j < 5; j++) {
                            double a = j * 1.2566;
                            c.drawCircle(x + (float) Math.cos(a) * 5, y - 16 + (float) Math.sin(a) * 5, 3.5f, p);
                        }
                        setColor(0xffff9a2a);
                        c.drawCircle(x, y - 16, 3, p);
                        break;
                    }
                    case 1: {
                        setColor(0xffeee0cc);
                        c.drawRect(x - 3, y - 8, x + 3, y, p);
                        setColor(0xffd8382f);
                        c.drawArc(x - 10, y - 20, x + 10, y, 180, 180, true, p);
                        setColor(0xffffffff);
                        c.drawCircle(x - 4, y - 11, 1.8f, p);
                        c.drawCircle(x + 3, y - 14, 1.5f, p);
                        break;
                    }
                    case 3: {
                        setColor(0xfffafcff);
                        c.drawOval(x - 16, y - 8, x + 16, y + 4, p);
                        setColor(0x33a0b8d0);
                        c.drawOval(x - 12, y - 2, x + 14, y + 4, p);
                        break;
                    }
                    default: {
                        int g = (level == 2) ? 0xff58e6ff : (r > 0.7f ? 0xffff7ad8 : 0xff7df5ff);
                        setColor((0x33 << 24) | (g & 0x00ffffff));
                        c.drawCircle(x, y - 8, 14, p);
                        setColor(0xffd9f0f5);
                        c.drawRect(x - 2.5f, y - 8, x + 2.5f, y, p);
                        setColor(g);
                        c.drawArc(x - 9, y - 18, x + 9, y - 1, 180, 180, true, p);
                        break;
                    }
                }
            }
        }

        private void drawFireflies(Canvas c) {
            if (!hasFireflies[level] || !particlesOn) return;
            int base = level == 2 ? 0xffe8ff6a : 0xffff9cf0;
            for (int i = 0; i < 16; i++) {
                float fx = (rnd(i, 21) * 420 - sceneryOffset * 0.25f) % 420;
                if (fx < 0) fx += 420;
                float fy = 380 + rnd(i, 22) * 250 + (float) Math.sin(animation * 0.6f + i * 1.7f) * 14;
                float tw = 0.5f + 0.5f * (float) Math.sin(animation * 1.3f + i * 2.1f);
                setColor(((int) (60 * tw) << 24) | (base & 0x00ffffff));
                c.drawCircle(fx, fy, 9, p);
                setColor(((int) (120 + 130 * tw) << 24) | (base & 0x00ffffff));
                c.drawCircle(fx, fy, 2.4f, p);
            }
        }

        // ---------- ПОГОДА: листья / снег / искры ----------
        private void spawnWeather(int i, boolean anywhere) {
            wx[i] = random.nextFloat() * 440 - 20;
            wy[i] = anywhere ? random.nextFloat() * 800 : -10 - random.nextFloat() * 40;
            wph[i] = random.nextFloat() * 6.28f;
            switch (level) {
                case 0:
                    wvy[i] = 0.5f + random.nextFloat() * 0.7f;
                    wvx[i] = -0.4f;
                    wsz[i] = 2.5f + random.nextFloat() * 1.5f;
                    break;
                case 1:
                    wvy[i] = 0.9f + random.nextFloat() * 1.1f;
                    wvx[i] = -0.8f - random.nextFloat();
                    wsz[i] = 3.5f + random.nextFloat() * 2.5f;
                    break;
                case 3:
                    wvy[i] = 0.8f + random.nextFloat() * 1.4f;
                    wvx[i] = -0.3f;
                    wsz[i] = 1.5f + random.nextFloat() * 2f;
                    break;
                default:
                    wvy[i] = -0.3f - random.nextFloat() * 0.6f;
                    wvx[i] = -0.2f;
                    wsz[i] = 1.5f + random.nextFloat() * 2f;
                    break;
            }
        }

        private void initWeather() {
            for (int i = 0; i < wx.length; i++) spawnWeather(i, true);
        }

        private void updateWeather(float dt) {
            int n = weatherCount[level];
            for (int i = 0; i < n; i++) {
                wph[i] += 0.05f * dt;
                wx[i] += (wvx[i] + (float) Math.sin(wph[i]) * 0.5f - speed * 0.15f) * dt;
                wy[i] += wvy[i] * dt;
                if (level == 4) {
                    if (wy[i] < -20) {
                        wy[i] = 815;
                        wx[i] = random.nextFloat() * 420;
                    }
                } else if (wy[i] > 815) {
                    spawnWeather(i, false);
                }
                if (wx[i] < -25) wx[i] = 425;
                if (wx[i] > 425) wx[i] = -25;
            }
        }

        private void drawWeather(Canvas c) {
            if (!particlesOn) return;
            int n = weatherCount[level];
            for (int i = 0; i < n; i++) {
                if (level == 3) {
                    setColor(0xeeffffff);
                    c.drawCircle(wx[i], wy[i], wsz[i], p);
                } else if (level == 4) {
                    float tw = 0.5f + 0.5f * (float) Math.sin(wph[i] * 3);
                    setColor(((int) (80 + 150 * tw) << 24) | ((i & 1) == 0 ? 0x00ff8be8 : 0x007df5ff));
                    c.drawCircle(wx[i], wy[i], wsz[i], p);
                } else {
                    int col = level == 0 ? 0xaa9be36a
                            : (i % 3 == 0 ? 0xffe0702a : (i % 3 == 1 ? 0xffd9a030 : 0xffb03a20));
                    setColor(col);
                    c.save();
                    c.rotate(wph[i] * 40f, wx[i], wy[i]);
                    c.drawOval(wx[i] - wsz[i], wy[i] - wsz[i] * 0.6f,
                            wx[i] + wsz[i], wy[i] + wsz[i] * 0.6f, p);
                    c.restore();
                }
            }
                        }
                // ---------- СЛАЙМ + АКСЕССУАРЫ ----------
        private int skinColor(int i) {
            if (i == 9) {
                hsv[0] = (animation * 40f) % 360f;
                hsv[1] = 0.65f;
                hsv[2] = 1f;
                return Color.HSVToColor(hsv);
            }
            return skinCols[i];
        }

        private int darken(int color) {
            return Color.rgb((int) (Color.red(color) * 0.55f),
                    (int) (Color.green(color) * 0.55f),
                    (int) (Color.blue(color) * 0.55f));
        }

        private void drawSlime(Canvas c, float x, float y, float shadowY) {
            float bounce = jumping ? 0 : (float) Math.sin(animation * 2.2f) * 2.5f;
            float squash = jumping ? 0.91f : 1f + (float) Math.sin(animation * 2f) * 0.025f;

            p.setShader(new RadialGradient(x + 34, shadowY + 5, 48,
                    new int[]{0x88000000, 0x44000000, 0x00000000},
                    null, Shader.TileMode.CLAMP));
            c.drawOval(x - 12, shadowY - 8, x + 82, shadowY + 15, p);
            p.setShader(null);

            drawSlimeCore(c, x, y, bounce, squash, skin, acc);

            if (shield && state == PLAY) {
                setColor(0x995eeeff);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3);
                c.drawCircle(x + 34, y + 32, 47, p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        private void drawSlimeCore(Canvas c, float x, float y, float bounce, float squash,
                                   int sk, int ac) {
            int col = skinColor(sk);
            int dark = darken(col);

            c.save();
            c.translate(x + 34, y + 32 + bounce);
            c.scale(1f / squash, squash);
            c.translate(-x - 34, -y - 32 - bounce);

            p.setShader(new RadialGradient(x + 22, y + 15, 75,
                    new int[]{0xffffffff, col, dark, darken(dark)},
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
            c.drawArc(x + 23, y + 34 + bounce, x + 46, y + 52 + bounce, 15, 150, false, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.FILL);

            drawAccessory(c, x, y + bounce, ac, col);
            c.restore();
        }

        // x, top: левый верхний угол тела слайма (68x65)
        private void drawAccessory(Canvas c, float x, float top, int ac, int col) {
            float cx = x + 34;
            switch (ac) {
                case 1: { // очки
                    setColor(0xee111820);
                    c.drawRoundRect(x + 7, top + 17, x + 33, top + 40, 8, 8, p);
                    c.drawRoundRect(x + 35, top + 17, x + 61, top + 40, 8, 8, p);
                    c.drawRect(x + 31, top + 22, x + 37, top + 26, p);
                    setColor(0x55ffffff);
                    c.drawRoundRect(x + 11, top + 20, x + 19, top + 25, 3, 3, p);
                    c.drawRoundRect(x + 39, top + 20, x + 47, top + 25, 3, 3, p);
                    break;
                }
                case 2: { // колпак
                    setColor(0xffff5fa8);
                    tmp.reset();
                    tmp.moveTo(cx - 16, top + 7);
                    tmp.lineTo(cx + 16, top + 7);
                    tmp.lineTo(cx + 5, top - 34);
                    tmp.close();
                    c.drawPath(tmp, p);
                    setColor(0xffffe14a);
                    c.drawRect(cx - 12, top - 4, cx + 11, top + 1, p);
                    c.drawCircle(cx + 5, top - 35, 5, p);
                    break;
                }
                case 3: { // кепка
                    setColor(0xffd8382f);
                    c.drawArc(cx - 29, top - 5, cx + 29, top + 35, 180, 180, true, p);
                    c.drawRoundRect(cx, top + 12, cx + 42, top + 19, 4, 4, p);
                    setColor(0xffffffff);
                    c.drawCircle(cx - 2, top + 3, 3, p);
                    break;
                }
                case 4: { // наушники
                    setColor(0xff2b2f3a);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(5);
                    c.drawArc(cx - 34, top - 10, cx + 34, top + 44, 180, 180, false, p);
                    p.setStyle(Paint.Style.FILL);
                    setColor(0xffff4d6d);
                    c.drawRoundRect(x - 6, top + 22, x + 7, top + 46, 5, 5, p);
                    c.drawRoundRect(x + 61, top + 22, x + 74, top + 46, 5, 5, p);
                    break;
                }
                case 5: { // кошачьи ушки
                    int d = darken(col);
                    for (int s = 0; s < 2; s++) {
                        float m = s == 0 ? 1 : -1;
                        float bx = cx + m * -24;
                        setColor(d);
                        tmp.reset();
                        tmp.moveTo(bx - m * 2 - 9 * m * -1 * -1, top + 14);
                        tmp.lineTo(cx + m * -21, top - 15);
                        tmp.lineTo(cx + m * -5, top + 4);
                        tmp.close();
                        c.drawPath(tmp, p);
                        setColor(0xffff9ec4);
                        tmp.reset();
                        tmp.moveTo(cx + m * -22, top + 6);
                        tmp.lineTo(cx + m * -20, top - 7);
                        tmp.lineTo(cx + m * -12, top + 2);
                        tmp.close();
                        c.drawPath(tmp, p);
                    }
                    break;
                }
                case 6: { // цветок
                    float fx = x + 50, fy = top + 6;
                    setColor(0xffff7eb6);
                    for (int j = 0; j < 5; j++) {
                        double a = j * 1.2566;
                        c.drawCircle(fx + (float) Math.cos(a) * 6, fy + (float) Math.sin(a) * 6, 5, p);
                    }
                    setColor(0xffffe14a);
                    c.drawCircle(fx, fy, 4.5f, p);
                    break;
                }
                case 7: { // цилиндр
                    setColor(0xff1b1b22);
                    c.drawRoundRect(cx - 19, top - 38, cx + 19, top + 2, 4, 4, p);
                    c.drawRoundRect(cx - 30, top - 3, cx + 30, top + 6, 4, 4, p);
                    setColor(0xffc0392b);
                    c.drawRect(cx - 19, top - 11, cx + 19, top - 3, p);
                    break;
                }
                case 8: { // шляпа мага
                    setColor(0xff5b34a8);
                    tmp.reset();
                    tmp.moveTo(cx - 20, top + 3);
                    tmp.lineTo(cx + 22, top + 3);
                    tmp.lineTo(cx + 8, top - 52);
                    tmp.close();
                    c.drawPath(tmp, p);
                    setColor(0xff4b2a8a);
                    c.drawOval(cx - 32, top - 4, cx + 32, top + 9, p);
                    setColor(0xffffd84a);
                    c.drawRect(cx - 17, top - 9, cx + 19, top - 3, p);
                    c.drawCircle(cx + 3, top - 24, 4, p);
                    break;
                }
                case 9: { // корона
                    setColor(0xffffcf3a);
                    tmp.reset();
                    tmp.moveTo(cx - 22, top + 6);
                    tmp.lineTo(cx - 22, top - 16);
                    tmp.lineTo(cx - 11, top - 5);
                    tmp.lineTo(cx, top - 22);
                    tmp.lineTo(cx + 11, top - 5);
                    tmp.lineTo(cx + 22, top - 16);
                    tmp.lineTo(cx + 22, top + 6);
                    tmp.close();
                    c.drawPath(tmp, p);
                    setColor(0xffd59a00);
                    c.drawRect(cx - 22, top + 1, cx + 22, top + 6, p);
                    setColor(0xffe0244a);
                    c.drawCircle(cx, top - 3, 3.5f, p);
                    break;
                }
                case 10: { // нимб
                    setColor(0x55ffe98a);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(9);
                    c.drawOval(cx - 22, top - 26, cx + 22, top - 14, p);
                    setColor(0xffffe98a);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(4);
                    c.drawOval(cx - 22, top - 26, cx + 22, top - 14, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                }
                default:
                    break;
            }
        }

        // ---------- ПРЕПЯТСТВИЯ И МОНЕТА ----------
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
                if (level == 3) {
                    setColor(0xf0ffffff);
                    c.drawRoundRect(obstacleX - 2, ground - 86, obstacleX + obstacleW + 2, ground - 70, 8, 8, p);
                }
            } else {
                p.setShader(new LinearGradient(obstacleX, ground - 25, obstacleX, ground,
                        0xff7c8589, 0xff242d32, Shader.TileMode.CLAMP));
                c.drawRoundRect(obstacleX, ground - 25, obstacleX + obstacleW, ground, 5, 5, p);
                p.setShader(null);
                for (int i = 0; i < 3; i++) {
                    float x = obstacleX + 5 + i * 19;
                    tmp.reset();
                    tmp.moveTo(x, ground - 23);
                    tmp.lineTo(x + 10, ground - 78);
                    tmp.lineTo(x + 20, ground - 23);
                    tmp.close();
                    p.setShader(new LinearGradient(x, ground - 78, x + 20, ground,
                            0xfff0f3f1, 0xff59666b, Shader.TileMode.CLAMP));
                    c.drawPath(tmp, p);
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
                // ---------- ЧАСТИЦЫ ----------
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

        // ---------- ИГРОВАЯ ЛОГИКА ----------
        private void resetRun() {
            score = 0;
            scoreAcc = 0;
            slimeY = ground - 65;
            velocity = 0;
            obstacleX = 500;
            coinX = 750;
            coinY = 540;
            speed = levelStart[level];
            obstacleType = random.nextInt(3);
            jumping = false;
            shield = false;
            shieldTime = 0;
            combo = 0;
            shake = 0;
            newUnlock = false;
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
            sceneryOffset += speed * dt;
            updateParticles(dt);
            updateWeather(dt);

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
                speed = Math.min(speed + 0.18f, levelMax[level]);
            }
            if (coinX < -30) {
                coinX = 480 + random.nextInt(180);
                coinY = ground - 90 - random.nextInt(100);
            }

            RectF player = new RectF(slimeX + 7, slimeY + 8, slimeX + 61, slimeY + 60);
            RectF obstacle;
            if (obstacleType == 0) {
                obstacle = new RectF(obstacleX, ground - 65, obstacleX + obstacleW, ground);
            } else if (obstacleType == 1) {
                obstacle = new RectF(obstacleX, ground - 80, obstacleX + obstacleW, ground);
            } else {
                obstacle = new RectF(obstacleX, ground - 78, obstacleX + obstacleW, ground);
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
                    int old = bestL[level];
                    if (score > bestL[level]) bestL[level] = score;
                    newUnlock = level < NLEV - 1
                            && old < levelNeed[level + 1] && score >= levelNeed[level + 1];
                    saveData();
                    playTone(ToneGenerator.TONE_PROP_NACK, 300);
                    return;
                }
            }

            RectF coin = new RectF(coinX - 17, coinY - 17, coinX + 17, coinY + 17);
            if (RectF.intersects(player, coin)) {
                coins += 1 + level; // на тяжёлых уровнях монеты ценнее
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
                c.translate((random.nextFloat() - 0.5f) * shake,
                        (random.nextFloat() - 0.5f) * shake);
            }

            drawBackground(c);
            drawFireflies(c);
            drawCoin(c);
            drawObstacle(c);
            drawSlime(c, slimeX, slimeY, ground);
            drawParticles(c);
            drawBlades(c, 26, 806, 56, 1.35f, grassB[level], grassA[level], 17);
            drawWeather(c);

            c.restore();

            panel(c, 12, 15, 130, 60, 0xaa102334);
            drawText(c, tr("SCORE", "СЧЁТ", "PUNKTE"), 70, 34, 12, 0xffb9d5df);
            drawText(c, String.valueOf(score), 70, 53, 20, Color.WHITE);

            panel(c, 145, 15, 255, 60, 0xaa102334);
            drawText(c, tr("COINS", "МОНЕТЫ", "MÜNZEN"), 200, 34, 12, 0xffffdf75);
            drawText(c, String.valueOf(coins), 200, 53, 20, Color.WHITE);

            panel(c, 270, 15, 388, 60, 0xaa102334);
            drawTextA(c, tr("BEST: ", "РЕКОРД: ", "BEST: ") + bestL[level],
                    329, 45, 17, Color.WHITE, 106, Paint.Align.CENTER);

            drawTextA(c, levelNames[level][lang], 14, 80, 12, 0xddffffff, 220, Paint.Align.LEFT);

            if (state == PLAY) {
                panel(c, 345, 75, 388, 118, 0xaa102334);
                setColor(Color.WHITE);
                c.drawRoundRect(358, 87, 364, 106, 2, 2, p);
                c.drawRoundRect(369, 87, 375, 106, 2, 2, p);
            }
                }
                // ---------- ЭКРАНЫ ----------
        private void drawMenu(Canvas c) {
            drawBackground(c);
            drawFireflies(c);

            drawText(c, "SLIME", 200, 170, 67, Color.WHITE);
            drawText(c, "RUSH", 200, 235, 67, skinColor(skin));
            drawText(c, tr("THE LITTLE SLIME ADVENTURE", "ПРИКЛЮЧЕНИЯ МАЛЕНЬКОГО СЛАЙМА",
                    "DAS KLEINE SCHLEIM-ABENTEUER"), 200, 268, 13, 0xffffffff);

            float bounce = (float) Math.sin(animation * 2.2f) * 2.5f;
            drawSlimeCore(c, 166, 300, bounce, 1f + (float) Math.sin(animation * 2f) * 0.025f, skin, acc);

            button(c, tr("PLAY", "ИГРАТЬ", "SPIELEN"), 400, 0xff20a96b);
            button(c, tr("LEVELS", "УРОВНИ", "LEVEL"), 470, 0xffc0702d);
            button(c, tr("SHOP", "МАГАЗИН", "SHOP"), 540, 0xff287db9);
            button(c, tr("SETTINGS", "НАСТРОЙКИ", "EINSTELLUNGEN"), 610, 0xff6555a8);

            drawText(c, levelNames[level][lang], 200, 700, 15, 0xffffffff);
            drawText(c, tr("BEST SCORE: ", "РЕКОРД: ", "BESTWERT: ") + bestL[level],
                    200, 725, 18, Color.WHITE);
            drawText(c, tr("TAP TO JUMP \u2022 AVOID OBSTACLES",
                    "ТАП — ПРЫЖОК \u2022 ИЗБЕГАЙ ПРЕПЯТСТВИЙ",
                    "TIPPEN = SPRINGEN \u2022 HINDERNISSE MEIDEN"), 200, 765, 12, 0xffffffff);
            drawWeather(c);
        }

        private void drawLevels(Canvas c) {
            drawBackground(c);
            setColor(0x88071522);
            c.drawRect(0, 0, 400, 800, p);

            drawText(c, tr("LEVELS", "УРОВНИ", "LEVEL"), 200, 85, 35, Color.WHITE);

            for (int i = 0; i < NLEV; i++) {
                float y = 135 + i * 100;
                boolean open = levelUnlocked(i);
                panel(c, 30, y, 370, y + 88, open ? 0xcc142b3c : 0xcc1a1f26);

                // мини-превью уровня
                c.save();
                c.clipRect(45, y + 8, 125, y + 80);
                setColor(skyCols[i][1]);
                c.drawRect(45, y + 8, 125, y + 80, p);
                setColor(skyCols[i][3]);
                c.drawRect(45, y + 40, 125, y + 80, p);
                setColor(hillTop[i]);
                c.drawRect(45, y + 44, 125, y + 80, p);
                for (int k = 0; k < 3; k++) {
                    float tx = 58 + k * 26;
                    setColor(k == 1 ? nearC[i] : midC[i]);
                    tmp.reset();
                    tmp.moveTo(tx, y + 18 + (k == 1 ? 0 : 8));
                    tmp.lineTo(tx - 13, y + 66);
                    tmp.lineTo(tx + 13, y + 66);
                    tmp.close();
                    c.drawPath(tmp, p);
                }
                setColor(groundTop[i]);
                c.drawRect(45, y + 66, 125, y + 80, p);
                if (!open) {
                    setColor(0xaa000000);
                    c.drawRect(45, y + 8, 125, y + 80, p);
                }
                c.restore();

                drawTextA(c, levelNames[i][lang], 140, y + 33, 17,
                        open ? Color.WHITE : 0xff9aa5b0, 220, Paint.Align.LEFT);
                if (open) {
                    drawTextA(c, tr("BEST: ", "РЕКОРД: ", "BEST: ") + bestL[i], 140, y + 58, 13,
                            0xffffdf75, 220, Paint.Align.LEFT);
                    if (i == level) {
                        drawTextA(c, tr("SELECTED", "ВЫБРАН", "GEWÄHLT"), 140, y + 78, 12,
                                0xff63ed9c, 220, Paint.Align.LEFT);
                    }
                } else {
                    drawTextA(c, tr("LOCKED: " + levelNeed[i] + " PTS ON PREVIOUS LEVEL",
                            "ЗАКРЫТО: " + levelNeed[i] + " ОЧКОВ НА ПРЕДЫДУЩЕМ",
                            "GESPERRT: " + levelNeed[i] + " PUNKTE IM VORHERIGEN"),
                            140, y + 58, 12, 0xffff9a7a, 220, Paint.Align.LEFT);
                }
                if (i == level) {
                    setColor(0xffffffff);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(3);
                    c.drawRoundRect(30, y, 370, y + 88, 20, 20, p);
                    p.setStyle(Paint.Style.FILL);
                }
            }
            button(c, tr("BACK", "НАЗАД", "ZURÜCK"), 655, 0xff596b7b);
        }

        private void drawShop(Canvas c) {
            drawBackground(c);
            setColor(0x88071522);
            c.drawRect(0, 0, 400, 800, p);

            drawText(c, tr("SLIME SHOP", "МАГАЗИН СЛАЙМА", "SLIME-SHOP"), 200, 70, 33, Color.WHITE);

            panel(c, 40, 95, 195, 140, shopTab == 0 ? 0xff287db9 : 0x99142b3c);
            drawTextA(c, tr("SKINS", "СКИНЫ", "SKINS"), 117, 125, 17, Color.WHITE, 140, Paint.Align.CENTER);
            panel(c, 205, 95, 360, 140, shopTab == 1 ? 0xff287db9 : 0x99142b3c);
            drawTextA(c, tr("ACCESSORIES", "АКСЕССУАРЫ", "ZUBEHÖR"), 282, 125, 15, Color.WHITE, 145, Paint.Align.CENTER);

            panel(c, 100, 150, 300, 190, 0xaa102334);
            drawText(c, tr("YOUR COINS: ", "МОНЕТЫ: ", "MÜNZEN: ") + coins, 200, 177, 19, 0xffffdf75);

            int n = shopTab == 0 ? NSKIN : NACC;
            int pages = (n + 5) / 6;
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 2; col++) {
                    int idx = shopPage * 6 + r * 2 + col;
                    if (idx >= n) continue;
                    float x0 = col == 0 ? 30 : 205;
                    float y0 = 205 + r * 147;
                    boolean equipped = shopTab == 0 ? skin == idx : acc == idx;
                    boolean owned = shopTab == 0 ? skinOwn[idx] : accOwn[idx];
                    int price = shopTab == 0 ? skinPrices[idx] : accPrices[idx];

                    panel(c, x0, y0, x0 + 165, y0 + 135, equipped ? 0xee1f5a3f : 0xcc142b3c);

                    c.save();
                    c.translate(x0 + 82, y0 + 62);
                    c.scale(0.8f, 0.8f);
                    drawSlimeCore(c, -34, -32, 0, 1f, shopTab == 0 ? idx : skin, shopTab == 0 ? acc : idx);
                    c.restore();

                    String name = shopTab == 0 ? skinNames[idx][lang] : accNames[idx][lang];
                    drawTextA(c, name, x0 + 82, y0 + 105, 14, Color.WHITE, 150, Paint.Align.CENTER);

                    String action;
                    int actionColor;
                    if (equipped) {
                        action = tr("EQUIPPED", "ВЫБРАНО", "GEWÄHLT");
                        actionColor = 0xff63ed9c;
                    } else if (owned) {
                        action = tr("SELECT", "ВЫБРАТЬ", "WÄHLEN");
                        actionColor = 0xffffffff;
                    } else {
                        action = price + " " + tr("COINS", "МОНЕТ", "MÜNZEN");
                        actionColor = coins >= price ? 0xffffdf75 : 0xffff9a7a;
                    }
                    drawTextA(c, action, x0 + 82, y0 + 124, 12, actionColor, 150, Paint.Align.CENTER);
                }
            }

            drawText(c, (shopPage + 1) + " / " + pages, 200, 652, 13, 0xffd8e9e9);
            panel(c, 30, 660, 100, 721, 0xff596b7b);
            drawText(c, "<", 65, 700, 30, Color.WHITE);
            panel(c, 110, 660, 290, 721, 0xff596b7b);
            drawText(c, tr("BACK", "НАЗАД", "ZURÜCK"), 200, 698, 21, Color.WHITE);
            panel(c, 300, 660, 370, 721, 0xff596b7b);
            drawText(c, ">", 335, 700, 30, Color.WHITE);
        }

        private void settingsRow(Canvas c, float y, String label, String value, int valueColor) {
            panel(c, 35, y, 365, y + 74, 0xcc142b3c);
            drawTextA(c, label, 55, y + 45, 20, Color.WHITE, 170, Paint.Align.LEFT);
            drawTextA(c, value, 345, y + 45, 20, valueColor, 140, Paint.Align.RIGHT);
        }

        private void drawSettings(Canvas c) {
            drawBackground(c);
            setColor(0x88071522);
            c.drawRect(0, 0, 400, 800, p);

            drawText(c, tr("SETTINGS", "НАСТРОЙКИ", "EINSTELLUNGEN"), 200, 125, 36, Color.WHITE);

            settingsRow(c, 190, tr("SOUND", "ЗВУК", "TON"),
                    soundOn ? tr("ON", "ВКЛ", "AN") : tr("OFF", "ВЫКЛ", "AUS"),
                    soundOn ? 0xff63ed9c : 0xffff7777);
            settingsRow(c, 280, tr("PARTICLES", "ЭФФЕКТЫ", "EFFEKTE"),
                    particlesOn ? tr("ON", "ВКЛ", "AN") : tr("OFF", "ВЫКЛ", "AUS"),
                    particlesOn ? 0xff63ed9c : 0xffff7777);
            settingsRow(c, 370, tr("LANGUAGE", "ЯЗЫК", "SPRACHE"), langNames[lang], 0xffffdf75);

            button(c, tr("BACK", "НАЗАД", "ZURÜCK"), 580, 0xff596b7b);
        }

        private void drawPause(Canvas c) {
            setColor(0xaa071522);
            c.drawRect(0, 0, 400, 800, p);

            panel(c, 35, 245, 365, 570, 0xee142b3c);

            drawText(c, tr("PAUSED", "ПАУЗА", "PAUSE"), 200, 310, 39, Color.WHITE);
            drawText(c, tr("Take a little break!", "Небольшой перерыв!", "Kleine Pause!"),
                    200, 350, 16, 0xffc8dce5);

            button(c, tr("RESUME", "ПРОДОЛЖИТЬ", "WEITER"), 390, 0xff20a96b);
            button(c, tr("RESTART", "ЗАНОВО", "NEUSTART"), 465, 0xff287db9);
            button(c, tr("MENU", "МЕНЮ", "MENÜ"), 540, 0xff596b7b);
        }

        private void drawGameOver(Canvas c) {
            setColor(0xbb071522);
            c.drawRect(0, 0, 400, 800, p);

            panel(c, 30, 195, 370, 665, 0xee142b3c);

            drawText(c, levelNames[level][lang], 200, 225, 14, 0xffb9d5df);
            drawText(c, tr("GAME OVER", "ИГРА ОКОНЧЕНА", "SPIEL VORBEI"), 200, 285, 37, 0xffff7777);
            drawText(c, tr("YOUR SCORE", "ТВОЙ СЧЁТ", "DEIN PUNKTESTAND"), 200, 340, 17, 0xffb9d5df);
            drawText(c, String.valueOf(score), 200, 400, 55, Color.WHITE);
            drawText(c, tr("BEST: ", "РЕКОРД: ", "BEST: ") + bestL[level], 200, 440, 19, 0xffffdf75);
            drawText(c, tr("COINS: ", "МОНЕТЫ: ", "MÜNZEN: ") + coins, 200, 468, 18, 0xffffdf75);
            if (newUnlock) {
                drawText(c, tr("NEW LEVEL UNLOCKED!", "ОТКРЫТ НОВЫЙ УРОВЕНЬ!",
                        "NEUES LEVEL FREIGESCHALTET!"), 200, 495, 15, 0xff63ed9c);
            }

            button(c, tr("TRY AGAIN", "ЕЩЁ РАЗ", "NOCHMAL"), 510, 0xff20a96b);
            button(c, tr("MAIN MENU", "ГЛАВНОЕ МЕНЮ", "HAUPTMENÜ"), 585, 0xff596b7b);
        }

        // ---------- УПРАВЛЕНИЕ ----------
        private void jump() {
            if (state != PLAY || jumping) return;
            velocity = -17.5f;
            jumping = true;
            spawnParticles(slimeX + 34, ground - 5, 5);
            playTone(ToneGenerator.TONE_PROP_BEEP, 70);
        }

        private void buyOrSelect(int idx) {
            boolean[] own = shopTab == 0 ? skinOwn : accOwn;
            int[] price = shopTab == 0 ? skinPrices : accPrices;
            if (shopTab == 1 && acc == idx && idx != 0) { // повторный тап снимает аксессуар
                acc = 0;
                saveData();
                playTone(ToneGenerator.TONE_PROP_ACK, 80);
                return;
            }
            if (!own[idx]) {
                if (coins < price[idx]) {
                    playTone(ToneGenerator.TONE_PROP_NACK, 150);
                    return;
                }
                coins -= price[idx];
                own[idx] = true;
            }
            if (shopTab == 0) skin = idx;
            else acc = idx;
            saveData();
            playTone(ToneGenerator.TONE_PROP_ACK, 120);
        }

        private void handleShopTap(float x, float y) {
            int n = shopTab == 0 ? NSKIN : NACC;
            int pages = (n + 5) / 6;

            if (y >= 95 && y <= 140) {
                if (x >= 40 && x <= 195) { shopTab = 0; shopPage = 0; }
                else if (x >= 205 && x <= 360) { shopTab = 1; shopPage = 0; }
                return;
            }
            if (y >= 660 && y <= 721) {
                if (x >= 30 && x <= 100) shopPage = (shopPage + pages - 1) % pages;
                else if (x >= 300 && x <= 370) shopPage = (shopPage + 1) % pages;
                else if (x >= 110 && x <= 290) state = MENU;
                return;
            }
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 2; col++) {
                    float x0 = col == 0 ? 30 : 205;
                    float y0 = 205 + r * 147;
                    if (x >= x0 && x <= x0 + 165 && y >= y0 && y <= y0 + 135) {
                        int idx = shopPage * 6 + r * 2 + col;
                        if (idx < n) buyOrSelect(idx);
                        return;
                    }
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN
                    && event.getActionMasked() != MotionEvent.ACTION_POINTER_DOWN) {
                return true;
            }

            float scale = Math.min(getWidth() / 400f, getHeight() / 800f);
            if (scale <= 0) return true;
            float dx = (getWidth() - 400 * scale) / 2f;
            float dy = (getHeight() - 800 * scale) / 2f;

            int idx = event.getActionIndex();
            float x = (event.getX(idx) - dx) / scale;
            float y = (event.getY(idx) - dy) / scale;

            if (state == MENU) {
                if (y >= 400 && y <= 461) startGame();
                else if (y >= 470 && y <= 531) state = LEVELS;
                else if (y >= 540 && y <= 601) { state = SHOP; shopPage = 0; }
                else if (y >= 610 && y <= 671) state = SETTINGS;
            } else if (state == PLAY) {
                if (x >= 335 && y <= 130) {
                    state = PAUSE;
                    shake = 0;
                } else {
                    jump();
                }
            } else if (state == LEVELS) {
                if (y >= 655 && y <= 716) {
                    state = MENU;
                } else {
                    for (int i = 0; i < NLEV; i++) {
                        float top = 135 + i * 100;
                        if (y >= top && y <= top + 88) {
                            if (levelUnlocked(i)) {
                                level = i;
                                initWeather();
                                saveData();
                                playTone(ToneGenerator.TONE_PROP_ACK, 100);
                                state = MENU;
                            } else {
                                playTone(ToneGenerator.TONE_PROP_NACK, 150);
                            }
                            break;
                        }
                    }
                }
            } else if (state == SHOP) {
                handleShopTap(x, y);
            } else if (state == SETTINGS) {
                if (y >= 190 && y <= 264) {
                    soundOn = !soundOn;
                    saveData();
                } else if (y >= 280 && y <= 354) {
                    particlesOn = !particlesOn;
                    saveData();
                } else if (y >= 370 && y <= 444) {
                    lang = (lang + 1) % 3;
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
                if (y >= 510 && y <= 571) startGame();
                else if (y >= 585 && y <= 646) state = MENU;
            }

            invalidate();
            return true;
        }
    }
                }
