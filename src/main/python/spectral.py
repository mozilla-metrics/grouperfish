#/usr/bin/env python

""" Wrapper script for Spectral KMeans
    Performs KMeans on document matrix. For spherical kmeans, the matrix should
    be normalized. Refer corpusutil documentation for more details. 
"""

import math
import logging
import sys
import time

import argparse
import corpusutil
import cPickle
import numpy as np
import scipy.sparse as ssp
import scipy.io as spio


def gen_args():
    parser = argparse.ArgumentParser(description='KMeans Clusterer')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('-opinion', metavar = 'input',type = file,\
                        help='Tab Separated input opinions file')
    group.add_argument('-corpus', metavar = 'corpus', type = file,\
                       help='Pickled corpus')
    parser.add_argument('-mindfpercent', metavar = 'mindfpercent', action = 'store', type =\
                        int, default = 0.05, dest = 'mindfpercent', help = 'min\
                        required documents where term occurs (Default = 0.05)')
    parser.add_argument('-maxdfpercent', metavar = 'maxdfpercent', action =\
                        'store', type = float, default = 99, dest =\
                        'maxdfpercent', help = 'Maximum number of documents in\
                        which term can occure 0-100 (Default = 99)')
    parser.add_argument('-mintf', metavar = 'mintf', action = 'store', type = int,\
                        default = 1, dest = 'minfrequency', help = 'Minimum\
                        number of occurrences of term in corpus (Default = 1)')
    parser.add_argument('-stopwords', action = 'store', type = file, \
                        dest = 'stopwords', help = 'Space separated\
                        sequence of stop words ')
    parser.add_argument('-sessionid',action = 'store', dest = 'sessionid',\
                        default = str(int(time.time()*100000)), help =\
                        'Generate unique session id. Default = time\
                        dependent')
    parser.add_argument('-v',action = 'store_true', default = False, dest =\
                        'verbose', help = 'Generate verbose output. Default =\
                        No')
    parser.add_argument('-saveint',action = 'store_true', default = False, dest =\
                        'saveint', help = 'Save intermediary output. Default =\
                        No')
    parser.add_argument('-usebigrams',action = 'store_true', default = False, dest =\
                        'usebigrams', help = 'Use bigrams. Default =\
                        No')
    # K Means Options
    parser.add_argument('-classical',action = 'store_true', default = False, dest =\
                        'classical', help = 'Select type of\
                        KMeans to use Spherical or Euclidean. Default: Spherical')
    parser.add_argument('-k', metavar = 'k', action = 'store', type = int, dest\
                        = 'k',default = None, help = 'Number of clusters to\
                        generate. No input leads to finding k.')
    parser.add_argument('-n', metavar = 'n', action = 'store', type = int, dest=\
                                     'n', help = 'Max number of iterations')
    parser.add_argument('-delta', metavar = 'delta', action = 'store', default\
                        = 0.005, type = float, dest = 'delta', help = 'Quit\
                        see if difference in objective function is less than\
                        delta. Default = 0.005')
    parser.add_argument('-rc',action = 'store_true', default = False, dest =\
                        'randomcentroids', help = 'Generate centroids by\
                        partitioning matrix deterministically or randomize\
                        selection of columns. Default = false')
    parser.add_argument('-tf' ,action = 'store_true', default = False, dest =\
                        'tf', help = 'Select type of\
                        Vectors (tf/tfidf)  Default: tfidf')
    return parser

def main():
    parser = gen_args()
    args = parser.parse_args()
    sessionid = args.sessionid
    logger =  logging.getLogger(__name__)
    logger.addHandler(logging.StreamHandler())
    if args.verbose:
        logger.setLevel(logging.DEBUG)
    if args.stopwords:
        stopwords = args.stopwords.read().split()
    else:
        stopwords = None
    if args.opinion:
        corpus = corpusutil.create(args.opinion)
    else:
        corpus = cPickle.load(args.corpus)
    spectralcc = corpusutil.SpectralCoClusterer(corpus = corpus, mindfpercent\
                                                = args.mindfpercent,\
                                                maxdfpercent =\
                                                args.maxdfpercent,\
                                                minfrequency =\
                                                args.minfrequency,\
                                                verbose = args.verbose,\
                                                usebigrams = args.usebigrams,\
                                                tf = args.tf,\
                                                stopwords = stopwords,\
                                                k = args.k,\
                                                n = args.n, delta = args.delta,\
                                                randomcentroids =\
                                                args.randomcentroids,\
                                                sessionid = sessionid,\
                                                classical = args.classical)

    result = spectralcc.run()
    fclouds = result['fclouds']
    docclouds = result['docclouds']
    A = result['A']
    An = result['An']
    Z = result['Z']
    spio.mmwrite(open('A_'+sessionid+'.mtx','w'), A, comment = 'CSC\
                     Matrix', field = 'real')
    if args.saveint:
        spio.mmwrite(open('An_'+sessionid+'.mtx','w'), An, comment = 'CSC\
                     Matrix', field = 'real')
        spio.mmwrite(open('Z_'+sessionid+'.mtx', 'w'), Z, comment = 'CSC\
                     Matrix', field = 'real')
    kmeansvis = open("spectral-concept_clouds_"+str(sessionid)+'.html','w')
    kmeansvis.write(docclouds)
    kmeansvis.close()
    kmeansvis = open("spectral-features_clusters_"+str(sessionid)+'.txt','w')
    kmeansvis.write(fclouds)
    kmeansvis.close()

if __name__ == "__main__":
    sys.exit(main())

