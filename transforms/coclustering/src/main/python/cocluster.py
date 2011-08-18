#/usr/bin/env python
"""
Eshwaran Vijaya Kumar
"""
import math
import os
import subprocess
import sys
try:
    import json
except ImportError:
    import simplejson as json

class CoClusterDriver:

    def __init__(self, **kwargs):
        self.cwd = kwargs.get('cwd')
        self.INPUT = kwargs.get('INPUT')
        self.PARAMETERS = kwargs.get('PARAMETERS')
        self.PREPROCESSOR = kwargs.get('PREPROCESSOR')
        self.STOPWORDS = kwargs.get('STOPWORDS')
        self.TEMP = kwargs.get('TEMP')
        self.An = kwargs.get('An')
        self.Z = kwargs.get('Z')
        self.PIGAn = kwargs.get('PIGAn')
        self.PIGPREPROCESSOR = kwargs.get('PIGPREPROCESSOR')
        self.PIGZ = kwargs.get('PIGZ')
        self.MAHOUTKMEANS = kwargs.get('MAHOUTKMEANS')
        self.MAHOUTSSVD = kwargs.get('MAHOUTSSVD')
        self.DISTANCEMEASURECLASSPATH = kwargs.get('DISTANCEMEASURECLASSPATH')
        self.MAHOUT_JAR = kwargs.get('MAHOUTJAR')
        self.L_HDFS_STORE = kwargs.get('L')
        self.GF_JAR = kwargs.get('GFJAR')
        self.NUM_RESULTS = str(kwargs.get('NUMRESULTS'))
        self.TOPDOCS = kwargs.get('TOPDOCS')
        self.TOPFEATURES = kwargs.get('TOPFEATURES')
        self.CLUSTERS = kwargs.get('CLUSTERS')
        self.CLUSTEREDPOINTS = kwargs.get('CLUSTEREDPOINTS')
        self.GFDISPLAY = kwargs.get('GFDISPLAY')
        self.RESULTS = kwargs.get('RESULTS')
        self.TAGS = kwargs.get('TAGS')


    def compute_An(self):
        curr_cmd = ('pig -param_file ' + self.An + ' ' +\
                    self.PIGAn).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Pig Job: ' + self.PIGAn + ' failed')

    def compute_KMeans(self):
        self.DELTA = self.transform_params["KMEANS_DELTA"]
        self.N = self.transform_params["KMEANS_NUM_ITERATIONS"]
        self.DISTANCEMEASURE = self.DISTANCEMEASURECLASSPATH + '.' +\
                                        self.transform_params["KMEANS_DISTANCE"]
        curr_cmd = ('hadoop jar ' + self.MAHOUT_JAR +\
                    ' ' + self.MAHOUTKMEANS + ' -i '+\
                    self.cwd + '/' + self.TEMP + '/Z ' +\
                    ' -o ' + self.cwd + '/' + self.TEMP + '/kmeans/out ' +\
                    ' -c ' + self.cwd + '/' + self.TEMP + '/kmeans/rc' +\
                    ' -k ' + self.K + ' -x ' + self.N +\
                    ' -dm ' + self.DISTANCEMEASURE + ' -cd ' + self.DELTA +\
                    ' -ow -cl').split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Hadoop Job: ' + self.MAHOUTKMEANS + ' failed')

    def compute_SSVD(self):
        self.BLOCK_HEIGHT = self.transform_params["SSVD_BLOCK_HEIGHT"]
        #TODO Once SSVD jars are modified. set --reduceTasks to value in JSON
        curr_cmd = ('hadoop jar ' + self.MAHOUT_JAR +\
                    ' ' + self.MAHOUTSSVD + ' --input '+\
                    self.cwd + '/' + self.TEMP + '/' + 'An/part*' +\
                    ' --output ' + self.cwd + '/' + self.TEMP + '/SVD' +\
                    ' --tempDir ' + self.cwd + '/' + self.TEMP + '/SVDtemp' +\
                    ' -k ' + self.L + ' -p ' + self.OVERSHOOT +\
                    ' -r ' + self.BLOCK_HEIGHT + ' --reduceTasks ' +\
                    '1').split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Hadoop Job: ' + self.MAHOUTSSVD + ' failed')

    def compute_Z(self):
        self.save_L()
        curr_cmd = ('pig -param_file ' + self.Z + ' ' +\
                    self.PIGZ).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Pig Job: ' + self.PIGZ + ' failed')

    def generate_Anparams(self):
        try:
            f = open(os.getcwd() + '/' + self.An,'w')
            f.write('TEMP = ' + self.cwd + '/' + self.TEMP + '\n')
            for k,v in self.map_params.items():
                f.write(k + ' = ' + v + '\n')
        except IOError:
            sys.exit('Can\'t write in local directory ' + os.getcwd())
        f.close()

    def generate_Zparams(self):
        f = open(os.getcwd() + '/' + self.Z,'w')
        f.write('TEMP = ' + self.cwd + '/' + self.TEMP + '\n')
        for k,v in self.map_params.items():
            f.write(k + ' = ' + v + '\n')
        f.close()

    def generate_preprocessorparams(self):
        f = open(os.getcwd() + '/' + self.PREPROCESSOR,'w')
        for k,v in self.text_params.items():
            if k != 'STOPWORDS':
                f.write(k + ' = ' + v + '\n')
        for k,v in self.map_params.items():
            f.write(k + ' = ' + v + '\n')
        f.write('TEMP = ' + self.cwd + '/' + self.TEMP + '\n')
        f.write('INPUT = ' + self.cwd + '/' + self.INPUT + '\n' )
        f.write('STOPWORDS = ' + self.cwd + '/' + self.TEMP + '/' +\
                self.STOPWORDS + '\n')
        f.close()

    def generate_stopwords(self):
        # Write stopwords
        f = open(os.getcwd() + '/' + self.STOPWORDS,'w')
        for s in self.text_params["STOPWORDS"]:
            f.write(unicode(s + '\n').encode("utf-8"))
        f.close()
        curr_cmd = ('hadoop fs -put ' + self.STOPWORDS + ' ' +\
                                             self.cwd + '/' + self.TEMP\
                        + '/.').split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit(self.STOPWORDS + ' could not be stored on HDFS')

    def parse_parameters(self):
        # Store PARAMETERS file locally
        curr_cmd = ('hadoop fs -get ' + self.PARAMETERS +' .').split()
        curr_process = subprocess.Popen(curr_cmd)
        try:
            f = open(os.getcwd() + '/' + self.PARAMETERS,'r')
            parameters = json.loads(f.read())
            f.close()
        except IOError:
            sys.exit('couldn\'t read from local copy of '+self.PARAMETERS)
        self.text_params = parameters['text']
        self.map_params = parameters['mapreduce']
        self.transform_params = parameters['transform']

    def preprocess_data(self):
        curr_cmd = ('pig -param_file ' + self.PREPROCESSOR + ' ' +\
                    self.PIGPREPROCESSOR).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Pig Job: ' + self.PIGPREPROCESSOR + ' failed')

    def run(self):
        self.validate_input()
        self.parse_parameters()
        # Creating temp directory
        curr_cmd = ('hadoop fs -test -d ' + self.cwd + '/' + self.TEMP).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            curr_cmd = ('hadoop fs -mkdir ' + self.cwd + '/' + self.TEMP).\
                    split()
            curr_process = subprocess.Popen(curr_cmd)
            curr_process.wait()
            if curr_process.returncode != 0:
                sys.exit(self.TEMP + ' could not be created on HDFS')
        self.generate_preprocessorparams()
        self.generate_stopwords()
        self.preprocess_data()
        self.generate_Anparams()
        self.compute_An()
        self.validate_k()
        self.compute_SSVD()
        self.generate_Zparams()
        self.compute_Z()
        self.compute_KMeans()
        self.write_output()

    def save_L(self):
        try:
            f = open(self.L_HDFS_STORE,'w')
            f.write(str(self.L))
        except IOError:
            sys.exit('Can\'t write in local directory ' + os.getcwd())
        f.close()
        curr_cmd = ('hadoop fs -put ' + self.L_HDFS_STORE + ' ' +\
                                             self.cwd + '/' + self.TEMP\
                        + '/.').split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit(self.L_HDFS_STORE + ' could not be stored on HDFS')

    def validate_input(self):
         # Test if directory exists
        curr_cmd = ('hadoop fs -test -d ' + self.cwd).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('HDFS directory passed as argument is not valid')
        # Test if input file exists.
        curr_cmd = ('hadoop fs -test -e ' + self.cwd+'/' + self.INPUT).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit(self.INPUT+' not found')
        # Test if parameters.json file exists
        curr_cmd = ('hadoop fs -test -e '+self.cwd+'/'+self.PARAMETERS).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit(self.PARAMETERS+' not found')
        # Test local files and external libraries
        try:
            f = open(self.MAHOUT_JAR)
        except IOError:
            sys.exit(self.MAHOUT_JAR + ' cannot be found.')
        if f:
            f.close()
        try:
            f = open(self.PIGAn)
        except IOError:
            sys.exit(self.PIGAn + ' cannot be found.')
        if f:
            f.close()
        try:
            f = open(self.PIGPREPROCESSOR)
        except IOError:
            sys.exit(self.PIGPREPROCESSOR + ' cannot be found.')
        if f:
            f.close()
        try:
            f = open(self.PIGZ)
        except IOError:
            sys.exit(self.PIGZ + ' cannot be found.')
        if f:
            f.close()

    def validate_k(self):
        # Extract relevant values from Json.
        self.K = int(self.transform_params["KMEANS_NUM_CLUSTERS"])
        self.MULTIPLIER = int(self.transform_params["SSVD_MULTIPLIER"])
        self.L = int(math.ceil(math.log(self.K,2)))
        # Load matrix dimensions
        curr_cmd = ('hadoop fs -cat ' + self.cwd + '/' + self.TEMP +\
                    '/ndocs/part*').split()
        curr_process = subprocess.Popen(curr_cmd, stdout = subprocess.PIPE)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Possible problem in ' + self.PIGAn)
        self.m = int((curr_process.communicate()[0]).strip())
        curr_cmd = ('hadoop fs -cat ' + self.cwd + '/' + self.TEMP +\
                    '/nfeatures' + '/part*').split()
        curr_process = subprocess.Popen(curr_cmd, stdout = subprocess.PIPE)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Possible problem in' + self.PIGAn)
        self.n = int((curr_process.communicate()[0]).strip())
        self.OVERSHOOT = self.L * self.MULTIPLIER
        if (self.L + self.OVERSHOOT) > min(self.m, self.n):
            sys.exit('Request for larger k than is possible to cluster for this\
                 configuration')
        self.K = str(self.K)
        self.MULTIPLIER = str(self.MULTIPLIER)
        self.L = str(self.L)
        self.OVERSHOOT = str(self.OVERSHOOT)

    def write_output(self):
        # Extract relevant cluster to write output from.
        curr_cmd = ('hadoop fs -ls ' + self.cwd + '/' + self.TEMP +\
                    '/kmeans/out/').split()
        curr_process = subprocess.Popen(curr_cmd, stdout = subprocess.PIPE)
        curr_process.wait()
        col_id = self.text_params['DOC_COL_ID']
        text_id = self.text_params['TEXT_COL_ID']
        if curr_process.returncode != 0:
            sys.exit('Cannot find all versions of '+self.CLUSTERS)
        clusterlist = curr_process.communicate()[0]
        clusterlist = clusterlist.split('\n')
        clusterlist = [c for c in clusterlist if c.find(self.CLUSTERS) != -1]
        temp = [t.rpartition(self.CLUSTERS) for t in clusterlist]
        cluster_it_ids = []
        for t in temp:
            cluster_it_ids.append(int(t[2]))
        max_it_id = max(cluster_it_ids)
        self.CLUSTERS = self.CLUSTERS + str(max_it_id)
        # Execute display printer.
        curr_cmd = ('hadoop jar ' + os.getcwd() + '/' + self.GF_JAR + ' ' + \
                    self.GFDISPLAY + ' ' +  self.cwd + '/' + self.TEMP +\
                    '/kmeans/out/' + self.CLUSTEREDPOINTS + ' ' + self.cwd + '/'\
                    + self.TEMP + '/kmeans/out/' + self.CLUSTERS + ' ' +\
                    self.cwd + '/' + self.TEMP + '/doc_map' + ' ' + self.cwd +\
                    '/' + self.TEMP + '/feature_map' + ' ' + self.cwd + '/' +\
                    self.INPUT + ' ' +  self.cwd + '/' + self.TAGS + ' ' +\
                    self.cwd + '/' + self.RESULTS + ' ' + self.TOPDOCS + ' ' +
                    self.TOPFEATURES + ' ' + self.NUM_RESULTS + ' ' + col_id +\
                    ' ' + text_id).split()
        curr_process = subprocess.Popen(curr_cmd)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit('Cannot write output')


"""
    def generate_tags(self):
        curr_pig = ['pig',self.pig_gen_tags]
        curr_process = subprocess.Popen(curr_pig)
        curr_process.wait()
        if curr_process.returncode != 0:
            sys.exit(1)
        curr_hdfs = ['hadoop','fs','-text',self.cwd+'/'+'tags'+'/part*']
        curr_process = subprocess.Popen(curr_hdfs, stdout = subprocess.PIPE)
        stdout = curr_process.communicate()[0]
        tags = {}
        for currline in stdout.split('\n'):
            currline = currline.split('\t')
            print currline
            tags[currline[0]] = currline[1]
        f = open(os.getcwd()+'/tags.json','w')
        f.write(json.dumps(tags))

"""
def main():
    """ Main entry point to script to perform coclustering.
    """
    try:
        cwd = sys.argv[1]
    except IndexError:
        return 'HDFS current working directory required'
    INPUT = 'input.tsv'
    PARAMETERS = 'parameters.json'
    PREPROCESSOR = 'preprocessor.param'
    RESULTS = 'results.json'
    TAGS = 'tags.json'
    An = 'An.param'
    Z = 'Z.param'
    STOPWORDS = 'stopwords'
    TEMP = 'temp'
    PIGPREPROCESSOR = 'co_cluster_preprocessor.pig'
    PIGAn = 'co_cluster_normalized_matrix_generator.pig'
    PIGZ = 'co_cluster_Z_generator.pig'
    MAHOUT_JAR = '/usr/lib/mahout/mahout-core-0.5-job.jar'
    GROUPERFISH_JAR = 'grouperfish-transforms-coclustering-0.3-SNAPSHOT-job.jar'
    MAHOUTSSVD = 'org.apache.mahout.driver.MahoutDriver ssvd'
    MAHOUTKMEANS = 'org.apache.mahout.driver.MahoutDriver kmeans'
    MAHOUT_DISTANCE_MEASURE_CLASSPATH = 'org.apache.mahout.common.distance'
    GF_DISPLAY = 'com.mozilla.grouperfish.transforms.coclustering' +\
                                            '.display.WriteCoClusteringOutput'
    MAHOUTKMEANS_CLUSTEREDPOINTS = 'clusteredPoints'
    MAHOUTKMEANS_CLUSTERS = 'clusters-'
    RESULTS_JSON_TOP_DOCS = 'TOP_DOCS'
    RESULTS_JSON_TOP_FEATURES = 'TOP_FEATURES'
    NUM_RESULTS = 10
    L = 'l'
    coclusterer = CoClusterDriver(cwd = cwd, INPUT = INPUT, PARAMETERS =\
                                  PARAMETERS, PREPROCESSOR = PREPROCESSOR,\
                                 STOPWORDS = STOPWORDS, TEMP = TEMP, An = An,\
                                  Z = Z,PIGPREPROCESSOR = PIGPREPROCESSOR,\
                                  PIGAn = PIGAn, PIGZ = PIGZ, MAHOUTJAR =\
                                  MAHOUT_JAR, MAHOUTSSVD = MAHOUTSSVD,\
                                  MAHOUTKMEANS = MAHOUTKMEANS, L = L,\
                                  DISTANCEMEASURECLASSPATH =\
                                  MAHOUT_DISTANCE_MEASURE_CLASSPATH, GFDISPLAY\
                                  = GF_DISPLAY, CLUSTEREDPOINTS =\
                                  MAHOUTKMEANS_CLUSTEREDPOINTS, CLUSTERS =\
                                  MAHOUTKMEANS_CLUSTERS, TOPDOCS =\
                                  RESULTS_JSON_TOP_DOCS, TOPFEATURES =\
                                  RESULTS_JSON_TOP_FEATURES, NUMRESULTS =\
                                  NUM_RESULTS, GFJAR = GROUPERFISH_JAR,\
                                 RESULTS = RESULTS, TAGS = TAGS)
    coclusterer.run()

if __name__ == "__main__":
    sys.exit(main())

