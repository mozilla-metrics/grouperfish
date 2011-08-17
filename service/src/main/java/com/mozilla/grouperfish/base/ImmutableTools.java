package com.mozilla.grouperfish.base;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/** Inefficient "functional" maps. */
public class ImmutableTools {

    public static <K, V> Map<K, V> immutable(final Map<K, V> in) {
        return new ImmutableMap.Builder<K, V>().putAll(in).build();
    }

    public static <K, V> Map<K, V> put(final Map<K, V> in, final K key, final V value) {
        return new ImmutableMap.Builder<K, V>().putAll(in).put(key, value).build();
    }

    public static <K, V> Map<K, V> putAll(final Map<K, V> a, final Map<K, V> b) {
        return new ImmutableMap.Builder<K, V>().putAll(a).putAll(b).build();
    }

}
