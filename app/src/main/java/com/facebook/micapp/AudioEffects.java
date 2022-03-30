package com.facebook.micapp;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.util.Vector;

public class AudioEffects {
    final static String TAG = "mic.fx";

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

    void enableAudioEffects(int sessionid) {
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

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("audio_effects {\n");
        str.append("  aec_available: " + isAecAvailable() + "\n");
        str.append("  agc_available: " + isAgcAvailable() + "\n");
        str.append("  ns_available: " + isNsAvailable() + "\n");
        str.append("  aec_allocated: " + (mAcousticEchoCanceler != null) + "\n");
        str.append("  agc_allocated: " + (mAutomaticGainControl != null) + "\n");
        str.append("  ns_allocated: " + (mNoiseSuppressor != null) + "\n");
        str.append("  aec_enabled: " + isAecEnabled() + "\n");
        str.append("  agc_enabled: " + isAgcEnabled() + "\n");
        str.append("  ns_enabled: " + isNsEnabled() + "\n");
        str.append("}\n");
        return str.toString();
    }
}
