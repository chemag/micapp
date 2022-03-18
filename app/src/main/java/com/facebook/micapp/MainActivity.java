package com.facebook.micapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.media.*;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public final static String TAG = "mic";
    PendingIntent mPermissionIntent;
    boolean mRun = true;
    EditText mPeak;
    EditText mRms;

    EditText mMaxPeak;
    EditText mMaxRms;

    EditText mMinPeak;
    EditText mMinRms;

    TextView mInfo;
    CheckBox mRecordCheck;

    double mMaxPeakVal = -100;
    double mMaxRMSVal = -100;
    double mMinPeakVal = 0;
    double mMinRMSVal = 0;

    AudioTrack mPlayer = null;
    byte[] mSound = null;

    boolean mRecord = true;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    //UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);


                }
            }
        }
    };

    public static String[] retrieveNotGrantedPermissions(Context context) {
        ArrayList<String> nonGrantedPerms = new ArrayList<>();
        try {
            String[] manifestPerms =
                    context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                            .requestedPermissions;
            if (manifestPerms == null || manifestPerms.length == 0) {
                return null;
            }

            for (String permName : manifestPerms) {
                int permission = ActivityCompat.checkSelfPermission(context, permName);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    nonGrantedPerms.add(permName);
                    Log.d(TAG, "Failed to get permission for:" + permName);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }

        return nonGrantedPerms.toArray(new String[nonGrantedPerms.size()]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPeak = findViewById(R.id.peakVal);
        mRms = findViewById(R.id.rmsVal);
        mMaxPeak = findViewById(R.id.peakMaxVal);
        mMaxRms = findViewById(R.id.rmsMaxVal);
        mMinPeak = findViewById(R.id.peakMinVal);
        mMinRms = findViewById(R.id.rmsMinVal);
        mInfo = (TextView) findViewById(R.id.editMic);
        ((Button) findViewById(R.id.activeButton)).setOnClickListener(this::onClick);
        // make sure the right permissions are set
        String[] permissions = retrieveNotGrantedPermissions(this);

        if (permissions != null && permissions.length > 0) {
            int REQUEST_ALL_PERMISSIONS = 0x4562;
            Log.d(TAG, "Request permissions: " + permissions);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);
        }
        Button play = (Button) findViewById(R.id.playSoundButton);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playSound();
            }
        });
        mRecordCheck = (CheckBox) findViewById(R.id.recordCheck);
        Runnable checkTask = () -> {
            check();
        };

        // start the thread
        Log.d(TAG, "Start check!");
        new Thread(checkTask).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void playSound_static() {
        int usage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
        int type = AudioAttributes.CONTENT_TYPE_SPEECH;
        int samplerate = 48000;
        int buffersize = AudioTrack.getMinBufferSize(
                samplerate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (mPlayer == null) {
            InputStream is = this.getResources().openRawResource(R.raw.voices_48khz_s16pcm);
            int size;
            byte[] sound = null;
            int read = 0;
            try {
                size = is.available();
                sound = new byte[size];
                read = is.read(sound);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mPlayer =
                    new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(usage)
                                    .setContentType(type)
                                    .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(samplerate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build())
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .setBufferSizeInBytes(read)
                            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                            .build();
            mPlayer.write(sound, 0, read);

        }
        mPlayer.stop();
        mPlayer.setPlaybackHeadPosition(0);
        mPlayer.play();
    }

    void playSound() {
        int usage = AudioAttributes.USAGE_VOICE_COMMUNICATION;
        int type = AudioAttributes.CONTENT_TYPE_SPEECH;
        int samplerate = 48000;
        int buffersize = AudioTrack.getMinBufferSize(
                samplerate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (mSound == null) {
            InputStream is = this.getResources().openRawResource(R.raw.voices_48khz_s16pcm);
            int size;
            int read = 0;
            try {
                size = is.available();
                mSound = new byte[size];
                read = is.read(mSound);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                final AudioTrack player =
                        new AudioTrack.Builder()
                                .setAudioAttributes(new AudioAttributes.Builder()
                                        .setUsage(usage)
                                        .setContentType(type)
                                        .build())
                                .setAudioFormat(new AudioFormat.Builder()
                                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                        .setSampleRate(samplerate)
                                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                        .build())
                                .setTransferMode(AudioTrack.MODE_STREAM)
                                .setBufferSizeInBytes(buffersize)
                                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                                .build();
                int played = 0;
                player.play();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int contentType = -1;
                        AudioAttributes ats = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            ats = player.getAudioAttributes();

                            contentType = ats.getContentType();

                            int usage = ats.getUsage();
                            int flags = ats.getFlags();
                            mInfo.append("Player:\nusage: " + usage + "\nContent type = " + contentType + "\nflags: " + flags);
                        }
                    }
                });

                while (played < mSound.length) {
                    int written = player.write(mSound, played, buffersize);
                    if (written > 0) {
                        played += written;
                    } else {
                        Log.d(TAG, "Failed to write to player: " + written);
                        break;
                    }
                }


                player.release();
            }
        });
        t.start();


    }

    void check() {

        Log.d(TAG, "\n\n--------------- Check Audio ---------------------");
        final AudioManager aman = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adevs = aman.getDevices(AudioManager.GET_DEVICES_INPUTS);

        for (AudioDeviceInfo info : adevs) {
            Log.d(TAG, "product_name: " + info.getProductName());
            int[] channels = info.getChannelCounts();

            Log.d(TAG, "type: " + info.getType());
            Log.d(TAG, "id: " + info.getId());
            for (int channel : channels) {
                Log.d(TAG, "-- ch.count: " + channel);
            }
            int[] rates = info.getSampleRates();
            for (int rate : rates) {
                Log.d(TAG, "-- ch.rate: " + rate);
            }

        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.d(TAG, "Open audio using VOICE_COMMUNICATION source");
        AudioRecord recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(48000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(2 * AudioRecord.getMinBufferSize(480000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
                .build();

        float sampleRate = 48000;
        final byte audioData[] = new byte[(int) (sampleRate * 2)];
        short[] shorts = new short[(int) sampleRate];

        int previousVolume = aman.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        aman.setStreamVolume(AudioManager.STREAM_VOICE_CALL, aman.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
        int now = aman.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        Log.d(TAG, "PREV: " + previousVolume + ", NOW: " + now + ", mx = " + aman.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
        Log.d(TAG, "Fixed vol =  " + aman.isVolumeFixed());

        BufferedOutputStream os = null;
        if (mRecord) {
            // open the record file path
            File[] externalStorageVolumes =
                    ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
            File primaryExternalStorage = externalStorageVolumes[0];
            String filename = primaryExternalStorage + "/capture_48kHz.raw";
            Log.d(TAG, "Record to \"" + filename + "\"");
            try {
                os = new BufferedOutputStream(new FileOutputStream(filename));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found for recording ", e);
                return;
            }
            final BufferedOutputStream fos = os;
        }
        final BufferedOutputStream fos = os;
        Thread rec = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start Recording");
                recorder.startRecording();

                List<MicrophoneInfo> minfo = null;
                StringBuilder strBuilder = new StringBuilder();
                try {
                    minfo = recorder.getActiveMicrophones();

                    for (MicrophoneInfo mic : minfo) {
                        Log.d(TAG, "Address: " + mic.getAddress());
                        Log.d(TAG, "Desc: " + mic.getDescription());
                        Log.d(TAG, "Direc: " + mic.getDirectionality());
                        Log.d(TAG, "Sens: " + mic.getSensitivity());
                        strBuilder.append("Path:" + mic.getAddress());
                        strBuilder.append("\nDesc:" + mic.getDescription());
                        strBuilder.append("\nDirectiviy:" + directionalityToText(mic.getDirectionality()));
                        strBuilder.append("\nSensitivity:" + mic.getSensitivity());
                        strBuilder.append("\nMax spl:" + mic.getMaxSpl());
                        strBuilder.append("\nMin spl:" + mic.getMaxSpl());
                        strBuilder.append("\nFreq response:" + freqResponse(mic.getFrequencyResponse()));
                        strBuilder.append("\n------------\n\n");

                        Log.d(TAG, "Loop:" + strBuilder.toString());
                    }
                    final String text = strBuilder.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Set tring:  " + text);
                            mInfo.setText(text);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (mRun) {
                    int read_bytes = recorder.read(audioData, 0, audioData.length);
                    //Log.d(TAG, "Read: "+read_bytes);
                    ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    double sum = 0;
                    double max = 0;
                    double min = 65000;
                    for (int i = 0; i < shorts.length; i++) {
                        double norm = shorts[i] / Math.pow(2, 15);
                        sum += norm * norm;
                        if (norm > max) {
                            max = norm;
                        }
                        if (norm < min) {
                            min = norm;
                        }
                    }
                    double val = sum / (double) shorts.length;
                    double nval = val;
                    final double dB = Math.round(floatToDB(Math.sqrt(nval)));
                    final double peak_dB = Math.round(floatToDB(max));

                    if (dB < mMinRMSVal) {
                        mMinRMSVal = dB;
                    }
                    if (dB > mMaxRMSVal) {
                        mMaxRMSVal = dB;
                    }

                    if (peak_dB < mMinPeakVal) {
                        mMinPeakVal = peak_dB;
                    }
                    if (peak_dB > mMaxPeakVal) {
                        mMaxPeakVal = peak_dB;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPeak.setText(String.valueOf(peak_dB));
                            mRms.setText(String.valueOf(dB));
                            mMaxPeak.setText(String.valueOf(mMaxPeakVal));
                            mMinPeak.setText(String.valueOf(mMinPeakVal));
                            mMaxRms.setText(String.valueOf(mMaxRMSVal));
                            mMinRms.setText(String.valueOf(mMinRMSVal));
                        }
                    });
                    if (fos != null) {
                        try {
                            fos.write(audioData, 0, read_bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        Log.d(TAG, "Start rec thread");
        rec.start();

        try {
            rec.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        recorder.stop();
        recorder.release();
        if (fos != null) {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public double dBToFloat(double val) {
        return Math.pow(val / 20.0, 10);
    }


    public double floatToDB(double val) {
        if (val <= 0)
            return -100;
        return 20 * Math.log10(val);
    }

    public void onClick(View v) {
        Log.d(TAG, "click, run = " + mRun);
        if (!mRun) {
            mRun = true;
            mMaxPeakVal = -100;
            mMinPeakVal = 0;
            mMaxRMSVal = -100;
            mMinRMSVal = 0;

            Runnable checkTask = () -> {
                check();
            };

            // start the thread
            Log.d(TAG, "Start check!");
            new Thread(checkTask).start();

        } else {
            mRun = false;
        }
    }

    static String freqResponse(java.util.List<android.util.Pair<Float, Float>> response) {
        StringBuilder bldr = new StringBuilder();
        for (Pair<Float, Float> pair : response) {
            bldr.append(String.format("%d Hz %.2f\n dB", (int) (pair.first.floatValue()), pair.second.doubleValue()));
        }

        return bldr.toString();
    }

    public static final int DIRECTIONALITY_UNKNOWN = 0;

    /**
     * Microphone directionality type: omni.
     */
    public static final int DIRECTIONALITY_OMNI = 1;

    /**
     * Microphone directionality type: bi-directional.
     */
    public static final int DIRECTIONALITY_BI_DIRECTIONAL = 2;

    /**
     * Microphone directionality type: cardioid.
     */
    public static final int DIRECTIONALITY_CARDIOID = 3;

    /**
     * Microphone directionality type: hyper cardioid.
     */
    public static final int DIRECTIONALITY_HYPER_CARDIOID = 4;

    /**
     * Microphone directionality type: super cardioid.
     */
    public static final int DIRECTIONALITY_SUPER_CARDIOID = 5;

    static String directionalityToText(int directionality) {
        switch (directionality) {
            case MicrophoneInfo.DIRECTIONALITY_OMNI:
                return "Omni";
            case MicrophoneInfo.DIRECTIONALITY_BI_DIRECTIONAL:
                return "Figure 8";
            case MicrophoneInfo.DIRECTIONALITY_CARDIOID:
                return "Cardioid";
            case MicrophoneInfo.DIRECTIONALITY_HYPER_CARDIOID:
                return "Hyper cardioid";
            case MicrophoneInfo.DIRECTIONALITY_SUPER_CARDIOID:
                return "Super cardioid";
        }
        return "Unknown";
    }
}
