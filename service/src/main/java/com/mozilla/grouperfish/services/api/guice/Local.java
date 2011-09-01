package com.mozilla.grouperfish.services.api.guice;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * When a local resource is requested as a parameter, the client
 * will not try to share it. Examples: The local FileSystem, a
 * local memory grid instead of a HazelCast grid.
 *
 * Whether injectors can still pass shared resources depends on
 * the resource: A local FileSystem needs to be local, because
 * other local processes need to read from it. A 'local' grid
 * might simply be an optimization over a shared grid.
 */
@BindingAnnotation @Target(PARAMETER) @Retention(RUNTIME)
public @interface Local { }
