#/usr/bin/env python

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
import scipy.linalg as spla
from sparsesvd import sparsesvd

class SpectralClusterer:
    ''' Implementation of Normalized Spectral Clustering by Ng, Jordan and
    Weiss. 
    There are two options: 1) Given a set of points X, you can compute the
    similarity matrix using the radial basis kernel and the cosine similarity or
    L2 norm. 2) Use a readily available similarity matrix for co-clustering. 
    '''
    def __init__(self,**kwargs):
        ''' 
        Args:
            X: A matrix where all vectors are data points. 
            A: A similarity matrix.
            usecosine: True: Uses cosine similarity along with Radial Basis
                Function. Default: False.
            sigma: Radial basis function parameter
            k - Number of clusters to generate
            n - Number of iterations
            randomcentroids - Generate Centroids by partitioning matrix
                determininstically or randomize selection of columns.
            delta = Convergence Parameter
            classical - Boolean that determines whether to use classical kmeans or
                not. Default = TRUE
            verbose - Enables debug setting
        '''
        self.logger =  logging.getLogger(__name__)
        self.logger.addHandler(logging.StreamHandler())
        self.verbose = kwargs.get('verbose')
        if self.verbose:
            self.logger.setLevel(logging.DEBUG)
        self.X = kwargs.get('X')
        if self.X is None:
            self.A = kwargs.get('A')
        else: 
            self.usecosine = kwargs.get('usecosine')
            self.sigma = kwargs.get('sigma')
            self.logger.debug('Computing Similarity Matrix')
            self.getA()
        self.k = kwargs.get('k')
        if self.k is None:
            self.MIN_K = kwargs.get('MIN_K')
            self.MAX_K = kwargs.get('MAX_K')
            self.SAMPLE_SIZE_PERCENT = kwargs.get('SAMPLE_SIZE_PERCENT')
            self.logger.debug('k not fed in. Figuring out k between range %d\
                              and %d and using sample size %d'\
                              ,self.MIN_K,self.MAX_K,self.SAMPLE_SIZE_PERCENT)
        self.n = kwargs.get('n')
        self.randomcentroids = kwargs.get('randomcentroids')
        self.delta = kwargs.get('delta')
        self.classical = kwargs.get('classical')

    def getA(self):
        self.A = ssp.lil_matrix((self.X.shape[1],self.X.shape[1]),dtype ='f')
        ''' Compute Similarity Matrix. '''
        for ii in xrange(self.X.shape[1]):
            for jj in xrange(self.X.shape[1]):
               val =  self.getKval(ii,jj)
               self.A[ii,jj] = val
               self.A[jj,ii] = val
        self.A = self.A.tocsc()

    def getD(self):
        ''' Compute Diagonal Matrix of Similarity matrix A. '''
        nrows = self.A.shape[0]
        d = np.zeros(nrows)
        II = np.arange(0,nrows,1)
        JJ = II
        for ii in range(nrows):
            d[ii] = self.A[ii,:].sum()
        self.D = ssp.coo_matrix((d,(II,JJ)),shape = (nrows,nrows)).tocsc()

    def getinnerKval(self,x,y):
        ''' Uses usecosine to compute either inner kernel. '''
        if self.usecosine is True:
            return (x.T*y)[0,0]
        else:
            return (-((x-y).T*(x-y))[0,0])/(2*self.sigma*self.sigma)

    def getinvsqrtD(self):
        ''' Compute D^(-0.5) '''
        nrows = self.A.shape[0]
        d = np.zeros(nrows)
        II = np.arange(0,nrows,1)
        JJ = II
        for ii in range(nrows):
            temp = math.sqrt(self.A[ii,:].sum())
            d[ii] = 1/temp
        self.modD = ssp.coo_matrix((d,(II,JJ)),shape = (nrows,nrows)).tocsc()

    def getKval(self,ii,jj):
        ''' Compute composition of kernels:
            usecosine: 
                True: K(x,y) = exp(x.T*y)
                False: K(x,y) = exp(-||x-y||*||x-y||/(2*sigma*sigma))
        '''
        x = self.X[:,ii]
        y = self.X[:,jj]
        return math.exp(self.getinnerKval(x,y))

    def getL(self):
        ''' Compute unnormalized graph Laplacian L. '''
        self.L = self.D - self.A

    def getLsym(self):
        ''' Compute normalized Laplacian. '''
        self.getinvsqrtD()
        self.Lsym = self.modD * self.L * self.modD

    def getT(self):
        ''' Compute row normalized version of U. '''
        self.T = ssp.lil_matrix((self.U.shape[0],self.U.shape[1]),dtype ='f')
        for ii in range(self.U.shape[0]):
            curr_row = self.U[ii,:].todense()
            self.T[ii,:] = (1/spla.norm(curr_row))*curr_row
        self.T = self.T.tocsc()

    def getU(self):
        ''' Compute top k eigen vectors. '''
        U,s,V = spla.svd(self.Lsym.todense(),full_matrices = 1)
        #result = sparsesvd(self.Lsym, self.k)
        if self.k is None:
            self.U = ssp.csc_matrix(U[:,0:self.MAX_K])
        else:
            self.U = ssp.csc_matrix(U[:,0:self.k])

    def run(self):
        ''' Main Method that drives SpectralClusterer.
        Refer Ulrike's Tutorial on Spectral Clustering to understand notation.
        Return:
            Clusters: A dict with keys as cluster IDs and values as a list of
            vector IDs.
        '''
        self.logger.debug('Generating Degree Matrix')
        self.getD()
        self.logger.debug('Generating Unnormalized Laplacian Matrix')
        self.getL()
        self.logger.debug('Generating Normalized Laplacian Matrix')
        self.getLsym()
        self.logger.debug('Generating Eigenvectors  Matrix')
        self.getU()
        self.logger.debug('Generating Normalized Eigenvectors Matrix')
        self.getT()
        self.logger.debug('Doing KMeans')
        data = (self.T.T).tocsc()
        if self.k is None:
            self.k = corpusutil.find_no_clusters(X = data, samplesize =\
                                      self.SAMPLE_SIZE_PERCENT,mink =\
                                      self.MIN_K, maxk = self.MAX_K,\
                                      classical = self.classical,\
                                      verbose = self.verbose)
            self.logger.debug('k found to be %d',self.k)
        kmeans = corpusutil.KMeans(data = data, k = self.k, n = self.n,\
                                   delta = self.delta, randomcentroids =\
                                   self.randomcentroids, verbose =\
                                   self.verbose, classical = self.classical)
        result = kmeans.run()
        return result['clusters']


def gen_args():
    parser = argparse.ArgumentParser(description='Spectral Clusterer')
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
    parser.add_argument('-mindfpercent', metavar = 'mindfpercent', action =\
                        'store', type = int, default = 0.05, dest =\
                        'mindfpercent', help = 'min required documents\
                        where term occurs (Default = 0.05)')
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
    parser.add_argument('-usebigrams',action = 'store_true', default = False,\
                        dest = 'usebigrams', help = 'Use bigrams. Default =\
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
    # Spectral Arguments:
    parser.add_argument('-usecosine', action = 'store_true', default = False,\
                        dest = 'usecosine', help = 'Use cosine kernel.Default =\
                        No.')
    parser.add_argument('-sigma', metavar = 'sigma', action = 'store', default\
                        = 1, type = float, dest = 'sigma', help = 'Radial basis\
                        function parameter: sigma')
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
    if args.classical:
        normalize = True
    else:
        normalize = False
    if args.opinion or args.corpus:
        if args.opinion:
            corpus = corpusutil.create(args.opinion)
        else:
            corpus = cPickle.load(args.corpus)
        logger.debug("Number of documents in corpus: %d ", len(corpus))
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
    X = result['data']
    if args.k is None:
        MIN_K = 2
        MAX_K = 50
        SAMPLE_SIZE_PERCENT = 100
        spectral = SpectralClusterer(X = X, usecosine = args.usecosine, sigma =\
                                     args.sigma, n = args.n, delta =\
                                     args.delta, MIN_K = MIN_K, MAX_K = MAX_K,\
                                     SAMPLE_SIZE_PERCENT = SAMPLE_SIZE_PERCENT,\
                                     randomcentroids = args.randomcentroids,\
                                     classical = args.classical, verbose = \
                                     args.verbose)
    else:
        spectral = SpectralClusterer(X = X, usecosine = args.usecosine, sigma = \
                                     args.sigma, k = args.k, n = args.n, delta = \
                                     args.delta, randomcentroids = \
                                     args.randomcentroids, classical =\
                                     args.classical, verbose =\
                                     args.verbose)
    clusters = spectral.run()
    result = corpusutil.getcentroids(X, clusters, normalize)
    centroids = result['centroids']
    centroiddict = result['centroiddict']
    logger.info(" %d Clusters Generated ",len(clusters))
    vis_output = corpusutil.genconceptclouds(centroids = centroids,\
                                             centroiddict = centroiddict,\
                                             featuredict = featuredict,\
                                             corpus = corpus,\
                                             clusters = clusters,\
                                             docids = docids,\
                                             sessionid = sessionid)
    kmeansvis = open("Spectral-concept_clouds_"+str(sessionid)+'.html','w')
    kmeansvis.write(vis_output)
    kmeansvis.close()
    vis_output = corpusutil.genfeatureclouds(centroids.todense(),centroiddict,\
                                                     featuredict,sessionid)
    kmeansvis = open("Spectral-feature_clusters_"+str(sessionid)+'.html','w')
    kmeansvis.write(vis_output)
    kmeansvis.close()




if __name__ == "__main__":
    sys.exit(main())







