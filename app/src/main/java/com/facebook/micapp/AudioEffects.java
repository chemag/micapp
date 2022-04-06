package com.facebook.micapp;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.util.Vector;
import java.util.UUID;

public class AudioEffects {
    final static String TAG = "micapp.audiofx";

    int mAudioSession = -1;
    AcousticEchoCanceler mAcousticEchoCanceler = null;
    AutomaticGainControl mAutomaticGainControl = null;
    NoiseSuppressor mNoiseSuppressor = null;

    Vector<StatusUpdatedListener> listeners = new Vector<>();

    void disableAudioEffects() {
        try {
            if (mAcousticEchoCanceler != null && mAcousticEchoCanceler.getEnabled()) {
                Log.d(TAG, "Release aec");
                mAcousticEchoCanceler.release();
            }
        } catch (IllegalStateException is) {
            Log.e(TAG, "Wrong state");
        }
        try {
            if (mAutomaticGainControl != null && mAutomaticGainControl.getEnabled()) {
                Log.d(TAG, "Release agc");
                mAutomaticGainControl.release();
            }
        } catch (IllegalStateException is) {
            Log.e(TAG, "Wrong state");
        }
        try {
            if (mNoiseSuppressor != null && mNoiseSuppressor.getEnabled()) {
                Log.d(TAG, "Release ns");
                mNoiseSuppressor.release();
            }
        } catch (IllegalStateException is) {
            Log.e(TAG, "Wrong state");
        }
    }

    void createAudioEffects(int sessionid) {
        disableAudioEffects();
        mAudioSession = sessionid;
        if (mAudioSession < 0) {
            return;
        }

        Log.d(TAG, "Create audio effects, session: " + mAudioSession);
                if (mAudioSession != -1) {
        if (mAcousticEchoCanceler != null) {
            Log.d(TAG, "Release aec");
            mAcousticEchoCanceler.release();
        }
        mAcousticEchoCanceler = AcousticEchoCanceler.create(mAudioSession);
        if (mAcousticEchoCanceler != null) {
            mAcousticEchoCanceler.setControlStatusListener(new AudioEffect.OnControlStatusChangeListener() {
                @Override
                public void onControlStatusChange(AudioEffect effect, boolean controlGranted) {
                    Log.d(TAG, "Status changes: " + effect.getDescriptor() + ", granted: " + controlGranted);
                    for (StatusUpdatedListener listener: listeners) {
                        listener.onStatusUpdates();
                    }
                }
            });
        } else {
            Log.e(TAG, "Could not create a AEC controller");
        }

        if (mAutomaticGainControl != null) {
            Log.d(TAG, "Release agc");
            mAutomaticGainControl.release();
        }
        mAutomaticGainControl = AutomaticGainControl.create(mAudioSession);
        if (mAutomaticGainControl != null) {
            mAutomaticGainControl.setControlStatusListener(new AudioEffect.OnControlStatusChangeListener() {
                @Override
                public void onControlStatusChange(AudioEffect effect, boolean controlGranted) {
                    Log.d(TAG, "Status changes: " + effect.getDescriptor() + ", granted: " + controlGranted);
                    for (StatusUpdatedListener listener: listeners) {
                        listener.onStatusUpdates();
                    }
                }
            });
        } else {
            Log.e(TAG, "Could not create a AGC controller");
        }

        if (mNoiseSuppressor != null) {
            Log.d(TAG, "Release ns");
            mNoiseSuppressor.release();
        }
        mNoiseSuppressor = NoiseSuppressor.create(mAudioSession);
        if (mNoiseSuppressor != null) {
            mNoiseSuppressor.setControlStatusListener(new AudioEffect.OnControlStatusChangeListener() {
                @Override
                public void onControlStatusChange(AudioEffect effect, boolean controlGranted) {
                    Log.d(TAG, "Status changes: " + effect.getDescriptor() + ", granted: " + controlGranted);
                    for (StatusUpdatedListener listener: listeners) {
                        listener.onStatusUpdates();
                    }
                }
            });
            Log.d(TAG, "Status ns enabled: " + mNoiseSuppressor.getEnabled());

        } else {
            Log.e(TAG, "Could not create a NS controller");
        }
    } else {
        Log.d(TAG, "No audio session is available");
    }

    }

    private boolean setStatus(AudioEffect effect, boolean enable) {
        if (effect != null) {
            try {
                int status = effect.setEnabled(enable);
                if (status == AudioEffect.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Wrong state for action");

                } else if (status == AudioEffect.ERROR_DEAD_OBJECT) {
                    Log.e(TAG, "Dead object");
                } else if (status != AudioEffect.SUCCESS) {
                    Log.e(TAG, "Operation failed: " + status);
                }
            } catch (IllegalStateException is) {
                Log.e(TAG, "Effect is in wrong state: " + is.getMessage());
                return false;
            }

            return effect.getEnabled();
        }
        else return false;
    }

    public boolean setAecStatus(boolean enable) {
        return setStatus(mAcousticEchoCanceler, enable);
    }

    public boolean setAgcStatus(boolean enable) {
        return setStatus(mAutomaticGainControl, enable);
    }

    public boolean setNsStatus(boolean enable) {
        return setStatus(mNoiseSuppressor, enable);
    }

    public boolean isAecEnabled() {
        try {
            if (mAcousticEchoCanceler != null)
                return mAcousticEchoCanceler.getEnabled();
            else return false;
        } catch (IllegalStateException is) {
            Log.e(TAG, "Aec is in wrong state: " + is.getMessage());
        }
        return false;
    }

    public boolean isAecAvailable() {
        return AcousticEchoCanceler.isAvailable();
    }

    public boolean isAgcEnabled() {
        try {
            if (mAutomaticGainControl != null)
                return mAutomaticGainControl.getEnabled();
            else return false;
        } catch (IllegalStateException is) {
            Log.e(TAG, "Agc is in wrong state: " + is.getMessage());
        }
            return false;
    }

    public boolean isAgcAvailable() {
        return AutomaticGainControl.isAvailable();
    }

    public boolean isNsEnabled() {
        try {
            if (mNoiseSuppressor != null)
                return mNoiseSuppressor.getEnabled();
            else return false;
        } catch (IllegalStateException is) {
            Log.e(TAG, "Ns is in wrong state: " + is.getMessage());
        }
        return false;
    }

    public boolean isNsAvailable() {
        return NoiseSuppressor.isAvailable();
    }

    public void addStatusUpdateListener(StatusUpdatedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    public interface StatusUpdatedListener {
        public void onStatusUpdates();
    }

    public String typeToString(UUID type) {
        if (type.compareTo(AudioEffect.EFFECT_TYPE_AEC) == 0)
            return "aec";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_AGC) == 0)
            return "agc";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_BASS_BOOST) == 0)
            return "bass_boost";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_ENV_REVERB) == 0)
            return "env_reverb";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_EQUALIZER) == 0)
            return "equalizer";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_NS) == 0)
            return "ns";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_PRESET_REVERB) == 0)
            return "preset_reverb";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_VIRTUALIZER) == 0)
            return "virtualizer";
        else if (type.compareTo(AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING) == 0)
            return "dynamics_processing";
        else if (android.os.Build.VERSION.SDK_INT >= 31) {
            if (type.compareTo(AudioEffect.EFFECT_TYPE_HAPTIC_GENERATOR) == 0)
                return "haptic_generator";
        }
        return "unknown";
    }

    public String toString(int indent) {
        String tab = Utils.getIndentation(indent);
        StringBuilder str = new StringBuilder();
        AudioEffect.Descriptor[] descriptor_array = AudioEffect.queryEffects();
        str.append(tab + "audio_effect_descriptors {\n");
        for (AudioEffect.Descriptor descriptor: descriptor_array) {
            indent += 1;
            tab = Utils.getIndentation(indent);
            str.append(tab + "descriptor {\n");
            indent += 1;
            tab = Utils.getIndentation(indent);
            str.append(tab + "connectMode: \"" + descriptor.connectMode + "\"\n");
            str.append(tab + "implementor: \"" + descriptor.implementor + "\"\n");
            str.append(tab + "name: \"" + descriptor.name + "\"\n");
            str.append(tab + "type: \"" + typeToString(descriptor.type) + "\"\n");
            str.append(tab + "uuid: \"" + descriptor.uuid.toString() + "\"\n");
            indent -= 1;
            tab = Utils.getIndentation(indent);
            str.append(tab + "}\n");
            indent -= 1;
            tab = Utils.getIndentation(indent);
        }
        str.append(tab + "}\n");
        str.append(getStatusAsString(indent, false));
        return str.toString();
    }

    public String getStatusAsString(int indent, boolean full) {
        String tab = Utils.getIndentation(indent);
        StringBuilder str = new StringBuilder();
        str.append(tab + "audio_effects {\n");
        indent += 1;
        tab = Utils.getIndentation(indent);
        str.append(tab + "aec_available: " + isAecAvailable() + "\n");
        str.append(tab + "agc_available: " + isAgcAvailable() + "\n");
        str.append(tab + "ns_available: " + isNsAvailable() + "\n");
        if (full) {
            str.append(tab + "aec_allocated: " + (mAcousticEchoCanceler != null) + "\n");
            str.append(tab + "agc_allocated: " + (mAutomaticGainControl != null) + "\n");
            str.append(tab + "ns_allocated: " + (mNoiseSuppressor != null) + "\n");
            str.append(tab + "aec_enabled: " + isAecEnabled() + "\n");
            str.append(tab + "agc_enabled: " + isAgcEnabled() + "\n");
            str.append(tab + "ns_enabled: " + isNsEnabled() + "\n");
        }
        indent -= 1;
        tab = Utils.getIndentation(indent);
        str.append(tab + "}\n");
        return str.toString();
    }
}
