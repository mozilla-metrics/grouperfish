#!/bin/bash

DATA=$1
K=$2
ALPHA=0.1
RHO=0.1
D=`wc -l $DATA | cut -f 1 -d " "`
echo $D
B=$3
FEATURE_INDEX=$4
POWER_T=0.5
INITIAL_T=1.0
BATCH_SIZE=256

rm /tmp/vw.cache
./vowpal_wabbit/vw "$DATA" --lda "$K" --lda_alpha "$ALPHA" --lda_rho "$RHO" --lda_D "$D" --minibatch "$BATCH_SIZE" --power_t "$POWER_T" --initial_t "$INITIAL_T" -b "$B" --cache_file /tmp/vw.cache --passes 10 -p "lda-$K-predictions.dat" --readable_model "lda-$K-topics.dat"
python vowpalwabbit.py -t "lda-$K-topics.dat" -f "$FEATURE_INDEX" > "lda-$K-topics.txt"
