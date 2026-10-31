package com.slimerush.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

public class BachMusic {

    private AudioTrack track;
    private Thread musicThread;
    private volatile boolean playing = false;
    private volatile float volume = 0.22f;

    private static final int RATE = 22050;

    // Ноты прелюдии до мажор:
    // синтезированная версия для фоновой музыки игры
    private final int[][] notes = {
        {60,64,67,72},
        {60,64,67,72},
        {62,65,69,74},
        {62,65,69,74},
        {59,62,67,71},
        {59,62,67,71},
        {60,64,67,72},
        {60,64,67,72},
        {55,59,62,67},
        {55,59,62,67},
        {57,60,64,69},
        {57,60,64,69},
        {53,57,60,65},
        {53,57,60,65},
        {55,59,62,67},
        {55,59,62,67}
    };

    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
    }

    public void start() {
        if (playing) return;

        playing = true;

        musicThread = new Thread(() -> {
            int min = AudioTrack.getMinBufferSize(
                RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            );

            track = new AudioTrack.Builder()
                .setAudioAttributes(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(Math.max(min, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

            short[] buffer = new short[2048];
            track.play();

            int index = 0;

            while (playing) {
                int[] chord = notes[index % notes.length];

                for (int i = 0; i < buffer.length; i++) {
                    double t = i / (double) RATE;
                    double sample = 0;

                    for (int n = 0; n < 4; n++) {
                        double frequency =
                            440.0 * Math.pow(
                                2.0, (chord[n] - 69) / 12.0
                            );

                        double phase =
                            2.0 * Math.PI * frequency *
                            (t + index * 0.092);

                        sample += Math.sin(phase) * 0.25;
                    }

                    double fade = Math.min(
                        1.0,
                        Math.min(i / 250.0,
                        (buffer.length - i) / 250.0)
                    );

                    sample *= volume * fade;
                    buffer[i] = (short) (
                        Math.max(-1, Math.min(1, sample))
                        * 32767
                    );
                }

                if (!playing) break;

                track.write(
                    buffer, 0, buffer.length
                );

                index++;
            }

            if (track != null) {
                track.stop();
                track.release();
                track = null;
            }
        });

        musicThread.start();
    }

    public void stop() {
        playing = false;

        if (track != null) {
            track.pause();
            track.flush();
        }
    }
}
