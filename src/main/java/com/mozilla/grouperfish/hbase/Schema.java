package com.mozilla.grouperfish.hbase;

import org.apache.hadoop.hbase.util.Bytes;

/**
 * Helps to address table names, column families and column qualifiers without
 * duplicating their string / byte[] literals too much (DRY pinciple).
 *
 * This <em>static</em> collection of identifiers gives some compile time safety
 * when referencing database items. It is not to be confused with the model
 * classes.
 */
public abstract class Schema {

	public static interface Documents {

		public static final String TABLE = table(Documents.class);

		public static enum Main {
			NAMESPACE, COLLECTION_KEY, ID, TEXT, MEMBER_OF;

			public byte[] qualifier = asQualifier(this);
			public static final byte[] FAMILY = family(Main.class);
		}

		public static enum Processing {
			ID, VECTOR_IDF, VECTOR_TFIDF;

			public byte[] qualifier = asQualifier(this);
			public static final byte[] FAMILY = family(Processing.class);
		}
	}

	public static interface Collections {

		public static final String TABLE = table(Collections.class);

		public static enum Main {
			NAMESPACE, KEY, SIZE, MODIFIED;

			/** These qualifiers are used per clustering configuration. */
			public static enum Configuration {
				REBUILT, PROCESSED;

				/**
				 * Produces a configuration-specific qualifier.
				 *
				 * @param configuration
				 *            Name of the configuration the attribute is of.
				 * @return A column qualifier for use with the HBase api.
				 */
				public byte[] qualifier(final String configuration) {
					return Bytes.toBytes(configuration + ':' + this.name().toLowerCase());
				}
			}

			public byte[] qualifier = asQualifier(this);
			public static final byte[] FAMILY = family(Main.class);
		}

		public static enum Processing {
			DICTIONARY;

			public byte[] qualifier = asQualifier(this);
			public static final byte[] FAMILY = family(Processing.class);
		}
	}

	public static interface Clusters {

		public static final String TABLE = table(Clusters.class);

		public static enum Main {
			NAMESPACE, KEY, TIMESTAMP, LABEL, SIZE;

			public byte[] qualifier = asQualifier(this);
			public static final byte[] FAMILY = family(Main.class);
		}

		public static enum Documents {
			; // The document IDs of the cluster members are used.
			public static final byte[] FAMILY = family(Documents.class);
		}

	}

	private static byte[] family(Class<?> clazz) {
		return Bytes.toBytes(clazz.getSimpleName().toLowerCase());
	}

	private static byte[] asQualifier(Enum<?> column) {
		return Bytes.toBytes(column.name().toLowerCase());
	}

	private static String table(Class<?> clazz) {
		return clazz.getSimpleName().toLowerCase();
	}

}
