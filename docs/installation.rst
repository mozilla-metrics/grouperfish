.. _installation:

============
Installation
============


Prerequisites
-------------

These are the requirements to run Grouperfish.
For development, see :ref:`hacking`.

* A machine running **Linux / Unix** (supporting Windows currently not planned).
  (we are using Red Hat 5.2 and -- for development -- Mac OS X 10.6+).

* **JRE 6** or higher

* **Python 2.6** or higher (*not* tested with 3.x)

* **NumPy 1.6** or higher

* **ElasticSearch 0.17.6** or above

* **Hadoop, HBase and Pig** from the Cloudera distribution (cdh3u0)

The daemons for ElasticSearch/Hadoop/HBase do not need to be running on the
same machines as Grouperfish. For Hadoop/HBase you will need to make sure that
the configuration is on your classpath (easiest with a local installation).


Prepare your installation
-------------------------

* Obtain a grouperfish tarball[*] and unpack it into a directory of your choice.

   tar xzf grouperfish-0.1.tar
   cd grouperfish-0.1

* Under ``config``, modify the ``elasticsearch.yml`` so that Grouperfish will
  be able to discover your cluster.

* In the ``hazelcast.xml``, have a look at ``<network>`` section.
  If your network does not support multicast based discovery, make changes
  as described in the `Hazelcast documentation`_:

.. _`Hazelcast documentation`:
   http://www.hazelcast.com/docs/1.9.4/manual/multi_html/ch09.html

[*] right now, the only way to do so is to build it from source.
    See :ref:`hacking`.


Launch the daemon
-----------------

To run grouperfish (currently, no service wrapper is available):

    ./bin/grouperfish

Grouperfish will be listening on port 61732
(mnemonic: ``FISH = 0xF124 = 61732``).
