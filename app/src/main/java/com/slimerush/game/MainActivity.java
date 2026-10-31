
package com.slimerush.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        gameView = new GameView();
        setContentView(gameView);
    }

    class GameView extends SurfaceView implements SurfaceHolder.Callback {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float slimeX = 120;
        float slimeY;
        float velocity = 0;

        float obstacleX;
        float obstacleWidth = 55;
        float obstacleHeight = 70;
        float obstacleSpeed = 10;

        float ground;

        int score = 0;
        int bestScore = 0;

        boolean gameOver = false;
        boolean jumping = false;

        Thread gameThread;
        boolean running = false;

        public GameView() {
            super(MainActivity.this);
            getHolder().addCallback(this);
            setFocusable(true);
        }

        void resetGame() {
            slimeY = ground - 65;
            velocity = 0;
            obstacleX = getWidth() + 250;
            obstacleSpeed = 10;
            score = 0;
            gameOver = false;
            jumping = false;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            ground = getHeight() - 130;
            resetGame();
            running = true;

            gameThread = new Thread(() -> {
                while (running) {
                    Canvas canvas = null;

                    try {
                        canvas = getHolder().lockCanvas();

                        if (canvas != null) {
                            updateGame();
                            drawGame(canvas);
                        }
                    } finally {
                        if (canvas != null) {
                            getHolder().unlockCanvasAndPost(canvas);
                        }
                    }

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });

            gameThread.start();
        }

        void updateGame() {
            if (gameOver) return;

            velocity += 0.8f;
            slimeY += velocity;

            if (slimeY >= ground - 65) {
                slimeY = ground - 65;
                velocity = 0;
                jumping = false;
            }

            obstacleX -= obstacleSpeed;

            if (obstacleX < -obstacleWidth) {
                obstacleX = getWidth() + 150;
                score += 100;
                obstacleSpeed += 0.3f;
            }

            RectF slime = new RectF(
                slimeX, slimeY,
                slimeX + 65, slimeY + 60
            );

            RectF obstacle = new RectF(
                obstacleX,
                ground - obstacleHeight,
                obstacleX + obstacleWidth,
                ground
            );

            if (RectF.intersects(slime, obstacle)) {
                gameOver = true;
                if (score > bestScore) {
                    bestScore = score;
                }
            }

            score++;
        }

        void drawGame(Canvas c) {

            c.drawColor(Color.rgb(25, 35, 65));

            // Background
            p.setColor(Color.rgb(45, 65, 105));
            c.drawCircle(getWidth() - 80, 130, 55, p);

            // Ground
            p.setColor(Color.rgb(65, 170, 85));
            c.drawRect(0, ground, getWidth(), getHeight(), p);

            // Obstacle
            p.setColor(Color.rgb(240, 65, 65));
            c.drawRoundRect(
                new RectF(
                    obstacleX,
                    ground - obstacleHeight,
                    obstacleX + obstacleWidth,
                    ground
                ),
                10, 10, p
            );

            // Slime
            p.setColor(Color.rgb(50, 240, 110));
            c.drawRoundRect(
                new RectF(
                    slimeX, slimeY,
                    slimeX + 65, slimeY + 60
                ),
                20, 20, p
            );

            // Eyes
            p.setColor(Color.WHITE);
            c.drawCircle(slimeX + 20, slimeY + 22, 7, p);
            c.drawCircle(slimeX + 45, slimeY + 22, 7, p);

            p.setColor(Color.BLACK);
            c.drawCircle(slimeX + 22, slimeY + 22, 3, p);
            c.drawCircle(slimeX + 47, slimeY + 22, 3, p);

            // Title and score
            p.setColor(Color.WHITE);
            p.setTextSize(42);
            p.setFakeBoldText(true);
            c.drawText("SLIME RUSH", 30, 60, p);

            p.setTextSize(28);
            c.drawText("SCORE: " + score / 10, 30, 110, p);
            c.drawText("BEST: " + bestScore / 10, 30, 150, p);

            p.setFakeBoldText(false);

            if (gameOver) {
                p.setColor(Color.argb(190, 0, 0, 0));
                c.drawRect(
                    0, 0, getWidth(), getHeight(), p
                );

                p.setColor(Color.WHITE);
                p.setTextSize(48);
                c.drawText(
                    "GAME OVER",
                    getWidth() / 2f - 145,
                    getHeight() / 2f,
                    p
                );

                p.setTextSize(25);
                c.drawText(
                    "TAP TO RESTART",
                    getWidth() / 2f - 110,
                    getHeight() / 2f + 60,
                    p
                );
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                if (gameOver) {
                    resetGame();
                } else if (!jumping) {
                    velocity = -18;
                    jumping = true;
                }

                return true;
            }

            return true;
        }

        @Override
        public void surfaceChanged(
            SurfaceHolder holder, int format, int width, int height
        ) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            running = false;

            if (gameThread != null) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
        }
                
