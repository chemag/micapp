package com.facebook.micapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {
    public final static String TAG = "micapp.main";
    PendingIntent mPermissionIntent;
    TextView mSpltText;;

    TextView mInfo;
    CheckBox mRecordCheck;
    Spinner mInputSpinner;
    Spinner mAudioSourceSpinner;
    Player mAudioPlayer;
    Recorder mAudioRecorder;
    int mAudioSession = -1;
    AudioEffects mAudioEffects;
    ToggleButton mAecButton = null;

    ToggleButton mAgcButton = null;

    ToggleButton mNsButton = null;
    ToggleButton mActiveButton = null;
    boolean mRecord = true;

    Handler handler = new Handler();
    float mRecSec = 10.0f;
    int  mAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
    int[] mDeviceIds = null;

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

    private void getInfo(boolean extendedTesting) {
        if (mAudioEffects == null) {
            return;
        }
        File[] externalStorageVolumes =
                ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        File primaryExternalStorage = externalStorageVolumes[0];

        Log.d(TAG, "primaryExternalStorage: "+ primaryExternalStorage.getAbsolutePath());
        String filename = primaryExternalStorage.getAbsolutePath() + "/micapp_info.txt";
        (new File(filename)).delete();
        FileWriter writer = null;
        try {
            writer = new FileWriter(filename);
            writer.write("micapp\n\n");
            writer.write(Utils.getAllAudioDeviceInfo(this));
            writer.write(Utils.getAllMicrophoneInfo(this));
            writer.write(mAudioEffects.toString());

            if (extendedTesting) {
                int audioSessionId = -1;
                // With extened testing take default settings or cli settings and setup te routing
                // verifying availability of hw effects
                Log.d(TAG, "Call rec");
                record(mAudioSource, mDeviceIds, 0);
                try {
                    Thread.sleep((long)(1000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Get audio manager");
                AudioManager audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                List<AudioRecordingConfiguration> audio_record_configs = audio_manager.getActiveRecordingConfigurations();
                if (audio_record_configs.size() == 0) {
                    writer.write("Failed to start recording");
                } else {
                    for (AudioRecordingConfiguration config : audio_record_configs) {
                        writer.write("Effects default {\n");
                        AudioDeviceInfo audio_device_info = config.getAudioDevice();
                        writer.write(Utils.getAudioDeviceInfo(audio_device_info, 1));
                        List<AudioEffect.Descriptor> audio_effects_descriptors = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            audio_effects_descriptors = config.getClientEffects();
                            for (AudioEffect.Descriptor descr : audio_effects_descriptors) {
                                writer.write(Utils.getAudioEffectInfo(descr, 1));
                            }
                        }
                        writer.write("}\n");
                        audioSessionId = config.getClientAudioSessionId();
                        Log.d(TAG, "Enable effects: " + audioSessionId);
                        mAudioEffects.createAudioEffects(audioSessionId);
                        writer.write("Effects instantiated {\n");
                        writer.write(mAudioEffects.getStatusAsString(1));
                        mAudioEffects.setAecStatus(true);
                        mAudioEffects.setAgcStatus(true);
                        mAudioEffects.setNsStatus(true);
                        writer.write("}\n");
                        writer.write("Effects enabled {\n");
                        writer.write(mAudioEffects.getStatusAsString(1));
                        mAudioEffects.setAecStatus(false);
                        mAudioEffects.setAgcStatus(false);
                        mAudioEffects.setNsStatus(false);
                        writer.write("}\n");
                        writer.write("Effects disabled {\n");
                        writer.write(mAudioEffects.getStatusAsString(1));
                        writer.write("}\n");
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void record(int audioSource, int[] inputIds, float secs) {
        mInfo.append("Start record");
        mInfo.append("\nAudio source: "+ audioSource + " for " + secs + " secs");
        Vector<String> inputs = Utils.lookupIdsStrings(inputIds, this);
        Vector<Recorder> recorders = new Vector<>();
        for (String input: inputs) {
            Log.d(TAG, "Start a new recorder:" + input);
            final Recorder rec = new Recorder(this);
            recorders.add(rec);
            (new Thread(() -> rec.checkAndRecord(audioSource, input, true))).start();
        }

        if (secs > 0) {
            try {
                Thread.sleep((long)(secs * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Recorder rec : recorders) {
                rec.stopRecording();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "***\n*** On create\n***\n");


        // make sure the right permissions are set
        String[] permissions = retrieveNotGrantedPermissions(this);
        mAudioPlayer = new Player(this);
        if (permissions != null && permissions.length > 0) {
            int REQUEST_ALL_PERMISSIONS = 0x4562;
            Log.d(TAG, "Request permissions: " + permissions);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ALL_PERMISSIONS);
        }

        mAudioEffects = new AudioEffects();
        Bundle extras = this.getIntent().getExtras();

        if (extras != null) {
            setContentView(R.layout.activity_main_clean);
            mInfo = (TextView) findViewById(R.id.cleanInfo);
            if (extras.containsKey("inputid")) {
                String[] splits = extras.getString("inputid").split("[,]");
                mDeviceIds = new int[splits.length];
                for (int i = 0; i < splits.length; i++){
                    mDeviceIds[i] = Integer.valueOf(splits[i]);
                }
            }

            if (extras.containsKey("audiosource")) {
                mAudioSource = Integer.valueOf(extras.getString("audiosource"));
            }


            if (extras.containsKey("timesec")) {
                mRecSec = Float.valueOf(extras.getString("timesec"));
            }
            if (extras.containsKey("nogui")) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean extendedVerification = false;
                        if (extras.containsKey("fxverify")) {
                            extendedVerification = true;
                        }
                        getInfo(extendedVerification);
                        Log.d(TAG, "No gui, closing down");
                        System.exit(0);
                    }
                });
                t.start();

            }

            if (extras.containsKey("rec")) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "cli recording");
                        record(mAudioSource, mDeviceIds, mRecSec);
                        Log.d(TAG, "Exit");
                        System.exit(0);
                    }
                });
                t.start();
            }

            return;
        }

        setContentView(R.layout.activity_main);

        mSpltText = findViewById(R.id.splText);

        mInfo = (TextView) findViewById(R.id.editMic);
        mActiveButton = (ToggleButton) findViewById(R.id.activeButton);
        mActiveButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecorder();
                } else {
                    Log.d(TAG, "Turn off");
                    stopRecorder();
                }
            }
        });

        Button play = (Button) findViewById(R.id.playSoundButton);
        play.setOnClickListener(view -> mAudioPlayer.playSound());

        ArrayAdapter<String> aa = new ArrayAdapter(this,  android.R.layout.simple_spinner_item);
        aa.addAll(Utils.getDevices(this));
        mInputSpinner = (Spinner)findViewById(R.id.inputSpinner);
        mInputSpinner.setAdapter(aa);
        mInputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mActiveButton.isChecked()) {
                    startRecorder();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        aa = new ArrayAdapter(this,  android.R.layout.simple_spinner_item);
        aa.addAll(Utils.getAudioSources());
        mAudioSourceSpinner = (Spinner)findViewById(R.id.audioSourceTypeSpinner);
        mAudioSourceSpinner.setAdapter(aa);
        mAudioSourceSpinner.setSelection(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        mAudioSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mActiveButton.isChecked()) {
                    startRecorder();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mAecButton = (ToggleButton) findViewById(R.id.addAECButton);
        mAecButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEffects.setAecStatus(isChecked);
                checkEffectStatus();
            }
        });
        mAgcButton = (ToggleButton) findViewById(R.id.addAGCButton);
        mAgcButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEffects.setAgcStatus(isChecked);
                checkEffectStatus();
            }
        });
        mNsButton = (ToggleButton) findViewById(R.id.addNSButton);
        mNsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAudioEffects.setNsStatus(isChecked);
                checkEffectStatus();
            }
        });
        Button tmp = (Button) findViewById(R.id.resetSpl);
        tmp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudioRecorder != null) {
                    mAudioRecorder.resetSpl();
                }
            }
        });

        Runnable checkTask = () -> {
            Log.d(TAG, "Record check manipulated");
            startRecorder();
        };
        mRecordCheck = findViewById(R.id.recordCheck);

        mRecordCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Start check?");
                if (isChecked) {
                    new Thread(checkTask).start();
                } else {
                    stopRecorder();
                    if (mActiveButton.isEnabled()) {
                        startRecorder();
                    }
                }
            }
        });


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


    void createAudioEffects() {
        mAudioEffects.createAudioEffects(mAudioSession);
    }

    void disableAudioEffects() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAudioEffects.disableAudioEffects();
                checkEffectStatus();
            }

        });
    }


    public void stopRecorder() {
        disableAudioEffects();
        if (mAudioRecorder != null) {
            mAudioRecorder.stopRecording();
        }
    }

    public void startRecorder() {
        stopRecorder();

        Runnable checkTask = () -> {
            if (mAudioRecorder == null) {
                mAudioRecorder = new Recorder(this);
                mAudioRecorder.addStatsListener(new Recorder.RecordStatsUpdateListener() {
                    @Override
                    public void InputTextUpdated(String text) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mInfo.setText(text);
                            }
                        });
                    }

                    @Override
                    public void InputSplUpdated(double maxPeak, double maxRMS, double minPeak, double minRMS, double currentPeak, double currentRMS) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String text = String.format("Peak %4d dB (%4d dB / %4d dB)\nRMS %4d dB (%4d dB / %4d dB)",
                                        (int)currentPeak, (int)maxPeak, (int)minPeak, (int)currentRMS, (int)maxRMS, (int)minRMS);
                                mSpltText.setText(text);
                            }
                        });
                    }
                });
            }
            Log.d(TAG, "start recorder, record to file: " + mRecordCheck.isChecked());
            mAudioSession = mAudioRecorder.checkAndRecord(mAudioSourceSpinner.getSelectedItemPosition(),
                                                          mInputSpinner.getSelectedItem().toString(),
                                                          mRecordCheck.isChecked());
            Log.d(TAG, "check effecs: "+mAudioSession);
            if (mAudioSession > 0) {
                createAudioEffects();
            } else {
                mInfo.setText("Failed to create recorder");
            }

        };

        // start the thread
        Log.d(TAG, "Start check!");
        new Thread(checkTask).start();

        // Better not check to often (sometimes there seems to be some delay in fx handling)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkEffectStatus();
            }
        }, 2000);
    }

    void checkEffectStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAudioEffects.isAecAvailable()) {
                    mAecButton.setBackgroundColor(getColor(R.color.design_default_color_background));
                    mAecButton.setChecked(mAudioEffects.isAecEnabled());
                } else {
                    mAecButton.setBackgroundColor(getColor(R.color.design_default_color_error));
                    mAecButton.setChecked(false);
                }
                if (mAudioEffects.isAgcAvailable()) {
                    mAgcButton.setBackgroundColor(getColor(R.color.design_default_color_background));
                    mAgcButton.setChecked(mAudioEffects.isAgcEnabled());
                } else {
                    mAgcButton.setBackgroundColor(getColor(R.color.design_default_color_error));
                    mAgcButton.setChecked(false);
                }
                if (mAudioEffects.isNsAvailable()) {
                    mNsButton.setBackgroundColor(getColor(R.color.design_default_color_background));
                    mNsButton.setChecked(mAudioEffects.isNsEnabled());
                } else {
                    mNsButton.setBackgroundColor(getColor(R.color.design_default_color_error));
                    mNsButton.setChecked(false);
                }
            }
        });


    }

}
