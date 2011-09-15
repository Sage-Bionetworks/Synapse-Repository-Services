# Unit tests for file download functions
# 
# Author: Matt Furia
###############################################################################

.setUp <- function(){
	synapseClient:::.setCache("localSourceFile",tempfile())
	synapseClient:::.setCache("cacheDir", file.path(tempdir(), ".synapseCache"))
	synapseClient:::.setCache("localJpegFile", file.path(tempdir(), "plot.jpg"))
}

.tearDown <- function(){
	if(file.exists(synapseClient:::.getCache('localSourceFile')))
		file.remove(synapseClient:::.getCache('localSourceFile'))
	synapseClient:::.deleteCache("localSourceFile")
	
	if(!is.null(synapseClient:::.getCache('localDestFile')) && file.exists(synapseClient:::.getCache('localDestFile')))
		file.remove(synapseClient:::.getCache('localDestFile'))
	synapseClient:::.deleteCache("localDestFile")

	if(file.exists(synapseClient:::.getCache("cacheDir")))
		unlink(synapseClient:::.getCache("cacheDir"), recursive=TRUE)
	synapseClient:::.deleteCache("cacheDir")
}

#unitTestBigDownload <- function(){
#	d <- matrix(nrow=1000, ncol=1000, data=1)
#	for(i in 1:100){
#		write(d,file = synapseClient:::.getCache("localSourceFile"),ncolumns=1000, sep="\t", append=TRUE)
#	}
#	sourceChecksum <- tools::md5sum(synapseClient:::.getCache("localSourceFile"))
#	synapseClient:::.setCache("destFile", synapseClient:::synapseDownloadFile(url= paste("file://", synapseClient:::.getCache("localSourceFile"), sep="")))
#	destChecksum <- tools::md5sum(synapseClient:::.getCache("destFile"))
#	checkEquals(as.character(sourceChecksum), as.character(destChecksum))
#}

unitTestLocalFileDownload <- function(){
	d <- matrix(nrow=100, ncol=100, data=1)
	save(d,file = synapseClient:::.getCache("localSourceFile"))
	sourceChecksum <- as.character(tools::md5sum(synapseClient:::.getCache("localSourceFile")))
	synapseClient:::.setCache("destFile", synapseClient:::synapseDownloadFile(url= paste("file://", gsub("[A-Z]:","",synapseClient:::.getCache("localSourceFile")), sep="")))
	destChecksum <- as.character(tools::md5sum(synapseClient:::.getCache("destFile")))
	if(file.exists(synapseClient:::.getCache('destFile')))
		file.remove(synapseClient:::.getCache('destFile'))
	checkEquals(as.character(sourceChecksum), as.character(destChecksum))
}

## local file uploads are not supported with current synapseUploadFile implementation
unitTestLocalFileUpload <- function(){
	url <- paste("file://", gsub("^[A-Z]:", "", tempfile()), sep="")
	d <- matrix(nrow=100, ncol=100, data=1)
	save(d,file = synapseClient:::.getCache("localSourceFile"))
	sourceChecksum <- tools::md5sum(synapseClient:::.getCache("localSourceFile"))
	parsedUrl <- synapseClient:::.ParsedUrl(url)
	synapseClient:::.setCache("localDestFile", parsedUrl@path)
	synapseClient:::synapseUploadFile(url=url, srcfile = synapseClient:::.getCache("localSourceFile"), checksum=sourceChecksum)
	checkEquals(as.character(tools::md5sum(synapseClient:::.getCache('localSourceFile'))), as.character(tools::md5sum(synapseClient:::.getCache('localDestFile'))))
	
	## clean up and try again using curlReader function
	file.remove(synapseClient:::.getCache("localDestFile"))
	
	synapseClient:::.curlReaderUpload(url, synapseClient:::.getCache("localSourceFile"))
	checkEquals(as.character(tools::md5sum(synapseClient:::.getCache('localSourceFile'))), as.character(tools::md5sum(synapseClient:::.getCache('localDestFile'))))
}

unitTestMd5Sum <- 
		function()
{
	## check that download happens when sourcefile is changed
	d <- diag(x=1, nrow=10, ncol = 10)
	save(d,file =synapseClient:::.getCache("localSourceFile"))
	srcChecksum <- as.character(tools::md5sum(synapseClient:::.getCache("localSourceFile")))
	
	url <- paste("file://", synapseClient:::.getCache("localSourceFile"), sep="")
	destFile <- synapseClient:::synapseDownloadFile(url, cacheDir=synapseClient:::.getCache("cacheDir"))
	
	destFileChecksum <- as.character(tools::md5sum(destFile))
	checkEquals(srcChecksum, destFileChecksum)
	
	d <- diag(x=2, nrow=20, ncol = 20)
	save(d, file = synapseClient:::.getCache("localSourceFile"))
	srcChecksum <- as.character(tools::md5sum(synapseClient:::.getCache("localSourceFile")))
	
	## make sure that the 10x10 matrix has a different checksum that the 20x20 matrix
	checkTrue(destFileChecksum != srcChecksum)
		
	newDestFile <- synapseClient:::synapseDownloadFile(url, cacheDir=synapseClient:::.getCache("cacheDir"), checksum=srcChecksum)
	
	## check that the new and old destfiles have the same name
	checkTrue(destFile == newDestFile)
	
	## check that the new and old destFiles don't have the same checksum
	checkTrue(destFileChecksum != as.character(tools::md5sum(newDestFile)))
	
	## check that the new destfile has the same checksum as the source file
	checkEquals(srcChecksum, as.character(tools::md5sum(newDestFile)))
}


#unitTestZippedDownload <- 
#		function()
#	
#{
#	
#}

#unitTestDownoad <- 
#		function()
#{
#	## create a jpeg object
#	jpeg(synapseClient:::.getCache("localJpegFile"))
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


