package com.mozilla.grouperfish.cli;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.bagheera.DocumentLoader;
import com.mozilla.grouperfish.input.OpinionStream;


public class Grouperfish {

    public static final String PROPERTIES_RESOURCE_NAME = "/grouperfish.properties";

    public static final Logger log = LoggerFactory.getLogger(Grouperfish.class);

	static private void exit(String message, int status) {
		(status == 0 ? System.out : System.err).println(message);
		System.exit(status);
	}

	static private void exit(int status) {
		System.exit(status);
	}

	public int load(String prefix, InputStream in) {
		int n = new DocumentLoader(prefix).load(new OpinionStream(in));
		log.info("Loaded {} documents (prefix: {})", n, prefix);
		return 0;
	}

	static public void main(final String[] args) {

	    final String USAGE = "Usage: java -jar grouperfish.jar [load PREFIX | help]\n"
            + "   load URL read input.mozilla.org tsv data from stdin to bagheera map at URL\n"
            + "   help     print this message and exit";

		final List<String> arguments = Arrays.asList(args);
		int i = 0;
		while (arguments.size() > i && arguments.get(i).startsWith("--")) {
		    if ("--help".equals(arguments.get(i))) {
		        exit(USAGE, 0);
		    }
		    exit(USAGE, 1);
		    ++i;
		}
		if (arguments.size() == i)
			exit(USAGE, 1);

		final String command = arguments.get(i);
		++i;
		final List<String> cmdArgs = arguments.subList(i, arguments.size());

		if ("help".equals(command)) {
			exit(USAGE, 0);
		}

		if ("load".equals(command) && cmdArgs.size() == 1) {
			exit(new Grouperfish().load(cmdArgs.get(0), System.in));
		} else {
			exit(USAGE, 1);
		}
	}

}
