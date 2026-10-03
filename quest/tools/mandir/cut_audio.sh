#!/bin/sh
# Cuts the fetched recordings into the clips the sanctum plays.
# Mono 16-bit WAV, so the Spatial SDK can place each sound at its object.
set -eu
here="$(cd "$(dirname "$0")" && pwd)"
src="$here/sources/audio"
out="$here/../../app/src/main/assets/mandir/audio"
mkdir -p "$out"

clip() {
  name="$1"; input="$2"; shift 2
  ffmpeg -v error -y -i "$src/$input" "$@" -ac 1 -ar 44100 -c:a pcm_s16le "$out/$name.wav"
}

# One pull of the hanging ghanta: the first strike, left to ring out.
clip ghanta ghanta.flac -ss 0.45 -t 4.2 \
  -af "afade=t=in:d=0.01,afade=t=out:st=1.4:d=2.8:curve=exp,loudnorm=I=-14:TP=-1.5"
# The full peal, for the end of the aarti.
clip ghanta_peal ghanta.flac -ss 0.45 \
  -af "afade=t=in:d=0.01,loudnorm=I=-16:TP=-1.5"
clip aarti_bell aarti_bell.flac -af "loudnorm=I=-18:TP=-2"
clip shankh shankh.ogg -af "afade=t=out:st=5.6:d=0.9,loudnorm=I=-15:TP=-1.5"
clip aarti_chant aarti_chant.ogg -t 48 -af "afade=t=in:d=1.5,afade=t=out:st=43:d=5,loudnorm=I=-20:TP=-2"
clip coins coins.ogg -ss 0.6 -t 1.4 -af "afade=t=out:st=1.0:d=0.4,loudnorm=I=-18:TP=-2"
# A peanut shell, slowed and lowered until it has the weight of a coconut.
clip coconut crack.ogg -ss 2.62 -t 0.6 \
  -af "asetrate=48000*0.62,aresample=48000,lowpass=f=2600,afade=t=out:st=0.4:d=0.3,loudnorm=I=-14:TP=-1"
clip crackle crackle.ogg -ss 0 -t 9.5 \
  -af "highpass=f=180,afade=t=in:d=0.4,afade=t=out:st=9.0:d=0.5,loudnorm=I=-30:TP=-8"
clip ignite crackle.ogg -ss 24.6 -t 1.1 \
  -af "highpass=f=300,afade=t=in:d=0.05,afade=t=out:st=0.5:d=0.6,loudnorm=I=-22:TP=-4"
ls -la "$out"
