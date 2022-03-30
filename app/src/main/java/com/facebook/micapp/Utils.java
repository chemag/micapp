package com.facebook.micapp;

import android.Manifest;
import android.content.Context;
import android.media.AudioDescriptor;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioProfile;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.List;
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


    public static String locationToString(int location) {
        switch (location) {
            case MicrophoneInfo.LOCATION_UNKNOWN:
                return "unknown";
            case MicrophoneInfo.LOCATION_MAINBODY:
                return "mainbody";
            case MicrophoneInfo.LOCATION_MAINBODY_MOVABLE:
                return "mainbody movable";
            case MicrophoneInfo.LOCATION_PERIPHERAL:
                return "peripheral";
            default:
                return location  + " is no valid location";
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

    public static String getAllInputInfo(Context context) {
        StringBuilder str =  new StringBuilder();
        final AudioManager aman = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adevs = aman.getDevices(AudioManager.GET_DEVICES_INPUTS);
        str.append("Number of inputs: " + adevs.length);
        str.append("\n----\n");
        for (AudioDeviceInfo info : adevs) {
            str.append("\n" + info.getProductName());
            str.append("\nAddress:" + info.getAddress());
            str.append("\nType: " + Utils.audioDeviceTypeToString( info.getType()));
            str.append(" (" + info.getType() + ")");
            str.append("\nid: " + info.getId());
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                List<AudioDescriptor> descs = info.getAudioDescriptors();
                for (AudioDescriptor desc: descs) {
                    str.append("\n" + desc.getDescriptor());
                }
            }
            int[] channels = info.getChannelCounts();

            for (int channel : channels) {
                str.append("\n-- ch.count: " + channel);
            }
            int[] rates = info.getSampleRates();
            for (int rate : rates) {
                str.append("\n-- ch.rate: " + rate);
            }
            str.append("\n----\n");
        }

        return str.toString();
    }

    public static String getAllMicrophoneInfo(Context context) {
        StringBuilder str =  new StringBuilder();
        final AudioManager audio_manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
            List<MicrophoneInfo> microphones = audio_manager.getMicrophones();
            str.append("microphones {\n");
            str.append("  size: " + microphones.size() + "\n");
            for (MicrophoneInfo microphone_info : microphones) {
                str.append("  microphone_info {\n");
                str.append("    address: \"" + microphone_info.getAddress() + "\"\n");
                List<Pair<Integer, Integer>> channel_mappings = microphone_info.getChannelMapping();
                str.append("    channel_mappings {\n");
                for (Pair<Integer, Integer> channel_mapping: channel_mappings) {
                    str.append("      channel_mapping {\n");
                    str.append("        channel_index: " + channel_mapping.first + "\n");
                    str.append("        channel_mapping_type: " + channel_mapping.second + "\n");
                    str.append("      }\n");
                }
                str.append("    }\n");
                str.append("    description: \"" + microphone_info.getDescription() + "\"\n");
                str.append("    directionality: " + microphone_info.getDirectionality() + "\n");
                str.append("    directionality_str: \"" + Utils.directionalityToText(microphone_info.getDirectionality()) + "\"\n");
                str.append("    frequency_responses {\n");
                List<Pair<Float, Float>> frequency_responses = microphone_info.getFrequencyResponse();
                for (Pair<Float, Float> frequency_response: frequency_responses) {
                    str.append("      frequency_response {\n");
                    str.append(String.format("        frequency_hz: %5.0f\n", frequency_response.first));
                    str.append(String.format("        response_db: %5.1f\n", frequency_response.second));
                    str.append("      }\n");
                }
                str.append("    }\n");
                str.append("    group: " + microphone_info.getGroup() + "\n");
                str.append("    id: " + microphone_info.getId() + "\n");
                str.append("    index_in_the_group: " + microphone_info.getIndexInTheGroup() + "\n");
                str.append("    location: " + microphone_info.getLocation() + "\n");
                str.append("    location_str: \"" + Utils.locationToString(microphone_info.getLocation()) + "\"\n");
                str.append("    max_spl_1000hz_db: " + microphone_info.getMaxSpl() + "\n");
                str.append("    min_spl_1000hz_db: " + microphone_info.getMinSpl() + "\n");
                MicrophoneInfo.Coordinate3F orientation = microphone_info.getOrientation();
                if (orientation == MicrophoneInfo.ORIENTATION_UNKNOWN) {
                    str.append("    orientation: unknown\n");
                } else {
                    str.append("    orientation {\n");
                    str.append("      x: " + orientation.x + "\n");
                    str.append("      y: " + orientation.y + "\n");
                    str.append("      z: " + orientation.z + "\n");
                    str.append("    }\n");
                }
                MicrophoneInfo.Coordinate3F position = microphone_info.getPosition();
                if (position == MicrophoneInfo.POSITION_UNKNOWN) {
                    str.append("    position: unknown\n");
                } else {
                    str.append("    position {\n");
                    str.append("      x: " + position.x + "\n");
                    str.append("      y: " + position.y + "\n");
                    str.append("      z: " + position.z + "\n");
                    str.append("    }\n");
                }
                float sensitivity = microphone_info.getSensitivity();
                if (sensitivity == MicrophoneInfo.SENSITIVITY_UNKNOWN) {
                    str.append("    sensitivity_94db_1kHz: unknown\n");
                } else {
                    str.append("    sensitivity_94db_1kHz: " + sensitivity + "\n");
                }
                str.append("    type: " + microphone_info.getType() + "\n");
                str.append("    type_str: \"" + Utils.audioDeviceTypeToString(microphone_info.getType()) + "\"\n");
                str.append("  }\n");
            }
            str.append("}\n");

            return str.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static AudioDeviceInfo getMatchingDeviceInfo(String id, Context context) {
        final AudioManager aman = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adevs = aman.getDevices(AudioManager.GET_DEVICES_INPUTS);

        for (AudioDeviceInfo info : adevs) {
            String tmp = info.getProductName().toString() + "." + Utils.audioDeviceTypeToString( info.getType());
            if (tmp.equals(id)) {
                return info;
            }
        }

        return null;
    }

}
