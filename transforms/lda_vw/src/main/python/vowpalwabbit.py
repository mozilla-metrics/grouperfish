# ***** BEGIN LICENSE BLOCK *****
# Version: MPL 1.1/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Mozilla Public License Version
# 1.1 (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
# http://www.mozilla.org/MPL/
#
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
# for the specific language governing rights and limitations under the
# License.
#
# The Original Code is Mozilla Grouperfish.
#
# The Initial Developer of the Original Code is
# Mozilla Foundation.
# Portions created by the Initial Developer are Copyright (C) 2011
# the Initial Developer. All Rights Reserved.
#
# Contributor(s):
# Xavier Stevens <xstevens@mozilla.com>
# Alternatively, the contents of this file may be used under the terms of
# either the GNU General Public License Version 2 or later (the "GPL"), or
# the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the MPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the MPL, the GPL or the LGPL.
#
# ***** END LICENSE BLOCK *****

import sys
import getopt
import re

help_message = '''
Example Usage:
python vowpalwabbit.py -p lda-predictions.dat -t lda-topics.dat -f feature-index.txt
'''

class Usage(Exception):
	def __init__(self, msg):
		self.msg = msg

def read_feature_index(feature_index_file):
    feature_index = []
    feature_index_in = open(feature_index_file, "r")
    for line in feature_index_in:
        feature_index.append(line.strip())
    feature_index_in.close()
    return feature_index

def read_lda_results(predictions_file, topics_file, num_top_words, num_features):
    topics = read_topics_file(topics_file, num_top_words, num_features)
    doc_top_topic=None
    if predictions_file:
        doc_top_topic = read_predictions_file(predictions_file)
    
    return (topics,doc_top_topic)

def read_predictions_file(predictions_file):
    doc_top_topic = []
    doc_id = 0
    space_pattern = re.compile(r'\s')
    
    predictions_in = open(predictions_file, "r")
    
    for line in predictions_in:
        max_topic = -1
        max_topic_score = 0.0
        k = 0
        for topic_score in space_pattern.split(line.strip()):
            if float(topic_score) > max_topic_score:
                max_topic = k
                max_topic_score = float(topic_score)
            # increment topic
            k += 1
        
        doc_top_topic.append((doc_id, max_topic, max_topic_score))
        # increment doc_id
        doc_id += 1
    
    predictions_in.close()
    
    return doc_top_topic

def read_topics_file(topics_file, num_words_per_topic, num_features):
    topics = []
    
    space_pattern = re.compile(r'\s')
    num_topics_pattern = re.compile(r'lda:([0-9]+)')
    starts_with_number = re.compile(r'^[0-9]')
    
    feature_id = 0
    topics_in = open(topics_file, "r")
    for line in topics_in:
        if len(topics) > 0:
            k = 0
            for score in space_pattern.split(line.strip()):
                topics[k].append((feature_id,float(score)))
                k += 1
            feature_id += 1
            if feature_id >= num_features:
                break
        elif num_topics_pattern.match(line):
            m = num_topics_pattern.match(line.strip())
            k = int(m.group(1))
            print "LDA k = %d" % (k)
            topics = [None]*k
            topics = [[] for i in range(len(topics))]
    
    topics_in.close()
    
    for i in range(len(topics)):
        topic_sum = sum([s for f,s in topics[i]])
        topics[i] = [(f, s / topic_sum) for f,s in topics[i]]
        topics[i] = sorted(topics[i], key=lambda feature_prob: feature_prob[1], reverse=True)
        topics[i] = topics[i][:num_words_per_topic]
    
    return topics

def main(argv=None):
    if argv is None:
        argv = sys.argv
        
	try:
	    try:
	        opts, args = getopt.getopt(argv[1:], "h:p:f:t:", ["help"])
	    except getopt.error, msg:
	        raise Usage(msg)
	    
	    # option processing
	    predictions_file = None
	    topics_file = None
	    feature_index_file = None
	    num_docs = 0
	    for option, value in opts:
	        if option in ("-h", "--help"):
	            raise Usage(help_message)
	        elif option == "-p":
	            predictions_file = value
	        elif option == "-f":
	            feature_index_file = value
	        elif option == "-t":
	            topics_file = value
	    
	    feature_index = read_feature_index(feature_index_file)
	    topics,doc_top_topic = read_lda_results(predictions_file, topics_file, 20, len(feature_index))
	    for k in range(len(topics)):
	        print "==== Topic %d ====" % (k)
	        for feature_score in topics[k]:
	            feature_id = feature_score[0]
	            if (feature_id < len(feature_index)):
	                print "\t %s => %0.5f" % (feature_index[feature_id], feature_score[1])
	            else:
	                print "\t feature[%d]=?? => %0.5f" % (feature_score[0], feature_score[1])
	except Usage, err:
	    print >> sys.stderr, sys.argv[0].split("/")[-1] + ": " + str(err.msg)
	    print >> sys.stderr, "\t for help use --help"
	    return 2

if __name__ == "__main__":
    sys.exit(main())