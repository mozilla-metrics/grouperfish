package com.mozilla.grouperfish.base;

public class Assert {

	public static final String PREFIX = "[ASSERTION FAILED] ";

	public static void nonNull(Object... values) {
		int i = 0;
		for (Object value : values) {
			++i;
			if (value == null) {
				String message = String.format("%s Value %d/%d is null.", PREFIX, i, values.length);
				throw new IllegalArgumentException(message);
			}
		}
	}

	public static void check(boolean... values) {
		int i = 0;
		for (boolean value : values) {
			++i;
			if (!value) {
				String msg = String.format("%s Check %d/%d failed!", PREFIX, i, values.length);
				throw new IllegalArgumentException(msg);
			}
		}
	}

	public static void unreachable() {
		String message = String.format("%s Code should be unreachable!\n", PREFIX);
		throw new IllegalStateException(message);
	}

	public static void unreachable(String message, Object... objects) {
		String msg = String.format("%s Code should be unreachable: %s\n", PREFIX, String.format(message, objects));
		throw new IllegalStateException(msg);
	}

	/** Use this where java wants a return type T. Silly, really... */
	public static <T> T unreachable(Class<T> returnType) {
		String msg = String.format("%s Code should be unreachable!\n", PREFIX);
		throw new IllegalStateException(msg);
	}

	/** @see #unreachable(Class) */
	public static <T> T unreachable(Class<T> returnType, String message, Object... objects) {
		String msg = String.format("%s Code should be unreachable: %s\n", PREFIX, String.format(message, objects));
		throw new IllegalStateException(msg);
	}

	/** @see #unreachable(Class) */
	public static <T> T unreachable(Class<T> returnType, Exception problem) {
		String msg = String.format("%s Code should be unreachable\n", PREFIX);
		throw new IllegalStateException(msg, problem);
	}

}
