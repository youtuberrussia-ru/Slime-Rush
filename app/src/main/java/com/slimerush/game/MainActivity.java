
package com.slimerush.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.*;
import android.content.SharedPreferences;
import android.media.ToneGenerator;
import android.media.AudioManager;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            5894 | 1024 | 512
        );
        setContentView(new GameView());
    }

    class GameView extends View {

        Paint p = new Paint(3);
        Random random = new Random();
        SharedPreferences save;
        ToneGenerator sound;

        final int MENU=0, PLAY=1, SHOP=2, OVER=3;
        int state=MENU;

        int coins, best, score, skin=0;
        boolean[] owned={true,false,false,false,false};

        int[] skins={
            Color.rgb(55,255,100),
            Color.rgb(40,170,255),
            Color.rgb(255,90,180),
            Color.rgb(255,205,35),
            Color.rgb(170,90,255)
        };

        String[] skinNames={
            "GREEN SLIME","BLUE SLIME",
            "PINK SLIME","GOLD SLIME","PURPLE SLIME"
        };

        int[] prices={0,50,100,200,350};

        float slimeX=95, slimeY=580, velocity=0;
        float ground=650;
        float obstacleX=500;
        float obstacleW=65, obstacleH=80;
        float coinX=750, coinY=540;
        float speed=7;
        float animation=0;

        int obstacleType=0;
        boolean jumping=false;
        boolean soundOn=true;

        long last=0;
        float sx, ox, oy;

        GameView() {
            super(MainActivity.this);

            save=getSharedPreferences("slime_save",0);
            coins=save.getInt("coins",50);
            best=save.getInt("best",0);
            skin=save.getInt("skin",0);

            for(int i=0;i<5;i++)
                owned[i]=save.getBoolean("owned"+i,i==0);

            sound=new ToneGenerator(AudioManager.STREAM_MUSIC,65);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        }

        void saveData() {
            SharedPreferences.Editor e=save.edit();
            e.putInt("coins",coins);
            e.putInt("best",best);
            e.putInt("skin",skin);

            for(int i=0;i<5;i++)
                e.putBoolean("owned"+i,owned[i]);

            e.apply();
        }

        void color(int c) {
            p.setColor(c);
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
        }

        void text(Canvas c,String s,float x,float y,
                  int size,int col) {
            color(col);
            p.setTextSize(size);
            p.setTypeface(Typeface.create(
                "sans-serif-rounded",Typeface.BOLD));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s,x,y,p);
        }

        void round(Canvas c,float l,float t,float r,float b,
                   float radius,int col) {
            color(col);
            c.drawRoundRect(l,t,r,b,radius,radius,p);
        }

        void button(Canvas c,String s,float y,int col) {
            round(c,65,y,335,y+65,22,0x55000000);
            round(c,65,y-4,335,y+59,22,col);
            text(c,s,200,y+37,25,Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas real) {
            super.onDraw(real);

            float scale=Math.min(
                getWidth()/400f,getHeight()/800f);

            float dx=(getWidth()-400*scale)/2;
            float dy=(getHeight()-800*scale)/2;

            real.save();
            real.translate(dx,dy);
            real.scale(scale,scale);

            if(state==MENU) drawMenu(real);
            if(state==SHOP) drawShop(real);

            if(state==PLAY) {
                update();
                drawWorld(real);
                postInvalidateDelayed(16);
            }

            if(state==OVER) {
                drawWorld(real);
                drawOver(real);
            }

            real.restore();
        }

        // REALISTIC BACKGROUND

        void background(Canvas c) {

            p.setShader(new LinearGradient(
                0,0,0,800,
                new int[]{
                    0xff42b8ed,
                    0xffa8e3ff,
                    0xffe0f5ff
                },
                null,Shader.TileMode.CLAMP
            ));
            c.drawRect(0,0,400,800,p);
            p.setShader(null);

            // Sun
            p.setShader(new RadialGradient(
                325,115,65,
                new int[]{0xfffff8b0,0x00fff8b0},
                null,Shader.TileMode.CLAMP
            ));
            c.drawCircle(325,115,65,p);
            p.setShader(null);

            color(Color.WHITE);
            c.drawCircle(325,115,25,p);

            // Clouds
            for(int i=0;i<4;i++) {
                float x=(i*125+30)%440;
                float y=115+(i%2)*65;

                color(0x99ffffff);
                c.drawOval(x,y,x+75,y+25,p);
                c.drawCircle(x+20,y,18,p);
                c.drawCircle(x+42,y-7,22,p);
                c.drawCircle(x+60,y+2,16,p);
            }

            // Mountains
            Path mountain=new Path();
            mountain.moveTo(0,430);
            mountain.lineTo(75,230);
            mountain.lineTo(145,410);
            mountain.lineTo(230,255);
            mountain.lineTo(335,420);
            mountain.lineTo(400,280);
            mountain.lineTo(400,650);
            mountain.lineTo(0,650);
            mountain.close();

            color(0xff6d9eae);
            c.drawPath(mountain,p);

            // Mountain snow
            Path snow=new Path();
            snow.moveTo(75,230);
            snow.lineTo(105,315);
            snow.lineTo(83,300);
            snow.lineTo(68,320);
            snow.close();

            color(Color.WHITE);
            c.drawPath(snow,p);

            // Distant forest
            for(int i=0;i<12;i++) {
                float x=i*40;
                color(0xff397b55);
                c.drawCircle(x,510,35,p);
                c.drawRect(x-15,510,x+15,660,p);
            }

            // Ground
            p.setShader(new LinearGradient(
                0,620,0,800,
                0xff55c95b,0xff24763b,
                Shader.TileMode.CLAMP
            ));
            c.drawRect(0,620,400,800,p);
            p.setShader(null);

            color(0xffa1e879);
            c.drawRect(0,620,400,630,p);

            // Small grass
            color(0xff2b9b43);
            for(int i=0;i<35;i++) {
                float x=(i*47)%400;
                float y=640+(i*17)%150;
                c.drawLine(x,y,x-4,y-9,p);
                c.drawLine(x,y,x+5,y-7,p);
            }
        }

        // GLOSSY SLIME

        void drawSlime(Canvas c,float x,float y) {

            float bounce=(float)Math.sin(animation)*2;

            // Shadow
            p.setShader(new RadialGradient(
                x+32,ground+3,43,
                new int[]{0x66000000,0x00000000},
                null,Shader.TileMode.CLAMP
            ));
            c.drawOval(x-8,ground-5,x+78,ground+13,p);
            p.setShader(null);

            // Body gradient
            p.setShader(new RadialGradient(
                x+22,y+12,75,
                new int[]{
                    0xffffffff,
                    skins[skin],
                    darken(skins[skin]),
                    darken(darken(skins[skin]))
                },
                new float[]{0f,0.3f,0.75f,1f},
                Shader.TileMode.CLAMP
            ));

            RectF body=new RectF(
                x,y+bounce,x+68,y+65+bounce
            );

            c.drawOval(body,p);
            p.setShader(null);

            // Glossy highlight
            p.setShader(new LinearGradient(
                x,y,x+40,y+35,
                0x99ffffff,0x00ffffff,
                Shader.TileMode.CLAMP
            ));
            c.drawOval(
                x+10,y+5+bounce,
                x+48,y+26+bounce,p
            );
            p.setShader(null);

            color(0x55ffffff);
            c.drawCircle(x+12,y+38+bounce,5,p);
            c.drawCircle(x+56,y+43+bounce,4,p);

            // Eyes
            for(int i=0;i<2;i++) {
                float ex=x+22+i*26;
                float ey=y+28+bounce;

                color(Color.WHITE);
                c.drawOval(
                    ex-9,ey-13,ex+9,ey+12,p
                );

                color(0xff14243a);
                c.drawCircle(ex+2,ey,5,p);

                color(Color.WHITE);
                c.drawCircle(ex+3,ey-3,2,p);
            }

            // Smile
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f);
            p.setStrokeCap(Paint.Cap.ROUND);
            color(0xff16452b);
            c.drawArc(
                x+23,y+34+bounce,
                x+46,y+52+bounce,
                15,150,false,p
            );
            p.setStyle(Paint.Style.FILL);
        }

        int darken(int c) {
            return Color.rgb(
                (int)(Color.red(c)*0.55f),
                (int)(Color.green(c)*0.55f),
                (int)(Color.blue(c)*0.55f)
            );
        }

        // THREE OBSTACLE TYPES

        void drawObstacle(Canvas c) {

            if(obstacleType==0) {

                // Wooden fence
                round(c,obstacleX,ground-65,
                    obstacleX+obstacleW,ground,
                    5,0xff8b4d26);

                round(c,obstacleX-8,ground-65,
                    obstacleX+obstacleW+8,ground-52,
                    4,0xffbd793c);

                round(c,obstacleX-8,ground-29,
                    obstacleX+obstacleW+8,ground-16,
                    4,0xffbd793c);

                color(0xffe6b078);
                c.drawLine(obstacleX+12,ground-60,
                    obstacleX+12,ground-5,p);
            }
            else if(obstacleType==1) {

                // Stone wall
                round(c,obstacleX,ground-80,
                    obstacleX+obstacleW,ground,
                    7,0xff777d83);

                color(0xffaeb7bd);
                c.drawLine(obstacleX,ground-55,
                    obstacleX+obstacleW,ground-55,p);

                c.drawLine(obstacleX+25,ground-80,
                    obstacleX+25,ground-55,p);

                c.drawLine(obstacleX+38,ground-55,
                    obstacleX+38,ground,p);
            }
            else {

                // Spiked barrier
                round(c,obstacleX,ground-25,
                    obstacleX+obstacleW,ground,
                    5,0xff343b48);

                for(int i=0;i<3;i++) {
                    float x=obstacleX+10+i*23;

                    Path spike=new Path();
                    spike.moveTo(x,ground-23);
                    spike.lineTo(x+10,ground-78);
                    spike.lineTo(x+20,ground-23);
                    spike.close();

                    color(0xffc4d0da);
                    c.drawPath(spike,p);
                }
            }
        }

        void drawCoin(Canvas c) {
            p.setShader(new RadialGradient(
                coinX-5,coinY-6,24,
                new int[]{
                    0xfffffffd,
                    0xffffe34a,
                    0xffd58a00
                },
                null,Shader.TileMode.CLAMP
            ));
            c.drawCircle(coinX,coinY,16,p);
            p.setShader(null);

            color(0xfffff4a0);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            c.drawCircle(coinX,coinY,11,p);
            p.setStyle(Paint.Style.FILL);
        }

        void reset() {
            score=0;
            slimeY=ground-65;
            velocity=0;
            obstacleX=500;
            coinX=750;
            speed=7;
            obstacleType=random.nextInt(3);
            jumping=false;
            state=PLAY;
            last=System.currentTimeMillis();
            invalidate();
        }

        void update() {

            long now=System.currentTimeMillis();
            float dt=Math.min((now-last)/16.67f,2);
            last=now;

            animation+=0.12f*dt;

            velocity+=0.8f*dt;
            slimeY+=velocity*dt;

            if(slimeY>=ground-65) {
                slimeY=ground-65;
                velocity=0;
                jumping=false;
            }

            obstacleX-=speed*dt;
            coinX-=speed*dt;

            if(obstacleX < -100) {
                obstacleX=460+random.nextInt(160);
                obstacleType=random.nextInt(3);
                score+=100;
                speed=Math.min(speed+0.2f,14);
            }

            if(coinX < -30) {
                coinX=480+random.nextInt(180);
                coinY=ground-90-random.nextInt(100);
            }

            RectF player=new RectF(
                slimeX+7,slimeY+8,
                slimeX+61,slimeY+60
            );

            float oh=obstacleType==0?65:
                     obstacleType==1?80:78;

            RectF obstacle=new RectF(
                obstacleX,ground-oh,
                obstacleX+obstacleW,ground
            );

            if(RectF.intersects(player,obstacle)) {
                state=OVER;
                best=Math.max(best,score);
                saveData();
                if(soundOn) sound.startTone(
                    ToneGenerator.TONE_PROP_NACK,180);
                return;
            }

            RectF coin=new RectF(
                coinX-16,coinY-16,
                coinX+16,coinY+16
            );

            if(RectF.intersects(player,coin)) {
                coins+=10;
                score+=25;
                coinX=480+random.nextInt(200);
                coinY=ground-90-random.nextInt(100);
                saveData();

                if(soundOn) sound.startTone(
                    ToneGenerator.TONE_PROP_BEEP,100);
            }

            score++;
        }

        void drawWorld(Canvas c) {
            background(c);
            drawObstacle(c);
            drawCoin(c);
            drawSlime(c,slimeX,slimeY);

            round(c,12,12,388,75,18,0xaa173c65);

            text(c,"SCORE: "+score/10,
                95,49,21,Color.WHITE);

            text(c,"BEST: "+best/10,
                210,49,20,Color.WHITE);

            text(c,"● "+coins,
                325,49,20,0xffffdf59);

            text(c,"TAP TO JUMP",
                200,765,19,Color.WHITE);
        }

        void drawMenu(Canvas c) {
            background(c);

            text(c,"SLIME RUSH",
                200,190,45,Color.WHITE);

            text(c,"THE GELLY ADVENTURE",
                200,230,17,0xffeaffff);

            drawSlime(c,166,320);

            button(c,"PLAY",460,0xff22ae65);
            button(c,"SKINS",545,0xff367fd1);

            text(c,"BEST: "+best/10,
                200,680,23,Color.WHITE);

            text(c,"COINS: "+coins,
                200,720,23,0xffffdf59);
        }

        void drawShop(Canvas c) {
            background(c);

            text(c,"SLIME SKINS",
                200,90,35,Color.WHITE);

            text(c,"COINS: "+coins,
                200,135,23,0xffffdf59);

            for(int i=0;i<5;i++) {
                float y=175+i*105;

                round(c,25,y,375,y+85,
                    15,0xdd183e60);

                color(skins[i]);
                c.drawOval(42,y+12,102,y+72,p);

                color(Color.WHITE);
                c.drawCircle(61,y+35,5,p);
                c.drawCircle(83,y+35,5,p);

                text(c,skinNames[i],
                    235,y+35,17,Color.WHITE);

                String action;

                if(skin==i) action="EQUIPPED";
                else if(owned[i]) action="SELECT";
                else action="BUY "+prices[i];

                text(c,action,
                    235,y+65,17,0xffffdf59);
            }

            button(c,"BACK",720,0xff367fd1);
        }

        void drawOver(Canvas c) {
            color(0xbb10243c);
            c.drawRect(0,0,400,800,p);

            text(c,"GAME OVER",
                200,300,42,Color.WHITE);

            text(c,"SCORE: "+score/10,
                200,365,27,Color.WHITE);

            text(c,"BEST: "+best/10,
                200,410,24,0xffffdf59);

            button(c,"RESTART",485,0xff22ae65);
            button(c,"MAIN MENU",570,0xff367fd1);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {

            if(e.getAction()!=MotionEvent.ACTION_DOWN)
                return true;

            float scale=Math.min(
                getWidth()/400f,getHeight()/800f);

            float dx=(getWidth()-400*scale)/2;
            float dy=(getHeight()-800*scale)/2;

            float x=(e.getX()-dx)/scale;
            float y=(e.getY()-dy)/scale;

            if(state==MENU) {

                if(y>=450 && y<=525) reset();
                else if(y>=535 && y<=610) {
                    state=SHOP;
                    invalidate();
                }

            } else if(state==SHOP) {

                if(y>=700) {
                    state=MENU;
                } else {
                    int i=(int)((y-175)/105);

                    if(i>=0 && i<5) {
                        if(owned[i]) {
                            skin=i;
                        } else if(coins>=prices[i]) {
                            coins-=prices[i];
                            owned[i]=true;
                            skin=i;
                        }
                        saveData();
                    }
                }

                invalidate();

            } else if(state==PLAY) {

                if(!jumping) {
                    velocity=-18;
                    jumping=true;

                    if(soundOn) sound.startTone(
                        ToneGenerator.TONE_PROP_BEEP,100);
                }

            } else if(state==OVER) {

                if(y>=470 && y<=550) reset();
                else if(y>=555 && y<=640) {
                    state=MENU;
                    invalidate();
                }
            }

            return true;
        }
    }
                             }
                
