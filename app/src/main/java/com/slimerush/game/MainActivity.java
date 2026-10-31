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

    class GameView extends SurfaceView implements SurfaceHolder.Callback {

        Paint p = new Paint();
        float slimeX = 160;
        float slimeY = 500;
        float velocity = 0;
        float ground;
        int score = 0;
        boolean gameOver = false;
        boolean jumping = false;

        public GameView() {
            super(MainActivity.this);
            getHolder().addCallback(this);
            setFocusable(true);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            ground = getHeight() - 150;
            new Thread(() -> {
                while (true) {
                    if (getHolder().getSurface().isValid()) {
                        Canvas c = getHolder().lockCanvas();
                        if (c != null) {
                            update();
                            drawGame(c);
                            getHolder().unlockCanvasAndPost(c);
                        }
                    }
                    try {
                        Thread.sleep(16);
                    } catch (Exception e) {}
                }
            }).start();
        }

        void update() {
            if (!gameOver) {
                velocity += 0.8f;
                slimeY += velocity;

                if (slimeY >= ground - 60) {
                    slimeY = ground - 60;
                    velocity = 0;
                    jumping = false;
                }
                score++;
            }
        }

        void drawGame(Canvas c) {
            c.drawColor(Color.rgb(25, 35, 65));

            p.setColor(Color.rgb(80, 190, 100));
            c.drawRect(0, ground, getWidth(), getHeight(), p);

            p.setColor(Color.rgb(50, 230, 100));
            c.drawRoundRect(
                new RectF(slimeX, slimeY, slimeX + 65, slimeY + 60),
                20, 20, p
            );

            p.setColor(Color.WHITE);
            p.setTextSize(50);
            c.drawText("SLIME RUSH", 40, 80, p);
            c.drawText("SCORE: " + score / 10, 40, 145, p);

            if (gameOver) {
                p.setTextSize(55);
                c.drawText("GAME OVER", 40, getHeight() / 2f, p);
                c.drawText("TAP TO RESTART", 40, getHeight() / 2f + 70, p);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (gameOver) {
                    score = 0;
                    gameOver = false;
                    slimeY = ground - 60;
                    velocity = 0;
                } else if (!jumping) {
                    velocity = -17;
                    jumping = true;
                }
                return true;
            }
            return true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder h, int f, int w, int he) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder h) {}
    }
}
