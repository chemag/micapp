#!/usr/bin/env python3

import os
import subprocess
import argparse
import time
APPNAME_MAIN = 'com.facebook.micapp'
DUT_FILE_PATH = '/storage/emulated/0/Android/data/com.facebook.micapp/files/'
debug = False


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
        process = subprocess.Popen(cmd, shell=True,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
    except Exception:
        ret = False
        print('Failed to run command: ' + cmd)

    return ret, stdout.decode(), stderr.decode()


def wait_for_exit(serial, debug=0):
    adb_cmd = f'adb {serial} shell pidof {APPNAME_MAIN}'
    pid = -1
    current = 1
    while (current != -1):
        if pid == -1:
            ret, stdout, stderr = run_cmd(adb_cmd, False)
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


def pull_info(serial, name):
    adb_cmd = f'adb -s {serial} shell am force-stop {APPNAME_MAIN}'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    # clean out old files
    adb_cmd = f'adb -s {serial} shell rm {DUT_FILE_PATH}*.txt'
    adb_cmd = f'adb -s {serial} shell  am start -e nogui 1 -n {APPNAME_MAIN}/.MainActivity'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    wait_for_exit(serial)

    adb_cmd = f'adb -s {serial} shell ls {DUT_FILE_PATH}*.txt'
    ret, stdout, stderr = run_cmd(adb_cmd, debug)
    output_files = [stdout]

    if len(output_files) == 0:
        exit(0)

    for file in output_files:
        if file == '':
            print('No file found')
            continue
        # pull the output file
        base_file_name = os.path.basename(file).strip()
        filename = f'{os. path. splitext(base_file_name)[0]}_{name}.txt'
        adb_cmd = f'adb -s {serial} pull {file.strip()} {filename}'
        run_cmd(adb_cmd, debug)
        with open(filename, "r") as fl:
            print(f'{fl.read()}')

        print('\n__________________\n')
        print(f'Data also available in {filename}')


def main():
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('-s', '--serial', default=None)
    parser.add_argument('-d', '--debug', action='store_true')

    options = parser.parse_args()

    global debug
    debug = options.debug
    # get model and serial number
    model, serial = get_device_info(options.serial, False)
    if type(model) is dict:
        if 'model' in model:
            model = model.get('model')
        else:
            model = list(model.values())[0]
    pull_info(options.serial, model)


if __name__ == '__main__':
    main()
