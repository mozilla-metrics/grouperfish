#/usr/bin/env python

import math
import logging
import string
import sys
import time

import argparse
import corpusutil
import cPickle
import numpy as np
import scipy.sparse as ssp
import scipy.io as spio


def gen_args():
    parser = argparse.ArgumentParser(description='Visualize Graph Clustering\
                                     Output. Note that exact same parameters\
                                    fed during adjacency list generation\
                                     must be used')
    parser.add_argument('-index', metavar = 'index', type = file,\
                        help = 'Inverted index used to generate adjacency list')
    parser.add_argument('-featuredict', metavar = 'featuredict', type = file,\
                        help = 'A dict with feature id as token and feature as\
                        value.')
    parser.add_argument('-docids', metavar = 'docids', type = file,\
                        help = 'A list of all the doc ids')
    parser.add_argument('-nodepartitions', metavar = 'nodepartitions',type =\
                        file, help = 'A list of node partitions from Louvain\
                        code')
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
    return parser

def main():
    parser = gen_args()
    args = parser.parse_args()
    sessionid = args.sessionid
    logger =  logging.getLogger(__name__)
    logger.addHandler(logging.StreamHandler())
    if args.verbose:
        logger.setLevel(logging.DEBUG)
    index = cPickle.load(args.index)
    featuredict = cPickle.load(args.featuredict)
    docids = cPickle.load(args.docids)
    nodeclusterinfo = dict([[string.atoi(k) for k in x.split()] for x in\
                            args.nodepartitions])
    datacreator = corpusutil.GenerateVectors(index = index, featuredict =\
                                             featuredict, docids = docids)
    result = datacreator.create()
    data = result['data']
    clusters = corpusutil.getclusters(nodeclusterinfo)
    centroids = corpusutil.getcentroids(data,clusters)
    if args.saveint:
        cPickle.dump(clusters,open("graph_data_clusters_"+sessionid+'.pck','w'))
        spio.mmwrite(open("graph_data_centroids_"+sessionid+'.mtx','w'),centroids,comment="CSC\
                         Matrix", field = 'real')
    logger.info(" %d Clusters Generated ",len(clusters))
    vis_output = corpusutil.generate_featureclouds(centroids.todense(),featuredict,sessionid)
    graphvis = open("graph-output_"+sessionid+'.html','w')
    graphvis.write(vis_output)
    graphvis.close()

if __name__ == "__main__":
    sys.exit(main())

