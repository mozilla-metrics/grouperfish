package com.mozilla.grouperfish.services.api.guice;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;


/**
 * When a shared resource is requested as a parameter, the client
 * intends to share the service with other cluster members. Modules
 * should provide appropriate services (Hadoop FS, HazelCast map...).
 *
 * Shared is the default.
 *
 * Injection might still pass a local resource, but should do so
 * only if the modus operandi is guaranteed to be standalone: Here
 * local/shared makes no difference. Examples for this are
 * testing/development setups.
 */
@BindingAnnotation @Target(PARAMETER) @Retention(RUNTIME)
public @interface Shared { }
