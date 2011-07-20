# Filename: corpusutil.py

import logging
import math
import operator
import re
import string

import css
import enchant
import numpy as np
import scipy.sparse as ssp
import scipy.io as spio
import scipy.linalg as spla
from nltk import bigrams
from nltk.metrics import edit_distance
from nltk import PorterStemmer
from nltk.corpus import wordnet
from nltk.util import trigrams as nltk_trigrams
from nltk.tokenize import word_tokenize as nltk_word_tokenize
from nltk.probability import FreqDist
from nltk.corpus.util import LazyCorpusLoader
from nltk.corpus.reader.api import CorpusReader
from nltk.corpus.reader.util import StreamBackedCorpusView, concat
from sparsesvd import sparsesvd


def create(reader):
    """ Splits corpus into fields.

    Refer  wiki.mozilla.org/firefox/input/data for more on the fields.

    Args:
        reader: A file object opened in read mode

    Returns:
        A dict with keys as doc IDs and values as a list of the other fields.

    """


    fields = enum('ID', 'TIME' ,'TYPE', 'PRODUCT','VERSION', 'PLATFORM',\
              'LOCALE', 'MANUFACTURER', 'DEVICE', 'URL','DESCRIPTION')
    numfields = 11
    opinions = reader.read()
    corpus = {}
    currfield = []
    currdocument = [0]* (numfields -1)
    numtabsencountered = 0
    escaped = False
    for c in opinions:
        if escaped == False:
            if c == '\\':
                escaped = True
                continue
            elif c == '\t':
                if numtabsencountered  == fields.ID:
                    docid = long(string.join(currfield,''))
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.TIME:
                    currdocument[fields.TIME-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.TYPE:
                    currdocument[fields.TYPE-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.PRODUCT:
                    currdocument[fields.PRODUCT-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.VERSION:
                    currdocument[fields.VERSION-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.PLATFORM:
                    currdocument[fields.PLATFORM-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.LOCALE:
                    currdocument[fields.LOCALE-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.MANUFACTURER:
                    currdocument[fields.MANUFACTURER-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.DEVICE:
                    currdocument[fields.DEVICE-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                elif numtabsencountered == fields.URL:
                    currdocument[fields.URL-1] = string.join(currfield,'')
                    currfield = []
                    numtabsencountered = numtabsencountered + 1
                    continue
                else:
                    numtabsencountered = numtabsencountered + 1
            elif c == '\n':
                currdocument[fields.DESCRIPTION-1] = string.join(currfield,'')
                currfield = []
                assert (docid not in corpus), "generate_corpus code is wrong"
                corpus[docid] = currdocument
                currdocument = [0]* (numfields -1)
                numtabsencountered = 0
                continue
        currfield.append(c)
        escaped = False
    return corpus


def enum(*sequential, **named):
    enums = dict(zip(sequential, range(len(sequential))), **named)
    return type('enum', (), enums)

def extract_topfeatures(centroids,centroiddict,featuredict,nfeaturesreq):
    """ Extracts top nfeaturesreq  from centroids
    Args:
        centroids: A matrix with each column containing one observation.
        centroidict: Dict mapping from centroid id to centroid column ids
        featuredict: A map from matrix indices to features
        nfeaturesreq: Number of features to extract
    Returns:
        topfeatures: A dict of dicts. The key of dict is the centroid id.
        The value is a dict with key as feature and value as matrix
        value.
    """
    invcentroiddict = dict((v,k) for k,v in centroiddict.iteritems())
    lindex = centroids.shape[0] - nfeaturesreq
    rindex = centroids.shape[0] + 1
    sortedargs  =  np.matrix.argsort(centroids,axis = 0)[lindex:rindex,:]
    ii = 0
    topfeatures = {}
    while ii < sortedargs.shape[1]:
        colfeatures = {}
        jj = 0
        while jj < sortedargs.shape[0]:
            feature = featuredict[sortedargs[jj,ii]]
            colfeatures[feature] = centroids[sortedargs[jj,ii],ii]
            jj = jj + 1
        topfeatures[invcentroiddict[ii]] = colfeatures
        ii = ii + 1
    return topfeatures

def find_no_clusters(**kwargs):
    ''' Find the number of clusters.
    Args:
        X : The p X n matrix where every column is an observation.
        samplesize: Percentage of n from which to subsample.
    Returns:
        k
    References: 
        1. @article{tibshirani2005cluster,
        title={Cluster validation by prediction strength},
        author={Tibshirani, R. and Walther, G.},
        journal={Journal of Computational and Graphical Statistics},
        volume={14},
        number={3},
        pages={511--528},
        year={2005},
        publisher={ASA}
        }
        2. http://blog.echen.me/2011/03/19/counting-clusters/

    '''
    X = kwargs.get('X')
    samplesize = kwargs.get('samplesize')
    mink = kwargs.get('mink')
    maxk = kwargs.get('maxk')
    n = kwargs.get('n')
    delta = kwargs.get('delta')
    randomcentroids = kwargs.get('randomcentroids')
    verbose = kwargs.get('verbose')
    classical = kwargs.get('classical')
    result = gensubsamples(X,samplesize)
    Xtr = result.get('Xtr')
    Xte = result.get('Xte')
    THRESHOLD = 0.8
    if mink >= Xtr.shape[1]:
        mink = int (0.1*Xtr.shape[1])
    if maxk >= Xtr.shape[1]:
        maxk = int (0.9*Xtr.shape[1])
    logger = logging.getLogger(__name__)
    logger.addHandler(logging.StreamHandler())
    if verbose:
        logger.setLevel(logging.DEBUG)
    ps_array = {}
    for k in range(mink,maxk+1,1):
        kmeans = KMeans(data = Xtr, k = k, n = n, delta = delta,\
                        randomcentroids = randomcentroids, verbose =\
                        verbose, classical = classical)
        result = kmeans.run()
        centroids_tr = result['centroids']
        clusters_tr = result['clusters']
        logger.debug('%d Iteration: Number of training clusters\
                    generated: %d',k,len(clusters_tr))
        kmeans =  KMeans(data = Xte, k = k, n = n, delta = delta,\
                        randomcentroids = randomcentroids, verbose =\
                        verbose, classical = classical)
        result = kmeans.run()
        centroids_te = result['centroids']
        clusters_te = result['clusters']
        logger.info('%d Iteration: Number of test clusters\
                    generated: %d',k,len(clusters_tr))
        if len(clusters_tr) != len(clusters_te):
            logger.warning('Warning: Bailing out early because number of test\
                           and training clusters are not equal.')
            return (k-1)
        predictions_te = getpredictions(centroids_tr,Xte,classical)
        predictions_tr = getpredictions(centroids_te,Xtr,classical)
        ps_te = getpredictionstrength(clusters_te,predictions_te)
        logger.debug('Test Prediction Strength is %f',ps_te)
        ps_tr = getpredictionstrength(clusters_tr,predictions_tr)
        logger.debug('Training Prediction Strength is %f',ps_tr)
        ps = 0.5*(ps_te+ps_tr)
        logger.debug('Prediction Strength is %f',ps)
        if ps > THRESHOLD:
            return k
        else:
            ps_array[k] = ps
    k = max(ps_array.iteritems(), key=operator.itemgetter(1))[0]
    logger.debug('No k above threshold found, k corresponding to max is %d',k)
    return k



def generate_index(**kwargs):
    """ Generates inverted index from corpus.

    Corpus generated only from description field, documents are cleaned up using
    the  cleandocument method.

    Args:
        corpus: A dict that has been run through create method.
        descriptionfield: int identifier of the field to create index using.
        maxdfpercent: Maximum percentage of the docs in which term can occur
        mindfpercent: Minimum  percentage of the docs in which term can occur
        minfrequency: Minimum total occurrences of term in corpus
        usebigrams: Should I use bigrams or not. Default = false
        stopwords: A list of stop words to use.

    Returns:
        index: A dict with term as key and a value as a dict. The value dict
        has key as doc ID where the token is present, value as a pair with the
        first element as number of occurrences of the token in the doc and
        second element as size of doc post clean up.
        featuredict: A dict with tokenid as key and feature as value. keys start
        from 0 onwards.
        docids: A list of all the doc ids.The list is ordered such that the
        docs with actual content are present to the front.
        ndocs_content: The number of docs with content
    """
    index = {}
    ndocs = 0
    descriptionfield = 9
    docids = []
    corpus = kwargs.get('corpus')
    maxdfpercent = kwargs.get('maxdfpercent')
    mindfpercent = kwargs.get('mindfpercent')
    minfrequency = kwargs.get('minfrequency')
    usebigrams = kwargs.get('usebigrams')
    stopwords = kwargs.get('stopwords')
    cleaner = _CleanDoc()
    for currdocid,doc in corpus.iteritems():
        docids.append(currdocid)
        doc = cleaner.clean(doc[descriptionfield],usebigrams,stopwords)
        ndocs = ndocs + 1
        for term in doc:
            if term in index:
                currworddocslist = index[term]
                if currdocid in currworddocslist:
                    temp = currworddocslist[currdocid]
                    temp = (temp[0] + 1,temp[1])
                    currworddocslist[currdocid] = temp
                else:
                    currworddocslist[currdocid] = (1,len(doc))
            else:
                index[term] = {currdocid:(1,len(doc))}
    featuredict = {}
    featureid = 0
    for term,docs in index.items():
        noccurs = sum(zip(*list(docs.values()))[0])
        if not is_termfeature(len(docs), noccurs, ndocs, maxdfpercent,\
                              mindfpercent, minfrequency):
            del index[term]
            continue
        featuredict[featureid] = term
        featureid = featureid + 1
    docs_content = set()
    for term,docs in index.iteritems():
        for doc,info in docs.iteritems():
            docs_content.add(doc)
    docs_empty = [doc for doc in docids if doc not in docs_content]
    docids = list(docs_content)
    docids.extend(docs_empty)
    return\
{'index':index,'featuredict':featuredict,'docids':docids,'ndocs_content':len(docs_content)}

def gensubsamples(X,samplesize):
    ''' Subsample from X and paritition that sample into test and training.
    Args:
        X: A csc matrix with columns as observations
        samplesize: Percentage samples size required.
    Returns:
        Xtr: Training csc matrix
        Xte: Test csc matrix
    '''
    nvectors = X.shape[1]
    nreq = int(math.floor((nvectors*samplesize)/100))
    if nreq % 2 is not 0:
        nreq = nreq - 1
    samplesize = (int) (nreq/2)
    nfeatures = X.shape[0]
    Xtr = np.mat(np.zeros((nfeatures,samplesize)))
    Xte = np.mat(np.zeros((nfeatures,samplesize)))
    v_ids = range(0,nvectors,1)
    np.random.shuffle(v_ids)
    v_req = v_ids[0:nreq]
    ii = 0
    for v in v_req:
        if ii < samplesize: 
            Xtr[:,ii] = X[:,v].todense()
        else:
            Xte[:,ii-samplesize] = X[:,v].todense()
        ii = ii + 1
    return {'Xtr':ssp.csc_matrix(Xtr),'Xte':ssp.csc_matrix(Xte)}

def generate_subset(corpus,**kwargs):
    """ Generates a subset from corpus.
    Args: (arg name is case sensitive)
        corpus: A dict that has been run through create method.
        type:  issue,praise,suggestion,rating
        product:  firefox, mobile
        version:  version identifier
        platform:  mac, linux, android, maemo, winxp, vista etc
        locale: locale identifier such as en-US
        manufacturer: manufacturer identifier ( mobile only)
        device: device identifier (mobile only)
    """
    doctype = kwargs.get('doctype')
    product = kwargs.get('product')
    version = kwargs.get('version')
    platform = kwargs.get('platform')
    locale = kwargs.get('locale')
    manufacturer = kwargs.get('manufacturer')
    device = kwargs.get('device')
    fields = enum('ID', 'TIME' ,'TYPE', 'PRODUCT','VERSION', 'PLATFORM',\
              'LOCALE', 'MANUFACTURER', 'DEVICE', 'URL','DESCRIPTION')
    subset = {}
    select = True
    for docid, doc in corpus.iteritems():
        if doctype is not None:
            if str.lower(doc[fields.TYPE-1]) != str.lower(doctype):
                select = False
        if product is not None:
            if str.lower(doc[fields.PRODUCT-1]) != str.lower(product):
                select = False
        if version is not None:
            if str.lower(doc[fields.VERSION-1]) != str.lower(version):
                select = False
        if locale is not None:
            if str.lower(doc[fields.LOCALE-1]) != str.lower(locale):
                select = False
        if platform is not None:
            if str.lower(doc[fields.PLATFORM-1]) != str.lower(platform):
                select = False
        if manufacturer is not None:
            if str.lower(doc[fields.MANUFACTURER]) !=\
                        str.lower(manufacturer):
                select = False
        if device is not None:
            if str.lower(doc[fields.DEVICE]) != str.lower(device):
                select = False
        if select is True:
            subset[docid] = doc
        select = True
    return subset

def genclustermetadata(clusterid, v_ids, docids, corpus, sessionid):
    """ Generate relevant text docs corresponding to clusterid. """
    title = 'cluster%d_%s.txt'%(clusterid,sessionid)
    docf = open(title,'w')
    contents = 'Cluster %d\n'%(clusterid,)
    clusterdocids = [docids[v] for v in v_ids]
    for  d_id in clusterdocids:
        contents += '\t'.join(corpus[d_id])
        contents += '\n'
    docf.write(contents)
    docf.close()
    return css.undecoratedhyperlink(title,str(clusterid))


def genconceptclouds(**kwargs):

    """ Take top ten features from every cluster and create cloud.
    Cloud is generated and saved to file identified by sessionid

    Args:
        centroids: A  CSC matrix where every column vector is a centroid vector.
        centroiddict: A dict mapping from cluster id to centroid columns
        featuredict: A dict with key : matrix index (int) and value: features (str)
        clusters: A dict with key as cluster id and values as list of
        vector ids that belong to it
        corpus: The corpus.
        docids: List of docids, their indices are vector ids.

    Returns:
        An html object

    """
    centroids = kwargs.get('centroids')
    centroiddict = kwargs.get('centroiddict')
    featuredict = kwargs.get('featuredict')
    sessionid = kwargs.get('sessionid')
    corpus = kwargs.get('corpus')
    clusters = kwargs.get('clusters')
    docids = kwargs.get('docids')
    NUM_FEATURES_REQ = 10
    FONT_URL = \
    'http://fonts.googleapis.com/css?family=Yanone+Kaffeesatz:regular,bold'
    STYLE_URL = 'wordcloud.css'
    divstr = ''
    topfeatures = extract_topfeatures(centroids.todense(),centroiddict,\
                                      featuredict,\
                                      NUM_FEATURES_REQ)
    for clusterid,features in topfeatures.iteritems():
        features = mapfeatures_to_cloudbins(features)
        if corpus is None:
            divstr += css.generate_single_cloud(clusterid,features)
        else:
            clusterinfo = genclustermetadata(clusterid,clusters[clusterid],docids,\
                                             corpus,sessionid)
            divstr += css.generate_single_cloud(clusterinfo, features)
    bodystr = css.generate_body(divstr)
    return css.wrap_into_html(bodystr,sessionid,FONT_URL,STYLE_URL)

def genfeatureclouds(centroids,centroiddict,featuredict,sessionid):
    """  Create feature cluster clouds by generating feature clusters.
    Generate feature cluster clouds by determining for every feature which
    cluster it most belongs to by finding centroid where it has max value. Top
    ten such features for every cluster are extracted and visualized using as an
    HTML page.

    Args:
        centroids: A  dense matrix where every column vector is a centroid vector.
        centroiddict: A dictionary mapping from centroid ids to columns of
        centroids
        featuredict: A dict with key : matrix index (int) and value: features
        (str)
    Returns:
        An html object
    """
    NUM_FEATURES_REQ = 10
    FONT_URL = \
    'http://fonts.googleapis.com/css?family=Yanone+Kaffeesatz:regular,bold'
    STYLE_URL = 'wordcloud.css'
    divstr = ''
    invcentroiddict = dict((v,k) for k,v in centroiddict.iteritems())
    featureclusters = {}
    for index,feature in featuredict.iteritems():
        cvid = centroids[index,:].argmax()
        cid = invcentroiddict[cvid]
        if cid in featureclusters:
            featureclusters[cid].append((feature,centroids[index,cvid]))
        else:
            featureclusters[cid] = [(feature,centroids[index,cvid])]
    for cid,features in featureclusters.iteritems():
        topfeatures = dict(sorted(features,key = lambda feature:\
                               feature[1])[0:NUM_FEATURES_REQ])
        topfeatures = mapfeatures_to_cloudbins(topfeatures)
        divstr += css.generate_single_cloud(cid,topfeatures)
    bodystr = css.generate_body(divstr)
    return css.wrap_into_html(bodystr,sessionid,FONT_URL,STYLE_URL)

def getcentroids(data,clusters, normalize = True):
    """ Uses clusters to generate centroids
    Args:
        data: A csc matrix where columsn are observation
        clusters: A dict with key as cluster id and values as list of
        vector ids that belong to it
        Normalize centroids or not. (Default = True)
    Returns:
        dict containing: centroids, centroiddict
    """
    k = len(clusters)
    newcentroids =  np.mat(np.zeros((data.shape[0],k)))
    invnorms = np.zeros(k)
    normsII = np.arange(0,k,1)
    normsJJ = normsII
    centroiddict = {}
    ii = 0
    for centroid,v_ids in clusters.iteritems():
        for v in v_ids:
            newcentroids[:,ii] = newcentroids[:,ii] +\
                    data[:,v].todense()
        newcentroids[:,ii] = newcentroids[:,ii] *\
                (float(1)/len(v_ids))
        normcentroid = math.sqrt(newcentroids[:,ii].T *\
                                 newcentroids[:,ii])
        if normcentroid is not 0:
            invnorms[ii] =\
                    1/(math.sqrt(newcentroids[:,ii].T *\
                                 newcentroids[:,ii]))
        else:
            invnorms[ii] = 0
        assert(centroid not in centroiddict), 'Logic in getcentroids is wrong'
        centroiddict[centroid] = ii
        ii  = ii + 1
    if normalize is True:
        diag = ssp.coo_matrix((invnorms,(normsII,normsJJ)),shape = (k,k)).tocsc()
        return {'centroids':ssp.csc_matrix(newcentroids)*diag,\
                'centroiddict':centroiddict}
    else:
        return {'centroids':ssp.csc_matrix(newcentroids),\
                'centroiddict':centroiddict}

def getclusters(nodeclusterinfo):
    """ Generate clusters from partition info
    Args:
        nodeclusterinfo: A dict where keys are vector ids and values are the
        clusters they belong to
    Returns:
        clusters: A dict of all the clusters as keys and docids as values
    """
    clusters = {}
    for node,clusterid in nodeclusterinfo.iteritems():
        if clusterid not in clusters:
            clusters[clusterid] = [node]
        else:
            temp = clusters[clusterid]
            temp.append(node)
            clusters[clusterid] = temp
    return clusters

def getpredictions(centroids,X,classical):
    ''' Uses centroids to predict X.
    Args:
        centroids: A csc matrix where every column is a centroid.
        X: A csc matrix where every column is an observation.
        classical: A boolean to determine whether clusters 
    Returns:
        predictions: A dict where every key is an column index in X and value\
                is a set of its neighbors according to centroids.
    References:
        1. @article{tibshirani2005cluster,
        title={Cluster validation by prediction strength},
        author={Tibshirani, R. and Walther, G.},
        journal={Journal of Computational and Graphical Statistics},
        volume={14},
        number={3},
        pages={511--528},
        year={2005},
        publisher={ASA}
        }
        2. http://blog.echen.me/2011/03/19/counting-clusters/


    '''
    clusters = {}
    for ii in range(0,X.shape[1],1):
        distances = {}
        a = X[:,ii]
        for jj in range(0,centroids.shape[1],1):
            b = centroids[:,jj]
            if classical is True:
                distances[jj] = math.sqrt(((a-b).T*(a-b)).todense())
            else:
                distances[jj] = (a.T*b).todense()
        sortedkeys = distances.keys()
        sortedkeys.sort(cmp = lambda a,b: cmp(distances[a],distances[b]))
        MIN = 0
        MAX = len(sortedkeys) - 1
        if classical is True:
            cid = sortedkeys[MIN]
        else:
            cid = sortedkeys[MAX]
        if cid in clusters:
            temp = clusters[cid]
            temp.add(ii)
            clusters[cid] = temp
        else:
            temp = set([ii])
            clusters[cid] = temp
    predictions = {}
    for cid,vectors in clusters.iteritems():
        for v in vectors:
            predictions[v] = vectors.difference(set([v]))
    return predictions



def getpredictionstrength(clusters,predictions):
    ''' Find pairs that occur in the clusters as predicted.
    Args:
        clusters: A dict with keys as cluster ids and values as list of vector\
                ids that belong to it.
        predictions: A dict with keys as vector ids and values as a set of\
                vectors that are in the same cluster as the key.
    Returns:
        predictionstrength

    References:
        1. @article{tibshirani2005cluster,
        title={Cluster validation by prediction strength},
        author={Tibshirani, R. and Walther, G.},
        journal={Journal of Computational and Graphical Statistics},
        volume={14},
        number={3},
        pages={511--528},
        year={2005},
        publisher={ASA}
        }
        2. http://blog.echen.me/2011/03/19/counting-clusters/

    '''
    ps = []
    for cid, vectors in clusters.iteritems():
        count = 0
        nc = len(vectors)
        for v in vectors:
            neighbors = set(vectors)
            neighbors.remove(v)
            predicted = predictions[v]
            count += len(neighbors.intersection(predicted))
        if nc > 1:
            currps = (float(count))/(nc*(nc-1))
        else:
            currps = float(count)
        ps.append(currps)
    return min(ps)



def is_termfeature(ndocs, noccurs, ncorpus, maxdfpercent, mindfpercent,\
                   minfrequency):
    """ Checks using mindfpercent, maxdfpercent (and minfrequency) if term is
    feature.

    Args:
        ndocs: Number of unique occurrences of term in docs
        noccurs: Total occurrences of term in docs
        ncorpus: Size of corpus
        maxdfpercent: Maximum percentage of the docs in which term can occur
        mindfpercent: Minimum  percentage of the docs in which term can occur
        minfrequency: Minimum total occurrences of term in corpus

    Returns:
        Boolean
    """

    if minfrequency is None:
        if ((float(ndocs)/float(ncorpus))*100) > maxdfpercent:
            return False
        elif ((float(ndocs)/float(ncorpus))*100) < mindfpercent:
            return False
        else:
            return True
    else:
        if ((float(ndocs)/float(ncorpus))*100) > maxdfpercent:
            return False
        elif ((float(ndocs)/float(ncorpus))*100) < mindfpercent:
            return False
        elif noccurs < minfrequency:
            return False
        else:
            return True

def mapfeatures_to_cloudbins(features):
    """ Helper function to genconceptclouds.
    Maps each feature value to  one of n bins where n is determined by length of
    features.
    Args:
        features: A dict with key as feature and value as the value to be used
        for feature generation
    Returns:
        out_features: A dict with key as numeric integer
        from 1 onwards that maps the feature to class values and value as
        feature
    """
    sorted_features = sorted(features.iteritems(),\
                             key=operator.itemgetter(1),reverse=True)
    ii = 1
    out_features = {}
    for (feature,value) in sorted_features:
        out_features[ii] = feature
        ii = ii +1
    return out_features


class _CleanDoc:
    '''
     Cleans document applying certain filers.
    '''
    def __init__(self):
        self.languagedet = _LangDetect()
        self.rreplacer = _RepeatReplacer()
        self.sreplacer = _SpellingReplacer()
        self.delchars = ''.join(c for c in map(chr, range(256)) if not c.isalpha())
        self.stemmer = PorterStemmer()
        self.USELESS_DOC_SIZE = 2

    def clean(self,doc, usebigrams = False,stopwords=None):
        '''
        Cleans document applying certain filters.
        Args:
            doc: A string
            usebigrams: Use bigrams or not
            stopList: A list of stop words

        Returns:
            A list of tokens
        '''
        if self.languagedet.detect(doc) is not 'en':
            return []
        doc = [d.translate(None,self.delchars) for d in doc.split()]
        if len(doc) <= self.USELESS_DOC_SIZE:
            return []
        if usebigrams is True:
            doc_bigrams = bigrams(doc)
            doc_bigrams = [ ' '.join(t) for t in doc]
        if stopwords is not None:
            doc = [self.sreplacer.replace(self.rreplacer.replace(t.lower())) for t\
                       in doc if t.lower() not in stopwords]
        else:
            doc = [self.sreplacer.replace(self.rreplacer.replace(t.lower())) for t\
                       in doc if t.lower()]
        if usebigrams is True:
            doc_bigrams.extend(doc)
            return doc_bigrams
        else:
            return doc



class GenerateVectors:

    """ Generates vectors from a given corpus
    Args:
        corpus - A file object for the corpus file
        mindfpercent - Minimum number of documents in which term should exist
        maxdfpercent - Max number of documents in which terms can exist
        minfrequency - Minimum occurrences of term in corpus required
        verbose - Enables Debug setting
        usebigrams - Usebigrams
        tf - Generate Term Frequency Vectors
        normalize - Normalize each vector
        stopwords - List of stopwords
    Methods:
        create - Wrapper for generate_vectors
        generate_vectors - Generates an vectors from inverted index
    """
    def __init__(self, **kwargs):
        self.corpus = kwargs.get('corpus')
        self.mindfpercent = kwargs.get('mindfpercent')
        self.maxdfpercent = kwargs.get('maxdfpercent')
        self.minfrequency = kwargs.get('minfrequency')
        self.stopwords = kwargs.get('stopwords')
        self.usebigrams = kwargs.get('usebigrams')
        self.normalize = kwargs.get('normalize')
        self.tf = kwargs.get('tf')
        assert (self.normalize is not None), "Decide whether normalization is\
        needed or not"
        assert (self.tf is not None), "Decide whether tf/idf vectors are needed"
        self.index = kwargs.get('index')
        self.featuredict = kwargs.get('featuredict')
        self.docids = kwargs.get('docids')
        verbose = kwargs.get('verbose')
        self.ndocs_content = kwargs.get('ndocs_content')
        self.logger = logging.getLogger(__name__)
        self.logger.addHandler(logging.StreamHandler())
        if verbose:
            self.logger.setLevel(logging.DEBUG)

    def create(self):

        """ Main method that generates the vectors."""

        self.logger.info("Cleaning corpus")
        if type(self.corpus) is file:
            self.corpus = create(self.corpus)
        if self.index is None:
            self.logger.info("Creating inverted index")
            result = generate_index(corpus = self.corpus, maxdfpercent =\
                                    self.maxdfpercent, mindfpercent =\
                                    self.mindfpercent, minfrequency =\
                                    self.minfrequency, usebigrams =\
                                    self.usebigrams, stopwords = self.stopwords)
            self.index = result['index']
            self.featuredict = result['featuredict']
            self.docids = result['docids']
            self.ndocs_content = result['ndocs_content']
        self.ndocs = len(self.docids)
        self.logger.debug("Corpus has %d total docs",self.ndocs)
        assert (self.ndocs_content is not None), "Must pass number of docs with\
        content"
        self.logger.debug("Corpus has %d docs with actual\
                              content",self.ndocs_content)
        self.logger.debug("Index has %d features", len(self.index))
        self.logger.info("Creating vectors")
        return self.generate_vectors()


    def generate_vectors(self):

        """ Generates doc vectors as  columns of a CSC matrix.
        Matrix columns are indices of docids which is returned
        """

        self.logger.debug("Walking through index to prune terms and non zero\
                          count")
        nelements = 0
        nfeatures = 0
        for term,docs in self.index.iteritems():
            nelements = nelements + len(docs)
            nfeatures = nfeatures + 1
        self.logger.debug("Number of non zero elements = %d",nelements)
        # Creating COO matrix for efficient sparse matrix building
        self.logger.debug("Number of features = %d",nfeatures)
        II = np.zeros(nelements)
        JJ = np.zeros(nelements)
        val = np.zeros(nelements)
        iielement = 0
        iiterm = 0
        normsq = np.zeros(self.ndocs_content)
        for term,docs in self.index.iteritems():
            self.logger.debug("Working with feature %s",term)
            for doc,info in docs.iteritems():
                tf = float(info[0])/info[1]
                idf = math.log10(float(self.ndocs)/float(len(docs)))
                if self.tf:
                    weight = tf
                else:
                    weight = tf*idf
                II[iielement] = iiterm
                JJ[iielement] = self.docids.index(doc)
                val[iielement] = weight
                normsq[self.docids.index(doc)] = normsq[self.docids.index(doc)]\
                + weight*weight
                iielement = iielement + 1
            iiterm = iiterm + 1
        invnorms = np.zeros(self.ndocs_content)
        ii = 0
        while ii < self.ndocs_content:
            try:
                invnorms[ii] = 1/math.sqrt(normsq[ii])
            except ZeroDivisionError:
                self.logger.debug("Vector of all zeros: %d is possibly empty of\
                             features",self.docids[ii])
                invnorms[ii] = 0
            ii = ii + 1
        vecs = ssp.coo_matrix((val,(II,JJ)),shape=(nfeatures,self.ndocs_content)).tocsc()
        if self.normalize is True:
            normsII = np.arange(0,self.ndocs_content,1)
            normsJJ = normsII
            diag = ssp.coo_matrix((invnorms,(normsII,normsJJ)),shape =\
                                      (self.ndocs_content, self.ndocs_content)\
                                 ).tocsc()
            return { 'data':vecs*diag, 'docids':self.docids,\
                'featuredict':self.featuredict}
        else:
            return { 'data':vecs, 'docids':self.docids,\
                'featuredict':self.featuredict}

class KMeans:

    """ Batch KMeans . Does Spherical and classical KMeans.
    Please note that the input vectors should be normalized tf-idf vectors for
    Spherical KMeans. Classical KMeans use L2 Norm. 
    Args:
        data - CSC Matrix with rows as features and columns as points
        k - Number of clusters to generate
        n - Number of iterations
        randomcentroids - Generate Centroids by partitioning matrix 
        determininstically or randomize selection of columns. 
        delta = Convergence Parameter
        classical - Boolean that determines whether to use classical kmeans or
        not. Default = FALSE
        verbose - Enables debug setting

    Methods:
        chunks - Chunks up list
        converged - checks convergence
        getdeterministicpartitions - gets deterministic partitions for
        clustering
        getrandomizedpartitions - gets randomized partitions for clustering
        run

    References:
        Spherical KMeans:
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

    def __init__(self, **kwargs):
        self.data = kwargs.get('data')
        self.k = kwargs.get('k')
        self.n = kwargs.get('n')
        self.delta = kwargs.get('delta')
        self.randomcentroids = kwargs.get('randomcentroids')
        verbose = kwargs.get('verbose')
        if self.randomcentroids is None:
            self.randomcentroids = False
        self.classical = kwargs.get('classical')
        if self.classical is None:
            self.classical = False
        self.logger = logging.getLogger(__name__)
        self.logger.addHandler(logging.StreamHandler())
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

    def converged(self, newQ):
        """ Check convergence.
        Check whether difference between currQ and newQ less than delta.
        Args:
            newQ which is a new quality measure.
        Returns:
            Boolean indicating convergence
        """
        if math.fabs(self.Q - newQ) < self.delta:
            return True
        else:
            return False

    def getclosest(self,distances):
        """ Find closest point from dict of points.
        Args:
            dcentroids: Dict of points.
        Returns:
            Closest point key
        """
        sortedkeys = distances.keys()
        sortedkeys.sort(cmp = lambda a,b: cmp(distances[a],distances[b]))
        MIN = 0
        MAX = len(sortedkeys) - 1
        if self.classical is True:
            return sortedkeys[MIN]
        else:
            return sortedkeys[MAX]

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
        if self.classical is True:
            result = getcentroids(self.data, self.clusters, normalize = False)
        else:
            result = getcentroids(self.data,self.clusters, normalize = True)
        self.centroids = result['centroids']
        self.centroiddict = result['centroiddict']

    def getdist(self,a,b):
        """ Returns distance between two points.
        Args:
            a: A vector in csc
            b: A vector in csc
        Returns:
            dist
        """
        if self.classical is True:
            return math.sqrt(((a-b).T*(a-b)).todense())
        else:
            return (a.T*b).todense()

    def getQ(self, **kwargs):
        """ Finds the quality of clusters
        Args:
            centroids: A sparse csc matrix .
            clusters: A dict with key as cluster id and values as vectors
        Returns:
            Quality of clustering.
        """
        centroids = kwargs.get('centroids')
        clusters = kwargs.get('clusters')
        centroiddict = kwargs.get('centroiddict')
        Q = 0
        for c_id,v_ids in clusters.iteritems():
            for v in v_ids:
                cv_id = centroiddict[c_id]
                Q += self.getdist(self.data[:,v], centroids[:,cv_id])
        return Q

    #TODO: (Eshwaran) Generate global mean vector and generate centroids by
    # random perturbations of this vector and then compute clusters
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
        if self.classical is True:
            result = getcentroids(self.data,self.clusters, normalize = False)
        else:
            result = getcentroids(self.data,self.clusters, normalize = True)
        self.centroids = result['centroids']
        self.centroiddict = result['centroiddict']

    def run(self):
        """ Runs kmeans, returns clusters and centroids.
        Returns:
            clusters. A dict mapping cluster IDs to the corresponding vector IDs
            centroids. A CSC Matrix of all the centroids.
            centroiddict: A dict mapping cluster IDs to centroid vector IDs.
        """
        assert (self.data.shape[1] > self.k), "Number of clusters requested\
        greater than number of vectors"
        self.logger.debug("Data is of dimensions:\
                     (%d,%d)",self.data.shape[0],self.data.shape[1])
        self.logger.debug("Generating %d clusters ...",self.k)
        if self.randomcentroids:
            self.logger.debug("Generating centroids by randomized partioning")
            self.getrandomizedpartitions()
        else:
            self.logger.debug("Generating centroids by arbitrary partitioning")
            self.getdeterministicpartitions()
        self.Q = self.getQ(centroids = self.centroids, centroiddict =\
                           self.centroiddict, clusters = self.clusters)
        ii = 0
        new_clusters = {}
        while ii < self.n:
            self.logger.debug("Iteration %d",ii)
            newclusters = {}
            jj = 0
            while jj < self.data.shape[1]:
                actualk = len(self.clusters)
                if self.k is not actualk:
                    self.logger.debug("Number of clusters is %d and not k=%d",
                                      actualk, self.k)
                dcentroids = {}
                for cid,cv_id in self.centroiddict.iteritems():
                    dcentroids[cid] = self.getdist(self.data[:,jj],\
                                                   self.centroids[:,cv_id])
                closestcluster = self.getclosest(dcentroids)
                if closestcluster in newclusters:
                    newclusters[closestcluster].append(jj)
                else:
                    newclusters[closestcluster] = [jj]
                jj = jj+1
            self.logger.debug("Going to get new centroids...")
            if self.classical is True:
                result = getcentroids(self.data,newclusters, normalize = False)
            else:
                result = getcentroids(self.data,newclusters, normalize = True)
            newcentroids = result['centroids']
            newcentroiddict = result['centroiddict']
            self.logger.debug("Going to check convergence...")
            newQ = self.getQ(centroids = newcentroids, centroiddict =\
                             newcentroiddict, clusters = newclusters)
            if self.converged(newQ):
                break
            else:
                self.centroids = newcentroids
                self.centroiddict = newcentroiddict
                self.clusters =  newclusters
                self.Q  = newQ
            ii = ii + 1

        return {'clusters':self.clusters, 'centroiddict':\
                    self.centroiddict,'centroids':self.centroids}

class SpectralCoClusterer:

    """ SpectralCoClusterer: Performs SpectralCoClustering on input corpus. Note
    that according to Reference 2, it is supposed to work well in practice for
    TF. It has been tested in Reference 1 using Classical KMeans.

    Args:
        corpus - A dictionary with key as docid and
        maxdfpercent - Stats for cleaning up corpus
        mindfpercent - "
        minfrequency - "
        usebigrams - usebigrams or not
        tf - Generate tf/idf vectors
        k - Number of clusters to generate
        n - Number of iterations
        randomcentroids - Generate Centroids by partitioning matrix
        determininstically or randomize selection of columns.
        delta = Convergence Parameter
        classical - Boolean that determines whether to use classical kmeans or
        not. Default = TRUE
        stopwords - A list of stopwords
        verbose - Enables debug setting

    Main Methods:
        run: Generates normalized matrix, A, computes SVD to produce Z which is
        clustered. Returns A, Z
        visualize: Generates word clouds and document clouds. The document
        cloud is a concept cloud.
    """

    def __init__(self, **kwargs):
        self.corpus = kwargs.get('corpus')
        assert(self.corpus is not None),"Corpus cannot be empty"
        self.maxdfpercent = kwargs.get('maxdfpercent')
        assert(self.maxdfpercent is not None),"Maxdfpercent cannot be empty"
        self.mindfpercent = kwargs.get('mindfpercent')
        assert(self.mindfpercent is not None),"Maxdfpercent cannot be empty"
        self.minfrequency = kwargs.get('minfrequency')
        assert(self.maxdfpercent is not None),"Maxdfpercent cannot be empty"
        self.usebigrams = kwargs.get('usebigrams')
        self.tf = kwargs.get('tf')
        self.k = kwargs.get('k')
        self.n = kwargs.get('n')
        self.delta = kwargs.get('delta')
        self.randomcentroids = kwargs.get('randomcentroids')
        self.sessionid = kwargs.get('sessionid')
        self.verbose = kwargs.get('verbose')
        self.stopwords = kwargs.get('stopwords')
        if self.randomcentroids is None:
            self.randomcentroids = False
        self.classical = kwargs.get('classical')
        if self.classical is None:
            self.classical = True
        if self.classical is True:
            self.normalize = False
        else:
            self.normalize = True
        self.logger = logging.getLogger(__name__)
        self.logger.addHandler(logging.StreamHandler())
        if self.verbose:
            self.logger.setLevel(logging.DEBUG)
        self.logger.debug("Starting Spectral Co-Clustering debugging...")
        if self.k is None:
            self.MIN_K = 2
            self.MAX_K = 50
            self.SAMPLE_SIZE_PERCENT = 100
            self.logger.debug('k not fed in. Figuring out k between range %d\
                              and %d and using sample size %d'\
                              ,self.MIN_K,self.MAX_K,self.SAMPLE_SIZE_PERCENT)

    def run(self):
        """ Main method that drives Spectral Co-Clustering. """
        self.logger.debug("Generating Vectors")
        ALL_DIMENSIONS = 0
        vectorcreator = GenerateVectors(corpus = self.corpus,\
                                                   mindfpercent =\
                                                   self.mindfpercent,\
                                                   maxdfpercent =\
                                                   self.maxdfpercent,\
                                                   minfrequency =\
                                                   self.minfrequency,\
                                                   verbose = self.verbose,\
                                                   usebigrams =\
                                                   self.usebigrams,\
                                                   normalize = self.normalize,\
                                                   tf = self.tf,\
                                                   stopwords = self.stopwords)
        result = vectorcreator.create()
        self.A = result['data']
        self.nfeatures = self.A.shape[0]
        self.ndocs = self.A.shape[1]
        self.logger.debug("Word By Documentmatrix A has dim:(%d,%d)",\
                          self.nfeatures,self.ndocs)
        self.docids  = result['docids']
        self.featuredict = result['featuredict']
        self.logger.debug("Generating normalized Adjacency Matrix, A_n")
        self.genAn()
        self.logger.debug("Finding SVD of An")
        un,s,vnt = spla.svd(self.An.todense())
        self.logger.debug('Shape of un (%d,%d)', un.shape[0], un.shape[1])
        vn = vnt.T
        self.logger.debug('Shape of vn (%d,%d)', vn.shape[1], vn.shape[1])
        self.logger.debug("Generating Z matrix")
        self.getZ(un,vn)
        data = (self.Z.T).tocsc()
        if self.k is None:
            self.k = find_no_clusters(X = data, samplesize =\
                                      self.SAMPLE_SIZE_PERCENT,mink =\
                                      self.MIN_K, maxk = self.MAX_K,\
                                      classical = self.classical,\
                                      verbose = self.verbose)
            self.logger.debug('k found to be %d',self.k)
        kmeans = KMeans(data = data, k = self.k, n = self.n,\
                        delta = self.delta,randomcentroids =\
                        self.randomcentroids, verbose =\
                        self.verbose, classical = self.classical)
        result = kmeans.run()
        self.coclusters = result['clusters']
        self.logger.debug('Number of co-clusters produced: %d',\
                          len(self.coclusters))
        self.visualizeclusters()
        return { 'fclouds':self.fclouds,'docclouds':self.docclouds,\
                'A':self.A,'An':self.An,'Z':self.Z }

    def genAn(self):
        self.getinvsqrtD1()
        self.logger.debug('D1 dimensions are (%d,%d)',self.D1.shape[0],\
                          self.D1.shape[1])
        self.getinvsqrtD2()
        self.logger.debug('D2 dimensions are (%d,%d)',self.D2.shape[0],\
                          self.D2.shape[1])
        self.An = self.D1*self.A*self.D2
        self.logger.debug('An dimensions are (%d,%d)',self.An.shape[0],\
                          self.An.shape[1])

    def getinvsqrtD1(self):
        numwords = self.A.shape[0]
        d = np.zeros(numwords)
        II = np.arange(0,numwords,1)
        JJ = II
        for ii in range(numwords):
            temp = math.sqrt(self.A[ii,:].todense().sum())
            d[ii] = 1/temp
        self.D1 = ssp.coo_matrix((d,(II,JJ)),shape = (numwords,numwords)).tocsc()

    def getinvsqrtD2(self):
        numdocs = self.A.shape[1]
        d = np.zeros(numdocs)
        II = np.arange(0,numdocs,1)
        JJ = II
        for ii in range(numdocs):
            temp = math.sqrt(self.A[:,ii].todense().sum())
            d[ii] = 1/temp
        self.D2 = ssp.coo_matrix((d,(II,JJ)),shape = (numdocs,numdocs)).tocsc()

    def getZ(self,un,vn):
        ''' Get matrix Z.
        Assumptions: If k is being selected, we take the max k we want and use
        that to determine l which determines dimensions of Z.
        '''
        if self.k is None:
            self.l  = int(math.ceil(math.log(self.MAX_K,2)))
        else:
            self.l = int(math.ceil(math.log(self.k,2)))
        self.pruneun(un)
        self.prunevn(vn)
        self.getZfeatures()
        self.getZdocs()
        self.Z = ssp.vstack([self.Zfeatures,self.Zdocs])

    def pruneun(self,un):
        self.U = ssp.csc_matrix(un[:,1:(self.l+1)])

    def prunevn(self,vn):
        self.V = ssp.csc_matrix(vn[:,1:(self.l+1)])

    def getZfeatures(self):
        self.Zfeatures = self.D1*self.U

    def getZdocs(self):
        self.Zdocs = self.D2*self.V

    def visualizeclusters(self):
        self.splitclusters()
        self.getfclouds()
        self.getdocclouds()

    def splitclusters(self):
        self.dclusters = {}
        self.fclusters = {}
        for c_id, z_ids in self.coclusters.iteritems():
            for z in z_ids:
                if self.iszfeature(z):
                    f_id = self.getfid(z)
                    if c_id in self.fclusters:
                        temp = self.fclusters[c_id]
                        temp.append(self.featuredict[f_id])
                        self.fclusters[c_id] = temp
                    else:
                        self.fclusters[c_id] = [self.featuredict[f_id]]
                else:
                    d_id = self.getdid(z)
                    if c_id in self.dclusters:
                        temp = self.dclusters[c_id]
                        temp.append(d_id)
                        self.dclusters[c_id] = temp
                    else:
                        self.dclusters[c_id] = [d_id]
        self.logger.debug('Number of doc clusters: %d', len(self.dclusters))
        self.logger.debug('Number of feature clusters: %d', len(self.fclusters))

    def getdocclouds(self):
        result = getcentroids(self.A,self.dclusters, self.classical)
        dcentroids = result['centroids']
        dcentroiddict = result['centroiddict']
        self.docclouds = genconceptclouds(corpus = self.corpus,\
                                          centroids = dcentroids,\
                                          centroiddict = dcentroiddict,\
                                        featuredict = self.featuredict,\
                                        sessionid = self.sessionid,\
                                        clusters = self.dclusters,
                                         docids = self.docids)

    def getfclouds(self):
        self.fclouds = ''
        for cid, features in self.fclusters.iteritems():
            self.fclouds += 'Cluster %d:\n'%(cid,)
            self.fclouds += ','.join(features)
            self.fclouds += '\n'

    def iszfeature(self,z):
        if z < self.nfeatures:
            return True
        else:
            return False

    def getfid(self,z):
        assert (z < self.nfeatures), 'Run iszfeature prior to getfid'
        return z

    def getdid(self,z):
        assert (z >= self.nfeatures), 'Run iszfeaure prior to getdid'
        return z-self.nfeatures


class _LangIdCorpusReader(CorpusReader):
    '''
    LangID corpus reader
    Source: http://misja.posterous.com/language-detection-with-python-nltk
    '''
    CorpusView = StreamBackedCorpusView

    def _get_trigram_weight(self, line):
        '''
        Split a line in a trigram and its frequency count
        '''
        data = line.strip().split(' ')
        if len(data) == 2:
            return (data[1], int(data[0]))

    def _read_trigram_block(self, stream):
        '''
        Read a block of trigram frequencies
        '''
        freqs = []
        for i in range(20): # Read 20 lines at a time.
            freqs.append(self._get_trigram_weight(stream.readline()))
        return filter(lambda x: x !=None,freqs)

    def freqs(self, fileids=None):
        '''
        Return trigram frequencies for a language from the
        corpus        
        '''
        return concat([self.CorpusView(path, self._read_trigram_block)\
                       for path in self.abspaths(fileids=fileids)])

class _LangDetect(object):
    '''
    Language detection code.
    Source: http://misja.posterous.com/language-detection-with-python-nltk
    '''
    language_trigrams = {}
    langid = LazyCorpusLoader('langid', _LangIdCorpusReader,\
                                         r'(?!\.).*\.txt')

    def __init__(self, languages=['nl', 'en', 'fr', 'de', 'es']):
        for lang in languages:
            self.language_trigrams[lang] = FreqDist()
            for f in self.langid.freqs(fileids=lang+"-3grams.txt"):
                    self.language_trigrams[lang].inc(f[0],f[1])
    
    def detect(self, text):
        '''
        Detect the text's language                        
        '''
        words = nltk_word_tokenize(text.lower())
        trigrams = {}
        scores   = dict([(lang, 0) for lang in\
                         self.language_trigrams.keys()])

        for match in words:
            for trigram in self.get_word_trigrams(match):
                if not trigram in trigrams.keys():
                    trigrams[trigram] = 0
                trigrams[trigram] += 1

        total = sum(trigrams.values())

        for trigram, count in trigrams.items():
            for lang, frequencies in self.language_trigrams.items():
                # normalize and add to the total score
                scores[lang] += (float(frequencies[trigram]) /\
                                 float(frequencies.N())) * \
                        (float(count) / float(total))

        return sorted(scores.items(), key=lambda x: x[1],\
                      reverse=True)[0][0]

    def get_word_trigrams(self, match):
        return [''.join(trigram) for trigram in nltk_trigrams(match)\
                if trigram != None]      


class _RepeatReplacer(object):
    '''
    Replaces repeat of text. Eg. looove becomes love.
    Source: http://www.nltk.org/book
    '''
    def __init__(self):
        self.repeat_regexp = re.compile(r'(\w*)(\w)\2(\w*)')
        self.repl =r'\1\2\3'

    def replace(self, word):
        if wordnet.synsets(word):
            return word
        repl_word = self.repeat_regexp.sub(self.repl, word)
        if repl_word != word:
            return self.replace(repl_word)
        else:
            return repl_word

class _SpellingReplacer(object):
    '''
    Spelling correction.
    Source: http://www.nltk.org/book
    '''
    def __init__(self, dict_name='en',max_dist=2):
        self.spell_dict = enchant.Dict(dict_name)
        self.max_dist = 2

    def replace(self, word):
        if len(word) is not 0:
            if self.spell_dict.check(word):
                return word
            suggestions = self.spell_dict.suggest(word)
            if suggestions and edit_distance(word, suggestions[0])\
               <= self.max_dist:
                return suggestions[0]
            else:
                return word
        else:
            return word























