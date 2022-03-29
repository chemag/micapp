package com.facebook.micapp;

import android.Manifest;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.util.Log;
import android.util.Pair;

import java.util.Vector;

import androidx.annotation.RequiresPermission;

public class Utils {
    final static String TAG = "mic.utils";
    public static double dBToFloat(double val) {
        return Math.pow(10, val / 20.0);
    }

    public static double floatToDB(double val) {
        if (val <= 0)
            return -100;
        return 20 * Math.log10(val);
    }


    public static String freqResponse(java.util.List<android.util.Pair<Float, Float>> response) {
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

    public static String directionalityToText(int directionality) {
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

    static Vector<String> getDevices(Context context) {
        final AudioManager aman = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adevs = aman.getDevices(AudioManager.GET_DEVICES_INPUTS);
        Vector<String> names = new Vector<>();
        names.add("default");
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
            names.add(info.getProductName().toString() + "." + audioDeviceTypeToString(info.getType()));
        }

        return names;
    }

    static Vector<String> getAudioSources() {
        Vector<String> names = new Vector<>();
        for (int i = 0; i <= MediaRecorder.AudioSource.VOICE_PERFORMANCE; i++ ) {
            names.add(audioSourceToString(i));
        }
        return names;
    }

    public static String audioSourceToString(int source) {
        switch(source) {
            case MediaRecorder.AudioSource
                    .DEFAULT:
                return "default";
            case MediaRecorder.AudioSource.MIC:
                return "mic";
            case MediaRecorder.AudioSource.VOICE_UPLINK:
                return "voice up-link";
            case MediaRecorder.AudioSource.VOICE_DOWNLINK:
                return "voice down-link";
            case MediaRecorder.AudioSource.VOICE_CALL:
                return "call";
            case MediaRecorder.AudioSource.CAMCORDER:
                return "camcorder";
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                return "voice recognition";
            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                return "voice communication";
            case MediaRecorder.AudioSource.REMOTE_SUBMIX:
                return "remote submix";
            case MediaRecorder.AudioSource.UNPROCESSED:
                return "unprocessed";
            case MediaRecorder.AudioSource.VOICE_PERFORMANCE:
                return "performance";
            default:
                return source  + " is no source";

        }
    }


    public static String audioDeviceTypeToString(int source) {
        switch (source) {
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return "unknown";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "builtin earpiece";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "builtin speaker";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "wired headset";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "wired headphones";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "line analog";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "line digital";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "bt sco";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "bt a2dp";
            case AudioDeviceInfo.TYPE_HDMI:
                return "hdmi";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "hdmi arc";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "usb device";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "usb accessory";
            case AudioDeviceInfo.TYPE_DOCK:
                return "dock";
            case AudioDeviceInfo.TYPE_FM:
                return "fm";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "builtin mic";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "fm tuner";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "tv tuner";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "telephony";
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "aux line";
            case AudioDeviceInfo.TYPE_IP:
                return "ip";
            case AudioDeviceInfo.TYPE_BUS:
                return "bus";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "usb headset";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "hearing aid";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE:
                return "builtin speaker safe";
            case AudioDeviceInfo.TYPE_REMOTE_SUBMIX:
                return "remote sub mix";
            case AudioDeviceInfo.TYPE_BLE_HEADSET:
                return "ble headset";
            case AudioDeviceInfo.TYPE_BLE_SPEAKER:
                return "ble speaker";
            //TODO: case AudioDeviceInfo.TYPE_ECHO_REFERENCE:
                //return "echo reference";
            case AudioDeviceInfo.TYPE_HDMI_EARC:
                return "hdmi earc";
            default:
                return source  + " is no device type";
        }
    }

    public static String clean(String input) {
        char[] replace = {' '};

        String ret = input;
        for (char c: replace) {
            ret = ret.replace(c, '_');
        }

        return ret;
    }

}
