.. _transforms:

==========
Transforms
==========

Transforms are the heart of Grouperfish. 

**Transform Configurations**

The same transform (e.g. a clustering algorithm) might be used with different 
parameters to generate different results.  For this reason, the system 
contains a *transform configurations* for each result that should be 
generated.

Primarily, a transform configuration parameterizes its transform (e.g. for 
clustering, it might specify the desired number of clusters). It can also be 
used to tell the  Grouperfish batch system how to interact with a transform.
