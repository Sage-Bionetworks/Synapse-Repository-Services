system("perl getGSEs.pl")
all.gses <- read.table("all.GSEs.txt",sep="\t",header=TRUE,stringsAsFactors=FALSE,row.names=1)

output <- list()
lapply(1:nrow(all.gses), function(i) {  
	b <- list(gpl     =all.gses[i,1],
						lastUpdate    =all.gses[i,2],
  					species =all.gses[i,3],
						summary =all.gses[i,4],
						hasCelFile=all.gses[i,5],
						n_sample=all.gses[i,6])
  b
}) -> output

names(output) <- rownames(all.gses)


