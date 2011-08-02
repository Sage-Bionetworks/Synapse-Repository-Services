# Unit tests for file download functions
# 
# Author: Matt Furia
###############################################################################

.setUp <- function(){
	.setCache("localSourceFile",tempfile())
	.setCache("cacheDir", tempdir())
	.setCache("localJpegFile", file.path(tempdir(), "plot.jpg"))
}

.tearDown <- function(){
	if(file.exists(.getCache('localSourceFile')))
		file.remove(.getCache('localSourceFile'))
	.deleteCache("localSourceFile")
	
	if(!is.null(.getCache('localDestFile')) && file.exists(.getCache('localDestFile')))
		file.remove(.getCache('localDestFile'))
	.deleteCache("localDestFile")

	if(file.exists(.getCache("cacheDir")))
		unlink(.getCache("cacheDir"))
	.deleteCache("cacheDir")
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

unitTestMd5Sum <- 
		function()
{
	## check that download happens when sourcefile is changed
	d <- diag(x=1, nrow=10, ncol = 10)
	write(d,file = .getCache("localSourceFile"),ncolumns=10, sep="\t")
	srcChecksum <- as.character(md5sum(.getCache("localSourceFile")))
	
	url <- paste("file://", .getCache("localSourceFile"), sep="")
	destFile <- synapseDownloadFile(url, cacheDir=.getCache("cacheDir"))
	
	destFileChecksum <- as.character(md5sum(destFile))
	checkEquals(srcChecksum, destFileChecksum)
	
	d <- diag(x=2, nrow=20, ncol = 20)
	write(d, file = .getCache("localSourceFile"), ncolumns = 20, sep="\t")
	srcChecksum <- as.character(md5sum(.getCache("localSourceFile")))
	
	## make sure that the 10x10 matrix has a different checksum that the 20x20 matrix
	checkTrue(destFileChecksum != srcChecksum)
		
	newDestFile <- synapseDownloadFile(url, cacheDir=.getCache("cacheDir"), checksum=srcChecksum)
	
	## check that the new and old destfiles have the same name
	checkTrue(destFile == newDestFile)
	
	## check that the new and old destFiles don't have the same checksum
	checkTrue(destFileChecksum != as.character(md5sum(newDestFile)))
	
	## check that the new destfile has the same checksum as the source file
	checkEquals(srcChecksum, as.character(md5sum(newDestFile)))
}


unitTestZippedDownload <- 
		function()
	
{
	
}

#unitTestDownoad <- 
#		function()
#{
#	## create a jpeg object
#	jpeg(.getCache("localJpegFile"))
#	plot(1:10, 1:10)
#	dev.off()
#	
#	## upload the jpeg object
#	
#	
#}
##
#unitTestInvalidSourceFile <- function(){
#	
#}
#
#unitTestInvalidDestDir <- function(){
#	
#}


