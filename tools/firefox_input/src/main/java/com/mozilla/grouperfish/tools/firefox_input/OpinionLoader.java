package com.mozilla.grouperfish.tools.firefox_input;

import com.mozilla.grouperfish.bootstrap.Grouperfish;
import com.mozilla.grouperfish.util.loader.DocumentLoader;

public class OpinionLoader {

    public static void main(final String[] arguments) {

        if (arguments.length > 2 || (arguments.length >= 1 && "--help".equals(arguments[0]))) {
            System.err.println("arguments: [BASE_URL] NAMESPACE");
            System.exit(1);
        }

        final String baseUrl;
        final String namespace;
        if (arguments.length == 2) {
            baseUrl = arguments[0];
            namespace = arguments[1];
        }
        else {
            baseUrl = "http://localhost:" + Grouperfish.DEFAULT_PORT;
            namespace = arguments[0];
        }

        new DocumentLoader(baseUrl, namespace).load(new OpinionStream(System.in));
    }

}
