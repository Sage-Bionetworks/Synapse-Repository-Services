# TODO: Add comment
# 
# Author: furia
###############################################################################

unitTestDeleteFile <-
		function()
{
	
	file <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	path <- "/foo/bar"
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 1L)
	
	layer <- deleteFile(layer, layer$files[1L])
	checkTrue(length(layer$files) == 0L)
	checkEquals(length(dir(layer$cacheDir)), 0L)
}

unitTestCleanUp <-
		function()
{
	file <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file=file)
	path <- "/foo/bar"
	
	layer <- new(Class="Layer")
	layer <- addFile(layer, file, path)
	checkTrue(length(layer$files) == 1L)
	
	file2 <- tempfile()
	d <- diag(x=2, nrow=10, ncol=10)
	save(d, file=file2)
	path2 <- "/foo"
	layer <- addFile(layer, file2, path2)
	checkTrue(length(layer$files) == 2L)
	
	file3 <- tempfile()
	d <- diag(x=3, nrow=10, ncol=10)
	save(d, file=file3)
	path3 <- "/foo"
	layer <- addFile(layer, file3, path3)
	checkTrue(length(layer$files) == 3L)
	
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	
	layer <- deleteFile(layer, layer$files[1])
	checkTrue(length(layer$files) == 2L)
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	
	checkTrue(!file.exists(file.path(layer$cacheDir, path, gsub("^.+[\\\\/]", "", file))))
	## TODO fix this test
	##checkTrue(!file.exists(file.path(layer$cacheDir, path)))
	
	layer <- deleteFile(layer, layer$files[1])
	checkTrue(length(layer$files) == 1L)
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	checkTrue(!file.exists(file.path(layer$cacheDir, path2, gsub("^.+[\\\\/]", "", file2))))
	checkTrue(file.exists(file.path(layer$cacheDir, path2)))
	
	layer <- deleteFile(layer, layer$files[1])
	checkTrue(length(layer$files) == 0L)
	checkTrue(length(file.exists(file.path(layer$cacheDir, layer$files))) == 0)
	checkTrue(!file.exists(file.path(layer$cacheDir, path3, gsub("^.+[\\\\/]", "", file3))))
	checkTrue(!file.exists(file.path(layer$cacheDir, path3)))
}


unitTestDeleteFileFromRoot <-
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
	
	oldPath <- layer$files[1]
	layer <- deleteFile(layer, layer$files[1])
	checkEquals(length(layer$files), 0L)
	checkTrue(!file.exists(file.path(layer$cacheDir, oldPath)))
	
}


