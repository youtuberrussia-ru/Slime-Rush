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
        private static final int IC_PLAY = 0, IC_SHOP = 1, IC_GEAR = 2, IC_MAP = 3,
                IC_BACK = 4, IC_LOCK = 5, IC_STAR = 6, IC_COIN = 7, IC_SOUND = 8,
                IC_SPARK = 9, IC_GLOBE = 10, IC_CHECK = 11, IC_PAUSE = 12,
                IC_HOME = 13, IC_RETRY = 14;

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Typeface font = Typeface.DEFAULT_BOLD;
        private final Path tmp = new Path();
        private final Path farPath = new Path();
        private final Path nearPath = new Path();
        private final float[] hsv = new float[3];
        private final Random random = new Random();
        private final SharedPreferences prefs;
        private ToneGenerator sound;

        private int state = MENU;
        private int coins, score, skin, acc, lang, level, coinsRun;
        private final int[] bestL = new int[NLEV];
        private boolean soundOn, particlesOn, newUnlock, newBest;
        private final boolean[] skinOwn = new boolean[NSKIN];
        private final boolean[] accOwn = new boolean[NACC];
        private int shopTab, shopPage;

        // ================= ЯЗЫКИ =================
        private final String[] langNames = {"ENGLISH", "РУССКИЙ", "DEUTSCH"};
        private final String[] langCodes = {"EN", "RU", "DE"};

        private String tr(String en, String ru, String de) {
            return lang == 1 ? ru : (lang == 2 ? de : en);
        }

        // ================= СКИНЫ И АКСЕССУАРЫ =================
        private final int[] skinCols = {
                Color.rgb(66, 245, 125), Color.rgb(50, 174, 255),
                Color.rgb(255, 104, 185), Color.rgb(255, 207, 58),
                Color.rgb(177, 112, 255), Color.rgb(255, 86, 40),
                Color.rgb(150, 230, 255), Color.rgb(170, 255, 40),
                Color.rgb(70, 70, 95), Color.WHITE
        };
        private final int[] skinPrices = {0, 50, 100, 200, 350, 400, 450, 500, 600, 1000};
        private final String[][] skinNames = {
                {"MINT", "МЯТА", "MINZE"}, {"OCEAN", "ОКЕАН", "OZEAN"},
                {"BUBBLEGUM", "ЖВАЧКА", "KAUGUMMI"}, {"GOLD", "ЗОЛОТО", "GOLD"},
                {"GALAXY", "ГАЛАКТИКА", "GALAXIE"}, {"LAVA", "ЛАВА", "LAVA"},
                {"ICE", "ЛЁД", "EIS"}, {"TOXIC", "ЯД", "GIFT"},
                {"SHADOW", "ТЕНЬ", "SCHATTEN"}, {"RAINBOW", "РАДУГА", "REGENBOGEN"}
        };
        private final int[] accPrices = {0, 60, 80, 100, 150, 200, 250, 300, 450, 700, 800};
        private final String[][] accNames = {
                {"NONE", "НЕТ", "KEINE"}, {"SUNGLASSES", "ОЧКИ", "SONNENBRILLE"},
                {"PARTY HAT", "КОЛПАК", "PARTYHUT"}, {"CAP", "КЕПКА", "KAPPE"},
                {"HEADPHONES", "НАУШНИКИ", "KOPFHÖRER"}, {"CAT EARS", "УШКИ КОТА", "KATZENOHREN"},
                {"FLOWER", "ЦВЕТОК", "BLUME"}, {"TOP HAT", "ЦИЛИНДР", "ZYLINDER"},
                {"WIZARD HAT", "ШЛЯПА МАГА", "ZAUBERHUT"}, {"CROWN", "КОРОНА", "KRONE"},
                {"HALO", "НИМБ", "HEILIGENSCHEIN"}
        };

        // ================= УРОВНИ =================
        private final String[][] levelNames = {
                {"SUNNY FOREST", "СОЛНЕЧНЫЙ ЛЕС", "SONNIGER WALD"},
                {"AUTUMN FOREST", "ОСЕННИЙ ЛЕС", "HERBSTWALD"},
                {"NIGHT FOREST", "НОЧНОЙ ЛЕС", "NACHTWALD"},
                {"SNOWY FOREST", "ЗАСНЕЖЕННЫЙ ЛЕС", "VERSCHNEITER WALD"},
                {"MAGIC FOREST", "ВОЛШЕБНЫЙ ЛЕС", "ZAUBERWALD"}
        };
        private final int[] levelNeed = {0, 2000, 3000, 4000, 5000};
        private final float[] levelStart = {7f, 8f, 8.5f, 9f, 10f};
        private final float[] levelMax = {15f, 16f, 17f, 18f, 19.5f};
        private final int[][] obstaclePool = {
                {0, 1, 3, 5}, {0, 1, 2, 3, 5, 6}, {0, 2, 3, 4, 5, 6},
                {0, 1, 2, 3, 4, 5, 6}, {0, 1, 2, 3, 4, 5, 6}
        };

        // ================= ПАЛИТРЫ УРОВНЕЙ =================
        private final int[][] skyCols = {
                {0xff3a8fd8, 0xff7cc4ee, 0xffcdeedd, 0xfff7f2bd},
                {0xff5f7fa8, 0xffd9a07c, 0xfff0c27c, 0xfffadf9f},
                {0xff040a1c, 0xff0d1f45, 0xff1b3962, 0xff2a5570},
                {0xff6f93b8, 0xffa9c6dc, 0xffd6e6ef, 0xfff2f7fa},
                {0xff180a38, 0xff3a1a6a, 0xff7b3fa2, 0xffe08fc8}
        };
        private final int[] mtFar = {0xff8fb0c4, 0xffb88a78, 0xff2a3f66, 0xffaec3d2, 0xff6d4c9c};
        private final int[] mtNear = {0xff628c88, 0xff8c5c48, 0xff1b2c4a, 0xff8299ac, 0xff4a327c};
        private final int[] capCol = {0xfff2f6f8, 0, 0, 0xffffffff, 0xffffc8f0};
        private final int[] farC = {0xff5a9a82, 0xffa86a42, 0xff17344a, 0xff86a8ba, 0xff54308a};
        private final int[] midC = {0xff2f7a4f, 0xffbc5a2a, 0xff0f2a3d, 0xff5a8590, 0xff3a1f6e};
        private final int[] nearC = {0xff1b5a36, 0xff7e3220, 0xff08171f, 0xff3b6165, 0xff22104a};
        private final int[] trunkC = {0xff4a3220, 0xff3d2818, 0xff1a1410, 0xff4a3a30, 0xff2a1a3a};
        private final int[] grassA = {0xff86c45a, 0xffd9a441, 0xff3f7a4c, 0xfff2f7fb, 0xff5de0d0};
        private final int[] grassB = {0xff4a8a3a, 0xff9a5a26, 0xff25503a, 0xffc3d6e4, 0xffb070e8};
        private final int[] soilMid = {0xff8a6a3f, 0xff7a5430, 0xff3a3a2a, 0xff9fb6c8, 0xff342a5a};
        private final int[] soilBot = {0xff4a3b24, 0xff3d2a1c, 0xff1a1d17, 0xff6f879b, 0xff1d1538};
        private final int[] haze = {0xffd6f0e0, 0xffffd9a8, 0xff3a5a80, 0xffeaf4fa, 0xffb070d8};
        private final int[] grade = {0x14ffd27a, 0x1eff8a2a, 0x2a1030a0, 0x14a0c8ff, 0x24a040ff};
        private final float[] celX = {315, 300, 310, 320, 300};
        private final float[] celY = {115, 210, 110, 120, 150};
        private final float[] celR = {25, 38, 24, 20, 34};
        private final int[] celCol = {0xfffff2b0, 0xffffb86b, 0xfff0f4ff, 0xfffff6dd, 0xffffd6f6};
        private final boolean[] hasStars = {false, false, true, false, true};
        private final boolean[] hasClouds = {true, true, false, true, false};
        private final boolean[] hasRays = {true, true, false, false, false};
        private final boolean[] hasBirds = {true, true, false, false, false};
        private final boolean[] hasFireflies = {false, false, true, false, true};
        private final int[] weatherCount = {12, 30, 0, 36, 24};

        private final float[] farMt = {-10, 470, 30, 400, 70, 430, 120, 340, 170, 410,
                215, 375, 265, 330, 310, 405, 350, 370, 410, 440};
        private final float[] nearMt = {-10, 520, 40, 450, 90, 495, 150, 400, 200, 480,
                250, 430, 300, 500, 350, 445, 410, 510};

        private final Shader[] skyS = new Shader[NLEV];
        private final Shader[] glowS = new Shader[NLEV];
        private final Shader[] fogS = new Shader[NLEV];
        private final Shader[] fogLowS = new Shader[NLEV];
        private final Shader[] grassG = new Shader[NLEV];
        private final Shader[] soilS = new Shader[NLEV];
        private Shader dimS, vigS;

        // ================= ИГРОВОЕ СОСТОЯНИЕ =================
        private float slimeX = 83, slimeY, velocity;
        private final float ground = 650;
        private float obstacleX, obstacleW = 58;
        private float coinX, coinY;
        private float speed = 7, animation, sceneryOffset;
        private float shake, scoreAcc;
        private int obstacleType;
        private boolean jumping;
        private long lastFrame;
        private final float[] particleX = new float[32];
        private final float[] particleY = new float[32];
        private final float[] particleVX = new float[32];
        private final float[] particleVY = new float[32];
        private final int[] particleLife = new int[32];
        private final int[] particleCol = new int[32];
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

        private int starsFor(int s) {
            return s >= 3000 ? 3 : (s >= 1500 ? 2 : (s >= 500 ? 1 : 0));
        }

        private void buildRange(float[] pts, Path path) {
            path.reset();
            path.moveTo(pts[0], 650);
            for (int i = 0; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
            path.lineTo(pts[pts.length - 2], 650);
            path.close();
        }

        private void buildShaders() {
            for (int i = 0; i < NLEV; i++) {
                skyS[i] = new LinearGradient(0, 0, 0, 700, skyCols[i], null, Shader.TileMode.CLAMP);
                int hz = haze[i] & 0xffffff;
                fogS[i] = new LinearGradient(0, 440, 0, 650, hz, (0x99 << 24) | hz, Shader.TileMode.CLAMP);
                fogLowS[i] = new LinearGradient(0, 470, 0, 650, hz, (0x66 << 24) | hz, Shader.TileMode.CLAMP);
                grassG[i] = new LinearGradient(0, 632, 0, 668, grassA[i], grassB[i], Shader.TileMode.CLAMP);
                soilS[i] = new LinearGradient(0, 668, 0, 800, soilMid[i], soilBot[i], Shader.TileMode.CLAMP);
                glowS[i] = new RadialGradient(celX[i], celY[i], celR[i] * 5f,
                        new int[]{al(celCol[i], 0x99), al(celCol[i], 0x33), al(celCol[i], 0)},
                        null, Shader.TileMode.CLAMP);
            }
            dimS = new LinearGradient(0, 0, 0, 800,
                    new int[]{0xb0050d16, 0x40050d16, 0x70050d16, 0xd0050d16},
                    null, Shader.TileMode.CLAMP);
            vigS = new RadialGradient(200, 400, 560,
                    new int[]{0x00000000, 0x00000000, 0x66000000},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
            buildRange(farMt, farPath);
            buildRange(nearMt, nearPath);
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

        // ================= ЦВЕТ И ГЕОМЕТРИЯ =================
        private int al(int color, int alpha) {
            return (alpha << 24) | (color & 0xffffff);
        }

        private int mul(int color, float k) {
            return Color.rgb(Math.min(255, (int) (Color.red(color) * k)),
                    Math.min(255, (int) (Color.green(color) * k)),
                    Math.min(255, (int) (Color.blue(color) * k)));
        }

        private int lighten(int color, float k) {
            return Color.rgb((int) (Color.red(color) + (255 - Color.red(color)) * k),
                    (int) (Color.green(color) + (255 - Color.green(color)) * k),
                    (int) (Color.blue(color) + (255 - Color.blue(color)) * k));
        }

        private int darken(int color) {
            return mul(color, 0.55f);
        }

        private float rnd(int idx, int seed) {
            int h = idx * 374761393 + seed * 668265263;
            h = (h ^ (h >>> 13)) * 1274126177;
            h ^= (h >>> 16);
            return (h & 0xffff) / 65535f;
        }

        private void setColor(int color) {
            p.setColor(color);
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(1);
        }
                // ================= ТЕКСТ И ЭЛЕМЕНТЫ ИНТЕРФЕЙСА =================
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

        private void drawTextS(Canvas c, String s, float x, float y, float size,
                               int color, float maxW, Paint.Align align) {
            drawTextA(c, s, x, y + 2, size, 0x66000000, maxW, align);
            drawTextA(c, s, x, y, size, color, maxW, align);
        }

        private void drawTitle(Canvas c, String s, float x, float y, float size, int fill) {
            p.setShader(null);
            p.setTypeface(font);
            p.setTextSize(size);
            p.setTextAlign(Paint.Align.CENTER);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(size * 0.2f);
            p.setColor(0x59000000);
            c.drawText(s, x, y + size * 0.07f, p);
            p.setColor(0xff0e4a2a);
            c.drawText(s, x, y, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(fill);
            c.drawText(s, x, y, p);
            p.setStrokeJoin(Paint.Join.MITER);
            p.setStrokeWidth(1);
        }

        private void glass(Canvas c, float l, float t, float r, float b, float rad) {
            setColor(0x55000000);
            c.drawRoundRect(l, t + 5, r, b + 5, rad, rad, p);
            p.setShader(new LinearGradient(0, t, 0, b, 0xe0183448, 0xe00b1826, Shader.TileMode.CLAMP));
            c.drawRoundRect(l, t, r, b, rad, rad, p);
            p.setShader(null);
            setColor(0x12ffffff);
            float ir = Math.max(4, rad - 3);
            c.drawRoundRect(l + 3, t + 3, r - 3, t + Math.min(40f, (b - t) * 0.45f), ir, ir, p);
            setColor(0x44ffffff);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f);
            c.drawRoundRect(l, t, r, b, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void candy(Canvas c, float l, float t, float r, float b, int col, float rad) {
            setColor(0x59000000);
            c.drawRoundRect(l, t + 8, r, b + 8, rad, rad, p);
            setColor(mul(col, 0.55f));
            c.drawRoundRect(l, t + 5, r, b + 5, rad, rad, p);
            p.setShader(new LinearGradient(0, t, 0, b,
                    new int[]{lighten(col, 0.28f), col, mul(col, 0.86f)},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            c.drawRoundRect(l, t, r, b, rad, rad, p);
            p.setShader(null);
            setColor(0x40ffffff);
            float ir = Math.max(4, rad - 5);
            c.drawRoundRect(l + 5, t + 4, r - 5, t + (b - t) * 0.46f, ir, ir, p);
            setColor(0x66ffffff);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f);
            c.drawRoundRect(l, t, r, b, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }

        private void bar(Canvas c, float l, float t, float r, float b, float frac, int col) {
            float rad = (b - t) / 2f;
            setColor(0x66000000);
            c.drawRoundRect(l, t, r, b, rad, rad, p);
            if (frac > 0.02f) {
                p.setShader(new LinearGradient(l, 0, r, 0, lighten(col, 0.3f), col, Shader.TileMode.CLAMP));
                c.drawRoundRect(l, t, l + (r - l) * Math.min(1f, frac), b, rad, rad, p);
                p.setShader(null);
            }
        }

        private void toggle(Canvas c, float cx, float cy, boolean on) {
            float l = cx - 32, r = cx + 32, t = cy - 17, b = cy + 17;
            setColor(on ? 0xff1fb26e : 0xff4a5663);
            c.drawRoundRect(l, t, r, b, 17, 17, p);
            setColor(0x33000000);
            c.drawRoundRect(l, t, r, t + 9, 17, 17, p);
            float kx = on ? r - 17 : l + 17;
            setColor(0x55000000);
            c.drawCircle(kx, cy + 2, 14, p);
            setColor(Color.WHITE);
            c.drawCircle(kx, cy, 14, p);
        }

        private void starsRow(Canvas c, float cx, float cy, float s, int n) {
            for (int i = 0; i < 3; i++) {
                icon(c, IC_STAR, cx + (i - 1) * s * 2.4f, cy, s, i < n ? 0xffffd84a : 0x44ffffff);
            }
        }

        private void dim(Canvas c, int extra) {
            p.setShader(dimS);
            c.drawRect(0, 0, 400, 800, p);
            p.setShader(null);
            if (extra > 0) {
                setColor(extra << 24 | 0x050d16);
                c.drawRect(0, 0, 400, 800, p);
            }
        }

        private void coinChip(Canvas c, float l, float t, float r, float b) {
            glass(c, l, t, r, b, (b - t) / 2f);
            icon(c, IC_COIN, l + 22, (t + b) / 2f, 11, 0xffffd84a);
            drawTextA(c, String.valueOf(coins), r - 14, (t + b) / 2f + 7, 19, Color.WHITE,
                    r - l - 56, Paint.Align.RIGHT);
        }

        private void header(Canvas c, String title) {
            candy(c, 22, 22, 70, 70, 0xff596b7b, 24);
            icon(c, IC_BACK, 46, 46, 12, Color.WHITE);
            drawTextS(c, title, 180, 57, 25, Color.WHITE, 190, Paint.Align.CENTER);
            coinChip(c, 284, 28, 384, 64);
        }

        // ================= ИКОНКИ =================
        private void icon(Canvas c, int type, float cx, float cy, float s, int col) {
            setColor(col);
            switch (type) {
                case IC_PLAY:
                    tmp.reset();
                    tmp.moveTo(cx - s * 0.6f, cy - s * 0.85f);
                    tmp.lineTo(cx - s * 0.6f, cy + s * 0.85f);
                    tmp.lineTo(cx + s * 0.9f, cy);
                    tmp.close();
                    c.drawPath(tmp, p);
                    break;
                case IC_SHOP:
                    c.drawRoundRect(cx - s * 0.85f, cy - s * 0.3f, cx + s * 0.85f, cy + s * 0.9f,
                            s * 0.2f, s * 0.2f, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.22f);
                    c.drawArc(cx - s * 0.42f, cy - s * 0.95f, cx + s * 0.42f, cy + s * 0.2f,
                            180, 180, false, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                case IC_GEAR:
                    for (int k = 0; k < 8; k++) {
                        c.save();
                        c.rotate(k * 45, cx, cy);
                        c.drawRect(cx - s * 0.17f, cy - s, cx + s * 0.17f, cy - s * 0.6f, p);
                        c.restore();
                    }
                    c.drawCircle(cx, cy, s * 0.74f, p);
                    setColor(mul(col, 0.3f));
                    c.drawCircle(cx, cy, s * 0.3f, p);
                    break;
                case IC_MAP:
                    c.drawRect(cx - s * 0.62f, cy - s, cx - s * 0.42f, cy + s, p);
                    tmp.reset();
                    tmp.moveTo(cx - s * 0.42f, cy - s * 0.95f);
                    tmp.lineTo(cx + s * 0.95f, cy - s * 0.4f);
                    tmp.lineTo(cx - s * 0.42f, cy + s * 0.15f);
                    tmp.close();
                    c.drawPath(tmp, p);
                    break;
                case IC_BACK:
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.36f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    c.drawLine(cx + s * 0.45f, cy - s * 0.75f, cx - s * 0.4f, cy, p);
                    c.drawLine(cx - s * 0.4f, cy, cx + s * 0.45f, cy + s * 0.75f, p);
                    p.setStrokeCap(Paint.Cap.BUTT);
                    p.setStyle(Paint.Style.FILL);
                    break;
                case IC_LOCK:
                    c.drawRoundRect(cx - s * 0.7f, cy - s * 0.1f, cx + s * 0.7f, cy + s * 0.9f,
                            s * 0.2f, s * 0.2f, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.24f);
                    c.drawArc(cx - s * 0.42f, cy - s * 0.6f, cx + s * 0.42f, cy + s * 0.4f,
                            180, 180, false, p);
                    p.setStyle(Paint.Style.FILL);
                    setColor(mul(col, 0.3f));
                    c.drawCircle(cx, cy + s * 0.4f, s * 0.15f, p);
                    break;
                case IC_STAR:
                    tmp.reset();
                    for (int k = 0; k < 10; k++) {
                        double ang = Math.toRadians(-90 + k * 36);
                        float rr = (k % 2 == 0) ? s : s * 0.45f;
                        float px = cx + (float) Math.cos(ang) * rr;
                        float py = cy + (float) Math.sin(ang) * rr;
                        if (k == 0) tmp.moveTo(px, py); else tmp.lineTo(px, py);
                    }
                    tmp.close();
                    c.drawPath(tmp, p);
                    break;
                case IC_COIN:
                    c.drawCircle(cx, cy, s, p);
                    setColor(lighten(col, 0.6f));
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.16f);
                    c.drawCircle(cx, cy, s * 0.66f, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                case IC_SOUND:
                    tmp.reset();
                    tmp.moveTo(cx - s * 0.9f, cy - s * 0.3f);
                    tmp.lineTo(cx - s * 0.4f, cy - s * 0.3f);
                    tmp.lineTo(cx + s * 0.1f, cy - s * 0.8f);
                    tmp.lineTo(cx + s * 0.1f, cy + s * 0.8f);
                    tmp.lineTo(cx - s * 0.4f, cy + s * 0.3f);
                    tmp.lineTo(cx - s * 0.9f, cy + s * 0.3f);
                    tmp.close();
                    c.drawPath(tmp, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.2f);
                    c.drawArc(cx - s * 0.1f, cy - s * 0.5f, cx + s * 0.7f, cy + s * 0.5f, -45, 90, false, p);
                    c.drawArc(cx - s * 0.3f, cy - s * 0.9f, cx + s * 1.0f, cy + s * 0.9f, -45, 90, false, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                case IC_SPARK:
                    tmp.reset();
                    tmp.moveTo(cx, cy - s);
                    tmp.quadTo(cx, cy, cx + s, cy);
                    tmp.quadTo(cx, cy, cx, cy + s);
                    tmp.quadTo(cx, cy, cx - s, cy);
                    tmp.quadTo(cx, cy, cx, cy - s);
                    tmp.close();
                    c.drawPath(tmp, p);
                    c.drawCircle(cx + s * 0.8f, cy - s * 0.8f, s * 0.18f, p);
                    break;
                case IC_GLOBE:
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.16f);
                    c.drawCircle(cx, cy, s * 0.9f, p);
                    c.drawOval(cx - s * 0.4f, cy - s * 0.9f, cx + s * 0.4f, cy + s * 0.9f, p);
                    c.drawLine(cx - s * 0.9f, cy, cx + s * 0.9f, cy, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                case IC_CHECK:
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.3f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    c.drawLine(cx - s * 0.6f, cy, cx - s * 0.1f, cy + s * 0.5f, p);
                    c.drawLine(cx - s * 0.1f, cy + s * 0.5f, cx + s * 0.7f, cy - s * 0.5f, p);
                    p.setStrokeCap(Paint.Cap.BUTT);
                    p.setStyle(Paint.Style.FILL);
                    break;
                case IC_PAUSE:
                    c.drawRoundRect(cx - s * 0.6f, cy - s * 0.8f, cx - s * 0.15f, cy + s * 0.8f, 2, 2, p);
                    c.drawRoundRect(cx + s * 0.15f, cy - s * 0.8f, cx + s * 0.6f, cy + s * 0.8f, 2, 2, p);
                    break;
                case IC_HOME:
                    tmp.reset();
                    tmp.moveTo(cx - s, cy - s * 0.05f);
                    tmp.lineTo(cx, cy - s * 0.95f);
                    tmp.lineTo(cx + s, cy - s * 0.05f);
                    tmp.close();
                    c.drawPath(tmp, p);
                    c.drawRect(cx - s * 0.65f, cy - s * 0.05f, cx + s * 0.65f, cy + s * 0.85f, p);
                    setColor(mul(col, 0.3f));
                    c.drawRect(cx - s * 0.17f, cy + s * 0.3f, cx + s * 0.17f, cy + s * 0.85f, p);
                    break;
                case IC_RETRY:
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.3f);
                    c.drawArc(cx - s * 0.8f, cy - s * 0.8f, cx + s * 0.8f, cy + s * 0.8f, -50, 280, false, p);
                    p.setStyle(Paint.Style.FILL);
                    tmp.reset();
                    tmp.moveTo(cx + s * 0.1f, cy - s * 1.2f);
                    tmp.lineTo(cx + s * 0.1f, cy - s * 0.4f);
                    tmp.lineTo(cx + s * 0.95f, cy - s * 0.8f);
                    tmp.close();
                    c.drawPath(tmp, p);
                    break;
                default:
                    break;
            }
        }
                // ================= ГЛАВНЫЙ ЦИКЛ ОТРИСОВКИ =================
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

        // ================= ФОН: РЕАЛИСТИЧНЫЙ ЛЕС =================
        private void drawBackground(Canvas c) {
            int L = level;
            p.setStyle(Paint.Style.FILL);
            p.setShader(skyS[L]);
            c.drawRect(-20, -20, 420, 820, p);
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
            c.drawCircle(celX[L], celY[L], celR[L] * 5f, p);
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
                for (int k = 0; k < 5; k++) {
                    float cx = (k * 130f - sceneryOffset * 0.03f) % 580f;
                    if (cx < 0) cx += 580f;
                    cx -= 90;
                    float cy = 52 + (k * 47) % 120;
                    float s = 0.8f + (k % 3) * 0.25f;
                    cloud(c, cx, cy, s, L == 1 ? 0xccffe2c0 : (L == 3 ? 0xddf2f6fa : 0xe6ffffff),
                            L == 1 ? 0x33a0603a : 0x338aa4bd);
                }
            }
            drawBirds(c);

            drawRange(c, farMt, farPath, mtFar[L]);
            p.setShader(fogS[L]);
            c.drawRect(-20, 440, 420, 650, p);
            p.setShader(null);
            drawRange(c, nearMt, nearPath, mtNear[L]);
            p.setShader(fogLowS[L]);
            c.drawRect(-20, 470, 420, 650, p);
            p.setShader(null);

            drawTreeLayer(c, 0.04f, 34, 575, 120, 40, farC[L], 1, false);
            if (hasRays[L]) drawRays(c);
            drawTreeLayer(c, 0.10f, 50, 612, 190, 60, midC[L], 2, true);
            p.setShader(fogS[L]);
            c.drawRect(-20, 440, 420, 650, p);
            p.setShader(null);
            drawTreeLayer(c, 0.22f, 84, 646, 290, 90, nearC[L], 3, true);

            drawGround(c);

            setColor(grade[L]);
            c.drawRect(-20, -20, 420, 820, p);
            p.setShader(vigS);
            c.drawRect(-20, -20, 420, 820, p);
            p.setShader(null);
        }

        private void cloud(Canvas c, float cx, float cy, float s, int col, int shadow) {
            setColor(shadow);
            c.drawOval(cx - 46 * s, cy - 6 * s, cx + 46 * s, cy + 16 * s, p);
            setColor(col);
            c.drawOval(cx - 44 * s, cy - 12 * s, cx + 44 * s, cy + 12 * s, p);
            c.drawOval(cx - 26 * s, cy - 28 * s, cx + 14 * s, cy + 2 * s, p);
            c.drawOval(cx - 2 * s, cy - 22 * s, cx + 34 * s, cy + 6 * s, p);
            setColor(0x55ffffff);
            c.drawOval(cx - 20 * s, cy - 26 * s, cx + 4 * s, cy - 10 * s, p);
        }

        private void drawBirds(Canvas c) {
            if (!hasBirds[level]) return;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setShader(null);
            p.setColor(0xaa2a3a4a);
            for (int i = 0; i < 3; i++) {
                float bx = 440 - ((animation * 9f + i * 170f) % 520f);
                float by = 90 + i * 34 + (float) Math.sin(animation * 0.7f + i) * 6f;
                float fl = (float) Math.sin(animation * 4f + i) * 5f;
                tmp.reset();
                tmp.moveTo(bx - 9, by - fl);
                tmp.quadTo(bx - 4, by - 5, bx, by);
                tmp.quadTo(bx + 4, by - 5, bx + 9, by - fl);
                c.drawPath(tmp, p);
            }
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawRange(Canvas c, float[] pts, Path path, int col) {
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 320, 0, 650, col, mul(col, 0.7f), Shader.TileMode.CLAMP));
            c.drawPath(path, p);
            p.setShader(null);
            int n = pts.length / 2;
            for (int i = 1; i < n - 1; i++) {
                float px = pts[i * 2], py = pts[i * 2 + 1];
                float lx = pts[i * 2 - 2], ly = pts[i * 2 - 1];
                float rx = pts[i * 2 + 2], ry = pts[i * 2 + 3];
                if (py < ly && py < ry) {
                    setColor(0x2e000000);
                    tmp.reset();
                    tmp.moveTo(px, py);
                    tmp.lineTo(rx, ry);
                    tmp.lineTo(px, ry);
                    tmp.close();
                    c.drawPath(tmp, p);
                    setColor(0x16ffffff);
                    tmp.reset();
                    tmp.moveTo(px, py);
                    tmp.lineTo(lx, ly);
                    tmp.lineTo(px, ly);
                    tmp.close();
                    c.drawPath(tmp, p);
                    if (capCol[level] != 0) {
                        float cl = px + (lx - px) * 0.26f, cly = py + (ly - py) * 0.26f;
                        float cr = px + (rx - px) * 0.26f, cry = py + (ry - py) * 0.26f;
                        float w = cr - cl;
                        setColor(capCol[level]);
                        tmp.reset();
                        tmp.moveTo(px, py);
                        tmp.lineTo(cr, cry);
                        tmp.lineTo(cr - w * 0.25f, cry + 7);
                        tmp.lineTo(cr - w * 0.5f, cry - 2);
                        tmp.lineTo(cr - w * 0.75f, cry + 8);
                        tmp.lineTo(cl, cly);
                        tmp.close();
                        c.drawPath(tmp, p);
                    }
                }
            }
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

        private int treeType(float r) {
            switch (level) {
                case 0: return r < 0.55f ? 0 : (r < 0.8f ? 1 : 2);
                case 1: return r < 0.25f ? 0 : (r < 0.6f ? 1 : 2);
                case 2: return r < 0.7f ? 0 : 1;
                case 3: return 0;
                default: return r < 0.5f ? 0 : 1;
            }
        }

        private void drawTreeLayer(Canvas c, float factor, float spacing, float baseY,
                                   float hBase, float w, int color, int seed, boolean detail) {
            float shift = sceneryOffset * factor;
            int first = (int) Math.floor(shift / spacing);
            float frac = shift - first * spacing;
            int count = (int) (440 / spacing) + 3;
            for (int i = -1; i < count; i++) {
                int idx = first + i;
                float r = rnd(idx, seed);
                float x = i * spacing - frac + (rnd(idx, seed + 3) - 0.5f) * spacing * 0.6f;
                float h = hBase * (0.72f + 0.55f * rnd(idx, seed + 50));
                float ww = w * (0.85f + 0.3f * rnd(idx, seed + 90));
                float by = baseY + (rnd(idx, seed + 7) - 0.5f) * 6f;
                int col = mul(color, 0.9f + 0.2f * rnd(idx, seed + 11));
                int type = treeType(r);
                if (type == 0) drawConifer(c, x, by, h, ww, col, detail);
                else if (type == 1) drawOak(c, x, by, h, ww, col, detail);
                else drawBirch(c, x, by, h, ww, col, detail);
            }
                }
        
        private void drawConifer(Canvas c, float x, float base, float h, float w,
                                 int col, boolean detail) {
            float th = h * 0.16f;
            setColor(detail ? trunkC[level] : col);
            c.drawRect(x - w * 0.05f, base - th, x + w * 0.05f, base + 6, p);
            int tiers = detail ? 5 : 3;
            float foliage = h - th * 0.5f;
            float top = base - h;
            for (int k = 0; k < tiers; k++) {
                float apex = top + (k / (float) tiers) * foliage * 0.78f;
                float tierH = foliage * 0.42f;
                float hw = w * 0.5f * (0.38f + 0.62f * (k + 1) / tiers);
                float by = apex + tierH;
                setColor(col);
                tmp.reset();
                tmp.moveTo(x, apex);
                tmp.lineTo(x - hw, by);
                tmp.lineTo(x - hw * 0.5f, by - tierH * 0.13f);
                tmp.lineTo(x, by + tierH * 0.02f);
                tmp.lineTo(x + hw * 0.5f, by - tierH * 0.13f);
                tmp.lineTo(x + hw, by);
                tmp.close();
                c.drawPath(tmp, p);
                if (detail) {
                    setColor(0x2a000000);
                    tmp.reset();
                    tmp.moveTo(x, apex);
                    tmp.lineTo(x + hw, by);
                    tmp.lineTo(x + hw * 0.5f, by - tierH * 0.13f);
                    tmp.lineTo(x, by + tierH * 0.02f);
                    tmp.close();
                    c.drawPath(tmp, p);
                    setColor(al(lighten(col, 0.35f), 0x77));
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.5f);
                    c.drawLine(x, apex, x - hw, by, p);
                    p.setStyle(Paint.Style.FILL);
                    if (level == 3) {
                        setColor(0xf0ffffff);
                        tmp.reset();
                        tmp.moveTo(x, apex);
                        tmp.lineTo(x - hw * 0.5f, apex + tierH * 0.5f);
                        tmp.lineTo(x - hw * 0.2f, apex + tierH * 0.42f);
                        tmp.lineTo(x, apex + tierH * 0.55f);
                        tmp.lineTo(x + hw * 0.25f, apex + tierH * 0.44f);
                        tmp.lineTo(x + hw * 0.5f, apex + tierH * 0.5f);
                        tmp.close();
                        c.drawPath(tmp, p);
                    }
                }
            }
        }

        private void drawOak(Canvas c, float x, float base, float h, float w,
                             int col, boolean detail) {
            float th = h * 0.42f;
            setColor(detail ? trunkC[level] : col);
            tmp.reset();
            tmp.moveTo(x - w * 0.10f, base + 6);
            tmp.lineTo(x - w * 0.05f, base - th);
            tmp.lineTo(x + w * 0.05f, base - th);
            tmp.lineTo(x + w * 0.10f, base + 6);
            tmp.close();
            c.drawPath(tmp, p);
            if (detail) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(w * 0.04f);
                c.drawLine(x, base - th * 0.7f, x - w * 0.22f, base - th * 1.05f, p);
                c.drawLine(x, base - th * 0.8f, x + w * 0.2f, base - th * 1.1f, p);
                p.setStyle(Paint.Style.FILL);
            }
            float cy = base - th - (h - th) * 0.45f;
            float R = w * 0.5f;
            setColor(mul(col, 0.72f));
            c.drawCircle(x - R * 0.55f, cy + R * 0.25f, R * 0.62f, p);
            c.drawCircle(x + R * 0.55f, cy + R * 0.25f, R * 0.62f, p);
            c.drawCircle(x, cy + R * 0.35f, R * 0.7f, p);
            setColor(col);
            c.drawCircle(x, cy - R * 0.1f, R * 0.8f, p);
            c.drawCircle(x - R * 0.5f, cy - R * 0.05f, R * 0.55f, p);
            c.drawCircle(x + R * 0.5f, cy - R * 0.05f, R * 0.55f, p);
            if (detail) {
                setColor(al(lighten(col, 0.22f), 0xcc));
                c.drawCircle(x - R * 0.3f, cy - R * 0.4f, R * 0.5f, p);
                c.drawCircle(x + R * 0.1f, cy - R * 0.55f, R * 0.38f, p);
            }
        }

        private void drawBirch(Canvas c, float x, float base, float h, float w,
                               int col, boolean detail) {
            float th = h * 0.55f;
            setColor(detail ? 0xffe9e6dc : col);
            c.drawRect(x - w * 0.045f, base - th, x + w * 0.045f, base + 6, p);
            if (detail) {
                setColor(0xff3a352e);
                for (int k = 0; k < 5; k++) {
                    float yy = base - th * (0.2f + 0.16f * k);
                    c.drawRect(x - w * 0.045f, yy, x + w * 0.01f, yy + 2.5f, p);
                }
            }
            float cy = base - th - (h - th) * 0.35f;
            float R = w * 0.38f;
            setColor(mul(lighten(col, 0.1f), 0.8f));
            c.drawOval(x - R, cy - R * 1.5f, x + R, cy + R * 1.3f, p);
            setColor(lighten(col, 0.12f));
            c.drawOval(x - R * 0.85f, cy - R * 1.4f, x + R * 0.5f, cy + R * 0.6f, p);
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
                float h = maxH * (0.45f + 0.55f * rnd(idx, seed));
                float lean = (float) Math.sin(animation * 0.9f + idx * 0.7f) * 2.2f
                        + (rnd(idx, seed + 1) - 0.5f) * 5f;
                setColor((idx & 1) == 0 ? colA : colB);
                tmp.reset();
                tmp.moveTo(x - spacing * 0.28f, baseY);
                tmp.quadTo(x + lean * 0.4f, baseY - h * 0.6f, x + lean * 1.6f, baseY - h);
                tmp.quadTo(x + lean * 0.5f + spacing * 0.05f, baseY - h * 0.5f, x + spacing * 0.32f, baseY);
                tmp.close();
                c.drawPath(tmp, p);
            }
        }

        private void drawGround(Canvas c) {
            int L = level;
            p.setStyle(Paint.Style.FILL);
            p.setShader(soilS[L]);
            c.drawRect(-20, 668, 420, 820, p);
            p.setShader(null);

            for (int k = 0; k < 3; k++) {
                float y = 694 + k * 36;
                setColor(k % 2 == 0 ? 0x1e000000 : 0x14ffffff);
                tmp.reset();
                tmp.moveTo(-20, y);
                for (int x = 0; x <= 420; x += 40) {
                    tmp.lineTo(x, y + (float) Math.sin(x * 0.05f + k * 2f) * 4f);
                }
                tmp.lineTo(420, y + 9);
                tmp.lineTo(-20, y + 9);
                tmp.close();
                c.drawPath(tmp, p);
            }

            float sp = 64;
            int first = (int) Math.floor(sceneryOffset / sp);
            float frac = sceneryOffset - first * sp;
            for (int i = -1; i < 8; i++) {
                int idx = first + i;
                float sx = i * sp - frac + rnd(idx, 31) * 40;
                float sy = 684 + rnd(idx, 32) * 100;
                float sz = 3 + rnd(idx, 33) * 6;
                setColor(al(lighten(soilMid[L], 0.18f), 0xcc));
                c.drawOval(sx - sz, sy - sz * 0.65f, sx + sz, sy + sz * 0.65f, p);
                setColor(0x30ffffff);
                c.drawOval(sx - sz * 0.6f, sy - sz * 0.55f, sx + sz * 0.1f, sy - sz * 0.15f, p);
            }

            p.setShader(grassG[L]);
            c.drawRect(-20, 632, 420, 668, p);
            p.setShader(null);
            setColor(0x40000000);
            c.drawRect(-20, 668, 420, 672, p);

            drawBlades(c, 9, 646, 15, 0.9f, mul(grassB[L], 0.85f), grassB[L], 5);
            drawBlades(c, 11, 654, 15, 1.0f, grassA[L], lighten(grassA[L], 0.15f), 9);
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
                float y = 654 + rnd(idx, 9) * 12;
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
                        setColor(r > 0.7f ? 0xffe0702a : 0xffd9a030);
                        c.drawOval(x + 22, y - 3, x + 32, y + 2, p);
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
                        setColor(al(g, 0x33));
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
                setColor(al(base, (int) (60 * tw)));
                c.drawCircle(fx, fy, 9, p);
                setColor(al(base, (int) (120 + 130 * tw)));
                c.drawCircle(fx, fy, 2.4f, p);
            }
        }

        // ================= ПОГОДА =================
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
                // ================= СЛАЙМ =================
        private int skinColor(int i) {
            if (i == 9) {
                hsv[0] = (animation * 40f) % 360f;
                hsv[1] = 0.65f;
                hsv[2] = 1f;
                return Color.HSVToColor(hsv);
            }
            return skinCols[i];
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
        }

        private void drawSlimeCore(Canvas c, float x, float y, float bounce, float squash,
                                   int sk, int ac) {
            int col = skinColor(sk);
            int dark = darken(col);
            int darker = darken(dark);
            float t = y + bounce;

            p.setStyle(Paint.Style.FILL);
            c.save();
            c.translate(x + 34, y + 32 + bounce);
            c.scale(1f / squash, squash);
            c.translate(-x - 34, -y - 32 - bounce);

            p.setShader(new RadialGradient(x + 34, t + 64, 44,
                    new int[]{al(dark, 0x99), al(dark, 0)},
                    null, Shader.TileMode.CLAMP));
            c.drawOval(x - 10, t + 56, x + 78, t + 72, p);
            p.setShader(null);

            tmp.reset();
            tmp.moveTo(x + 34, t);
            tmp.cubicTo(x + 54, t, x + 68, t + 22, x + 68, t + 44);
            tmp.cubicTo(x + 68, t + 58, x + 60, t + 65, x + 48, t + 65);
            tmp.lineTo(x + 20, t + 65);
            tmp.cubicTo(x + 8, t + 65, x, t + 58, x, t + 44);
            tmp.cubicTo(x, t + 22, x + 14, t, x + 34, t);
            tmp.close();
            p.setShader(new RadialGradient(x + 22, t + 15, 80,
                    new int[]{0xffffffff, lighten(col, 0.7f), col, dark, darker},
                    new float[]{0f, 0.12f, 0.38f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            c.drawPath(tmp, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.2f);
            p.setColor(al(darker, 0x77));
            c.drawPath(tmp, p);
            p.setStyle(Paint.Style.FILL);

            setColor(0x4cffffff);
            c.drawCircle(x + 16, t + 50, 3.6f, p);
            c.drawCircle(x + 51, t + 52, 2.6f, p);
            c.drawCircle(x + 41, t + 58, 1.9f, p);
            c.drawCircle(x + 9, t + 38, 1.7f, p);

            c.save();
            c.rotate(-28, x + 20, t + 13);
            setColor(0xc8ffffff);
            c.drawOval(x + 7, t + 6.5f, x + 33, t + 19.5f, p);
            c.restore();
            setColor(0xa6ffffff);
            c.drawCircle(x + 47, t + 11, 2.6f, p);

            for (int i = 0; i < 2; i++) {
                float ex = x + 22 + i * 26;
                setColor(0x38000000);
                c.drawOval(ex - 9, t + 16.2f, ex + 9, t + 41.8f, p);
                setColor(Color.WHITE);
                c.drawOval(ex - 8.4f, t + 16f, ex + 8.4f, t + 40.4f, p);
                setColor(0xff172b32);
                c.drawCircle(ex + 2.2f, t + 30f, 6.3f, p);
                setColor(Color.WHITE);
                c.drawCircle(ex + 4.2f, t + 26.6f, 2.5f, p);
                setColor(0xd9ffffff);
                c.drawCircle(ex + 0.4f, t + 34f, 1.2f, p);
            }

            setColor(0xbfff8fb4);
            c.drawOval(x + 5.6f, t + 40.8f, x + 16.4f, t + 47.2f, p);
            c.drawOval(x + 51.6f, t + 40.8f, x + 62.4f, t + 47.2f, p);

            boolean inRun = state == PLAY || state == PAUSE || state == OVER;
            if (jumping && inRun) {
                tmp.reset();
                tmp.moveTo(x + 27, t + 43);
                tmp.quadTo(x + 34, t + 57, x + 41, t + 43);
                tmp.close();
                setColor(0xff16452b);
                c.drawPath(tmp, p);
                setColor(0xffff7d9c);
                c.drawOval(x + 30, t + 47, x + 38, t + 52.2f, p);
            } else {
                tmp.reset();
                tmp.moveTo(x + 28.5f, t + 44.5f);
                tmp.quadTo(x + 34, t + 51, x + 39.5f, t + 44.5f);
                setColor(0xff16452b);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2.6f);
                p.setStrokeCap(Paint.Cap.ROUND);
                c.drawPath(tmp, p);
                p.setStrokeCap(Paint.Cap.BUTT);
                p.setStyle(Paint.Style.FILL);
            }

            drawAccessory(c, x, t, ac, col);
            c.restore();
        }

        // x, top: левый верхний угол тела слайма (68x65)
        private void drawAccessory(Canvas c, float x, float top, int ac, int col) {
            float cx = x + 34;
            switch (ac) {
                case 1: {
                    setColor(0xee111820);
                    c.drawRoundRect(x + 7, top + 17, x + 33, top + 40, 8, 8, p);
                    c.drawRoundRect(x + 35, top + 17, x + 61, top + 40, 8, 8, p);
                    c.drawRect(x + 31, top + 22, x + 37, top + 26, p);
                    setColor(0x55ffffff);
                    c.drawRoundRect(x + 11, top + 20, x + 19, top + 25, 3, 3, p);
                    c.drawRoundRect(x + 39, top + 20, x + 47, top + 25, 3, 3, p);
                    break;
                }
                case 2: {
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
                case 3: {
                    setColor(0xffd8382f);
                    c.drawArc(cx - 29, top - 5, cx + 29, top + 35, 180, 180, true, p);
                    c.drawRoundRect(cx, top + 12, cx + 42, top + 19, 4, 4, p);
                    setColor(0xffffffff);
                    c.drawCircle(cx - 2, top + 3, 3, p);
                    break;
                }
                case 4: {
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
                case 5: {
                    for (int s = 0; s < 2; s++) {
                        float m = s == 0 ? 1 : -1;
                        setColor(darken(col));
                        tmp.reset();
                        tmp.moveTo(cx - m * 26, top + 14);
                        tmp.lineTo(cx - m * 21, top - 15);
                        tmp.lineTo(cx - m * 5, top + 4);
                        tmp.close();
                        c.drawPath(tmp, p);
                        setColor(0xffff9ec4);
                        tmp.reset();
                        tmp.moveTo(cx - m * 22, top + 6);
                        tmp.lineTo(cx - m * 20, top - 7);
                        tmp.lineTo(cx - m * 12, top + 2);
                        tmp.close();
                        c.drawPath(tmp, p);
                    }
                    break;
                }
                case 6: {
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
                case 7: {
                    setColor(0xff1b1b22);
                    c.drawRoundRect(cx - 19, top - 38, cx + 19, top + 2, 4, 4, p);
                    c.drawRoundRect(cx - 30, top - 3, cx + 30, top + 6, 4, 4, p);
                    setColor(0xffc0392b);
                    c.drawRect(cx - 19, top - 11, cx + 19, top - 3, p);
                    break;
                }
                case 8: {
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
                case 9: {
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
                case 10: {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(9);
                    p.setColor(0x55ffe98a);
                    c.drawOval(cx - 22, top - 26, cx + 22, top - 14, p);
                    p.setStrokeWidth(4);
                    p.setColor(0xffffe98a);
                    c.drawOval(cx - 22, top - 26, cx + 22, top - 14, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                }
                default:
                    break;
            }
                }
                // ================= ПРЕПЯТСТВИЯ И МОНЕТА =================
        private void pickObstacle() {
            int[] pool = obstaclePool[level];
            obstacleType = pool[random.nextInt(pool.length)];
            switch (obstacleType) {
                case 3: obstacleW = 84; break;
                case 4: obstacleW = 56; break;
                case 5: obstacleW = 72; break;
                case 6: obstacleW = 66; break;
                default: obstacleW = 58; break;
            }
        }

        private RectF obstacleRect() {
            switch (obstacleType) {
                case 0: return new RectF(obstacleX, ground - 65, obstacleX + obstacleW, ground);
                case 1: return new RectF(obstacleX, ground - 80, obstacleX + obstacleW, ground);
                case 3: return new RectF(obstacleX + 2, ground - 40, obstacleX + obstacleW - 2, ground);
                case 4: return new RectF(obstacleX + 6, ground - 170, obstacleX + obstacleW - 6, ground - 128);
                case 5: return new RectF(obstacleX + 6, ground - 52, obstacleX + obstacleW - 6, ground);
                case 6: return new RectF(obstacleX + 8, ground - 60, obstacleX + obstacleW - 8, ground);
                default: return new RectF(obstacleX, ground - 78, obstacleX + obstacleW, ground);
            }
        }

        private void drawObstacle(Canvas c) {
            float cx = obstacleX + obstacleW / 2f;
            if (obstacleType != 4) {
                p.setShader(new RadialGradient(cx, ground, obstacleW * 0.95f,
                        new int[]{0x77000000, 0x00000000}, null, Shader.TileMode.CLAMP));
                c.drawOval(cx - obstacleW * 0.8f, ground - 5, cx + obstacleW * 0.8f, ground + 13, p);
                p.setShader(null);
            }

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
                setColor(0xffb77b43);
                c.drawRoundRect(obstacleX - 7, ground - 65, obstacleX + obstacleW + 7, ground - 52, 6, 6, p);

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

            } else if (obstacleType == 2) {
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

            } else if (obstacleType == 3) {
                float l = obstacleX, top = ground - 40;
                p.setShader(new LinearGradient(0, top, 0, ground,
                        new int[]{0xff8a5a32, 0xff6a4222, 0xff4a2c16},
                        null, Shader.TileMode.CLAMP));
                c.drawRoundRect(l, top, l + obstacleW, ground, 14, 14, p);
                p.setShader(null);
                setColor(0x55301a0c);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                for (int i = 0; i < 3; i++) {
                    c.drawLine(l + 24 + i * 18, top + 8, l + 30 + i * 18, ground - 8, p);
                }
                setColor(0xff4a8a3a);
                c.drawRoundRect(l + 26, top - 3, l + obstacleW - 6, top + 5, 4, 4, p);
                setColor(0xffd9a86a);
                c.drawOval(l - 6, top + 2, l + 20, ground - 2, p);
                setColor(0xff9a6a3a);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);
                c.drawOval(l + 1, top + 10, l + 13, ground - 10, p);
                p.setStyle(Paint.Style.FILL);

            } else if (obstacleType == 4) {
                float bx = obstacleX + obstacleW / 2f, by = ground - 149;
                float flap = (float) Math.sin(animation * 3f) * 14f;
                setColor(0x33000000);
                c.drawOval(bx - 20, ground - 3, bx + 20, ground + 5, p);
                setColor(0xff3a5fbf);
                tmp.reset();
                tmp.moveTo(bx - 4, by - 2);
                tmp.quadTo(bx - 18, by - 22 - flap, bx - 30, by - 12 - flap);
                tmp.quadTo(bx - 16, by - 6, bx + 6, by + 2);
                tmp.close();
                c.drawPath(tmp, p);
                tmp.reset();
                tmp.moveTo(bx + 18, by - 4);
                tmp.lineTo(bx + 34, by - 12);
                tmp.lineTo(bx + 32, by + 4);
                tmp.close();
                c.drawPath(tmp, p);
                setColor(0xff4f7df0);
                c.drawOval(bx - 24, by - 14, bx + 22, by + 16, p);
                c.drawCircle(bx - 22, by - 6, 11, p);
                setColor(0xffdbe6ff);
                c.drawOval(bx - 14, by - 1, bx + 14, by + 14, p);
                setColor(0xffffb02e);
                tmp.reset();
                tmp.moveTo(bx - 31, by - 8);
                tmp.lineTo(bx - 42, by - 4);
                tmp.lineTo(bx - 31, by - 2);
                tmp.close();
                c.drawPath(tmp, p);
                setColor(Color.WHITE);
                c.drawCircle(bx - 24, by - 9, 3.5f, p);
                setColor(0xff172b32);
                c.drawCircle(bx - 25, by - 9, 1.8f, p);
                setColor(0xff6d96ff);
                tmp.reset();
                tmp.moveTo(bx - 2, by - 4);
                tmp.quadTo(bx - 12, by + 14 + flap, bx - 26, by + 10 + flap);
                tmp.quadTo(bx - 12, by + 2, bx + 6, by + 4);
                tmp.close();
                c.drawPath(tmp, p);

            } else if (obstacleType == 5) {
                setColor(0xff2f6a35);
                c.drawCircle(cx - 18, ground - 22, 22, p);
                c.drawCircle(cx + 18, ground - 22, 22, p);
                c.drawCircle(cx, ground - 30, 26, p);
                setColor(0xff3f8a45);
                c.drawCircle(cx - 6, ground - 36, 14, p);
                setColor(0xffe8e0c8);
                for (int k = 0; k < 5; k++) {
                    float tx = obstacleX + 12 + k * 12;
                    float ty = ground - 52 + (k % 2) * 6;
                    tmp.reset();
                    tmp.moveTo(tx - 4, ty + 8);
                    tmp.lineTo(tx, ty - 8);
                    tmp.lineTo(tx + 4, ty + 8);
                    tmp.close();
                    c.drawPath(tmp, p);
                }
                setColor(0xffd8382f);
                c.drawCircle(cx - 20, ground - 18, 3.5f, p);
                c.drawCircle(cx + 6, ground - 14, 3.5f, p);
                c.drawCircle(cx + 22, ground - 28, 3.5f, p);
                c.drawCircle(cx - 8, ground - 30, 3.5f, p);

            } else {
                float r = 33, by = ground - r;
                c.save();
                c.rotate(-obstacleX * 1.1f, cx, by);
                p.setShader(new RadialGradient(cx - 10, by - 12, r * 1.6f,
                        new int[]{0xffcfd3d0, 0xff8a908e, 0xff4a504e},
                        null, Shader.TileMode.CLAMP));
                c.drawCircle(cx, by, r, p);
                p.setShader(null);
                setColor(0x55303634);
                c.drawCircle(cx + 10, by + 6, 8, p);
                c.drawCircle(cx - 14, by + 10, 5, p);
                c.drawCircle(cx + 2, by - 14, 4.5f, p);
                setColor(0xff4a8a3a);
                c.drawOval(cx - 22, by - 8, cx - 8, by + 2, p);
                c.restore();
            }
        }

        private void drawCoin(Canvas c) {
            float sx = 0.35f + 0.65f * Math.abs((float) Math.cos(animation * 1.6f));
            p.setShader(new RadialGradient(coinX, coinY, 31,
                    new int[]{0x66fff3a0, 0x00fff3a0}, null, Shader.TileMode.CLAMP));
            c.drawCircle(coinX, coinY, 31, p);
            p.setShader(null);
            p.setShader(new RadialGradient(coinX - 5 * sx, coinY - 6, 24,
                    new int[]{0xfffffffd, 0xffffe34a, 0xffd58a00},
                    null, Shader.TileMode.CLAMP));
            c.drawOval(coinX - 16 * sx, coinY - 16, coinX + 16 * sx, coinY + 16, p);
            p.setShader(null);
            setColor(0xfffff4a0);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            c.drawOval(coinX - 11 * sx, coinY - 11, coinX + 11 * sx, coinY + 11, p);
            p.setStyle(Paint.Style.FILL);
        }

        // ================= ЧАСТИЦЫ =================
        private void spawnParticles(float x, float y, int count, int color) {
            if (!particlesOn) return;
            for (int n = 0; n < count; n++) {
                int i = random.nextInt(particleLife.length);
                particleX[i] = x;
                particleY[i] = y;
                particleVX[i] = (random.nextFloat() - 0.5f) * 7f;
                particleVY[i] = -random.nextFloat() * 6f - 1f;
                particleLife[i] = 20 + random.nextInt(18);
                particleCol[i] = color;
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
                    setColor(al(particleCol[i], Math.min(220, particleLife[i] * 8)));
                    c.drawCircle(particleX[i], particleY[i], 2.5f, p);
                }
            }
                           }
