#!/usr/bin/env python
# encoding: utf-8
"""
convert_r_to_grouperfish.py

Created by Xavier Stevens on 2011-09-06.
Copyright (c) 2011 Mozilla. All rights reserved.
"""

import sys
import getopt
import re
import json

help_message = '''
./convert_r_to_grouperfish.py -t topics.dat -d topdocs.dat -o output.json
'''


class Usage(Exception):
	def __init__(self, msg):
		self.msg = msg

def read_topics(topics_file):
    topic_features = {}
    space_pattern = re.compile("\s+")
    fin = open(topics_file, "r")
    topic_id = 0
    for line in fin:
        for feature in space_pattern.split(line.strip()):
            t = topic_features.setdefault(topic_id, [])
            t.append(feature)
        topic_id += 1
    fin.close()
    
    return topic_features
    
def read_docs(docs_file):
    topic_docs = {}
    space_pattern = re.compile("\s+")
    fin = open(docs_file, "r")
    topic_id = 0
    for line in fin:
        for doc_id in space_pattern.split(line.strip()):
            t = topic_docs.setdefault(topic_id, [])
            t.append(doc_id)
        topic_id += 1
    fin.close()
        
    return topic_docs

def prob(l):
    frequency = {}
    for n in l:
        frequency[n] = frequency.get(n, 0) + 1

    for n,c in frequency.iteritems():
        frequency[n] = float(c) / float(len(l))
    
    return frequency
    
def mode(l):
    frequency = {}
    for n in l:
        frequency[n] = frequency.get(n, 0) + 1

    max_freq = 0
    max_n = None
    for n,c in frequency.iteritems():
        if c > max_freq:
            max_freq = c
            max_n = n
            
    return max_n
    
def read_assignments(assignments_file):
    assignments = {}
    space_pattern = re.compile("\s+")
    fin = open(assignments_file, "r")
    doc_id = 0
    for line in fin:
        topics = [int(t) for t in space_pattern.split(line.strip()) if len(t) > 0]
        assignments[doc_id] = prob(topics)
        doc_id += 1

    return assignments

def write_output(output_file, grouperfish_json_dict):
    fout = open(output_file,"w")
    json.dump(grouperfish_json_dict, fout)
    fout.close()
    
def main(argv=None):
	if argv is None:
		argv = sys.argv
	try:
		try:
			opts, args = getopt.getopt(argv[1:], "ho:t:d:a:", ["help", "output="])
		except getopt.error, msg:
			raise Usage(msg)
		
		output_file = None
		topics_file = None
		docs_file = None
		assignments_file = None
		# option processing
		for option, value in opts:
			if option in ("-h", "--help"):
				raise Usage(help_message)
			if option in ("-o", "--output"):
				output_file = value
			if option == "-t":
			    topics_file = value
			if option == "-d":
			    docs_file = value
			if option == "-a":
			    assignments_file = value
		
		topic_features = read_topics(topics_file)
		top_docs = read_docs(docs_file)
		doc_topic_dists = read_assignments(assignments_file)
		grouperfish_json_dict = { "TOP_FEATURES":topic_features, "TOP_DOCS":top_docs, "DOC_TOPICS":doc_topic_dists }
		write_output(output_file, grouperfish_json_dict)
		
	except Usage, err:
		print >> sys.stderr, sys.argv[0].split("/")[-1] + ": " + str(err.msg)
		print >> sys.stderr, "\t for help use --help"
		return 2


if __name__ == "__main__":
	sys.exit(main())
