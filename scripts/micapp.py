#!/usr/bin/env python3

import argparse
import os
import subprocess
import sys
import time
import soundfile as sf
import re

from _version import __version__


APPNAME_MAIN = 'com.facebook.micapp'
DUT_FILE_PATH = '/storage/emulated/0/Android/data/com.facebook.micapp/files/'


FUNC_CHOICES = {
    'help': 'show help options',
    'info': 'provide audio uplink',
    'record': 'record an audioclip',
}

AUDIO_SOURCE_CHOICES = {
    'CAMCORDER': (5, 'microphone audio source tuned for video recording audio '
                  'source'),
    'DEFAULT': (0, 'default audio source'),
    'MIC': (1, 'microphone audio source'),
    'REMOTE_SUBMIX': (8, 'audio source for a submix of audio streams to be '
                      'presented remotely'),
    'UNPROCESSED': (9, 'microphone audio source tuned for unprocessed (raw) '
                    'sound if available, behaves like DEFAULT otherwise'),
    'VOICE_CALL': (4, 'voice call uplink + downlink audio source'),
    'VOICE_COMMUNICATION': (7, 'microphone audio source tuned for voice '
                            'communications such as VoIP'),
    'VOICE_DOWNLINK': (3, 'voice call downlink (Rx) audio source'),
    'VOICE_PERFORMANCE': (10, 'source for capturing audio meant to be '
                          'processed in real time and played back for '
                          'live performance'),
    'VOICE_RECOGNITION': (6, 'microphone audio source tuned for voice '
                          'recognition'),
    'VOICE_UPLINK': (2, 'voice call uplink (Tx) audio source'),
}

default_values = {
    'debug': 0,
    'func': 'help',
    'audiosource': None,
}


# returns info (device model and serial number) about the device where the
# test will be run
def get_device_info(serial_inp, debug=0):
    # list all available devices
    adb_cmd = 'adb devices -l'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    assert ret, 'error: failed to get adb devices'

    # parse list
    device_info = {}
    for line in stdout.split('\n'):
        if line == 'List of devices attached' or line == '':
            continue
        serial = line.split()[0]
        item_dict = {}
        for item in line.split()[1:]:
            # ':' used to separate key/values
            if ':' in item:
                key, val = item.split(':', 1)
                item_dict[key] = val
        # ensure the 'model' field exists
        if 'model' not in item_dict:
            item_dict['model'] = 'generic'
        device_info[serial] = item_dict
    assert len(device_info) > 0, 'error: no devices connected'
    if debug > 2:
        print('available devices: %r' % device_info)

    # select output device
    serial, model = None, None
    if serial_inp is None:
        # if user did not select a serial_inp, make sure there is only one
        # device available
        assert len(device_info) == 1, (
            'error: need to choose a device %r' % list(device_info.keys()))
        serial = list(device_info.keys())[0]
        model = device_info[serial]

    else:
        # if user forced a serial number, make sure it is available
        assert serial_inp in device_info, (
            'error: device %s not available' % serial_inp)
        serial = serial_inp
        model = device_info[serial]

    if debug > 0:
        print('selecting device: serial: %s model: %s' % (serial, model))

    return model, serial


def run_cmd(cmd, debug=0):
    ret = True
    try:
        if debug > 0:
            print(cmd, sep=' ')
        process = subprocess.Popen(cmd, shell=True,  # noqa: P204
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
    except Exception:
        ret = False
        print('Failed to run command: ' + cmd)

    return ret, stdout.decode(), stderr.decode()


def wait_for_exit(serial, debug=0):
    adb_cmd = f'adb -s {serial} shell pidof {APPNAME_MAIN}'
    pid = -1
    current = 1
    while (current != -1):
        if pid == -1:
            ret, stdout, stderr = run_cmd(adb_cmd, debug)
            pid = -1
            if len(stdout) > 0:
                pid = int(stdout)
        time.sleep(1)
        ret, stdout, stderr = run_cmd(adb_cmd, debug)
        current = -2
        if len(stdout) > 0:
            try:
                current = int(stdout)
            except Exception:
                print(f'wait for exit caught exception: {stdout}')
                continue
        else:
            current = -1


def pull_info(serial, name, extended, debug=0):
    adb_cmd = f'adb -s {serial} shell am force-stop {APPNAME_MAIN}'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    # clean out old files
    adb_cmd = f'adb -s {serial} shell rm {DUT_FILE_PATH}*.txt'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    extra = ''
    if extended:
        extra = '-e fxverify 1 '

    adb_cmd = (f'adb -s {serial} shell am start -e nogui 1 {extra}'
               f'-n {APPNAME_MAIN}/.MainActivity')
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    wait_for_exit(serial, debug)

    adb_cmd = f'adb -s {serial} shell ls {DUT_FILE_PATH}*.txt'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    output_files = [stdout]

    if len(output_files) == 0:
        exit(0)

    for file in output_files:
        if file == '':
            continue
        # pull the output file
        base_file_name = os.path.basename(file).strip()
        filename = f'{os. path. splitext(base_file_name)[0]}_{name}.txt'
        adb_cmd = f'adb -s {serial} pull {file.strip()} {filename}'
        run_cmd(adb_cmd, debug)
        with open(filename, 'r') as fl:
            print(f'{fl.read()}')
        if debug > 0:
            print(f'file output: {filename}')
        print('\n__________________\n')
        print(f'Data also available in {filename}')


def record(serial, name, audiosource=None, ids=None, timesec=10.0, debug=0):
    adb_cmd = f'adb -s {serial} shell am force-stop {APPNAME_MAIN}'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    # clean out old files
    adb_cmd = f'adb -s {serial} shell rm {DUT_FILE_PATH}*.raw'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    adb_cmd = (f'adb -s {serial} shell  am start -e rec 1 '
               f'{build_args(audiosource, ids, timesec)} '
               f'-n {APPNAME_MAIN}/.MainActivity')
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    time.sleep(1)
    wait_for_exit(serial)
    time.sleep(2)

    if debug:
        adb_cmd = f'adb -s {serial} shell ls -l {DUT_FILE_PATH}*.raw'
        ret, stdout, stderr = run_cmd(adb_cmd, debug)
        print(f'Files:\n{stdout}')

    adb_cmd = f'adb -s {serial} shell ls {DUT_FILE_PATH}*.raw'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    output_files = re.split('[ \n]', stdout.strip())

    if len(output_files) == 0 or len(stdout) == 0:
        print('No files created, most likely configuration is not supported '
              'on the device')
        print('check source and input settings and/or look at logcat output')
        exit(0)

    audiofiles = []
    for file in output_files:
        if file == '':
            continue
        # pull the output file
        base_file_name = os.path.basename(file).strip()
        adb_cmd = f'adb -s {serial} pull {file.strip()} {base_file_name}'
        run_cmd(adb_cmd, debug)
        # convert to wav, currently only 48k
        audio = sf.SoundFile(base_file_name, 'r', format='RAW',
                             samplerate=48000, channels=1, subtype='PCM_16',
                             endian='FILE')
        pcmname = f'{os.path.splitext(base_file_name)[0]}.wav'
        print(f'Convert {base_file_name} to wav: {pcmname}')
        wav = sf.SoundFile(pcmname, 'w', format='WAV', samplerate=48000,
                           channels=1, subtype='PCM_16', endian='FILE')
        wav.write(audio.read())
        wav.close()
        audio.close()
        audiofiles.append(pcmname)
        os.remove(base_file_name)

    for name in audiofiles:
        print(f'{name}')


def build_args(audiosource, inputids, timesec):
    ret = ''
    if audiosource is not None:
        audiosource_int = AUDIO_SOURCE_CHOICES[audiosource][0]
        ret = f'{ret} -e audiosource {audiosource_int} '
    if inputids is not None:
        ret = f'{ret} -e inputid {inputids} '
    if timesec is not None:
        ret = f'{ret} -e timesec {timesec} '
    return ret


def get_options(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        '-v', '--version', action='store_true',
        dest='version', default=False,
        help='Print version',)
    parser.add_argument(
        '-d', '--debug', action='count',
        dest='debug', default=default_values['debug'],
        help='Increase verbosity (use multiple times for more)',)
    parser.add_argument(
        '--quiet', action='store_const',
        dest='debug', const=-1,
        help='Zero verbosity',)
    parser.add_argument(
        '-s', '--serial', help='Android device serial number')
    parser.add_argument(
        'func', type=str, nargs='?',
        default=default_values['func'],
        choices=FUNC_CHOICES.keys(),
        metavar='%s' % (' | '.join('{}: {}'.format(k, v) for k, v in
                                   FUNC_CHOICES.items())),
        help='function arg',)
    parser.add_argument(
        '--audiosource', type=str,
        default=default_values['audiosource'],
        choices=AUDIO_SOURCE_CHOICES.keys(),
        metavar='%s' % (' | '.join('{}: {}'.format(k, v[1]) for k, v in
                                   AUDIO_SOURCE_CHOICES.items())),
        help='audiosource arg',)
    parser.add_argument(
        '--inputids', default=None)
    parser.add_argument(
        '-t', '--timesec', type=float, default=10.0)
    parser.add_argument(
        '--extended', action='store_true',
        help='Extended version of the function',)
    options = parser.parse_args(argv[1:])

    # implement help
    if options.func == 'help':
        parser.print_help()
        sys.exit(0)

    if options.serial is None and 'ANDROID_SERIAL' in os.environ:
        # read serial number from ANDROID_SERIAL env variable
        options.serial = os.environ['ANDROID_SERIAL']

    return options


def main(argv):
    options = get_options(argv)
    if options.version:
        print('version: %s' % __version__)
        sys.exit(0)

    # get model and serial number
    model, serial = get_device_info(options.serial, False)
    if type(model) is dict:
        if 'model' in model:
            model = model.get('model')
        else:
            model = list(model.values())[0]

    if options.func == 'info':
        pull_info(options.serial, model, options.extended, options.debug)
    if options.func == 'record':
        record(options.serial, model, options.audiosource, options.inputids,
               options.timesec, options.debug)


if __name__ == '__main__':
    main(sys.argv)
