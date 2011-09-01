import json, sys

from textcluster import Corpus


def process(inStream, outStream,
            fields={"id": "id", "text": "text"},
            limits={"clusters": 10, "top_documents": 10}):
    all = {}

    text_field = fields["text"]
    key_field = fields["id"]
    max_clusters = limits["clusters"]
    max_top_docs = limits["top_documents"]

    c = Corpus()
    for line in inStream:
        data = line.split('\t', 1)[1]
        doc = json.loads(data.decode("utf8"))
        key = doc[key_field]
        all[key] = doc
        text = c.add((key, doc[text_field]), key=key)

    clusters = c.cluster()
    results = []
    for c in clusters[:max_clusters]:
        tophits = [c.primary]
        tophits += [hit["object"] for hit in c.similars[:max_top_docs-1]]
        topdocs = []
        for (key, text) in tophits:
            topdocs.append(all[key])
        results.append({"top_documents": topdocs})

    json.dump({"clusters": results}, outStream)


def main(args):
    work_dir = args[1]
    parameters = json.load(open("%s/parameters.json" % work_dir))
    with open("%s/input.json.tsv" % work_dir) as inFile:
        with open("%s/output/results.json" % work_dir, "w+") as outFile:
            process(inFile, outFile, **parameters)

if __name__ == "__main__":
    main(sys.argv)
