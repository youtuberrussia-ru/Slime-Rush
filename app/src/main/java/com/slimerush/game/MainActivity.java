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
        
