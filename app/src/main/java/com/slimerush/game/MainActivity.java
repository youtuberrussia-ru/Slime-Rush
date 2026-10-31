
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

    private BachMusic music;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            5894 | 1024 | 512
        );

        music = new BachMusic();
        music.start();

        setContentView(new GameView());
    }

    @Override
    protected void onPause() {
        if (music != null) music.stop();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (music != null) music.start();
    }

    @Override
    protected void onDestroy() {
        if (music != null) music.stop();
        super.onDestroy();
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
        float obstacleW=65;
        float coinX=750, coinY=540;

        float speed=7;
        float animation=0;
        float sceneryOffset=0;

        int obstacleType=0;
        boolean jumping=false;
        boolean soundOn=true;

        long last=0;

        GameView() {
            super(MainActivity.this);

            save=getSharedPreferences("slime_save",0);
            coins=save.getInt("coins",50);
            best=save.getInt("best",0);
            skin=save.getInt("skin",0);

            for(int i=0;i<5;i++)
                owned[i]=save.getBoolean("owned"+i,i==0);

            sound=new ToneGenerator(
                AudioManager.STREAM_MUSIC,65
            );

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
                "sans-serif-rounded",Typeface.BOLD
            ));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(s,x,y,p);
        }

        void round(Canvas c,float l,float t,float r,float b,
                   float radius,int col) {

            color(col);
            c.drawRoundRect(l,t,r,b,radius,radius,p);
        }

        void button(Canvas c,String s,float y,int col) {
            round(c,65,y,335,y+65,22,0x66000000);
            round(c,65,y-4,335,y+59,22,col);
            text(c,s,200,y+37,25,Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas real) {
            super.onDraw(real);

            float scale=Math.min(
                getWidth()/400f,getHeight()/800f
            );

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

        // 2.5D ENVIRONMENT

        void background(Canvas c) {

            p.setShader(new LinearGradient(
                0,0,0,800,
                new int[]{
                    0xff152f4b,
                    0xff4e8194,
                    0xffb5c8b4,
                    0xffe6d8b3
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawRect(0,0,400,800,p);
            p.setShader(null);

            // Sun glow
            p.setShader(new RadialGradient(
                315,120,110,
                new int[]{
                    0x99fff3c2,
                    0x33fff3c2,
                    0x00fff3c2
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawCircle(315,120,110,p);
            p.setShader(null);

            color(0xffffedbd);
            c.drawCircle(315,120,27,p);

            // Distant mountains
            Path mountain=new Path();

            mountain.moveTo(0,430);
            mountain.lineTo(60,300);
            mountain.lineTo(115,385);
            mountain.lineTo(195,245);
            mountain.lineTo(285,390);
            mountain.lineTo(355,275);
            mountain.lineTo(400,365);
            mountain.lineTo(400,650);
            mountain.lineTo(0,650);
            mountain.close();

            p.setShader(new LinearGradient(
                0,245,0,650,
                0xff657f83,
                0xff314f4d,
                Shader.TileMode.CLAMP
            ));

            c.drawPath(mountain,p);
            p.setShader(null);

            // Mountain highlights
            Path light=new Path();

            light.moveTo(195,245);
            light.lineTo(230,320);
            light.lineTo(200,300);
            light.lineTo(180,325);
            light.close();

            color(0xffdce4dc);
            c.drawPath(light,p);

            // Far forest
            for(int i=0;i<16;i++) {
                float x=i*30-sceneryOffset*0.12f%30;

                color(0xff345e50);

                Path tree=new Path();
                tree.moveTo(x,510);
                tree.lineTo(x+18,440);
                tree.lineTo(x+36,510);
                tree.close();

                c.drawPath(tree,p);
                c.drawRect(x+15,500,x+21,650,p);
            }

            // Near forest
            for(int i=0;i<11;i++) {
                float x=i*48-sceneryOffset*0.3f%48;

                color(0xff254d3b);

                Path tree=new Path();
                tree.moveTo(x,570);
                tree.lineTo(x+24,480);
                tree.lineTo(x+48,570);
                tree.close();

                c.drawPath(tree,p);
                c.drawRect(x+20,560,x+28,650,p);
            }

            // Ground shadow
            p.setShader(new LinearGradient(
                0,615,0,800,
                new int[]{
                    0xff506e43,
                    0xff334c32,
                    0xff182b24
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawRect(0,615,400,800,p);
            p.setShader(null);

            // Ground surface
            p.setShader(new LinearGradient(
                0,620,0,800,
                0xff9a9160,
                0xff514a32,
                Shader.TileMode.CLAMP
            ));

            c.drawRect(0,630,400,800,p);
            p.setShader(null);

            // Ground edge
            color(0xff647d43);
            c.drawRect(0,620,400,635,p);

            // Ground texture
            for(int i=0;i<70;i++) {
                float x=(i*71-sceneryOffset)%430;
                if(x<0) x+=430;

                float y=642+(i*29)%145;

                color(i%2==0?
                    0x557d6b43:0x667b8651);

                c.drawCircle(x,y,1+(i%3),p);
            }

            // Grass details
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);

            for(int i=0;i<45;i++) {
                float x=(i*43-sceneryOffset)%420;
                if(x<0) x+=420;

                float y=632+(i*13)%23;

                color(i%2==0?
                    0xffb5bd77:0xff496c38);

                c.drawLine(x,y,x-4,y-9,p);
                c.drawLine(x,y,x+5,y-8,p);
            }

            p.setStyle(Paint.Style.FILL);
        }

        // VOLUMETRIC SLIME

        void drawSlime(Canvas c,float x,float y) {

            float bounce=(float)Math.sin(animation*2)*2;

            float squash=1f;

            if(jumping) {
                squash=0.91f;
            } else {
                squash=1f+
                    (float)Math.sin(animation*2)*0.025f;
            }

            // Ground shadow
            p.setShader(new RadialGradient(
                x+34,ground+4,48,
                new int[]{
                    0x88000000,
                    0x44000000,
                    0x00000000
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawOval(
                x-12,ground-8,
                x+82,ground+15,p
            );

            p.setShader(null);

            c.save();

            c.translate(x+34,y+32+bounce);
            c.scale(1f/squash,squash);
            c.translate(-x-34,-y-32-bounce);

            // Outer dark edge
            p.setShader(new RadialGradient(
                x+22,y+15,75,
                new int[]{
                    0xffffffff,
                    skins[skin],
                    darken(skins[skin]),
                    darken(darken(skins[skin]))
                },
                new float[]{0f,0.35f,0.78f,1f},
                Shader.TileMode.CLAMP
            ));

            RectF body=new RectF(
                x,y+bounce,
                x+68,y+65+bounce
            );

            c.drawOval(body,p);
            p.setShader(null);

            // Inner light
            p.setShader(new RadialGradient(
                x+23,y+14,39,
                new int[]{
                    0x99ffffff,
                    0x22ffffff,
                    0x00ffffff
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawOval(
                x+7,y+4+bounce,
                x+59,y+54+bounce,p
            );

            p.setShader(null);

            // Gloss reflection
            p.setShader(new LinearGradient(
                x+10,y+3,
                x+45,y+37,
                new int[]{
                    0xddffffff,
                    0x55ffffff,
                    0x00ffffff
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawOval(
                x+10,y+5+bounce,
                x+49,y+28+bounce,p
            );

            p.setShader(null);

            // Small reflections
            color(0x99ffffff);
            c.drawOval(
                x+13,y+13+bounce,
                x+22,y+17+bounce,p
            );

            color(0x55ffffff);
            c.drawCircle(
                x+54,y+42+bounce,4,p
            );

            // Eyes
            for(int i=0;i<2;i++) {

                float ex=x+22+i*26;
                float ey=y+29+bounce;

                color(0x55000000);
                c.drawOval(
                    ex-9,ey-12,
                    ex+10,ey+14,p
                );

                color(Color.WHITE);
                c.drawOval(
                    ex-8,ey-13,
                    ex+9,ey+12,p
                );

                color(0xff172b32);
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

            c.restore();
        }

        int darken(int c) {
            return Color.rgb(
                (int)(Color.red(c)*0.55f),
                (int)(Color.green(c)*0.55f),
                (int)(Color.blue(c)*0.55f)
            );
        }

        // DETAILED OBSTACLES

        void drawObstacle(Canvas c) {

            // Shadow
            p.setShader(new RadialGradient(
                obstacleX+32,ground,55,
                new int[]{
                    0x77000000,
                    0x00000000
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawOval(
                obstacleX-15,ground-5,
                obstacleX+85,ground+13,p
            );

            p.setShader(null);

            if(obstacleType==0) {

                // Wood body
                p.setShader(new LinearGradient(
                    obstacleX,0,
                    obstacleX+obstacleW,0,
                    new int[]{
                        0xff56321d,
                        0xffc18a4e,
                        0xff744322
                    },
                    null,Shader.TileMode.CLAMP
                ));

                c.drawRoundRect(
                    obstacleX,ground-65,
                    obstacleX+obstacleW,ground,
                    5,5,p
                );

                p.setShader(null);

                // Wood boards
                for(int i=0;i<4;i++) {
                    float x=obstacleX+8+i*15;

                    color(0x55804422);
                    c.drawLine(
                        x,ground-58,
                        x,ground-5,p
                    );
                }

                color(0xffe3b67b);
                c.drawLine(
                    obstacleX+8,ground-59,
                    obstacleX+obstacleW-8,
                    ground-59,p
                );

                round(
                    c,obstacleX-7,ground-65,
                    obstacleX+obstacleW+7,
                    ground-52,4,0xffb77b43
                );

                round(
                    c,obstacleX-7,ground-29,
                    obstacleX+obstacleW+7,
                    ground-16,4,0xff9c6334
                );

            } else if(obstacleType==1) {

                // Stone wall
                p.setShader(new LinearGradient(
                    obstacleX,ground-80,
                    obstacleX+obstacleW,ground,
                    new int[]{
                        0xffc0c4bf,
                        0xff777d79,
                        0xff414a49
                    },
                    null,Shader.TileMode.CLAMP
                ));

                c.drawRoundRect(
                    obstacleX,ground-80,
                    obstacleX+obstacleW,ground,
                    6,6,p
                );

                p.setShader(null);

                // Stone lines
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2);

                color(0xff454d4a);

                c.drawLine(
                    obstacleX,ground-53,
                    obstacleX+obstacleW,ground-53,p
                );

                c.drawLine(
                    obstacleX+25,ground-80,
                    obstacleX+25,ground-53,p
                );

                c.drawLine(
                    obstacleX+40,ground-53,
                    obstacleX+40,ground,p
                );

                p.setStyle(Paint.Style.FILL);

                color(0x55ffffff);
                c.drawLine(
                    obstacleX+7,ground-74,
                    obstacleX+18,ground-74,p
                );

            } else {

                // Metal base
                p.setShader(new LinearGradient(
                    obstacleX,ground-25,
                    obstacleX,ground,
                    0xff7c8589,
                    0xff242d32,
                    Shader.TileMode.CLAMP
                ));

                c.drawRoundRect(
                    obstacleX,ground-25,
                    obstacleX+obstacleW,ground,
                    5,5,p
                );

                p.setShader(null);

                // Metal spikes
                for(int i=0;i<3;i++) {

                    float x=obstacleX+8+i*23;

                    Path spike=new Path();

                    spike.moveTo(x,ground-23);
                    spike.lineTo(x+10,ground-78);
                    spike.lineTo(x+20,ground-23);
                    spike.close();

                    p.setShader(new LinearGradient(
                        x,ground-78,
                        x+20,ground,
                        0xfff0f3f1,
                        0xff59666b,
                        Shader.TileMode.CLAMP
                    ));

                    c.drawPath(spike,p);
                    p.setShader(null);
                }
            }
        }

        void drawCoin(Canvas c) {

            // Coin glow
            p.setShader(new RadialGradient(
                coinX,coinY,30,
                new int[]{
                    0x55fff3a0,
                    0x00fff3a0
                },
                null,Shader.TileMode.CLAMP
            ));

            c.drawCircle(coinX,coinY,30,p);
            p.setShader(null);

            // Gold body
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

            float dt=Math.min(
                (now-last)/16.67f,2
            );

            last=now;

            animation+=0.12f*dt;
            sceneryOffset+=speed*0.35f*dt;

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
                obstacleX,ground-
                obstacleX+65,ground
            );

            if(RectF.intersects(player,obstacle)) {

                state=OVER;
                best=Math.max(best,score);

                saveData();

                if(soundOn) sound.startTone(
                    ToneGenerator.TONE_PROP_NACK,180
                );

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
                    ToneGenerator.TONE_PROP_BEEP,100
                );
            }

            score++;
        }

        void drawWorld(Canvas c) {

            background(c);

            drawObstacle(c);
            drawCoin(c);
            drawSlime(c,slimeX,slimeY);

            p.setShader(new LinearGradient(
                0,12,0,75,
                0xdd132c3c,
                0x99203940,
                Shader.TileMode.CLAMP
            ));

            c.drawRoundRect(
                12,12,388,75,18,18,p
            );

            p.setShader(null);

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

                round(
                    c,25,y,375,y+85,
                    15,0xdd183e60
                );

                p.setShader(new RadialGradient(
                    58,y+35,35,
                    new int[]{
                        Color.WHITE,
                        skins[i],
                        darken(skins[i])
                    },
                    null,Shader.TileMode.CLAMP
                ));

                c.drawOval(
                    42,y+12,102,y+72,p
                );

                p.setShader(null);

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
                getWidth()/400f,getHeight()/800f
            );

            float dx=(getWidth()-400*scale)/2;
            float dy=(getHeight()-800*scale)/2;

            float x=(e.getX()-dx)/scale;
            float y=(e.getY()-dy)/scale;

            if(state==MENU) {

                if(y>=450 && y<=525) {
                    reset();
                }
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
                        }
                        else if(coins>=prices[i]) {
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
                        ToneGenerator.TONE_PROP_BEEP,100
                    );
                }

            } else if(state==OVER) {

                if(y>=470 && y<=550) {
                    reset();
                }
                else if(y>=555 && y<=640) {
                    state=MENU;
                    invalidate();
                }
            }

            return true;
        }
    }
                 }
