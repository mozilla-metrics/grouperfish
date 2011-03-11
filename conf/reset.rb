#! hbase shell

# erases the schema - everything is lost!

disable 'clusters'
drop 'clusters'

disable 'collections'
drop 'collections'

disable 'documents'
drop 'documents'
