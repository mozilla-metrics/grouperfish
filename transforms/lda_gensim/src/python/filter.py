#!/usr/bin/env python
# encoding: utf-8
"""
filter.py

Created by Xavier Stevens on 2011-09-19.
Copyright (c) 2011 Mozilla. All rights reserved.
"""

import sys
import getopt
import re
import json

help_message = '''
The help message goes here.
'''


class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

def filter_data(input_file, output_file, product="firefox", version="5.0", feedback_type="issues"):
    fin = open(input_file, "r")
    fout = open(output_file, "w")
    tab_pattern = re.compile("\t")
    for line in fin:
        line_splits = tab_pattern.split(line.strip())
        doc_json = json.loads(line_splits[1])
        if doc_json["product"] == product and doc_json["version"] == version and doc_json["type"] == feedback_type:
            fout.write(line)
    fin.close()
    fout.close()
    
def main(argv=None):
    if argv is None:
        argv = sys.argv
    try:
        try:
            opts, args = getopt.getopt(argv[1:], "ho:d:p:v:t:", ["help", "output="])
        except getopt.error, msg:
            raise Usage(msg)
    
        # option processing
        data_path = None
        output_path = None
        product = None
        version = None
        feedback_type = None
        for option, value in opts:
            if option == "-d":
                data_path = value
            if option in ("-h", "--help"):
                raise Usage(help_message)
            if option in ("-o", "--output"):
                output_path = value
            if option == "-p":
                product = value
            if option == "-v":
                version = value
            if option == "-t":
                feedback_type = value
        
        filter_data(data_path, output_path, product, version, feedback_type)
    except Usage, err:
        print >> sys.stderr, sys.argv[0].split("/")[-1] + ": " + str(err.msg)
        print >> sys.stderr, "\t for help use --help"
        return 2


if __name__ == "__main__":
    sys.exit(main())
