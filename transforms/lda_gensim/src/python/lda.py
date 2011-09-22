#!/usr/bin/env python
# encoding: utf-8
"""
lda.py

Created by Xavier Stevens on 2011-09-19.
Copyright (c) 2011 Mozilla. All rights reserved.
"""

import logging
import sys
import getopt
import json
import re, string
from gensim import corpora, models, similarities

help_message = '''
The help message goes here.
'''


class Usage(Exception):
	def __init__(self, msg):
		self.msg = msg

class GrouperfishCorpus:
    def __init__(self, data_path, stopwords_path):
        self.data_path = data_path
        self.stopwords = self.load_stopwords(stopwords_path)
        #self.feature_index = {}
        self.document_index = self.generate_document_index()
        #self.generate_indices()
        self.dictionary = self.generate_dictionary(min_df=4)
        
    def __iter__(self):
        for doc_id,text_vector in self.document_iter():
            #gensim only wants the text back
            yield self.dictionary.doc2bow(text_vector)
    
    def __len__(self):
        return len(self.document_index)
        
    def document_iter(self):
        fin = open(self.data_path, "r", buffering=32384)
        tab_pattern = re.compile("\t")
        space_pattern = re.compile("\s+")
        punct_pattern = re.compile('[%s]' % re.escape(string.punctuation))
        for line in fin:
            docid_json_splits = tab_pattern.split(line.strip())
            doc_json = json.loads(docid_json_splits[1])
            yield (doc_json["id"],space_pattern.split(punct_pattern.sub('', doc_json["text"].lower())))
        fin.close()
    
    def generate_document_index(self):
        document_index = {}
        line_number = 0
        for doc_id,text in self.document_iter():
            document_index[line_number] = doc_id
            line_number += 1
            
        return document_index
    
    def load_stopwords(self, stopwords_path):
        fin = open(stopwords_path, "r", buffering=8096)
        stopwords = set([''])
        for line in fin:
            stopwords.add(line.strip())
        fin.close()
        return stopwords
        
    def generate_dictionary(self, min_df=2, max_df_percentage=0.7):
        dictionary = corpora.Dictionary(text_vector for doc_id,text_vector in self.document_iter())
        total_features = len(dictionary.dfs)
        stop_ids = [dictionary.token2id[stopword] for stopword in self.stopwords if stopword in dictionary.token2id]
        freq_filter_ids = [tokenid for tokenid, docfreq in dictionary.dfs.iteritems() if docfreq < min_df or (float(docfreq)/float(total_features)) > max_df_percentage]
        dictionary.filter_tokens(stop_ids + freq_filter_ids)
        dictionary.compactify()
        
        return dictionary
        
    def generate_indices(self, min_df=2, max_df_percentage=0.6):
        fin = open(self.data_path, "r", buffering=32384)
        line_number = 0
        feature_freq = {}
        tab_pattern = re.compile("\t")
        space_pattern = re.compile("\s+")
        for line in fin:
            docid_json_splits = tab_pattern.split(line.strip())
            doc_id = docid_json_splits[0]
            doc_json = json.loads(docid_json_splits[1])
            self.document_index[line_number] = doc_json["id"]
            for w in space_pattern.split(doc_json["text"].lower()):
                ff = feature_freq.setdefault(w, 0)
                feature_freq[w] = ff + 1
            line_number += 1
        fin.close()
        
        # create the feature index
        index = 0
        total_features = len(feature_freq)
        for f,freq in feature_freq.iteritems():
            if freq > min_df and (float(freq)/float(total_features)) < max_df_percentage:
                self.feature_index[f] = index
                index += 1
        
def main(argv=None):
	if argv is None:
		argv = sys.argv
	try:
		try:
			opts, args = getopt.getopt(argv[1:], "ho:d:s:", ["help", "output="])
		except getopt.error, msg:
			raise Usage(msg)
	
		# option processing
		data_path = None
		stopwords_path = None
		for option, value in opts:
			if option == "-d":
				data_path = value
			if option in ("-h", "--help"):
				raise Usage(help_message)
			if option in ("-o", "--output"):
				output = value
			if option == "-s":
			    stopwords_path = value
			    
		#corpus = GrouperfishCorpus(data_path)
	except Usage, err:
		print >> sys.stderr, sys.argv[0].split("/")[-1] + ": " + str(err.msg)
		print >> sys.stderr, "\t for help use --help"
		return 2


if __name__ == "__main__":
	#main()
	logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
	k = 15
	corpus = GrouperfishCorpus("input.json.tsv", "stopwords-en.txt")
	lda = models.ldamodel.LdaModel(corpus, id2word=corpus.dictionary, num_topics=k, update_every=0, passes=100)
	lda.print_topics(k)