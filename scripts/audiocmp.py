#!/usr/bin/env python3
import argparse
from math import log10
import os
import pandas as pd
import numpy as np
import soundfile as sf
from scipy import signal
import sys


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
        return 20.0 * log10(val)

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
    """

    blocksize = audiofile.channels * audiofile.samplerate * 10
    peak_level = [0] * audiofile.channels
    rms = [0] * audiofile.channels
    peak = [0] * audiofile.channels
    total_level = [0] * audiofile.channels
    crest = [0] * audiofile.channels
    bias = [0] * audiofile.channels
    block_counter = 0
    audiofile.seek(start)

    while audiofile.tell() < audiofile.frames:
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
        block_counter += 1

    for channel in range(0, audiofile.channels):
        rms[channel] = np.sqrt(rms[channel] / block_counter)
        crest[channel] = round(floatToDB(peak_level[channel] / rms[channel]), 2)
        bias[channel] = round(
            floatToDB(
                total_level[channel] / (block_counter * 10 * audiofile.samplerate)
            ),
            2,
        )
        rms[channel] = round(floatToDB(rms[channel]), 2)
        peak_level[channel] = round(floatToDB(peak_level[channel]), 2)

    return rms, peak_level, crest, bias

MODE_CHOICES = {
    'safe': 'Check the highest crest factor and use that to adjust rms',
    'rms': 'rms to -24dB per file, clipping allowed',
    'peak': 'peak to -1 per file',
    'rms_common': 'use a rms value with no clipping in any file, max -1dB',
    'peak_common': 'use a peak value with no clipping in any file, max -1dB',
}


def adjust(audiofile, suffix, adjustment_db, workdir):
    sign = ''
    if adjustment_db > 0:
        sign = '+'
    new_name = f'{workdir}/{os.path.splitext(audiofile.name)[0]}_{sign}{round(adjustment_db,2)}_{suffix}.wav'
    output = sf.SoundFile(new_name, 'w', format='WAV', samplerate=48000,
         channels=1, subtype='PCM_16', endian='FILE')

    amplify_and_write_file(audiofile, output, adjustment_db)
    output.close()


def align(files, mode, workdir):
    file_props = []
    if not os.path.exists(workdir):
        os.mkdir(workdir)
    status_report_name = f'{workdir}/info.txt'
    report = open(status_report_name, 'w')
    report.write(f'Align files using \"{mode}\"" method\n')
    report.write('Input files and props\n____\n')
    for fname in files:
        af =  sf.SoundFile(fname, 'r')
        rms, peak_level, crest, bias = audio_levels(af)
        file_props.append([af, rms[0], peak_level[0], crest[0], bias[0]])
        report.write(f'{fname}\n')
        report.write('\n   rms  : {0:4.1f} dB'.format(rms[0]))
        report.write('\n   peak : {0:4.1f} dB'.format(peak_level[0]))
        report.write('\n   crest: {0:4.1f} dB'.format(crest[0]))
        report.write('\n   bias : {0:4.1f} dB'.format(bias[0]))
        report.write('\n____\n')
        
    labels = ['file', 'rms', 'peak', 'crest', 'bias']
    data = pd.DataFrame.from_records(
        file_props, columns=labels, coerce_float=True)

    print(f'{data}')
    peak_target = -1
    rms_target = -24
    if mode == 'rms':
        print('rms')
        for row in file_props:
            rms = row[1]
            diff = rms_target - rms
            adjust(row[0], mode, diff, workdir)
            
    elif mode == 'peak':        
        for row in file_props:
            peak = row[2]
            diff = peak_target - peak
            adjust(row[0], mode, diff, workdir)            

    elif mode == 'safe':
        print('safe')
        # take the one with largest crest factor and use
        # the crest to calculate a rms value (add 1 dB for safety)
        rms_target = -data['crest'].max() + 1

        for row in file_props:
            rms = row[1]
            diff = rms_target - rms
            adjust(row[0], mode, diff, workdir)

    elif mode == 'rms_common':
        print('peak_common')
        # take the one with highest peak and use
        # that to calculate a peak adjusted value
        rms_target = data['rms'].max()
        for row in file_props:
            rms = row[1]
            diff = rms_target - rms            
            adjust(row[0], mode, diff, workdir)

    elif mode == 'peak_common':
        print('peak_common')
        # take the one with highest peak and use
        # that to calculate a peak adjusted value
        peak_target = data['peak'].max()
        for row in file_props:
            peak = row[2]
            diff = peak_target - peak            
            adjust(row[0], mode, diff, workdir)

    for row in file_props:
        row[0].close()


def main(argv):
    parser = argparse.ArgumentParser(description='')
    parser.add_argument('files', nargs='+', help='file(s) to analyze (pcm mono)')    
    parser.add_argument(
        '--mode', type=str,
        default='safe',
        choices=MODE_CHOICES.keys(),
        metavar='%s' % (' | '.join("{}: {}".format(k, v) for k, v in
                                   MODE_CHOICES.items())),
        help='function arg',)
    parser.add_argument(
        '-o', '--output', type=str,
        default =  "audio_compare")

    options = parser.parse_args(argv[1:])
    
    if len(argv) == 1:
        parser.print_help()
        sys.exit(0)

    align(options.files, options.mode, options.output)
if __name__ == '__main__':
    main(sys.argv)
