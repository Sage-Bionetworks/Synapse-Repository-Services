# Unit tests for file download functions
# 
# Author: Matt Furia
###############################################################################

.setUp <- function(){
	.setCache("localSourceFile",tempfile())
}

.tearDown <- function(){
	if(file.exists(.getCache('localSourceFile')))
		file.remove(.getCache('localSourceFile'))
	.deleteCache("localSourceFile")
	if(!is.null(.getCache('localDestFile')) && file.exists(.getCache('localDestFile')))
		file.remove(.getCache('localDestFile'))
}

#unitTestBigDownload <- function(){
#	d <- matrix(nrow=1000, ncol=1000, data=1)
#	for(i in 1:100){
#		write(d,file = .getCache("localSourceFile"),ncolumns=1000, sep="\t", append=TRUE)
#	}
#	sourceChecksum <- md5sum(.getCache("localSourceFile"))
#	.setCache("destFile", synapseDownloadFile(url= paste("file://", .getCache("localSourceFile"), sep="")))
#	destChecksum <- md5sum(.getCache("destFile"))
#	checkEquals(as.character(sourceChecksum), as.character(destChecksum))
#}

unitTestLocalFileDownload <- function(){
	d <- matrix(nrow=100, ncol=100, data=1)
	write(d,file = .getCache("localSourceFile"),ncolumns=100, sep="\t")
	sourceChecksum <- md5sum(.getCache("localSourceFile"))
	.setCache("destFile", synapseDownloadFile(url= paste("file://", .getCache("localSourceFile"), sep="")))
	destChecksum <- md5sum(.getCache("destFile"))
	if(file.exists(.getCache('destFile')))
		file.remove(.getCache('destFile'))
	checkEquals(as.character(sourceChecksum), as.character(destChecksum))
}
## local file uploads are not supported with current synapseUploadFile implementation
unitTestLocalFileUpload <- function(){
	url <- paste("file://", tempfile(), sep="")
	d <- matrix(nrow=100, ncol=100, data=1)
	write(d,file = .getCache("localSourceFile"),ncolumns=100, sep="\t")
	sourceChecksum <- md5sum(.getCache("localSourceFile"))
	parsedUrl <- .ParsedUrl(url)
	.setCache("localDestFile", parsedUrl@path)
	synapseUploadFile(url=url, srcfile = .getCache("localSourceFile"), checksum=sourceChecksum)
	checkEquals(as.character(md5sum(.getCache('localSourceFile'))), as.character(md5sum(.getCache('localDestFile'))))
	
	## clean up and try again using curlReader function
	file.remove(.getCache("localDestFile"))
	
	.curlReaderUpload(url, .getCache("localSourceFile"))
	checkEquals(as.character(md5sum(.getCache('localSourceFile'))), as.character(md5sum(.getCache('localDestFile'))))
}

#unitTestMd5Sum <- function(){
#	## check that download happens when sourcefile is changed
#	
#	## check that download happens regardless of checksum when none is provided
#}
#
#unitTestInvalidSourceFile <- function(){
#	
#}
#
#unitTestInvalidDestDir <- function(){
#	
#}


