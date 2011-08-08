.. _hacking:

========
Hacking
========

Code Style
----------

Java
    This project follows the default eclipse code style, except that 4 spaces
    are used for indention rather than ``TAB``.
    Width limit is 120 columns. For the transforms, maven is encouraged as the
    build-tool.

Python
    Follow `PEP 8`_
    .. _`PEP 8`: http://www.python.org/dev/peps/pep-0008/

Other
    Follow the default convention of the language you are using.
    When in doubt, indent using 4 spaces, limit width depending on language
    (preferably 80 or 120 columns).


Repository Layout
-----------------

``core``
    The REST service and the batch system.
    This must not contain any code or dependencies that is related to specific
    algorithms/transforms.

``filters``
    One self-contained project folder per filter. Shared code goes to
    ``filters/commons``.

``transforms``
    One sub-directory per self-contained transform.
    Code shared by several transforms goes to ``transforms/commons``.

``docs``
    Sphinx-style documentation.

``data``
    Pure data that might be used by filters and/or transforms (e.g. static
    dictionaries).

``admin``
    The HTML admin frontend.

``tools``
    One sub-directory per self-contained tool. These tools can be used by the
    transforms to convert data formats etc. All tools will be on the
    transforms' path.
