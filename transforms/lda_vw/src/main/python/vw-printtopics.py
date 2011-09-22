#!/usr/bin/python

# printtopics.py: Prints the words that are most prominent in a set of
# topics.
#
# Copyright (C) 2010  Matthew D. Hoffman
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import sys

def loadtxt(filename):
    data = []
    passed_header = False
    for line in open(filename).readlines():
        line = line.strip()
        if passed_header:
            data.append(map(float, line.split()))
        elif line.startswith('lda:'):
            passed_header = True
    data = zip(*data)   # transpose data
    return data


def main():
    """
    Displays topics fit by vw's LDA. The first column gives the
    (expected) most prominent words in the topics, the second column
    gives their (expected) relative prominence.
    """
    if len(sys.argv) != 3:
        print >>sys.stderr, "Usage: vw-printtopics.py vocab-file topic-score-file"
        sys.exit(1)
    vocab = str.split(file(sys.argv[1]).read())
    testlambda = loadtxt(sys.argv[2])

    for k in range(0, len(testlambda)):
        lambdak = testlambda[k]

        # pitch extra topic rows
        lambdak = lambdak[0:(len(vocab)-1)]

        # normalize row
        the_sum = sum(lambdak)
        lambdak = [val / the_sum for val in lambdak]

        # resort by normalized value
        temp = zip(lambdak, range(0, len(lambdak)))
        temp = sorted(temp, key = lambda x: x[0], reverse=True)
        print 'topic %d:' % (k)
        # feel free to change the "53" here to whatever fits your screen nicely.
        for i in range(0, 20):
            print '%20s  \t---\t  %.4f' % (vocab[temp[i][1]], temp[i][0])
        print

if __name__ == '__main__':
    main()
