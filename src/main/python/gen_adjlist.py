#! /usr/bin/env python
# Filename: gen_adjlist.py
""" Generate adjacencylist from opinion feedback data."""
import cPickle
import collections
import logging
import math
import string
import StringIO
import sys
import time

import argparse
import corpusutil
import numpy as np


class GenerateAdjacencyList:

    """ Generates an adjacency list from a given corpus
    Args:
        corpus - A file object for the corpus file
        mindfpercent - Minimum number of documents in which term should exist
        maxdfpercent - Max number of documents in which terms can exist
        minfrequence - Minimum occurrences required
        verbose - Enables Debug setting
        stopwords - List of stopwords
    Methods:
        create - Wrapper for generate_adjlist
        generate_adjlist - Generates an adjacency list from inverted index
    """
    def __init__(self, **kwargs):
        self.corpus = kwargs.get('corpus')
        self.mindfpercent = kwargs.get('mindfpercent')
        self.maxdfpercent = kwargs.get('maxdfpercent')
        self.minfrequency = kwargs.get('minfrequency')
        self.stopwords = kwargs.get('stopwords')
        self.usebigrams = kwargs.get('usebigrams')
        verbose = kwargs.get('verbose')
        self.logger = logging.getLogger(type(self).__name__)
        self.logger.addHandler(logging.StreamHandler())
        if verbose:
            self.logger.setLevel(logging.DEBUG)

    def create(self):

        """ Main method that generates the adjacency list."""

        self.logger.debug("corpus has %d documents",len(self.corpus))
        self.logger.info("Creating inverted index")
        self.index, self.featuredict,self.docids,ndocs_content =\
        corpusutil.generate_index(self.corpus, self.maxdfpercent,\
                                  self.mindfpercent, self.minfrequency,\
                                  self.usebigrams,self.stopwords)
        self.ndocs = len(self.docids)
        self.logger.debug("corpus has %d documents with content",ndocs_content)
        self.logger.debug("Index has %d features", len(self.index))
        self.logger.info("Creating adjacency list")
        return { 'adjlist': self.generate_adjlist(), 'index': self.index,\
                'featuredict': self.featuredict, 'docids': self.docids}


    def generate_adjlist(self):
        docnorms = {}
        adjlist = {}
        for term, docs in self.index.iteritems():
            self.logger.debug("Working with feature: %s", term)
            tfidflist = {}
            for doc, info in docs.iteritems():
                tf = float(info[0])/info[1]
                idf = math.log10(float(self.ndocs)/float(len(docs)))
                tfidf = tf*idf
                docid = self.docids.index(doc)
                if docid not in docnorms:
                    docnorms[docid] = tfidf*tfidf
                else:
                    temp = docnorms[docid]
                    docnorms[docid] = temp + tfidf*tfidf
                if not tfidf:
                    tfidflist[docid] = tfidf
                else:
                    for otherdocid,othertfidf in tfidflist.iteritems():
                        temp = tfidf*othertfidf
                        if  (otherdocid,docid) not in adjlist and\
                                (docid,otherdocid) not in adjlist:
                            adjlist[(docid,otherdocid)] = temp
                        elif (otherdocid,docid) in adjlist:
                            temp2 = adjlist[(otherdocid,docid)]
                            adjlist[(otherdocid,docid)] = temp + temp2
                        else:
                            temp2 = adjlist[(docid,otherdocid)]
                            adjlist[(docid,otherdocid)] = temp + temp2
                    tfidflist[docid] = tfidf
        for docid,normsq in docnorms.iteritems():
            norm = math.sqrt(normsq)
            docnorms[docid] = norm
        self.logger.debug("Normalizing adjacency list")
        print len(adjlist)
        for (node1,node2),edge in adjlist.iteritems():
            norm1 = docnorms[node1]
            norm2 = docnorms[node2]
            adjlist[(node1,node2)] = edge/(norm1*norm2)
        return adjlist


def gen_args():
    parser = argparse.ArgumentParser(description='Adjacency List Generation')
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
    parser.add_argument('-usebigrams',action = 'store_true', default = False, dest =\
                        'usebigrams', help = 'Use bigrams. Default =\
                        No')
    return parser


def main():
    parser = gen_args()
    args = parser.parse_args()
    sessionid = args.sessionid
    logger =  logging.getLogger(__name__)
    logger.addHandler(logging.StreamHandler())
    if args.opinion:
        corpus = corpusutil.create(args.opinion)
    else:
        corpus = cPickle.load(args.corpus)
    if args.verbose:
        logger.setLevel(logging.DEBUG)
    if args.stopwords:
        stopwords = args.stopwords.read().split()
        adjlistcreator = GenerateAdjacencyList(corpus = corpus, mindfpercent =args.mindfpercent,\
                                      maxdfpercent = args.maxdfpercent,\
                          minfrequency=args.minfrequency, verbose =\
                                      args.verbose,usebigrams = args.usebigrams, stopwords = stopwords)
    else:
        adjlistcreator = GenerateAdjacencyList(corpus = corpus, mindfpercent =args.mindfpercent,\
                                      maxdfpercent = args.maxdfpercent,\
                          minfrequency=args.minfrequency, verbose =\
                                      args.verbose,usebigrams = args.usebigrams, stopwords = stopwords)
    result = adjlistcreator.create()
    adjlist = result['adjlist']
    index = result['index']
    featuredict = result['featuredict']
    docids = result['docids']
    cPickle.dump(featuredict,open("adjlist_featuredict_"+sessionid+'.pck','w'))
    cPickle.dump(index,open("adjlist_index_"+sessionid+'.pck','w'))
    cPickle.dump(docids,open("adjlist_docids_"+sessionid+'.pck','w'))
    adjlistfile = open("adjlist_"+sessionid+".out",'w')
    for nodes,edge in adjlist.iteritems():
        adjlistfile.write(str(nodes[0]) + " " + str(nodes[1]) + " " + str(edge)\
                          + "\n")


if __name__ == "__main__":
    sys.exit(main())













