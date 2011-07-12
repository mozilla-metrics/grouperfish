#/usr/bin/env python

""" Wrapper script for SVD + K Means.
    Performs Rank r (Default 250)  approximation of matrix. This is then
    clustered by KMeans. The output clusters are then visualized as follows: Generate    concept vectors for each cluster, generate both feature clouds and feature clusters.
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
from sparsesvd import sparsesvd


def gen_args():
    parser = argparse.ArgumentParser(description='KMeans Clusterer\
                                     With Dimensionality Recuction')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('-opinion', metavar = 'input',type = file,\
                        help='Tab Separated input opinions file')
    group.add_argument('-corpus', metavar = 'corpus', type = file,\
                       help='Pickled corpus')
    group.add_argument('-i', '--index', action = 'store', nargs = 4,\
                       metavar=("index", "featuredict",\
                                "docids"), dest='indexstuff', help = 'Index +\
                       featuredict + docids + ndocs_actualcontent.\
                       stopwords,mindfpercent,maxdfpercent,usebigrams can be\
                       ignored when this option is set',type = file)
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
                                     = 'k', help = 'Number of clusters to generate')
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
    # Dimensionality Reduction Options
    parser.add_argument('-r', metavar = 'r', action = 'store', default = 250,\
                        type = int, dest = 'r', help = 'Dimensionality red\
                        to perform. Minimum value which is default is 250.\
                        Suppose number of features is less than 250, we take\
                        half of the features.')
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
    if args.classical:
        normalize = True
    else:
        normalize = False
    if args.opinion:
        corpus = corpusutil.create(args.opinion)
        logger.debug("Number of documents in corpus: %d ", len(corpus))
        if args.stopwords:
            stopwords = args.stopwords.read().split()
            datacreator = corpusutil.GenerateVectors(corpus = corpus, mindfpercent\
                                                 = args.mindfpercent,\
                                                 maxdfpercent =\
                                                 args.maxdfpercent,\
                                                 minfrequency =\
                                                 args.minfrequency,\
                                                 verbose = args.verbose,\
                                                 usebigrams = args.usebigrams,\
                                                 normalize = normalize,\
                                                 tf = args.tf,\
                                                 stopwords = stopwords)
        else:
            datacreator = corpusutil.GenerateVectors(corpus = corpus, mindfpercent\
                                                 = args.mindfpercent,\
                                                 maxdfpercent =\
                                                 args.maxdfpercent,\
                                                 minfrequency =\
                                                 args.minfrequency,\
                                                 verbose = args.verbose,\
                                                 usebigrams = args.usebigrams,\
                                                 normalize = normalize,\
                                                 tf = args.tf,\
                                                 stopwords = None)
        result = datacreator.create()
        docids = result['docids']
        featuredict = result['featuredict']
    elif args.corpus:
        corpus = cPickle.load(args.corpus)
        logger.debug("Number of documents in corpus: %d ", len(corpus))
        if args.stopwords:
            stopwords = args.stopwords.read().split()
            datacreator = corpusutil.GenerateVectors(corpus = corpus, mindfpercent\
                                                 = args.mindfpercent,\
                                                 maxdfpercent =\
                                                 args.maxdfpercent,\
                                                 minfrequency =\
                                                 args.minfrequency,\
                                                 verbose = args.verbose,\
                                                 usebigrams = args.usebigrams,\
                                                 normalize = normalize,\
                                                 tf = args.tf,\
                                                 stopwords = stopwords)
        else:
            datacreator = corpusutil.GenerateVectors(corpus = corpus, mindfpercent\
                                                 = args.mindfpercent,\
                                                 maxdfpercent =\
                                                 args.maxdfpercent,\
                                                 minfrequency =\
                                                 args.minfrequency,\
                                                 verbose = args.verbose,\
                                                 usebigrams = args.usebigrams,\
                                                 normalize = normalize,\
                                                 tf = args.tf,\
                                                 stopwords = None)
        result = datacreator.create()
        docids = result['docids']
        featuredict = result['featuredict']
    else:
        index = cPickle.load(args.indexstuff[0])
        featuredict = cPickle.load(args.indexstuff[1])
        docids = cPickle.load(args.indexstuff[2])
        datacreator = corpusutil.GenerateVectors(index = index, featuredict =\
                                             featuredict, docids = docids,\
                                                ndocs_content = ndocs_content,\
                                                normalize = normalize,\
                                                tf = args.tf)
        result = datacreator.create()
    data = result['data']
    p = data.shape[0]
    n = data.shape[1]
    logger.debug(" Vectors are of dimensions: (%d,%d)",\
                 p, n)
    if args.saveint:
        cPickle.dump(docids,open("tfidfvectors_key_"+sessionid+'.pck','w'))
        spio.mmwrite(open("tfidfvectors_"+sessionid+".mtx",'w')\
                     ,data,comment="CSC Matrix",field = 'real')
    #DEFAULT_RANK chosen because it works well in practice. 
    DEFAULT_RANK = 250
    r = args.r
    maxr = min(p,n)
    logger.debug(" Data can have rank not greate than : %d", maxr)
    if maxr >= DEFAULT_RANK:
        if DEFAULT_RANK > r or r > maxr:
            r = DEFAULT_RANK
    else:
        r = int(maxr/2)
    logger.debug(" Going to generate rank %d approximation", r)
    ut,s,vt = sparsesvd(data,r)
    red_data = ssp.csc_matrix(np.dot(ut.T,np.dot(np.diag(s),vt)))
    logger.debug(" Generated rank %d approximation", r)
    if normalize:
        logger.debug(" Normalizing columns of reduced rank matrix...")
        invnorms = np.zeros(n)
        normsii = np.arange(0,n,1)
        normsjj = np.arange(0,n,1)
        for col in range(n):
            invnorms[col] = math.sqrt((red_data[:,col].T*red_data[:,col]).todense())
            if invnorms[col] is not 0:
                invnorms[col] = 1/invnorms[col]
        diag = ssp.coo_matrix((invnorms,(normsii,normsjj)),shape = (n,n)).tocsc()
        red_data = red_data*diag
    logger.debug(" Doing KMeans on reduced rank matrix...")
    kmeans = corpusutil.KMeans(data = red_data,k = args.k,n = args.n, delta =\
                               args.delta,randomcentroids =\
                               args.randomcentroids, verbose =
                               args.verbose,classical = args.classical)
    result = kmeans.run()
    clusters = result['clusters']
    centroids = result['centroids']
    centroiddict = result['centroiddict']
    if args.saveint:
        cPickle.dump(clusters,open("redrank_clusters_"+sessionid+'.pck','w'))
        spio.mmwrite(open("redrank_centroids_"+sessionid+'.mtx','w'),centroids,\
                     comment="CSC Matrix", field = 'real')
    logger.info(" %d Clusters Generated ",len(clusters))
    result = corpusutil.getcentroids(data,clusters)
    originalmat_centroids = result['centroids']
    originalmat_centroiddict = result['centroiddict']
    if args.saveint:
        spio.mmwrite(open("originalmat_centroids_"+sessionid+'.mtx','w'),\
                     originalmat_centroids,comment="CSC Matrix", field = 'real')
    vis_output = corpusutil.genconceptclouds(centroids = centroids,\
                                             centroiddict = centroiddict,\
                                             featuredict = featuredict,\
                                             corpus = corpus,\
                                             clusters = clusters,\
                                             docids = docids,\
                                             sessionid = sessionid)
    svdkmeansvis = open("svdkmeans-concept_clouds_"+str(sessionid)+'.html','w')
    svdkmeansvis.write(vis_output)
    svdkmeansvis.close()
    vis_output = corpusutil.genfeatureclouds(originalmat_centroids.todense(),\
                                             originalmat_centroiddict,\
                                             featuredict,sessionid)
    svdkmeansvis = open("svdkmeans-feature_clusters_"+str(sessionid)+'.html','w')
    svdkmeansvis.write(vis_output)
    svdkmeansvis.close()

if __name__ == "__main__":
    sys.exit(main())

