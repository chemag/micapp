# micapp

micapp is a tool to characterizate microphones in Android.


# 1. Prerequisites

For running encapp:
* adb connection to the device being tested.


# 2. Operation: Install App

Install the app:
```
$ ./gradlew installDebug
...
BUILD SUCCESSFUL in 2s
27 actionable tasks: 1 executed, 26 up-to-date
```

Check the app has been installed:
```
$ adb shell pm list packages |grep micapp
package:com.facebook.micapp
```

Uninstall the app:
```
$ adb shell cmd package uninstall com.facebook.micapp
Success
$ adb shell pm list packages |grep micapp
$
```

Build the app:
```
$ ./gradlew build
...
BUILD SUCCESSFUL in 2s
61 actionable tasks: 1 executed, 60 up-to-date
```


# 3. Operation: Get List of Available Mics and Properties

After installing the app, run:

```
$ ./scripts/micapp.py info
micapp

audio_device_info_array {
  size: 7
  audio_device_info {
    address: "bottom"
    channel_count: 2
    channel_mask: 12
    encoding: 2
    encoding_str: "pcm_16bit"
    encoding: 4
    encoding_str: "pcm_float"
    id: 8
    product_name: "..."
    sample_rate: 48000
    type: 15
    type_str: "builtin mic"
    hash_code: 39
    is_sink: false
    is_source: true
  }
  ...
}
microphone_info_array {
  size: 8
  microphone_info {
    address: "bottom"
    channel_mappings {
    }
  ...
}
audio_effect_descriptors {
  descriptor {
    connectMode: "Insert"
    implementor: "The Android Open Source Project"
    name: "Dynamics Processing"
  ...
}
audio_effects {
  aec_available: false
  agc_available: false
  ns_available: false
}

__________________

Data also available in micapp_info_device_name.txt
```

Note that the output contains 4 different Sections.
* (1) audio devices (`AudioDeviceInfo` array)
* (2) microphones (`MicrophoneInfo` array)
* (3) audio effect descriptors (`AudioEffect.Descriptor` array)
* (4) audio effects available

You can get extra output using the "`--extended`" switch:
* (5) audio effects operation (including used `AudioSource` and the
  corresponding default "`AudioDeviceInfo`"

```
$ ./scripts/micapp.py info --extended
...
audio_effects {
  aec_available: true
  agc_available: true
  ns_available: true
}

effects_operation {
  audio_source: VOICE_COMMUNICATION
  audio_device_info {
    address: "bottom"
    channel_count: 2
    channel_count: 16
    channel_index_mask: 65535
    channel_mask: 12
    encoding: 2
    encoding_str: "pcm_16bit"
    encoding: 4
    encoding_str: "pcm_float"
    id: 9
    product_name: "frodo"
    sample_rate: 32000
    type: 15
    type_str: "builtin mic"
    hash_code: 40
    is_sink: false
    is_source: true
  }
  created {
    audio_effects {
      aec_available: true
      agc_available: true
      ns_available: true
      aec_allocated: true
      agc_allocated: true
      ns_allocated: true
      aec_enabled: false
      agc_enabled: false
      ns_enabled: false
    }
  }
  enabled {
    audio_effects {
      aec_available: true
      agc_available: true
      ns_available: true
      aec_allocated: true
      agc_allocated: true
      ns_allocated: true
      aec_enabled: true
      agc_enabled: true
      ns_enabled: true
    }
  }
  disabled {
    audio_effects {
      aec_available: true
      agc_available: true
      ns_available: true
      aec_allocated: true
      agc_allocated: true
      ns_allocated: true
      aec_enabled: false
      agc_enabled: false
      ns_enabled: false
    }
  }
}
```

The extended option will start a recording and try to modify the effects. The samplerate option
works here too.

# 4. Operation: Mic Recording

Run the info command, and get the list of `audio_device_info` items under
`audio_device_info_array`. Select 1 or more mics. E.g. in our case, we will
capture from both `audio_device_info.id` 8 and 22.

```
$ ./scripts/micapp.py record --inputids 8,22  -t 5
...
$ ls capture*
capture_48kHz_.name.builtin_mic.8.wav
capture_48kHz_USB-Audio_-_HD_Web_Camera.usb_device.22.wav
```

You can also choose the default mic for a given `AudioSource`.

```
$ ./scripts/micapp.py -dd record --audiosource VOICE_CALL -t 2
...
```

Samplerate can be set.

```
$ ./scripts/micapp.py -dd record --audiosource VOICE_CALL -t 2 -r 16000
...
```


Adding `--sound XXX` will start playback while recording.

```
$ ./scripts/micapp.py record --sound chirp
...
```


# 5. Operation: Mic Level Comparison

Run the info command, and get the list of `audio_device_info` items under
`audio_device_info_array`. Select 1 or more mics. E.g. in our case, we will
capture from both `audio_device_info.id` 8 and 22.

```
$ ./scripts/micapp.py record --inputids 8,22  -t 5
...
$ ls capture*
capture_48kHz_.name.builtin_mic.8.wav
capture_48kHz_USB-Audio_-_HD_Web_Camera.usb_device.22.wav
```

Now compare the audio levels of both recordings:

```
$ ./scripts/audiocmp.py capture_48kHz_*
,filename,rms,peak,crest,bias
0,capture_48kHz_Smart_TV_Pro.builtin_mic.8.wav,-55.7,-30.09,25.61,-129.59
1,capture_48kHz_USB-Audio_-_HD_Web_Camera.usb_device.22.wav,-3.05,0.0,3.05,-100.0
```


# 6. Operation: Playout

`micapp` provides a playout mechanism that provides an audio source.

Start the playout. Note that you can select the exact audio file between
a human voice ("`voice`"), pink noise ("`noise`"), and a chirp
("`chirp`").

```
$ ./scripts/micapp.py play --sound voice
```

Stop the playout.

```
$ ./scripts/micapp.py play --stop
```


# 7. Requirements

Python packages:
* [soundfile](https://pypi.org/project/SoundFile/)
