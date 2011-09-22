.. _installation:

============
Installation
============


Prerequisites
-------------

These are the requirements to run Grouperfish.
For development, see :ref:`hacking`.

* A machine running a **Unix-style OS** (such as *Linux*).

  Support for windows currently not planned (and probably not easy to add).

  So far, we have been using Red Hat 5.2
  and -- for development -- Mac OS X 10.6+.

* **JRE 6** or higher

* **Python 2.6** or higher (*not* tested with 3.x)

* **ElasticSearch 0.17.6**

The ElasticSearch cluster does not need to be running on the same machines as
Grouperfish. For Hadoop/HBase you will need to make sure that the
configuration is on your classpath (easiest with a local installation).


Prepare your installation
-------------------------

* Obtain a grouperfish tarball [#]_ and unpack it into a directory of your choice.

  ::

      > tar xzf grouperfish-0.1.tar

      > cd grouperfish-0.1

* Under ``config``, modify the ``elasticsearch.yml`` and
  ``elasticsearch_hc.yml`` so that Grouperfish will be able to discover your
  cluster.
  **Advanced:** You can modify the ``elasticsearch.yml`` to make
  each Grouperfish instance run its own ElasticSearch data node. By default,
  Grouperfish depends on joining an existing cluster though. Refer to the
  `ElasticSearch configuration documentation`_ for details.

.. _`ElasticSearch configuration documentation`:
   http://www.elasticsearch.org/guide/reference/setup/configuration.html

* In the ``hazelcast.xml``, have a look at ``<network>`` section.
  If your network does not support multicast based discovery, make changes
  as described in the `Hazelcast documentation`_.

.. _`Hazelcast documentation`:
   http://www.hazelcast.com/docs/1.9.4/manual/multi_html/ch09.html

.. [#] right now, the only way is to build it from source. See :ref:`hacking`.


Launch the daemon
-----------------

To run grouperfish (currently, no service wrapper is available):

::

    grouperfish-0.1> ./bin/grouperfish -f

Grouperfish will be listening on port 61732
(mnemonic: ``FISH =  0xF124 = 61732``).

You can safely ignore the logback warning (which will only appear with ``-f``
given). It is due to an `error`_ in logback.

.. _error: http://jira.qos.ch/browse/LBCORE-198

Omit the ``-f`` to run grouperfish as a background process, detached from your
shell. You can use ``jps`` to determine the process id.
