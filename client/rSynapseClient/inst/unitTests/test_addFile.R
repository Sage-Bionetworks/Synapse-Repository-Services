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

unitTestAddDirNoPathTwoFiles <-
		function()
{
	dir <- tempfile()
	dir.create(file.path(dir,"/subdir"), recursive=T)
	file <- file.path(dir, "/subdir/myFile.rbin")
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	
	file2 <- file.path(dir, "myFile2.rbin")
	d <- diag(x=2,nrow=10, ncol=10)
	save(d, file=file2)
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, dir)
	checkTrue(all(file.path(gsub("^.+[\\\\/]", "", dir), c("subdir/myFile.rbin", "myFile2.rbin")) %in% layer$files))
	
}

unitTestAddDirNoPath <-
		function()
{
	dir <- tempfile()
	file <- file.path(dir, "/subdir/myFile.rbin")
	dir.create(file.path(dir, "/subdir"), recursive = TRUE)
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, dir)
	checkEquals(layer$files[1], sprintf("%s/%s", gsub("^.+[\\\\/]", "", dir), "subdir/myFile.rbin"))
	
}

unitTestAddDirAndFileTwoPaths <-
		function()
{
	layer <- new(Class="Layer")
	file <- tempfile()
	d <- diag(nrow=10,ncol=10)
	save(d, file=file)
	checkException(addFile(layer, c(tempdir(),file), c("one", "two")))
}

unitTestTwoFilesOnePath <-
		function()
{
	layer <- new(Class="Layer")
	file1 <- tempfile()
	d <- diag(nrow=10,ncol=10)
	save(d, file=file1)
	
	file2 <- tempfile()
	d <- diag(x=2,nrow=10,ncol=10)
	save(d, file=file2)
	path <- "aPath"
	layer <- addFile(layer, c(file1, file2), path)
	checkEquals(length(layer$files), 2L)
	checkTrue(all(file.path(path, gsub("^.+[\\\\/]+", "", c(file1, file2))) %in% layer$files))
}

unitTestTwoFilesTwoPaths <-
		function()
{
	layer <- new(Class="Layer")
	file1 <- tempfile()
	d <- diag(nrow=10,ncol=10)
	save(d, file=file1)
	
	file2 <- tempfile()
	d <- diag(x=2,nrow=10,ncol=10)
	save(d, file=file2)
	path1 <- "aPath"
	path2 <- "anotherPath"

	layer <- addFile(layer, c(file1, file2), c(path1, path2))
}

unitTestTwoFilesThreePaths <-
		function()
{
	layer <- new(Class="Layer")
	checkException(addFile(layer, c("foo", "bar"), c("one", "two", "three")))
}

unitTestOneFileOneDirTwoPaths <-
		function()
{
	layer <- new(Class="Layer")
	file1 <- tempfile()
	d <- diag(nrow=10,ncol=10)
	save(d, file=file1)
	
	file2 <- tempfile()
	d <- diag(x=2,nrow=10,ncol=10)
	save(d, file=file2)
	path1 <- "aPath"
	path2 <- "anotherPath"
	checkException(addFile(layer, c(tempdir(), file2), c(path1, path2)))
}

