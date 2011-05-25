package com.mozilla.grouperfish.hbase.keys;

/** Simplest possible keys for best debugging and worst hotspotting. */
public class SimpleKeys extends Keys {

	@Override
	protected String document(String ns, String ck, String docId) {
		if (docId == null)
			docId = "";
		int size = ns.length() + 1 + ck.length() + 1 + docId.length();

		return new StringBuilder(size).append(ns).append('/').append(ck).append('/').append(docId).toString();
	}

	@Override
	protected String cluster(String ns, String ck, long rebuildTS, String label) {
		String ts = Long.valueOf(rebuildTS).toString();
		final int size = "DEFAULT/".length() + ns.length() + 1 + ck.length() + 1 + ts.length() + 1 + ts.length();

		return new StringBuilder(size).append("DEFAULT/").append(ns).append('/').append(ck).append('/').append(ts)
				.append('/').append(label).toString();
	}

	@Override
	protected String collection(String ns, String ck) {
		final int size = ns.length() + 1 + ck.length();
		return new StringBuilder(size).append(ns).append('/').append(ck).toString();
	}

}
