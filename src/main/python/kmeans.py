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

class KMeans:

    """ Batch KMeans . Does Spherical KMeans  only
    Args:
        data - CSC Matrix with rows as features and columns as points
        k - Number of clusters to generate
        n - Number of iterations
        randomcentroids - Generate Centroids by partitioning matrix 
        determininstically or randomize selection of columns. 
        delta = Convergence Parameter
        verbose - Enables debug setting

    Methods:
        chunks - Chunks up list
        converged - checks convergence
        getdeterministicpartitions - gets deterministic partitions for
        clustering
        getrandomizedpartitions - gets randomized partitions for clustering
        getcentroids
        run
    """

    def __init__(self, data, k, n, delta, randomcentroids, verbose):
        self.data = data
        self.k = k
        self.n = n
        self.delta = delta
        self.randomcentroids = randomcentroids
        self.logger = logging.getLogger(__name__)
        if verbose:
            self.logger.setLevel(logging.DEBUG)
            self.logger.debug("Starting KMeans debugging...")

    def chunks(self,l,n):
        """ Chunks up list l into divisions of n"
        Args:
            l : A list
            n : The splits required
        Returns:
            A list of lists
        """
        return [l[i:i+n] for i in range(0, len(l), n)]

    def converged(self,clusters,newclusters):
        """ Check convergence.
         We check if difference of sum of  norm of sum of all the vectors for each cluster
         computed during prev iteration and curre iteration is less than delta
        """
        currnorms = np.zeros(self.k)
        newnorms = np.zeros(self.k)
        for centroid,v_ids in clusters.iteritems():
            currsum =  np.mat(np.zeros((self.data.shape[0],1)))
            for v in v_ids:
                currsum = currsum + self.data[:,v].todense()
            currnorms[centroid] = math.sqrt(currsum.T*currsum)
        for centroid,v_ids in newclusters.iteritems():
            newsum =  np.mat(np.zeros((self.data.shape[0],1)))
            for v in v_ids:
                newsum = newsum + self.data[:,v].todense()
            newnorms[centroid] = math.sqrt(newsum.T*newsum)
        if math.fabs(currnorms.sum() - newnorms.sum())< self.delta:
            return True
        else:
            return False

    def getdeterministicpartitions(self):
        """ Divide up the vectors among the k partitions """
        nvectors = self.data.shape[1]
        numsplits = int(math.floor(nvectors/self.k))
        v_ids = range(0,nvectors,1)
        v_idslist = self.chunks(v_ids,numsplits)
        ii = 0
        self.clusters = {}
        while ii < self.k:
            self.clusters[ii] = v_idslist[ii]
            ii = ii + 1
        self.centroids = corpusutil.getcentroids(self.data,self.clusters)
        return {'centroids':self.centroids,'clusters':self.clusters}

    def getrandomizedpartitions(self):
        """ Divide up the vectors among the k partitions """
        nvectors = self.data.shape[1]
        numsplits = int(math.floor(nvectors/self.k))
        v_ids = range(0,nvectors,1)
        np.random.shuffle(v_ids)
        v_idslist = self.chunks(v_ids,numsplits)
        ii = 0
        self.clusters = {}
        while ii < self.k:
            self.clusters[ii] = v_idslist[ii]
            ii = ii + 1
        self.centroids = corpusutil.getcentroids(self.data,self.clusters)
        return {'centroids':self.centroids,'clusters':self.clusters}

    def run(self):
        """ Runs spherical kmeans, returns clusters and centroids.
        Returns:
            clusters. A dict mapping cluster IDs to the corresponding vector IDs
            centroids. The clusters themeselves.
        References:
           1. @article{dhillon2001concept,
            title={Concept decompositions for large sparse text data using
            clustering},
            author={Dhillon, I.S. and Modha, D.S.},
            journal={Machine learning},
            volume={42},
            number={1},
            pages={143--175},
            year={2001},
            publisher={Springer}
                }
        """
        assert (self.data.shape[1] > self.k), "Number of clusters requested greater than\
                number of vectors"
        self.logger.debug("Data is of dimensions:\
                     (%d,%d)",self.data.shape[0],self.data.shape[1])
        self.logger.debug("Generating %d clusters ...",self.k)
        if self.randomcentroids:
            self.logger.debug("Generating centroids by randomized partioning")
            result = self.getrandomizedpartitions()
        else:
            self.logger.debug("Generating centroids by arbitrary partitioning")
            result = self.getdeterministicpartitions()
        centroids = result['centroids']
        clusters = result['clusters']
        ii = 0
        new_clusters = {}
        while ii < self.n:
            self.logger.debug("Iteration %d",ii)
            newclusters = {}
            jj = 0
            while jj < self.data.shape[1]:
                kk = 0
                dcentroids = [0]*self.k
                while kk < self.k:
                    dcentroids[kk] =\
                    (self.data[:,jj].T*self.centroids[:,kk]).todense()
                    kk = kk + 1
                dclosest = min(dcentroids)
                closestcluster = dcentroids.index(dclosest)
                if closestcluster in newclusters:
                    newclusters[closestcluster].append(jj)
                else:
                    newclusters[closestcluster] = [jj]
                jj = jj+1
            self.logger.debug("Going to get new centroids...")
            newcentroids = corpusutil.getcentroids(self.data,newclusters)
            self.logger.debug("Going to check convergence...")
            if self.converged(self.clusters,newclusters):
                break
            else:
                self.centroids = newcentroids
                self.clusters =  newclusters
            ii = ii + 1
            return {'clusters':self.clusters,'centroids':self.centroids}

def gen_args():
    parser = argparse.ArgumentParser(description='Spherical KMeans Clusterer')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('-opinion', metavar = 'input',type = file,\
                        help='Tab Separated input opinions file')
    group.add_argument('-corpus', metavar = 'corpus', type = file,\
                       help='Pickled corpus')
    group.add_argument('-i', '--index', action = 'store', nargs = 3,\
                       metavar=("index", "featuredict",\
                                "docids"), dest='indexstuff', help = 'Index +\
                       featuredict + docids.\
                       stopwords/mindfpercent/maxdfpercent/usebigrams can be\
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
    return parser

def main():
    parser = gen_args()
    args = parser.parse_args()
    sessionid = args.sessionid
    logger =  logging.getLogger(__name__)
    logger.addHandler(logging.StreamHandler())
    if args.verbose:
        logger.setLevel(logging.DEBUG)
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
                                                 stopwords = None)
        result = datacreator.create()
        docids = result['docids']
        featuredict = result['featuredict']
    else:
        index = cPickle.load(args.indexstuff[0])
        featuredict = cPickle.load(args.indexstuff[1])
        docids = cPickle.load(args.indexstuff[2])
        datacreator = corpusutil.GenerateVectors(index = index, featuredict =\
                                             featuredict, docids = docids)
        result = datacreator.create()
    data = result['data']
    if args.saveint:
        cPickle.dump(docids,open("data_key_"+sessionid+'.pck','w'))
        spio.mmwrite(open("data_"+sessionid+".mtx",'w')\
                     ,data,comment="CSC Matrix",field = 'real')
    kmeans = KMeans(data,args.k,args.n,args.delta,args.randomcentroids,args.verbose)
    result = kmeans.run()
    clusters = result['clusters']
    centroids = result['centroids']
    if args.saveint:
        cPickle.dump(clusters,open("data_clusters_"+sessionid+'.pck','w'))
        spio.mmwrite(open("data_centroids_"+sessionid+'.mtx','w'),centroids,comment="CSC\
                         Matrix", field = 'real')
    logger.info(" %d Clusters Generated ",len(clusters))
    if args.verbose:
        vis_output = corpusutil.generate_featureclouds(centroids.todense(),featuredict,sessionid)
        kmeansvis = open("kmeans-output_"+str(sessionid)+'.html','w')
        kmeansvis.write(vis_output)
        kmeansvis.close()

if __name__ == "__main__":
    sys.exit(main())

