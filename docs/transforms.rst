.. _transforms:

==========
Transforms
==========

.. I just cut and paste this from architecture

**Transform Configurations**

The same transform (e.g. a clustering algorithm) might be used with different parameters to generate different results.  For this reason, the system contains a *transform configurations* for each result that should be generated.

Primarily, a transform configuration serves to parameterize the associated transform (e.g. for clustering, it might specify the desired number of clusters). It can also be used to tell the  Grouperfish batch system how to interact with a transform.
