.. _hacking:

========
Hacking
========


Prerequisites
-------------

First, make sure that you satisfy the requirements for running grouperfish
(:ref:`installation`).

Maven
    We are using Maven 3 for build and dependency management of several
    Grouperfish components.

JDK 6
    Java 6 Standard Edition should work fine.

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


Coding Style
------------

In general, consistency with existing surrounding code / module is more
important for a patch than adherence to these rules (local consistency over global consistency).

Java
    This project follows the default eclipse code style, except that 4 spaces
    are used for indention rather than ``TAB``.
    For java projects, maven is encouraged as the build-tool.

Python
    Follow `PEP 8`_

    .. _`PEP 8`: http://www.python.org/dev/peps/pep-0008/

Other
    Follow the default convention of the language you are using.
    When in doubt, indent using 4 spaces, limit width depending on language
    (preferably 80 columns).


Repository Layout
-----------------

``transforms``
    One sub-directory per self-contained transform.
    Code shared by several transforms can go into ``transforms/commons``.

``service``
    The REST service and the batch system.
    This must not contain any code or dependencies that is related to specific
    algorithms/transforms.

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
       webui/
          index.html
          ...
       ...
    transforms/
       coclustering/
          install*
          ...


The build tree
^^^^^^^^^^^^^^

Each ``install`` script will put its components into the ``build`` directory
under the main project. When a user unpacks a grouperfish distribution, she
will see the contents of this directory:

::

    build/
        bin/
            grouperfish*
        lib/
            grouperfish-service.jar
        transforms/
            coclustering/
                coclustering*
                ...
        tools/
            webui/
                index.html
                ...
            ...


