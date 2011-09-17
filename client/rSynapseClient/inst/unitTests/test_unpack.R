.setUp <- 
		function()
{
	synapseClient:::.setCache("localJpegFile", file.path(tempdir(), "plot.jpg"))
	synapseClient:::.setCache("localZipFile", file.path(tempdir(), "files.zip"))	
	synapseClient:::.setCache("localTxtFile", file.path(tempdir(), "data.txt"))
	synapseClient:::.setCache("localCacheDir", file.path(tempdir(), ".synapseCache"))
	synapseClient:::.setCache("localUnpackDir", file.path(tempdir(), "files_unpacked"))
}

.tearDown <- 
		function()
{
	unlink(synapseClient:::.getCache("localJpegFile"), recursive=T)
	unlink(synapseClient:::.getCache("localZipFile"), recursive=T)
	unlink(synapseClient:::.getCache("localTxtFile"), recursive=T)
	unlink(synapseClient:::.getCache("localCacheDir"), recursive=T)
	synapseClient:::.deleteCache("localZipFile")	
	synapseClient:::.deleteCache("localTxtFile")
	synapseClient:::.deleteCache("localJpegFile")
	synapseClient:::.deleteCache("localDotDir")
}


unitTestNotCompressed <- 
		function()
{
	## create a jpeg object
	## TODO create an acutal jpeg file once X11 is installed on the bamboo AMI
	d <- data.frame(diag(2,20,20))
	write.table(d,file=synapseClient:::.getCache("localJpegFile"), sep="\t", quote=F, row.names=F, col.names=F)
#	jpeg(synapseClient:::.getCache("localJpegFile"))
#	plot(1:10, 1:10)
#	dev.off()
	
	
	file <- synapseClient:::.unpack(synapseClient:::.getCache("localJpegFile"))
	
	## file path should be same as localJpegFile cache value
	checkEquals(as.character(file), synapseClient:::.getCache("localJpegFile"))
	
	## check the md5sums
	checkEquals(tools::md5sum(as.character(file)), tools::md5sum(synapseClient:::.getCache("localJpegFile")))
	
	## check the rootDir attribute value
	checkEquals(attr(file,"rootDir"), tempdir())
	
}

unitTestDirectoriesStartingWithDot <-
		function()
{
	if(!file.exists(synapseClient:::.getCache("localCacheDir")))
		dir.create(synapseClient:::.getCache("localCacheDir"))
	oldDir <- getwd()
	setwd(synapseClient:::.getCache("localCacheDir"))
	dir.create(".foo")
	filePath <- file.path(synapseClient:::.getCache("localCacheDir"), ".foo/file.txt")
	d <- data.frame(diag(2,20,20))
	write.table(d, file=filePath, sep="\t", quote=F, row.names=F, col.names=F)
		
	suppressWarnings(zip(synapseClient:::.getCache("localZipFile"), files = filePath))
	setwd(oldDir)
	files <- synapseClient:::.unpack(synapseClient:::.getCache("localZipFile"))
	checkEquals(length(files), 1L)
}


unitTestZipFile <-
		function()
{
	## create local jpeg file
	## TODO create an acutal jpeg file once X11 is installed on the bamboo AMI
	d <- data.frame(diag(2,20,20))
	write.table(d,file=synapseClient:::.getCache("localJpegFile"), sep="\t", quote=F, row.names=F, col.names=F)
#	jpeg(synapseClient:::.getCache("localJpegFile"))
#	plot(1:10, 1:10)
#	dev.off()
	
	## create local text file
	d <- data.frame(diag(1,10,10))
	write.table(d,file=synapseClient:::.getCache("localTxtFile"), sep="\t", quote=F, row.names=F, col.names=F)
	
	## zip these two files
	suppressWarnings(zip(synapseClient:::.getCache("localZipFile"), files = c(synapseClient:::.getCache("localTxtFile"), synapseClient:::.getCache("localJpegFile"))))
	
	files <- synapseClient:::.unpack(synapseClient:::.getCache("localZipFile"))
	
	## make sure the unpack directory was named correctly
	checkEquals(attr(files, "rootDir"), synapseClient:::.getCache("localUnpackDir"))
	
	## check the contents of the unpack directory
	checkTrue(gsub("^/", "", gsub("^[A-Z]:/","", gsub("[/]+","/", gsub("[\\]+","/",synapseClient:::.getCache("localJpegFile"))))) %in% list.files(attr(files,"rootDir"), recursive=TRUE))
	checkTrue(gsub("^/", "", gsub("^[A-Z]:/","", gsub("[/]+","/", gsub("[\\]+","/",synapseClient:::.getCache("localTxtFile"))))) %in% list.files(attr(files,"rootDir"), recursive=TRUE))
	
}