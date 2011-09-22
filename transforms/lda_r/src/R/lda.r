library(lda)
library(RJSONIO)

# Example: R -f src/R/lda.r --no-save --slave --args input-firefox-5.0-issues-ldac.dat feature-index-en-10-0.7.txt 10 topics.dat topdocs.dat

args<-commandArgs(TRUE)

docs<-read.documents(args[1])
vocab<-read.vocab(args[2])
K<-as.integer(args[3])
alpha<-0.01
eta<-0.01
model<-lda.collapsed.gibbs.sampler(docs, K, vocab, 100, alpha, eta)
# Transposed for saving so we can read rows rather than columns
top_10_topic_words<-t(top.topic.words(model$topics, num.words = 10, by.score = TRUE))
top_20_docs_per_topic<-t(top.topic.documents(model$document_sums, num.documents=20, alpha))

e1<-sapply(1:ncol(top_10_topic_words),function(r) top_10_topic_words[,r],simplify=FALSE)
names(e1)<-0:(length(e1)-1)

e2<-sapply(1:ncol(top_20_docs_per_topic),function(r) top_20_docs_per_topic[,r],simplify=FALSE)
names(e2)<-0:(length(e2)-1)

e3<-lapply(model$assignments,function(r) {
  a0<-table(r)
  a1<-as.numeric(a0/length(r))
  names(a1)<-names(a0)
  return(a1)
})
names(e3) <- 1:length(e3)

json_doc_list <- list(TOP_FEATURES=e1, TOP_DOCS=e2, DOC_TOPICS=e3)
json_docs <- toJSON(json_doc_list)
writeLines(json_docs, "output.json")

#write.table(top_10_topic_words, file=args[4], quote=FALSE, row.names=FALSE, col.names=FALSE)
#write.table(top_20_docs_per_topic, file=args[5], quote=FALSE, row.names=FALSE, col.names=FALSE)
# Model assignments of topics per word per doc (post process in python)
#lapply(model$assignments, function(x) write.table(t(data.frame(x)), file="assignments.dat", append=TRUE, quote=FALSE, row.names=FALSE, col.names=FALSE))