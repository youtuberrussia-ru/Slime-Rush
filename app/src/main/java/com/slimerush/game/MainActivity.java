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
            best = prefs.getInt("            button(c, "BACK", 650, 0xff596b7b);
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
