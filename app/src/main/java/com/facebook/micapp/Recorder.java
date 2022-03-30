package com.facebook.micapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MicrophoneInfo;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Vector;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Recorder {
    final static String TAG = "mic.record";
    int mAudioSession = -1;
    Context mContext;
    boolean mIsRunning = false;
    String mStatusText = "";


    double mMaxPeakVal = -100;
    double mMaxRMSVal = -100;
    double mMinPeakVal = 0;
    double mMinRMSVal = 0;

    Vector<RecordStatsUpdateListener> mStatsListeners = new Vector<>();

    public Recorder(Context context) {
        mContext = context;
    }

    int checkAndRecord(int audioInputSource, String inputDevice, boolean record) {
        resetSpl();
        mIsRunning = true;
        Thread recordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Missing audio record permission");
                    return;
                }
                Log.d(TAG, "Open audio using " + Utils.audioSourceToString(audioInputSource) + " source");
                AudioRecord tmprec = null;
                try {
                    tmprec = new AudioRecord.Builder()
                            .setAudioSource(audioInputSource)
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(48000)
                                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                    .build())
                            .setBufferSizeInBytes(2 * AudioRecord.getMinBufferSize(480000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
                            .build();
                } catch(Exception ex) {
                    Log.e(TAG, "Failed to create recorder: " + ex.getMessage());
                    return;
                }
                final AudioRecord recorder = tmprec;
                AudioDeviceInfo routed_ok = null;
                if (!inputDevice.toLowerCase().equals("default")) {
                    Log.d(TAG, "\n\n--------------- Check Audio ---------------------");
                    Log.d(TAG, "Look for "+inputDevice);
                    routed_ok = Utils.getMatchingAudioDeviceInfo(inputDevice, mContext);
                    recorder.setPreferredDevice(routed_ok);
                }
                float sampleRate = 48000;
                int bufferSize = (int)sampleRate;
                final byte audioData[] = new byte[(int) (bufferSize * 2)];
                short[] shorts = new short[(int) bufferSize];

                BufferedOutputStream os = null;
                String filename = null;
                if (record) {
                    // open the record file path
                    File[] externalStorageVolumes =
                            ContextCompat.getExternalFilesDirs(mContext.getApplicationContext(), null);
                    File primaryExternalStorage = externalStorageVolumes[0];
                    AudioDeviceInfo info = routed_ok;
                    if (info == null) {
                        // Need to make this funny move since it seems the routing is not done until
                        // recording is started
                        info = recorder.getRoutedDevice();
                    }
                    String id = Utils.clean(info.getProductName().toString() + "." +
                                                  Utils.audioDeviceTypeToString( info.getType())+ "." +
                                                  info.getId());
                    filename = primaryExternalStorage + "/capture_48kHz_" + id + ".raw";
                    Log.d(TAG, "Record to \"" + filename + "\"");
                    try {
                        os = new BufferedOutputStream(new FileOutputStream(filename));
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File not found for recording ", e);
                        // TODO: how to break
                    }
                    final BufferedOutputStream fos = os;
                }
                final String filerecPath = filename;
                final BufferedOutputStream fos = os;
                Thread rec = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Start Recording");
                        recorder.startRecording();

                        List<MicrophoneInfo> minfo = null;
                        StringBuilder strBuilder = new StringBuilder();
                        if (filerecPath != null) {
                            strBuilder.append("Recording to: \"" + filerecPath);
                            strBuilder.append("\n------------\n\n");
                        }
                        try {
                            minfo = recorder.getActiveMicrophones();
                            strBuilder.append("Microphone info:\n");
                            strBuilder.append("\n--\n");
                            for (MicrophoneInfo mic : minfo) {
                                Log.d(TAG, "Input: " + recorder.getRoutedDevice().getProductName());
                                Log.d(TAG, "Address: " + mic.getAddress());
                                Log.d(TAG, "Desc: " + mic.getDescription());
                                Log.d(TAG, "Direc: " + mic.getDirectionality());
                                Log.d(TAG, "Sens: " + mic.getSensitivity());
                                strBuilder.append("Path:" + mic.getAddress());
                                strBuilder.append("\nDesc:" + mic.getDescription());
                                strBuilder.append("\nDirectiviy:" + Utils.microphoneInfoDirectionalityToString(mic.getDirectionality()));
                                strBuilder.append("\nSensitivity:" + mic.getSensitivity());
                                strBuilder.append("\nMax spl:" + mic.getMaxSpl());
                                strBuilder.append("\nMin spl:" + mic.getMaxSpl());
                                strBuilder.append("\n------------\n\n");


                            }
                            final String text = strBuilder.toString();
                            mStatusText = text;
                            for (RecordStatsUpdateListener listener : mStatsListeners) {
                                listener.InputTextUpdated(mStatusText);
                            }

                            AudioDeviceInfo audioDeviceInfo  = recorder.getRoutedDevice();
                            String descr = Utils.audioDeviceToString(audioDeviceInfo);
                            if (!inputDevice.equals("default") && descr.equals(inputDevice)) {
                                Log.d(TAG, "Preferred device succesfully activated");
                            } else if (!inputDevice.equals("default")) {
                                Log.d(TAG, "Wrong device is running!");
                                Log.d(TAG, "Wanted: \"" + inputDevice + "\"");
                                Log.d(TAG, "Routed: \"" + descr + "\"");
                                mIsRunning = false;
                                recorder.stop();
                                fos.close();
                                (new File(filerecPath)).delete();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        mAudioSession = recorder.getAudioSessionId();
                        while (mIsRunning) {
                            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                                Log.e(TAG, "No recording");
                                for (RecordStatsUpdateListener listener : mStatsListeners) {
                                    listener.InputTextUpdated("Recording failed");
                                }
                                break;
                            }
                            int read_bytes = recorder.read(audioData, 0, audioData.length);
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
                            final double dB = Math.round(Utils.floatToDB(Math.sqrt(nval)));
                            final double peak_dB = Math.round(Utils.floatToDB(max));

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


                            for (RecordStatsUpdateListener listener : mStatsListeners) {
                                listener.InputSplUpdated(mMaxPeakVal, mMaxRMSVal, mMinPeakVal, mMinRMSVal, peak_dB, dB);
                            }
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
                mIsRunning = false;
                if (fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        recordThread.start();


        while (mAudioSession == -1 && mIsRunning) {
            try {
                Log.d(TAG, "Session = " + mAudioSession  + ", running = " + mIsRunning);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Recorder started, as = "+mAudioSession + ", running = "+mIsRunning);
        return mAudioSession;
    }


    public void stopRecording() {
        mIsRunning = false;

        while (mAudioSession == -1) {
            try {
                Log.d(TAG, "Session = " + mAudioSession  + ", running = " + mIsRunning);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void resetSpl() {

        Log.e(TAG, "Reset spl");
        mMaxPeakVal = -100;
        mMinPeakVal = 0;
        mMaxRMSVal = -100;
        mMinRMSVal = 0;
        for (RecordStatsUpdateListener listener : mStatsListeners) {
            listener.InputSplUpdated(mMaxPeakVal, mMaxRMSVal, mMinPeakVal, mMinRMSVal, -100, -100);
        }
    }

    public void addStatsListener(RecordStatsUpdateListener listener) {
        if (!mStatsListeners.contains(listener)) {
            mStatsListeners.add(listener);

        }
    }

    public interface RecordStatsUpdateListener {
        public void InputTextUpdated(String text);

        public void InputSplUpdated(double maxPeak,
                                    double maxRMS,
                                    double minPeak,
                                    double minRMS,
                                    double currentPeak,
                                    double currentRMS);

    }
}
