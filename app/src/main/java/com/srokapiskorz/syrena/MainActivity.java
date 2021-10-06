package com.srokapiskorz.syrena;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;

import java.util.Locale;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private AudioTrack mAudioTrack;
    private Thread mMusicThread;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private volatile Boolean mRunning = true;
    private volatile Double mLastAcceleratorAmplitude = null;
    private volatile Double mLastAcceleratorAmplitudeDifference = 0.0;
    private volatile Double mFlywheel = 0.0;
    private final Double mFlywheelResistance = 0.995;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int bufferSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                double amplitude = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
                if(mLastAcceleratorAmplitude == null) {
                    mLastAcceleratorAmplitude = amplitude;
                    return;
                }
                double amplitudeDifference = amplitude - mLastAcceleratorAmplitude;
                double differenceFiltered = mLastAcceleratorAmplitudeDifference * 0.9 + amplitudeDifference*0.1;
                mLastAcceleratorAmplitude = amplitudeDifference;
                mLastAcceleratorAmplitude = amplitude;
                double newFlywheelValue = mFlywheel*mFlywheelResistance;
                if (differenceFiltered > 0) {
                    newFlywheelValue += Math.min(differenceFiltered, 10);
                }
                mFlywheel = newFlywheelValue;
                System.out.println(";" + String.format(Locale.GERMAN, "%.4f", differenceFiltered) + ";" + String.format(Locale.GERMAN, "%.4f", mFlywheel));

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Ignore
            }
        }, mSensor, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMusicThread = new Thread(mMusicRunnable);
        mMusicThread.start();

        mAudioTrack.play();
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
                if( mFlywheel < 2 ) {
                    continue;
                }
                double[] mSound = new double[441];
                short[] mBuffer = new short[441];
                double frequency = 20 * mFlywheel + 400;
                for (int i = 0; i < mSound.length; i++) {
                    mSound[i] = 8*Math.sin((2.0*Math.PI * frequency/44100.0*(double)i)) + 2*Math.sin((4.0*Math.PI * frequency/44100.0*(double)i)) + 1*Math.sin((8.0*Math.PI * frequency/44100.0*(double)i))
                                + 0.5*Math.sin((16.0*Math.PI * frequency/44100.0*(double)i)) + 0.5*Math.sin((32.0*Math.PI * frequency/44100.0*(double)i));
                    mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE/12);
                }
                mAudioTrack.write(mBuffer, 0, mSound.length);
            }
        }
    };
}