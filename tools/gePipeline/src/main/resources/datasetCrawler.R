#!/usr/bin/env Rscript

source('./src/main/resources/synapseWorkflow.R')

maxDatasetSize <- as.numeric(getMaxDatasetSizeArg())


library(XML)
library(RCurl)

## gpl.ids <- read.table("ncbiGPLIDs",stringsAsFactors=FALSE)[,1]

base.url <- "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term="
tail.url <- "AND+gse[ETYP]&usehistory=y&usehistory=y"

gpl.ids <- c(
		## "GPL80" ,
		## "GPL570",
		"GPL96" ##,
## "GPL97",
## "GPL571",
## "GPL8300",
## "GPL201",
## "GPL1261",
## "GPL8321",
## "GPL339",
## "GPL81",
## "GPL1355",
## "GPL85",
## "GPL341",
## "GPL5188",
## "GPL6244",
## "GPL5175",
## "GPL3921",
## "GPL6246",
## "GPL6193",
## "GPL6096"
)

res <- list()
if (T) {
	for(i in 1:length(gpl.ids)) { 
		x <- gpl.ids[i]
		cat(x,"\t")
		url <- paste(base.url,x,'+',tail.url,sep="")
		obj <- getURL(url)
		tmp <- xmlInternalTreeParse(obj)
		queryKey <- xmlValue(xpathSApply(tmp, "/eSearchResult/QueryKey/text()")[[1]])
		webenv <- xmlValue(xpathSApply(tmp, "/eSearchResult/WebEnv/text()")[[1]])
		url <- paste("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gds&query_key=",queryKey,"&WebEnv=",webenv,sep="")
		obj2 <- getURL(url)
		x<-xmlInternalTreeParse(url)
		gseids<-paste('GSE',sapply(xpathApply(x, "/eSummaryResult/DocSum/Item[@Name='GSE']/text()"),xmlValue),sep="")
		pdat<-sapply(xpathApply(x, "/eSummaryResult/DocSum/Item[@Name='PDAT']/text()"), xmlValue)
		n_samples<-sapply(xpathApply(x, "/eSummaryResult/DocSum/Item[@Name='n_samples']/text()"), xmlValue)
		## suppFile<-sapply(xpathApply(x, "/eSummaryResult/DocSum/Item[@Name='suppFile']/text()"), xmlValue)
		names(pdat) <- gseids
		cat(length(pdat), "total studies\n")
		## The following fails because 'n_samples' is 882 long, but 'suppFile' is only 670 long (for GPL96)
		## pdatSub <- pdat[(as.numeric(n_samples)<=maxDatasetSize) & (1:length(suppFile) %in% grep('CEL', suppFile))]
		## cat(length(pdatSub), " studies less than ", maxDatasetSize, "and have CEL files.\n")
		pdatSub <- pdat[as.numeric(n_samples)<=maxDatasetSize]
		cat(length(pdatSub), " studies less than ", maxDatasetSize, ".\n")
		res <- c(res,as.list(pdatSub))
	}
} else {
	# for debugging only
	res$GSE15308<-"2011/10/05"
	res$GSE23603<-"2011/10/03"
}
res <- res[!duplicated(names(res))]

finishWorkflowTask(output=res)

