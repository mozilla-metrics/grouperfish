#/usr/bin/env python

import logging
import sys
import time

import argparse
import corpusutil
import cPickle
import numpy as np

def gen_args():
    parser = argparse.ArgumentParser(description='Generate Subsets Of Opinion\
                                     Data')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('-opinion', metavar = 'input',type = file,\
                        help='Tab Separated input opinions file')
    group.add_argument('-corpus', metavar = 'corpus', type = file,\
                       help='Pickled corpus')
    parser.add_argument('-doctype',metavar = 'doctype', action = 'store', type \
                        = str, dest = 'doctype', help =\
                        'issue/praise/suggestion/rating')
    parser.add_argument('-product',metavar = 'product', action = 'store', type \
                        = str, dest = 'product', help = 'firefox/mobile')
    parser.add_argument('-version',metavar = 'version', action = 'store', type \
                        = str, dest = 'version', help = 'version identifier')
    parser.add_argument('-platform',metavar = 'platform', action = 'store', type \
                        = str, dest = 'platform', help = 'mac, linux, android,\
                        maemo, winxp, vista etc')
    parser.add_argument('-locale',metavar = 'locale', action = 'store', type \
                        = str, dest = 'locale', help = 'Locale identifier such\
                        as  en-US') 
    parser.add_argument('-manufacturer',metavar = 'manufacturer', action = 'store', type \
                        = str, dest = 'manufacturer', help = 'Manufacturer\
                        identifier for mobile')
    parser.add_argument('-device',metavar = 'device', action = 'store', type \
                        = str, dest = 'device', help = 'Device\
                        identifier for mobile')
    parser.add_argument('-sessionid',action = 'store', dest = 'sessionid',\
                        default = str(int(time.time()*100000)), help =\
                        'Generate unique session id. Default = time\
                        dependent')
    return parser

def main():
    parser = gen_args()
    args = parser.parse_args()
    sessionid = args.sessionid
    logger =  logging.getLogger(__name__)
    logger.addHandler(logging.StreamHandler())
    logger.setLevel(logging.DEBUG)
    if args.opinion:
        corpus = corpusutil.create(args.opinion)
    else:
        corpus = cPickle.load(args.corpus)
    logger.debug("Number of documents in corpus: %d", len(corpus))
    logger.debug("Going to Create subsets")
    subset = corpusutil.generate_subset(corpus,doctype = args.doctype, product=\
                                        args.product, version = args.version,\
                                        platform = args.platform, locale =\
                                        args.locale, manufacturer =\
                                        args.manufacturer, device = args.device)
    logger.debug("Number of documents in subset: %d", len(subset))
    cPickle.dump(subset,open('subset'+'-'+sessionid+'.pck','w'))

if __name__ == "__main__":
    sys.exit(main())

