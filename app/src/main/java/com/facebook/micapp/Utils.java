package com.facebook.micapp;

import android.content.Context;
import android.media.AudioDescriptor;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioProfile;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.media.audiofx.AudioEffect;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class Utils {
    final static int indentWidth = 4;
    final static String TAG = "micapp.utils";
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

    public static String microphoneInfoDirectionalityToString(int directionality) {
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
        final AudioManager audio_manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] audio_device_info_array = audio_manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        Vector<String> names = new Vector<>();
        names.add("default");
        for (AudioDeviceInfo audio_device_info : audio_device_info_array) {
            Log.d(TAG, "product_name: " + audio_device_info.getProductName());
            int[] channels = audio_device_info.getChannelCounts();

            Log.d(TAG, "type: " + audio_device_info.getType());
            Log.d(TAG, "id: " + audio_device_info.getId());
            for (int channel : channels) {
                Log.d(TAG, "-- ch.count: " + channel);
            }
            int[] rates = audio_device_info.getSampleRates();
            for (int rate : rates) {
                Log.d(TAG, "-- ch.rate: " + rate);
            }

            names.add(audioDeviceToString(audio_device_info));
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
                return "DEFAULT";
            case MediaRecorder.AudioSource.MIC:
                return "MIC";
            case MediaRecorder.AudioSource.VOICE_UPLINK:
                return "VOICE_UPLINK";
            case MediaRecorder.AudioSource.VOICE_DOWNLINK:
                return "VOICE_DOWNLINK";
            case MediaRecorder.AudioSource.VOICE_CALL:
                return "VOICE_CALL";
            case MediaRecorder.AudioSource.CAMCORDER:
                return "CAMCORDER";
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                return "VOICE_RECOGNITION";
            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                return "VOICE_COMMUNICATION";
            case MediaRecorder.AudioSource.REMOTE_SUBMIX:
                return "REMOTE_SUBMIX";
            case MediaRecorder.AudioSource.UNPROCESSED:
                return "UNPROCESSED";
            case MediaRecorder.AudioSource.VOICE_PERFORMANCE:
                return "VOICE_PERFORMANCE";
            default:
                return source  + " is no source";

        }
    }


    public static String microphoneInfoLocationToString(int location) {
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


    public static String audioFormatEncodingToString(int encoding) {
        switch (encoding) {
            case AudioFormat.ENCODING_AAC_ELD:
                return "aac_eld";
            case AudioFormat.ENCODING_AAC_HE_V1:
                return "aac_he_v1";
            case AudioFormat.ENCODING_AAC_HE_V2:
                return "aac_he_v2";
            case AudioFormat.ENCODING_AAC_LC:
                return "aac_lc";
            case AudioFormat.ENCODING_AAC_XHE:
                return "aac_xhe";
            case AudioFormat.ENCODING_AC3:
                return "ac3";
            case AudioFormat.ENCODING_AC4:
                return "ac4";
            case AudioFormat.ENCODING_DEFAULT:
                return "default";
            case AudioFormat.ENCODING_DOLBY_MAT:
                return "dolby_mat";
            case AudioFormat.ENCODING_DOLBY_TRUEHD:
                return "dolby_truehd";
            case AudioFormat.ENCODING_DRA:
                return "dra";
            case AudioFormat.ENCODING_DTS:
                return "dts";
            case AudioFormat.ENCODING_DTS_HD:
                return "dts_hd";
            case AudioFormat.ENCODING_DTS_UHD:
                return "dts_uhd";
            case AudioFormat.ENCODING_E_AC3:
                return "e_ac3";
            case AudioFormat.ENCODING_E_AC3_JOC:
                return "e_ac3_joc";
            case AudioFormat.ENCODING_IEC61937:
                return "iec61937";
            case AudioFormat.ENCODING_INVALID:
                return "invalid";
            case AudioFormat.ENCODING_MP3:
                return "mp3";
            case AudioFormat.ENCODING_MPEGH_BL_L3:
                return "mpegh_bl_l3";
            case AudioFormat.ENCODING_MPEGH_BL_L4:
                return "mpegh_bl_l4";
            case AudioFormat.ENCODING_MPEGH_LC_L3:
                return "mpeg_lc_l3";
            case AudioFormat.ENCODING_MPEGH_LC_L4:
                return "mpeg_lc_l4";
            case AudioFormat.ENCODING_OPUS:
                return "opus";
            case AudioFormat.ENCODING_PCM_16BIT:
                return "pcm_16bit";
            case AudioFormat.ENCODING_PCM_24BIT_PACKED:
                return "pcm_24bit_packed";
            case AudioFormat.ENCODING_PCM_32BIT:
                return "pcm_32bit";
            case AudioFormat.ENCODING_PCM_8BIT:
                return "pcm_8bit";
            case AudioFormat.ENCODING_PCM_FLOAT:
                return "pcm_float";
            default:
                return encoding  + " is no valid encoding";
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

    public static String getIndentation(int indent) {
        String tab = "";
        if (indent > 0){
            tab = String.format("%" + (indent * indentWidth) + "s", ' ');
        }
        return tab;
    }

    public static String getAudioEffectInfo(AudioEffect.Descriptor audio_effect_descriptor) {
        return getAudioEffectInfo(audio_effect_descriptor, 0);
    }

    public static String getAudioEffectInfo(AudioEffect.Descriptor audio_effect_descriptor, int indent) {
        StringBuilder str = new StringBuilder();
        String tab = getIndentation(indent);
        String tab2 = getIndentation(indent + 1);

        str.append(tab + "audio_effect_descriptor {\n");
        str.append(tab2 + "name: \"" + audio_effect_descriptor.name + "\"\n");
        str.append(tab2 + "impl: \"" + audio_effect_descriptor.implementor + "\"\n");
        str.append(tab2 + "uuid: \"" + audio_effect_descriptor.uuid + "\"\n");
        str.append(tab2 + "type: \"" + audio_effect_descriptor.type + "\"\n");
        str.append(tab + "connect mode: \"" + audio_effect_descriptor.connectMode + "\"\n");
        str.append("}\n");
        return str.toString();
    }

    public static String getAudioDeviceInfo(AudioDeviceInfo audio_device_info) {
        return getAudioDeviceInfo(audio_device_info, 0);
    }


    public static String getAudioDeviceInfo(AudioDeviceInfo audio_device_info, int indent) {
        StringBuilder str = new StringBuilder();
        String tab =  getIndentation(indent);
        String tab2 =  getIndentation(indent + 1);
        String tab3 =  getIndentation(indent + 2);
        str.append(tab + "audio_device_info {\n");
        str.append(tab2 + "address: \"" + audio_device_info.getAddress() + "\"\n");
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            List<AudioDescriptor> audio_descriptors = audio_device_info.getAudioDescriptors();
            for (AudioDescriptor audio_descriptor: audio_descriptors) {
                str.append(tab2 + "audio_descriptor {\n");
                str.append(tab3 + "descriptor: \"" + audio_descriptor.getDescriptor() + "\"\n");
                str.append(tab3 + "encapsulation_type: " + audio_descriptor.getEncapsulationType() + "\n");
                str.append(tab3 + "standard: " + audio_descriptor.getStandard() + "\n");
                str.append(tab3 + "hash_code: " + audio_descriptor.hashCode() + "\n");
                str.append(tab3 + "string: \"" + audio_descriptor.toString() + "\"\n");
                str.append(tab2 + "}\n");
            }
            List<AudioProfile> audio_profiles = audio_device_info.getAudioProfiles();
            for (AudioProfile audio_profile: audio_profiles) {
                str.append(tab2 + "audio_profile {\n");
                for (int channel_index_mask: audio_profile.getChannelIndexMasks()) {
                    str.append(tab3 + "channel_index_mask: " + channel_index_mask + "\n");
                }
                for (int channel_mask: audio_profile.getChannelMasks()) {
                    str.append(tab3 + "channel_mask: " + channel_mask + "\n");
                }
                str.append(tab3 + "format: " + audio_profile.getFormat() + "\n");
                str.append(tab3 + "encapsulation_type: " + audio_profile.getEncapsulationType() + "\n");
                str.append(tab3 + "format: " + audio_profile.getFormat() + "\n");
                for (int sample_rate: audio_profile.getSampleRates()) {
                    str.append(tab3 + "sample_rate: " + sample_rate + "\n");
                }
                str.append(tab3 + "hash_code: " + audio_profile.hashCode() + "\n");
                str.append(tab3 + "string: \"" + audio_profile.toString() + "\"\n");
                str.append(tab2 + "}\n");
            }
        }
        for (int channel_count: audio_device_info.getChannelCounts()) {
            str.append(tab2 + "channel_count: " + channel_count + "\n");
        }
        for (int channel_index_mask: audio_device_info.getChannelIndexMasks()) {
            str.append(tab2 + "channel_index_mask: " + channel_index_mask + "\n");
        }
        for (int channel_mask: audio_device_info.getChannelMasks()) {
            str.append(tab2 + "channel_mask: " + channel_mask + "\n");
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            for (int encapsulation_metadata_type: audio_device_info.getEncapsulationMetadataTypes()) {
                str.append(tab2 + "encapsulation_metadata_type: " + encapsulation_metadata_type + "\n");
            }
            for (int encapsulation_mode: audio_device_info.getEncapsulationModes()) {
                str.append(tab2 + "encapsulation_mode: " + encapsulation_mode + "\n");
            }
        }
        for (int encoding: audio_device_info.getEncodings()) {
            str.append(tab2 + "encoding: " + encoding + "\n");
            str.append(tab2 + "encoding_str: \"" + Utils.audioFormatEncodingToString(encoding) + "\"\n");
        }
        str.append(tab2 + "id: " + audio_device_info.getId() + "\n");
        str.append(tab2 + "product_name: \"" + audio_device_info.getProductName() + "\"\n");
        for (int sample_rate: audio_device_info.getSampleRates()) {
            str.append(tab2 + "sample_rate: " + sample_rate + "\n");
        }
        str.append(tab2 + "type: " + audio_device_info.getType() + "\n");
        str.append(tab2 + "type_str: \"" + Utils.audioDeviceTypeToString(audio_device_info.getType()) + "\"\n");
        str.append(tab2 + "hash_code: " + audio_device_info.hashCode() + "\n");
        str.append(tab2 + "is_sink: " + audio_device_info.isSink() + "\n");
        str.append(tab2 + "is_source: " + audio_device_info.isSource() + "\n");
        str.append(tab + "}\n");
        return str.toString();
    }

    public static String getAllAudioDeviceInfo(Context context) {
        return getAllAudioDeviceInfo(context, 0);
    }

    public static String getAllAudioDeviceInfo(Context context, int indent) {
        StringBuilder str = new StringBuilder();
        Log.d(TAG, "Build indenc: " + indent);
        String tab = getIndentation(indent);
        String tab2 = getIndentation(indent);
        final AudioManager audio_manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] audio_device_info_array = audio_manager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        str.append(tab + "audio_device_info_array {\n");
        str.append(tab2 + "size: " + audio_device_info_array.length + "\n");
        for (AudioDeviceInfo audio_device_info : audio_device_info_array) {
            str.append(getAudioDeviceInfo(audio_device_info, 1));
        }
        str.append(tab + "}\n");

        return str.toString();
    }

    public static String getAllMicrophoneInfo(Context context) {
        StringBuilder str = new StringBuilder();
        final AudioManager audio_manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
            List<MicrophoneInfo> microphones = audio_manager.getMicrophones();
            str.append("microphone_info_array {\n");
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
                str.append("    directionality_str: \"" + Utils.microphoneInfoDirectionalityToString(microphone_info.getDirectionality()) + "\"\n");
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
                str.append("    location_str: \"" + Utils.microphoneInfoLocationToString(microphone_info.getLocation()) + "\"\n");
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

    public static AudioDeviceInfo getMatchingAudioDeviceInfo(String id, Context context) {
        final AudioManager audio_manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] audio_device_info_array = audio_manager.getDevices(AudioManager.GET_DEVICES_INPUTS);

        for (AudioDeviceInfo audio_device_info : audio_device_info_array) {
            String tmp = audioDeviceToString(audio_device_info);
            Log.d(TAG, "Compare " + tmp + " with " + id);
            if (tmp.equals(id)) {
                return audio_device_info;
            }
        }

        return null;
    }

    public static String lookupIdString(int id, Context context) {
        final AudioManager aman = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adevs = aman.getDevices(AudioManager.GET_DEVICES_INPUTS);

        Log.d(TAG, "Looking for " + id);
        for (AudioDeviceInfo info : adevs) {
            Log.d(TAG, "Compare " + id + " with " + info.getId() + " (audioDeviceToString(info)) ");
            if (id ==  info.getId()){
                return audioDeviceToString(info);
            }
        }

        return "";
    }


    public static Vector<String> lookupIdsStrings(int[] ids, Context context) {

        Vector<String> inputs = new Vector<>();
        if (ids == null) {
            inputs.add("default");
        } else {
            for (int id: ids) {
                inputs.add(lookupIdString(id, context));
            }

        }

        return inputs;
    }

    public static String audioDeviceToString(AudioDeviceInfo audioDeviceInfo) {
        return audioDeviceInfo.getProductName().toString() + "." +
                audioDeviceTypeToString(audioDeviceInfo.getType()) + "." +
                audioDeviceInfo.getId();
    }

}
