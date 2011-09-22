.. _hacking:

========
Hacking
========


Prerequisites
-------------

First, make sure that you satisfy the requirements for running grouperfish
(:ref:`installation`).

Maven
    We are using Maven 3.0 for build and dependency management of several
    Grouperfish components.

JDK 6
    Java 6 Standard Edition should work fine.

Git & Mercurial
    To get the Grouperfish source and dependencies.

Sphinx
    For documentation.  Best installed by running

    ::

        easy_install Sphinx

The Source
    To obtain the (latest) source using **git**:

    ::

        > git clone git://github.com/mozilla-metrics/grouperfish.git
        > cd grouperfish
        > git checkout development


Building it:
------------

::

    > ./install             # Creates a build under ./build
    > ./install --package   # Creates grouperfish-$VERSION.tar.gz

When building, you might get Maven warnings due to expressions in the
``'version'`` field, which can be safely ignored.

Coding Style
------------

In general, consistency with existing surrounding code / the current module is
more important for a patch than adherence to the rules listed here (local
consistency wins over global consistency).

Wrap text (documentation, doc comments) and Python at 80 columns, everything
else (especially Java) at 120.

Java
    This project follows the default Eclipse code format, except that 4 spaces
    are used for indention rather than ``TAB``. Also, put
    ``else``/``catch``/``finally`` on a new line (much nicer diffs). Crank
    up the warnings for unused identifiers and dead code, they often point to
    real bugs.
    Help *readers* to reason about scope and side-effects:

    * Keep declarations and initializations together

    * Keep all declarations as local as possible.

    * Use ``final`` generously, especially for fields.

    * No ``static`` fields without ``final``.

    For Java projects (service, transforms, filters), *Maven* is encouraged as
    the build-tool (but not required). To edit  source files using Eclipse,
    the ``m2eclipse`` plugin can be used.

Python
    Follow `PEP 8`_

    .. _`PEP 8`: http://www.python.org/dev/peps/pep-0008/

Other
    Follow the default convention of the language you are using.
    When in doubt, indent using 4 spaces.



Repository Layout
-----------------

``transforms``
    One sub-directory per self-contained transform.
    Code shared by several transforms can go into ``transforms/commons``.

``service``
    The REST service and the batch system.
    This must not contain any code or any dependencies that are related to
    specific transforms.

``docs``
    Sphinx-style documentation.

``tools``
    One sub-directory per self-contained tool. These tools can be used by the
    transforms to convert data formats etc. All tools will be on the
    transforms' path.

``filters``
    One self-contained project folder per filter.
    Shared code goes to ``filters/commons``.

``integration-tests``
   A maven project for building and performing integration tests.
   We use `rest-assured`_ to talk to the REST interface from clients.

   .. _`rest-assured`: http://code.google.com/p/rest-assured/


Building
--------

The source tree
^^^^^^^^^^^^^^^

Each self-contained component (the batch service, each transform/tool/filter)
can have its own executable ``install`` script. Only components that do not
need build steps (such as static html tools) can work without such a script.
Each of these install scripts is in turn called by the main install script
when creating a grouperfish tarball.

::

    install*
    ...
    service/
        install*
        pom.xml
        ...
    tools/
        firefox_input/
            ...
        webui/
            index.html
            ...
        ...
    transforms/
        coclustering/
            install*
            ...


The Build Tree
^^^^^^^^^^^^^^

Each ``install`` script will put its components into the ``build`` directory
under the main project. When a user unpacks a grouperfish distribution, she
will see the contents of this directory:

Each component can have build results into ``data``, ``conf``, ``bin``. The
folder ``lib`` should be used where a component makes parts available to other
components (other binaries should go to the respective subfolder).

::

    build/
        bin/
            grouperfish*
        data/
            ...
        conf/
            ...
        lib/
            grouperfish-service.jar
            ...
        transforms/
            coclustering/
                coclustering*
                ...
        tools/
            firefox_input/
                ...
            webui/
                index.html
                ...
            ...


Components
----------

The Service Sub-Project
^^^^^^^^^^^^^^^^^^^^^^^

The ``service/`` folder in the source tree contains the REST and batch
system implementation. It is the code that is run when you "start"
Grouperfish, and which launches filters and transforms as needed.

The service is started using ``bin/grouperfish``. For development, the
alternative ``bin/littlefish`` is useful, which can be called directly from
the source tree (after an ``mvn compile`` or the equivalent eclipse build),
without packaging the service as a jar first.

It is organized into some basic shared packages, and three *modules* which
expose interfaces and components to be configured and replaced independent of
each other, for flexibility.

The shared packages contain:

``bootstrap``
    the entry point(s) to launch grouperfish

``base``
    shared general purpose helper code, e.g. for streams, immutable
    collections and JSON handling

``model``
    simple objects that represent data Grouperfish deals with

``util``
    special purpose utility classes, e.g. for import/export,
    TODO: move these to ``tools``


Service Modules
^^^^^^^^^^^^^^^

``services``
    Components that depend on the computing environment. By configuring these
    differently, users can chose alternative file systems, indexing or grid
    solutions can be integrated.
    Right now this flexibility is mostly used for mocking (testing).

``rest``
    The REST service is implemented as a couple of JAX-RS resources, managed
    by Jetty/Jersey. Other than the service itself (to be started/stopped),
    there is no functionality exposed api-wise.
    Most resources mainly encapsulate maps. The ``/run`` resource also
    interacts with the batch system.

``batch``
    The batch system implements scheduling and execution of tasks, and the
    preparation and cleanup for each task run.
    There are *handlers* for each stage of a task (fetch data, execute the
    transform, make results available). The *transform* objects implement the
    run itself: they manage child processes, or implement java-based
    algorithms directly.
    The *scheduling* is performed by a component that implements the
    ``BatchService`` interface. Usually one or more queues are used, but
    synchronous operation is also possible (for example in a command line
    version).


On Guice Usage
^^^^^^^^^^^^^^

Components from modules are instantiated using `Google Guice`_.
Each module has multiple packages ``….grouperfish.<module>.…``.
The ``….<module>.api`` package contains all interfaces of components that the
module offers. The ``….<module>.api.guice`` package has the Guice-specific
bindings (by implementing the Guice ``Module`` interface).
Launch Grouperfish with different bindings to customize or stub parts.

.. _`Google Guice`: http://code.google.com/p/google-guice/


Grouperfish uses *explicit dependency injection*: every class that needs a
service component simply takes a corresponding constructor argument, to be
provisioned on construction, without any Guice annotation. This means that
Guice imports are mostly used...

* where the application is configured (the bindings)

* where it is bootstrapped

* and in REST resources that are instantiated by `jersey-guice`__

.. __:
   http://jersey.java.net/nonav/apidocs/1.1.0-ea/contribs/jersey-guice/

