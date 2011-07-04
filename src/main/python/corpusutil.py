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
from nltk import bigrams
from nltk.metrics import edit_distance
from nltk import PorterStemmer
from nltk.corpus import wordnet



def cleandoc(doc, usebigrams = False,stopwords=None):
    """ Cleans document applying certain filers.

    Filters applied include tokenize by white space, replace repeats e.g. goood
    becomes good, correct spelling, stemming and lemmatize. 

    Args:
        doc: A string
        usebigrams: Use bigrams or not
        stopList: A list of stop words

    Returns:
        A list of tokens

    """

    class _RepeatReplacer(object):
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
        def __init__(self, dict_name='en',max_dist=2):
            self.spell_dict = enchant.Dict(dict_name)
            self.max_dist = 2

        def replace(self, word):
            if len(word) is not 0:
                if self.spell_dict.check(word):
                    return word
                suggestions = self.spell_dict.suggest(word)
                if suggestions and edit_distance(word, suggestions[0]) <= self.max_dist:
                    return suggestions[0]
                else:
                    return word
            else:
                return word

    rreplacer = _RepeatReplacer()
    sreplacer = _SpellingReplacer()
    delchars = ''.join(c for c in map(chr, range(256)) if not c.isalpha())
    doc = [d.translate(None,delchars) for d in doc.split()]
    if usebigrams is True:
        temp = doc
        doc = bigrams(temp)
        doc = [ ' '.join(t) for t in doc]
        doc.extend(temp)
    if stopwords is not None:
        doc = [sreplacer.replace(rreplacer.replace(t.lower())) for t in doc if t.lower() not in stopwords]
    else:
        doc = [sreplacer.replace(rreplacer.replace(t.lower())) for t in doc if t.lower()]
    stemmer = PorterStemmer()
    doc = [stemmer.stem(t) for t in doc]
    #Doing lower casing again just because the stemmers are doing
    #capitalizations. They also occasionally generate single letter chars
    if stopwords is not None:
        doc = [d.lower().translate(None,string.digits+string.punctuation) for d in doc if d.lower() not in\
               stopwords]
    else:
        doc =  [d.lower().translate(None,string.digits+string.punctuation) for d in doc ]
    return doc


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

def extract_topfeatures(data,featuredict,nfeaturesreq):
    """ Extracts top nfeaturesreq  from data
    Args:
        data: A matrix with each column containing one observation.
        featuredict: A map from matrix indices to features
        nfeaturesreq: Number of features to extract
    Returns:
        topfeatures: A dict of dicts. The key of dict is the  column
        identifier.The value is a dict with key as feature and value as matrix
        value.
    """
    lindex = data.shape[0] - nfeaturesreq
    rindex = data.shape[0] + 1
    sortedargs  =  np.matrix.argsort(data,axis = 0)[lindex:rindex,:]
    ii = 0
    topfeatures = {}
    while ii < sortedargs.shape[1]:
        colfeatures = {}
        jj = 0
        while jj < sortedargs.shape[0]:
            feature = featuredict[sortedargs[jj,ii]]
            colfeatures[feature] = data[sortedargs[jj,ii],ii]
            jj = jj + 1
        topfeatures[ii] = colfeatures
        ii = ii + 1
    return topfeatures

def generate_index(**kwargs):
    """ Generates inverted index from corpus.

    Corpus generated only from description field, documents are cleaned up using
    the  cleandocument method.

    Args:
        corpus: A dict that has been run through create method.
        descriptionfield: int identifier of the field to create index using.
        maxdfpercent: Maximum percentage of the docs in which term can occur
        mindfpercent: Minimum  percentage of the docs in which term can occur
        minfrequency: Minimum total occurrences of term in doc
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
    for currdocid,doc in corpus.iteritems():
        docids.append(currdocid)
        doc = cleandoc(doc[descriptionfield],usebigrams,stopwords)
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


#TODO (Eshwaran): Implement the algorithm in the reference
"""
References:
        @article{gottron2009document,
        title={Document word clouds: Visualising web documents as tag clouds
        to aid users in relevance decisions},
        author={Gottron, T.},
        journal={Research and Advanced Technology for Digital Libraries},
        pages={94--105},
        year={2009},
        publisher={Springer}
        }
"""

def generate_featureclouds(centroids,featuredict,sessionid):

    """ Take top ten features from every cluster and create cloud.
    Cloud is generated and saved to file identified by sessionid

    Args:
        centroids: A  dense matrix where every column vector is a centroid vector.
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
    topfeatures = extract_topfeatures(centroids,featuredict,NUM_FEATURES_REQ)
    for colid,features in topfeatures.iteritems():
        features = mapfeatures_to_cloudbins(features)
        divstr += css.generate_single_cloud(colid,features)
    bodystr = css.generate_body(divstr)
    return css.wrap_into_html(bodystr,sessionid,FONT_URL,STYLE_URL)

def generate_featureclusters(centroids,featuredict,sessionid):
    """  Create feature cluster clouds by generating feature clusters.
    Generate feature cluster clouds by determining for every feature which
    cluster it most belongs to by finding centroid where it has max value. Top
    ten such features for every cluster are extracted and visualized using as an
    HTML page.

    Args:
        centroids: A  dense matrix where every column vector is a centroid vector.
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
    featureclusters = {}
    for index,feature in featuredict.iteritems():
        cid = centroids[index,:].argmax()
        if cid in featureclusters:
            featureclusters[cid].append((feature,centroids[index,cid]))
        else:
            featureclusters[cid] = [(feature,centroids[index,cid])]
    for cid,features in featureclusters.iteritems():
        topfeatures = dict(sorted(features,key = lambda feature:\
                               feature[1])[0:NUM_FEATURES_REQ])
        topfeatures = mapfeatures_to_cloudbins(topfeatures)
        divstr += css.generate_single_cloud(cid,topfeatures)
    bodystr = css.generate_body(divstr)
    return css.wrap_into_html(bodystr,sessionid,FONT_URL,STYLE_URL)

def getcentroids(data,clusters):
    """ Uses clusters to generate centroids
    Args:
        clusters: A dict with key as cluster id and values as list of
        vector ids that belong to it
    Returns: csc matrix of new centroids
    """
    k = len(clusters)
    newcentroids =  np.mat(np.zeros((data.shape[0],k)))
    invnorms = np.zeros(k)
    normsII = np.arange(0,k,1)
    normsJJ = normsII
    for centroid,v_ids in clusters.iteritems():
        for v in v_ids:
            newcentroids[:,centroid] = newcentroids[:,centroid] +\
                    data[:,v].todense()
        newcentroids[:,centroid] = newcentroids[:,centroid]*(float(1)/len(v_ids))
        normcentroid = math.sqrt(newcentroids[:,centroid].T*newcentroids[:,centroid])
        if normcentroid is not 0:
            invnorms[centroid] =\
                    1/(math.sqrt(newcentroids[:,centroid].T*newcentroids[:,centroid]))
        else:
            invnorms[centroid] = 0
    diag = ssp.coo_matrix((invnorms,(normsII,normsJJ)),shape = (k,k)).tocsc()
    return ssp.csc_matrix(newcentroids)*diag

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
        minfrequency: Minimum total occurrences of term in doc

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
    """ Helper function to generate_featureclouds. 
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

class GenerateVectors:

    """ Generates tfidf vectors from a given corpus
    Args:
        corpus - A file object for the corpus file
        mindfpercent - Minimum number of documents in which term should exist
        maxdfpercent - Max number of documents in which terms can exist
        minfrequency - Minimum occurrences required
        verbose - Enables Debug setting
        usebigrams - Usebigrams 
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
        self.index = kwargs.get('index')
        self.featuredict = kwargs.get('featuredict')
        self.docids = kwargs.get('docids')
        verbose = kwargs.get('verbose')
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

            self.index, self.featuredict, self.docids, ndocs_content  =\
            result = generate_index(corpus = self.corpus, maxdfpercent = self.maxdfpercent,\
                                 mindfpercent = self.mindfpercent, minfrequency
                                    = self.minfrequency,\
                                 usebigrams = self.usebigrams, stopwords = self.stopwords)
            self.index = result['index']
            self.featuredict = result['featuredict']
            self.docids = result['docids']
            self.ndocs_content = result['ndocs_content']
        else:
            ndocs_content = None
        self.ndocs = len(self.docids)
        self.logger.debug("Corpus has %d total docs",self.ndocs)
        if ndocs_content is not None:
            self.logger.debug("Corpus has %d docs with actual\
                              content",ndocs_content)
        self.logger.debug("Index has %d features", len(self.index))
        self.logger.info("Creating vectors")
        return self.generate_vectors()


    def generate_vectors(self):

        """ Generates tfidf vectors as  columns of a CSC matrix.
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
        normsq = np.zeros(self.ndocs)
        for term,docs in self.index.iteritems():
            self.logger.debug("Working with feature %s",term)
            for doc,info in docs.iteritems():
                tf = float(info[0])/info[1]
                idf = math.log10(float(self.ndocs)/float(len(docs)))
                tfidf = tf*idf
                II[iielement] = iiterm
                JJ[iielement] = self.docids.index(doc)
                val[iielement] = tfidf
                normsq[self.docids.index(doc)] = normsq[self.docids.index(doc)] + tfidf*tfidf
                iielement = iielement + 1
            iiterm = iiterm + 1
        invnorms = np.zeros(self.ndocs)
        ii = 0
        while ii < self.ndocs:
            try:
                invnorms[ii] = 1/math.sqrt(normsq[ii])
            except ZeroDivisionError:
                self.logger.debug("Vector of all zeros: %d is possibly empty of\
                             features",self.docids[ii])
                invnorms[ii] = 0
            ii = ii + 1
        normsII = np.arange(0,self.ndocs,1)
        normsJJ = normsII
        diag = ssp.coo_matrix((invnorms,(normsII,normsJJ)),shape =\
                                  (self.ndocs,self.ndocs)).tocsc()
        vecs = ssp.coo_matrix((val,(II,JJ)),shape=(nfeatures,self.ndocs)).tocsc()
        return { 'data':vecs*diag, 'docids':self.docids,\
                'featuredict':self.featuredict}

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

    def __init__(self, data, k, n, delta, randomcentroids, verbose):
        self.data = data
        self.k = k
        self.n = n
        self.delta = delta
        self.randomcentroids = randomcentroids
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

    def converged(self,clusters,newclusters):
        """ Check convergence.
         We check if difference of sum of  norm of sum of all the vectors for each cluster
         computed during prev iteration and curre iteration is less than delta
         Args:
             clusters: A dict with key as cluster id and value as a list of docs
             belonging to it.
             newclusters: Similar datastructure for the new cluster
        Returns:
            Boolean indicating convergence
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
        self.centroids = getcentroids(self.data,self.clusters)
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
        self.centroids = getcentroids(self.data,self.clusters)
        return {'centroids':self.centroids,'clusters':self.clusters}

    def run(self):
        """ Runs spherical kmeans, returns clusters and centroids.
        Returns:
            clusters. A dict mapping cluster IDs to the corresponding vector IDs
            centroids. The clusters themeselves.
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
            newcentroids = getcentroids(self.data,newclusters)
            self.logger.debug("Going to check convergence...")
            if self.converged(self.clusters,newclusters):
                break
            else:
                self.centroids = newcentroids
                self.clusters =  newclusters
            ii = ii + 1
        return {'clusters':self.clusters,'centroids':self.centroids}

