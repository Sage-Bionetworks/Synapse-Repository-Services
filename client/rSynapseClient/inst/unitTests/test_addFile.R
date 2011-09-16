# TODO: Add comment
# 
# Author: mfuria
###############################################################################


unitTestOverwriteFile <-
		function()
{
	file <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	path <- "/foo/bar"
	checksum <- as.character(tools::md5sum(file))
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 1L)
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	
	d <- diag(x = 2, nrow=10, ncol=10)
	save(d, file=file)
	checksum2 <- as.character(tools::md5sum(file))
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 1L)
	checkEquals(checksum2, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	checkTrue(checksum2 != checksum)
	
	file2 <- tempfile()
	d <- diag(x = 2, nrow=10, ncol=10)
	save(d, file=file2)
	checksum3 <- as.character(tools::md5sum(file2))
	layer <- addFile(layer, file2, path)
	checkTrue(length(layer$files) == 2L)
	checkEquals(checksum3, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[2]))))
	
	## overwrite again
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 2L)
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	checkEquals(checksum3, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[2]))))
}

unitTestAddFileToRoot <-
		function()
{
	file <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	path <- "/"
	checksum <- as.character(tools::md5sum(file))
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 1L)
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	
	file <- tempfile()
	d <- diag(x = 2, nrow=10, ncol=10)
	save(d, file=file)
	path <- ""
	checksum <- as.character(tools::md5sum(file))
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 1L)
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	
	file <- tempfile()
	d <- diag(x = 2, nrow=10, ncol=10)
	save(d, file=file)
	checksum <- as.character(tools::md5sum(file))
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file)
	checkTrue(length(layer$files) == 1L)
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
}

unitTestMultipleSlashes <-
		function()
{
	file <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	path <- "////foo//bar"
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkEquals(layer$files[1], sprintf("%s/%s", "foo/bar", gsub("^.+[\\\\//]","",file)))
	
	path <- "////foo\\\\bar"
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkEquals(layer$files[1], sprintf("%s/%s", "foo/bar", gsub("^.+[\\\\//]","",file)))
	
	path <- "\\\\\\\\foo\\\\bar"
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkEquals(layer$files[1], sprintf("%s/%s", "foo/bar", gsub("^.+[\\\\//]","",file)))
	
}