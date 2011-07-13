package com.mozilla.grouperfish.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.ClusterAdapter;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Source;
import com.mozilla.grouperfish.input.StreamImporter;
import com.mozilla.grouperfish.jobs.Util;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;

import edu.emory.mathcs.backport.java.util.Collections;

public class Cli {

	public static final Logger log = LoggerFactory.getLogger(Cli.class);

	static private final String USAGE = "Usage: java -jar grouperfish.jar [--config PATH] \\\n"
			+ "              [import <ns> | export_clusters <ns> <ck> | help]\n"
			+ " import          read (opinion) data from stdin\n"
			+ " export_clusters export clusters to stdout\n"
			+ "   list          prints all documents of the given collection\n"
			+ "  job:*          start a hadoop job, such as a full cluster rebuild \n"
			+ "   help          print this message and exit";

	public Cli(Conf conf) {
		conf_ = conf;
	}

	static private void exit(String message, int status) {
		(status == 0 ? System.out : System.err).println(message);
		System.exit(status);
	}

	static private void exit(int status) {
		System.exit(status);
	}

	public void collectionInfo(String namespace, String collectionKey) {
		Assert.unreachable("Implementz me!");
	}

	public int load(String namespace, InputStream in) {
		final String CONF_KEY_PATTERNS = "input:import:collections:patterns";
		List<String> patterns = conf_.getList(String.class, CONF_KEY_PATTERNS);
		int[] counters = new StreamImporter(conf_, namespace, patterns).load(in);

		log.info("Counters: [used, discarded, docs, collections]: {}", Arrays.toString(counters));
		return 0;
	}

	public int exportClusters(String namespace, String collectionKey) {
		final CollectionRef ownerRef = new CollectionRef(namespace, collectionKey);
		final Collection collection;
		final Factory hbase = new Factory(conf_);

		final long lastRebuilt; {
			try {
				collection = hbase.source(Collection.class).get(ownerRef);
				final Long rebuilt = collection.get(Attribute.REBUILT);
				if (rebuilt == null) {
					log.warn("Cannot export clusters because collection {} / {} has not been rebuilt yet!",
							 ownerRef.namespace(), ownerRef.key());
					return 0;
				}
				lastRebuilt = Long.valueOf(rebuilt);
			} catch (IOException e) {
				log.warn("Collection {} / {} does not exist!", ownerRef.namespace(), ownerRef.key());
				return 1;
			}
		}

		log.info("Exporting clusters for collection {} / {}", ownerRef.namespace(), ownerRef.key());
		final Iterable<Cluster> clusters =
			new ClusterAdapter(hbase).all(ownerRef, lastRebuilt);

		final Source<Document> docLookup = hbase.source(Document.class);
		PrintStream out;
		try { out = new PrintStream(System.out, true, "UTF-8"); }
		catch (UnsupportedEncodingException e1) { return Assert.unreachable(Integer.class); }
		out.format("<!doctype html><html><head>");
		out.format("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />");
		out.format("<title>Clusters for %s</title>", StringUtils.escapeHTML(ownerRef.key()));
		out.format("<link rel='stylesheet' type='text/css' href='clusters.css' />");
		out.format("<h1>Collection <q>%s</q></h1>\n", StringUtils.escapeHTML(ownerRef.key()));


		List<Cluster> sorted = new ArrayList<Cluster>();
		for (final Cluster cluster : clusters) sorted.add(cluster);
		Collections.sort(sorted, new Comparator<Cluster>() {
			public int compare(Cluster o1, Cluster o2) {
				int s1 = o1.documents().size();
				int s2 = o2.documents().size();
				return Integer.signum(s2 - s1);
			}
		});

		int i = 0;
		for (final Cluster cluster : sorted) {
			++i;
			if ("Other Topics".equals(cluster.ref().label())) continue;
			out.format("<h2><q>%s</q> (%s messages)</h2>\n",
					   StringUtils.escapeHTML(cluster.ref().label()), cluster.documents().size());
			out.format("<ol>\n");
			List<DocumentRef> sortedDocs = new ArrayList<DocumentRef>(cluster.documents());
			{
				final Map<String, Double> scoreByDoc = new HashMap<String, Double>();
				int d = 0;
				for (final DocumentRef docRef : cluster.documents()) {
					scoreByDoc.put(docRef.id(), cluster.similarities().get(d));
					++d;
				}
				Collections.sort(sortedDocs, new Comparator<DocumentRef>() {
					public int compare(DocumentRef o1, DocumentRef o2) {
						return Integer.signum((int)(scoreByDoc.get(o1.id()) - scoreByDoc.get(o2.id())));
					}
				});
			}

			int j = 0; final int limit = 20;
			for (final DocumentRef docRef : sortedDocs) {
				try {
					j++;
					out.format("<li>%s</li>\n", StringUtils.escapeHTML(docLookup.get(docRef).text()));
					if (j == limit) break;
				} catch (final IOException e) {
					log.error("Failed to lookup document by id={} (owner: {} / {})", new Object[]{
							   docRef.id(), docRef.ownerRef().namespace(), docRef.ownerRef().key()
							  });
					e.printStackTrace();
					return 1;
				}
			}
			out.format("</ol>\n\n");
		}
		out.format("</body></html>");
		log.info("Exported {} clusters for collection {} / {} (size={}, rebuilt={})", new Object[]{
				     i, ownerRef.namespace(), ownerRef.key(),
				     collection.get(Attribute.SIZE), lastRebuilt
				 });

		return 0;
	}

	static public void main(final String[] args) {
		List<String> arguments = Arrays.asList(args);
		String configPath = null;
		int i = 0;
		{
			while (arguments.size() > i && arguments.get(i).startsWith("--")) {
				if ("--help".equals(arguments.get(i))) {
					exit(USAGE, 0);
				}
				if ("--config".equals(arguments.get(i))) {
					configPath = arguments.get(++i);
				} else {
					exit(USAGE, 1);
				}
				++i;
			}
			if (arguments.size() == i)
				exit(USAGE, 1);
		}
		final Conf conf = (new com.mozilla.grouperfish.conf.Factory()).conf(configPath);

		final String command = arguments.get(i);
		++i;
		final List<String> cmdArgs = arguments.subList(i, arguments.size());

		if ("help".equals(command)) {
			exit(USAGE, 0);
		}

		if ("import".equals(command) && cmdArgs.size() == 1) {
			new Cli(conf).load(cmdArgs.get(0), System.in);
		} else if ("list".equals(command) && cmdArgs.size() >= 2) {
			new Cli(conf).collectionInfo(cmdArgs.get(0), cmdArgs.get(1));
		} else if ("export_clusters".equals(command) && cmdArgs.size() >= 2) {
			new Cli(conf).exportClusters(cmdArgs.get(0), cmdArgs.get(1));
		} else if (command.startsWith("job:")) {
			exit(new Util(conf).run(command.substring(4), cmdArgs.toArray(new String[] {})));
		} else {
			exit(USAGE, 1);
		}
	}

	private final Conf conf_;

}
