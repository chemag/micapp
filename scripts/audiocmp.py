#!/usr/bin/env python3

import argparse
import math
import os
import pandas as pd
import numpy as np
import soundfile as sf
import sys


default_values = {
    'debug': 0,
    'mode': 'safe',
    'output': 'audio_compare',
}


def dBToFloat(val):
    """ "
    Calculates a float value ranging from -1.0 to 1.0
    Where 1.0 is 0dB
    """
    return 10 ** (val / 20.0)


def floatToDB(val):
    """
    Calculates the dB values from a floating point representation
    ranging between -1.0 to 1.0 where 1.0 is 0 dB
    """
    if val <= 0:
        return -100.0
    else:
        return 20.0 * math.log10(val)


def amplify_and_write_file(inputfile, outputfile, gain_dB):
    blocksize = inputfile.channels * inputfile.samplerate
    block_counter = 0
    inputfile.seek(0)
    factor = dBToFloat(gain_dB)

    while inputfile.tell() < inputfile.frames:
        data = inputfile.read(blocksize)
        for channel in range(0, inputfile.channels):
            if inputfile.channels == 1:
                data_ = data
            else:
                data_ = data[:, channel]
            outputfile.write(data_ * factor)
        block_counter += 1


def audio_levels(audiofile, start=0, end=-1):
    """
    Calculates rms and max peak level in dB
    Input: soundfile, start frame, end frame
    Output: rms, peak, crest, bias, floor
    where 
        * peak is the highest nominal value
        * rms is the total average of the squared values
        * crest is the ratios between peak and rms
        * bias is potential dc bias (low frequency residual)
        * floor is the lowest rms value in a non overlapping 250ms block
    """

    blocksize = audiofile.channels * int(audiofile.samplerate/4)
    peak_level = [0] * audiofile.channels
    rms = [0] * audiofile.channels
    peak = [0] * audiofile.channels
    total_level = [0] * audiofile.channels
    crest = [0] * audiofile.channels
    bias = [0] * audiofile.channels
    floor = [0] * audiofile.channels
    block_counter = 0
    audiofile.seek(start)

    while audiofile.tell() < audiofile.frames:
        tmp = [0] * audiofile.channels
        data = audiofile.read(blocksize)
        for channel in range(0, audiofile.channels):
            if audiofile.channels == 1:
                data_ = data
            else:
                data_ = data[:, channel]
            total_level[channel] += np.sum(data_)
            rms[channel] += np.mean(np.square(data_))
            peak[channel] = max(abs(data_))            
            if peak[channel] > peak_level[channel]:
                peak_level[channel] = peak[channel]
            tmp[channel] = floatToDB(np.mean(np.square(data_)))            
            if tmp[channel] < floor[channel]:
                floor[channel] = round(tmp[channel], 2)
        block_counter += 1

    for channel in range(0, audiofile.channels):
        rms[channel] = np.sqrt(rms[channel] / block_counter)
        crest[channel] = round(
            floatToDB(peak_level[channel] / rms[channel]), 2)
        # sign is not important now
        bias[channel] = round(
            floatToDB(abs(
                total_level[channel] /
                (block_counter * blocksize))
            ),
            2,
        )
        rms[channel] = round(floatToDB(rms[channel]), 2)
        peak_level[channel] = round(floatToDB(peak_level[channel]), 2)

    return rms, peak_level, crest, bias, floor


MODE_CHOICES = {
    'safe': 'Check the highest crest factor and use that to adjust rms',
    'rms': 'rms to -24dB per file, clipping allowed',
    'peak': 'peak to -1 per file',
    'rms_common': 'use a rms value with no clipping in any file, max -1dB',
    'peak_common': 'use a peak value with no clipping in any file, max -1dB',
}


def adjust_row(row, mode, target, workdir):
    if mode == 'rms':
        diff = target - row.rms
    elif mode == 'peak':
        diff = target - row.peak
    elif mode == 'safe':
        diff = target - row.rms
    elif mode == 'rms_common':
        diff = target - row.rms
    elif mode == 'peak_common':
        diff = target - row.peak
    adjust(row.af, mode, diff, workdir)
    row.af.close()


RMS_TARGET = -24
PEAK_TARGET = -1


def get_target(mode, data):
    # calculate the target
    if mode == 'rms':
        return RMS_TARGET
    elif mode == 'peak':
        return PEAK_TARGET
    elif mode == 'safe':
        # take the one with largest crest factor and use
        # the crest to calculate a rms value (add 1 dB for safety)
        return -data['crest'].max() + 1
    elif mode == 'rms_common':
        # take the one with highest peak and use
        # that to calculate a peak adjusted value
        return data['rms'].max()
    elif mode == 'peak_common':
        # take the one with highest peak and use
        # that to calculate a peak adjusted value
        return data['peak'].max()


def adjust(audiofile, suffix, adjustment_db, workdir):
    sign = ''
    if adjustment_db > 0:
        sign = '+'
    new_name = (f'{workdir}/{os.path.splitext(audiofile.name)[0]}_'
                f'{sign}{round(adjustment_db,2)}_{suffix}.wav')
    output = sf.SoundFile(new_name, 'w', format='WAV', samplerate=48000,
                          channels=1, subtype='PCM_16', endian='FILE')

    amplify_and_write_file(audiofile, output, adjustment_db)
    output.close()


def align(files, mode, workdir):
    file_props = []
    if not os.path.exists(workdir):
        os.mkdir(workdir)
    status_report_name = f'{workdir}/info.txt'
    with open(status_report_name, 'w') as report:

        report.write(f'alignment_mode: {mode}\n\n')
        for fname in files:
            af = sf.SoundFile(fname, 'r')
            rms, peak_level, crest, bias, floor = audio_levels(af)
            file_props.append([af, fname, rms[0], peak_level[0], crest[0],
                               bias[0], floor[0]])

            report.write(f'{fname}\n')
            report.write('\n   rms  : {0:4.1f} dB'.format(rms[0]))
            report.write('\n   peak : {0:4.1f} dB'.format(peak_level[0]))
            report.write('\n   crest: {0:4.1f} dB'.format(crest[0]))
            report.write('\n   bias : {0:4.1f} dB'.format(bias[0]))
            report.write('\n   floor: {0:4.1f} dB'.format(floor[0]))
            report.write('\n____\n')

    labels = ['af', 'filename', 'rms', 'peak', 'crest', 'bias', 'floor']
    data = pd.DataFrame.from_records(
        file_props, columns=labels, coerce_float=True)
    # do not print the SoundFile string
    labels = labels[1:]
    print(f'{data.to_csv(columns=labels)}')
    # get the target for adjustment
    target = get_target(mode, data)
    # run the adjustment
    data.apply(adjust_row, args=(mode, target, workdir), axis=1)


def get_options(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        '-d', '--debug', action='count',
        dest='debug', default=default_values['debug'],
        help='Increase verbosity (use multiple times for more)',)
    parser.add_argument(
        '--quiet', action='store_const',
        dest='debug', const=-1,
        help='Zero verbosity',)
    parser.add_argument(
        '--mode', type=str,
        default=default_values['mode'],
        choices=MODE_CHOICES.keys(),
        metavar='%s' % (' | '.join('{}: {}'.format(k, v) for k, v in
                                   MODE_CHOICES.items())),
        help='function arg',)
    parser.add_argument(
        '-o', '--output', type=str,
        default=default_values['output'])
    parser.add_argument(
        'files', nargs='+', help='file(s) to analyze (pcm mono)')

    options = parser.parse_args(argv[1:])

    if len(argv) == 1:
        parser.print_help()
        sys.exit(0)

    return options


def main(argv):
    options = get_options(argv)
    align(options.files, options.mode, options.output)


if __name__ == '__main__':
    main(sys.argv)
