package com.facebook.micapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {
    public final static String TAG = "mic";
    PendingIntent mPermissionIntent;
    TextView mSpltText;;

    TextView mInfo;
    CheckBox mRecordCheck;
    Spinner mInputSpinner;
    Spinner mAudioSourceSpinner;
    Player mAudioPlayer;
    Recorder mAudioRecorder;
    int mAudioSession = -1;
    AudioEffects mFx;
    ToggleButton mAecButton = null;

    ToggleButton mAgcButton = null;

    ToggleButton mNsButton = null;
    ToggleButton mActiveButton = null;
    boolean mRecord = true;

    Handler handler = new Handler();
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

    private void getInfo() {
        if (mFx == null) {
            return;
        }
        File[] externalStorageVolumes =
                ContextCompat.getExternalFilesDirs(getApplicationContext(), null);
        File primaryExternalStorage = externalStorageVolumes[0];

        Log.d(TAG, "primaryExternalStorage: "+ primaryExternalStorage.getAbsolutePath());
        String filename = primaryExternalStorage.getAbsolutePath() + "/micapp_info.txt";
        FileWriter writer = null;
        try {
            writer = new FileWriter(filename);
            writer.write("micapp\n\n");
            writer.write(Utils.getAllAudioDeviceInfo(this));
            writer.write(Utils.getAllMicrophoneInfo(this));

            writer.write("\nEffects:");
            if (mFx.isAecAvailable()) {
                writer.write("\n+ AEC is available");
            } else {
                writer.write("\n- No AEC available");
            }
            if (mFx.isAgcAvailable()) {
                writer.write("\n+ AGC is available");
            } else {
                writer.write("\n- No AGC available");
            }
            if (mFx.isNsAvailable()) {
                writer.write("\n+ NS is available");
            } else {
                writer.write("\n- No NS available");
            }
            writer.write("\n----\n");

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

        mFx = new AudioEffects();
        Bundle extras = this.getIntent().getExtras();

        if (extras != null) {
            if (extras.containsKey("nogui")) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getInfo();
                        Log.d(TAG, "No gui, closing down");
                        System.exit(0);
                    }
                }, 100);

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
                mFx.setAecStatus(isChecked);
                checkEffectStatus();
            }
        });
        mAgcButton = (ToggleButton) findViewById(R.id.addAGCButton);
        mAgcButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mFx.setAgcStatus(isChecked);
                checkEffectStatus();
            }
        });
        mNsButton = (ToggleButton) findViewById(R.id.addNSButton);
        mNsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mFx.setNsStatus(isChecked);
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
        mFx.enableAudioEffects(mAudioSession);
    }

    void disableAudioEffects() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFx.disableAudioEffects();
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
                if (mFx.isAecAvailable()) {
                    mAecButton.setBackgroundColor(getColor(R.color.design_default_color_background));
                    mAecButton.setChecked(mFx.isAecEnabled());
                } else {
                    mAecButton.setBackgroundColor(getColor(R.color.design_default_color_error));
                    mAecButton.setChecked(false);
                }
                if (mFx.isAgcAvailable()) {
                    mAgcButton.setBackgroundColor(getColor(R.color.design_default_color_background));
                    mAgcButton.setChecked(mFx.isAgcEnabled());
                } else {
                    mAgcButton.setBackgroundColor(getColor(R.color.design_default_color_error));
                    mAgcButton.setChecked(false);
                }
                if (mFx.isNsAvailable()) {
                    mNsButton.setBackgroundColor(getColor(R.color.design_default_color_background));
                    mNsButton.setChecked(mFx.isNsEnabled());
                } else {
                    mNsButton.setBackgroundColor(getColor(R.color.design_default_color_error));
                    mNsButton.setChecked(false);
                }
            }
        });


    }

}
