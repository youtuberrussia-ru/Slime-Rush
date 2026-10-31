
package com.slimerush.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.content.SharedPreferences;

import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random();

        SharedPreferences prefs;
        ToneGenerator sound;

        final int MENU = 0;
        final int PLAYING = 1;
        final int SHOP = 2;
        final int GAME_OVER = 3;

        int state = MENU;

        int score = 0;
        int bestScore;
        int coins;
        int selectedSkin;

        boolean[] owned = {true, false, false, false};

        int[] skinColors = {
            Color.rgb(50, 240, 110),
            Color.rgb(60, 160, 255),
            Color.rgb(255, 100, 180),
            Color.rgb(255, 200, 40)
        };

        String[] skinNames = {
            "GREEN SLIME",
            "BLUE SLIME",
            "PINK SLIME",
            "GOLD SLIME"
        };

        int[] prices = {0, 50, 100, 200};

        float scale = 1;
        float offsetX = 0;
        float offsetY = 0;

        float slimeX = 75;
        float slimeY = 625;
        float velocity = 0;

        float ground = 690;

        float obstacleX = 450;
        float obstacleWidth = 48;
        float obstacleHeight = 65;
        float obstacleSpeed = 8;

        float coinX = 650;
        float coinY = 575;

        boolean jumping = false;

        long lastTime = 0;

        GameView() {
            super(MainActivity.this);

            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            prefs = getSharedPreferences(
                "slime_rush_save",
                MODE_PRIVATE
            );

            bestScore = prefs.getInt("best", 0);
            coins = prefs.getInt("coins", 50);
            selectedSkin = prefs.getInt("skin", 0);

            for (int i = 0; i < 4; i++) {
                owned[i] = prefs.getBoolean("owned" + i, i == 0);
            }

            sound = new ToneGenerator(
                AudioManager.STREAM_MUSIC, 75
            );
        }

        void save() {
            SharedPreferences.Editor e = prefs.edit();

            e.putInt("best", bestScore);
            e.putInt("coins", coins);
            e.putInt("skin", selectedSkin);

            for (int i = 0; i < 4; i++) {
                e.putBoolean("owned" + i, owned[i]);
            }

            e.apply();
        }

        void playSound(int type) {
            try {
                sound.startTone(type, 130);
            } catch (Exception ignored) {}
        }

        void text(
            Canvas c, String value,
            float x, float y, float size, int color
        ) {
            p.setColor(color);
            p.setTextSize(size);
            p.setTypeface(Typeface.create(
                "sans-serif-rounded",
                Typeface.BOLD
            ));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(value, x, y, p);
        }

        void button(
            Canvas c, String label,
            float x, float y, float w, float h,
            int color
        ) {
            p.setColor(color);
            c.drawRoundRect(
                new RectF(x, y, x + w, y + h),
                20, 20, p
            );

            text(
                c, label,
                x + w / 2,
                y + h / 2 + 10,
                25, Color.WHITE
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            scale = Math.min(
                getWidth() / 400f,
                getHeight() / 800f
            );

            offsetX = (getWidth() - 400 * scale) / 2;
            offsetY = (getHeight() - 800 * scale) / 2;

            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);

            if (state == MENU) {
                drawMenu(canvas);
            } else if (state == SHOP) {
                drawShop(canvas);
            } else if (state == PLAYING) {
                updateGame();
                drawGame(canvas);
                postInvalidateDelayed(16);
            } else if (state == GAME_OVER) {
                drawGame(canvas);
                drawGameOver(canvas);
            }

            canvas.restore();
        }

        void drawBackground(Canvas c, int theme) {

            int[] skies = {
                Color.rgb(25, 40, 85),
                Color.rgb(55, 35, 100),
                Color.rgb(20, 80, 100)
            };

            int[] grounds = {
                Color.rgb(55, 160, 85),
                Color.rgb(130, 90, 170),
                Color.rgb(50, 145, 150)
            };

            c.drawColor(skies[theme]);

            p.setColor(Color.argb(170, 255, 255, 255));

            for (int i = 0; i < 15; i++) {
                float x = (i * 83) % 400;
                float y = (i * 47) % 360;
                c.drawCircle(x, y, 2, p);
            }

            p.setColor(Color.rgb(255, 220, 100));
            c.drawCircle(330, 120, 35, p);

            p.setColor(grounds[theme]);
            c.drawRect(0, ground, 400, 800, p);

            p.setColor(Color.rgb(35, 115, 65));
            c.drawRect(0, ground, 400, ground + 10, p);
        }

        void drawSlime(Canvas c) {

            p.setColor(skinColors[selectedSkin]);

            c.drawRoundRect(
                new RectF(
                    slimeX, slimeY,
                    slimeX + 62, slimeY + 58
                ),
                20, 20, p
            );

            p.setColor(Color.WHITE);

            c.drawCircle(slimeX + 19, slimeY + 20, 8, p);
            c.drawCircle(slimeX + 43, slimeY + 20, 8, p);

            p.setColor(Color.BLACK);

            c.drawCircle(slimeX + 21, slimeY + 20, 3, p);
            c.drawCircle(slimeX + 45, slimeY + 20, 3, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            c.drawArc(
                slimeX + 19, slimeY + 25,
                slimeX + 44, slimeY + 45,
                15, 150, false, p
            );
            p.setStyle(Paint.Style.FILL);
        }

        void drawObstacle(Canvas c) {

            p.setColor(Color.rgb(230, 75, 65));

            c.drawRoundRect(
                new RectF(
                    obstacleX,
                    ground - obstacleHeight,
                    obstacleX + obstacleWidth,
                    ground
                ),
                8, 8, p
            );

            p.setColor(Color.rgb(255, 145, 100));

            c.drawRect(
                obstacleX + 8,
                ground - obstacleHeight + 10,
                obstacleX + 15,
                ground - 15,
                p
            );
        }

        void drawCoin(Canvas c) {

            p.setColor(Color.rgb(255, 190, 20));
            c.drawCircle(coinX, coinY, 17, p);

            p.setColor(Color.rgb(255, 240, 100));
            c.drawCircle(coinX, coinY, 11, p);

            text(c, "$", coinX, coinY + 7, 19,
                Color.rgb(170, 105, 0));
        }

        void startGame() {

            score = 0;
            slimeY = ground - 58;
            velocity = 0;

            obstacleX = 450;
            obstacleSpeed = 8;

            coinX = 650;
            coinY = ground - 115;

            jumping = false;
            state = PLAYING;

            lastTime = System.currentTimeMillis();

            invalidate();
        }

        void updateGame() {

            long now = System.currentTimeMillis();

            float dt = Math.min(
                (now - lastTime) / 16.67f, 2f
            );

            lastTime = now;

            velocity += 0.8f * dt;
            slimeY += velocity * dt;

            if (slimeY >= ground - 58) {
                slimeY = ground - 58;
                velocity = 0;
                jumping = false;
            }

            obstacleSpeed = Math.min(
                8 + score / 500f,
                17
            );

            obstacleX -= obstacleSpeed * dt;
            coinX -= obstacleSpeed * dt;

            if (obstacleX < -obstacleWidth) {
                obstacleX = 430 + random.nextInt(150);
                obstacleHeight = 55 + random.nextInt(35);
                score += 10;
            }

            if (coinX < -30) {
                coinX = 450 + random.nextInt(250);
                coinY = ground - 100 - random.nextInt(60);
            }

            RectF slime = new RectF(
                slimeX + 5, slimeY + 5,
                slimeX + 57, slimeY + 55
            );

            RectF obstacle = new RectF(
                obstacleX, ground - obstacleHeight,
                obstacleX + obstacleWidth, ground
            );

            if (RectF.intersects(slime, obstacle)) {

                state = GAME_OVER;

                if (score > bestScore) {
                    bestScore = score;
                }

                save();
                playSound(ToneGenerator.TONE_PROP_NACK);

                return;
            }

            RectF coin = new RectF(
                coinX - 16, coinY - 16,
                coinX + 16, coinY + 16
            );

            if (RectF.intersects(slime, coin)) {

                coins += 10;
                score += 25;

                coinX = 450 + random.nextInt(250);
                coinY = ground - 100 - random.nextInt(60);

                save();

                playSound(ToneGenerator.TONE_PROP_BEEP);
            }

            score++;
        }

        void drawGame(Canvas c) {

            int theme = (score / 1500) % 3;

            drawBackground(c, theme);
            drawObstacle(c);
            drawCoin(c);
            drawSlime(c);

            text(c, "SCORE: " + score / 10,
                90, 55, 23, Color.WHITE);

            text(c, "BEST: " + bestScore / 10,
                290, 55, 20, Color.WHITE);

            p.setColor(Color.rgb(255, 205, 35));
            c.drawCircle(155, 90, 12, p);

            text(c, "" + coins,
                195, 98, 22, Color.WHITE);

            text(c, "TAP TO JUMP",
                200, 760, 19, Color.WHITE);
        }

        void drawMenu(Canvas c) {

            ground = 690;

            drawBackground(c, 0);

            text(c, "SLIME RUSH",
                200, 220, 48, Color.WHITE);

            text(c, "ENDLESS ADVENTURE",
                200, 265, 19, Color.rgb(170, 220, 255));

            slimeX = 169;
            slimeY = 380;

            drawSlime(c);

            button(c, "PLAY",
                75, 470, 250, 75,
                Color.rgb(45, 180, 85));

            button(c, "SHOP",
                75, 565, 250, 70,
                Color.rgb(70, 115, 220));

            text(c, "BEST: " + bestScore / 10,
                200, 680, 23, Color.WHITE);

            text(c, "COINS: " + coins,
                200, 720, 23, Color.rgb(255, 220, 60));
        }

        void drawShop(Canvas c) {

            drawBackground(c, 1);

            text(c, "SLIME SHOP",
                200, 100, 40, Color.WHITE);

            text(c, "YOUR COINS: " + coins,
                200, 150, 23, Color.rgb(255, 225, 70));

            for (int i = 0; i < 4; i++) {

                float y = 205 + i * 115;

                p.setColor(Color.rgb(35, 50, 90));

                c.drawRoundRect(
                    new RectF(25, y, 375, y + 95),
                    15, 15, p
                );

                p.setColor(skinColors[i]);

                c.drawRoundRect(
                    new RectF(45, y + 20, 100, y + 75),
                    15, 15, p
                );

                text(c, skinNames[i],
                    205, y + 35, 19, Color.WHITE);

                String action;

                if (selectedSkin == i) {
                    action = "EQUIPPED";
                } else if (owned[i]) {
                    action = "SELECT";
                } else {
                    action = prices[i] + " COINS";
                }

                text(c, action,
                    205, y + 70, 17,
                    Color.rgb(255, 220, 80));
            }

            button(c, "BACK",
                120, 680, 160, 65,
                Color.rgb(80, 95, 130));
        }

        void drawGameOver(Canvas c) {

            p.setColor(Color.argb(205, 0, 0, 0));
            c.drawRect(0, 0, 400, 800, p);

            text(c, "GAME OVER",
                200, 300, 43, Color.WHITE);

            text(c, "SCORE: " + score / 10,
                200, 365, 28, Color.WHITE);

            text(c, "BEST: " + bestScore / 10,
                200, 410, 25, Color.rgb(255, 220, 70));

            button(c, "RESTART",
                75, 480, 250, 70,
                Color.rgb(45, 180, 85));

            button(c, "MAIN MENU",
                75, 575, 250, 70,
                Color.rgb(70, 115, 220));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return true;
            }

            float x = (event.getX() - offsetX) / scale;
            float y = (event.getY() - offsetY) / scale;

            if (state == MENU) {

                if (y >= 470 && y <= 545) {
                    startGame();
                } else if (y >= 565 && y <= 635) {
                    state = SHOP;
                    invalidate();
                }

            } else if (state == SHOP) {

                if (y >= 680) {
                    state = MENU;
                } else {

                    int index = (int)((y - 205) / 115);

                    if (index >= 0 && index < 4) {

                        if (owned[index]) {

                            selectedSkin = index;

                        } else if (coins >= prices[index]) {

                            coins -= prices[index];
                            owned[index] = true;
                            selectedSkin = index;

                            playSound(
                                ToneGenerator.TONE_PROP_ACK
                            );
                        }

                        save();
                    }
                }

                invalidate();

            } else if (state == PLAYING) {

                if (!jumping) {
                    velocity = -17;
                    jumping = true;

                    playSound(
                        ToneGenerator.TONE_PROP_BEEP
                    );
                }

            } else if (state == GAME_OVER) {

                if (y >= 480 && y <= 550) {
                    startGame();
                } else if (y >= 575 && y <= 645) {
                    state = MENU;
                    invalidate();
                }
            }

            return true;
        }
    }
        }
                
