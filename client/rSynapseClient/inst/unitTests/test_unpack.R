.setUp <- 
		function()
{
	.setCache("localJpegFile", file.path(tempdir(), "plot.jpg"))
	.setCache("localZipFile", file.path(tempdir(), "files.zip"))	
	.setCache("localTxtFile", file.path(tempdir(), "data.txt"))
	.setCache("localUnpackDir", file.path(tempdir(), "files_unpacked"))
}

.tearDown <- 
		function()
{
	unlink(.getCache("localJpegFile"))
	unlink(.getCache("localZipFile"))
	unlink(.getCache("localTxtFile"))
	.deleteCache("localZipFile")	
	.deleteCache("localTxtFile")
	.deleteCache("localJpegFile")
}


unitTestNotCompressed <- 
		function()
{
	## create a jpeg object
	## TODO create an acutal jpeg file once X11 is installed on the bamboo AMI
	d <- data.frame(diag(2,20,20))
	write.table(d,file=.getCache("localJpegFile"), sep="\t", quote=F, row.names=F, col.names=F)
#	jpeg(.getCache("localJpegFile"))
#	plot(1:10, 1:10)
#	dev.off()
	
	
	file <- .unpack(.getCache("localJpegFile"))
	
	## file path should be same as localJpegFile cache value
	checkEquals(as.character(file), .getCache("localJpegFile"))
	
	## check the md5sums
	checkEquals(tools::md5sum(as.character(file)), tools::md5sum(.getCache("localJpegFile")))
	
	## check the rootDir attribute value
	checkEquals(attr(file,"rootDir"), tempdir())
	
}


unitTestZipFile <-
		function()
{
	## create local jpeg file
	## TODO create an acutal jpeg file once X11 is installed on the bamboo AMI
	d <- data.frame(diag(2,20,20))
	write.table(d,file=.getCache("localJpegFile"), sep="\t", quote=F, row.names=F, col.names=F)
#	jpeg(.getCache("localJpegFile"))
#	plot(1:10, 1:10)
#	dev.off()
	
	## create local text file
	d <- data.frame(diag(1,10,10))
	write.table(d,file=.getCache("localTxtFile"), sep="\t", quote=F, row.names=F, col.names=F)
	
	## zip these two files
	zip(.getCache("localZipFile"), files = c(.getCache("localTxtFile"), .getCache("localJpegFile")))
	
	files <- .unpack(.getCache("localZipFile"))
	
	## make sure the unpack directory was named correctly
	checkEquals(attr(files, "rootDir"), .getCache("localUnpackDir"))
	
	## check the contents of the unpack directory
	checkTrue(gsub("^/", "", gsub("^[A-Z]:/","", gsub("[/]+","/", gsub("[\\]+","/",.getCache("localJpegFile"))))) %in% list.files(attr(files,"rootDir"), recursive=TRUE))
	checkTrue(gsub("^/", "", gsub("^[A-Z]:/","", gsub("[/]+","/", gsub("[\\]+","/",.getCache("localTxtFile"))))) %in% list.files(attr(files,"rootDir"), recursive=TRUE))
	
}