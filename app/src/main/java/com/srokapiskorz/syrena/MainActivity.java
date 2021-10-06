package com.srokapiskorz.syrena;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private AudioTrack mAudioTrack;
    private Thread mMusicThread;
    private volatile Boolean mRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int mBufferSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSize, AudioTrack.MODE_STREAM);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAudioTrack.play();

        mMusicThread = new Thread(mMusicRunnable);
        mMusicThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRunning = false;
        try {
            mMusicThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mAudioTrack.stop();
        mAudioTrack.release();
    }

    Runnable mMusicRunnable = new Runnable() {
        @Override
        public void run(){
            while (mRunning) {
                // Sine wave
                double[] mSound = new double[44100];
                short[] mBuffer = new short[44100];
                for (int i = 0; i < mSound.length; i++) {
                    mSound[i] = Math.sin((2.0*Math.PI * 440.0/44100.0*(double)i));
                    mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE);
                }

                mAudioTrack.write(mBuffer, 0, mSound.length);
            }
        }
    };
}