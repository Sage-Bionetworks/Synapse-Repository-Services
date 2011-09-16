# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################



unitTestMoveFileNewPathDirExists <-
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
	
	checkException(moveFile(layer, layer$files[1], "/foo"))
	
}

unitTestMoveFileNewPath <-
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
	
	layer <- moveFile(layer, layer$files[1], "/foo/")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], file.path("foo", gsub("^.+[\\\\/]+","", file)))
	
	layer <- moveFile(layer, layer$files[1], "/")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], file.path(gsub("^.+[\\\\/]+","", file)))
	
	layer <- moveFile(layer, layer$files[1], "/foo/")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], file.path("foo", gsub("^.+[\\\\/]+","", file)))
	
	layer <- moveFile(layer, layer$files[1], "")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], file.path(gsub("^.+[\\\\/]+","", file)))
	
}


unitTestRenameFile <-
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
	
	layer <- moveFile(layer, layer$files[1], "/foo/")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], file.path("foo", gsub("^.+[\\\\/]+","", file)))
	
}

unitTestRenameFileNewPath <-
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
	
	oldPath <- layer$files[1]
	layer <- moveFile(layer, layer$files[1], "/foo/newFileName")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], "foo/newFileName")
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	checkTrue(!file.exists(file.path(layer$files, oldPath)))
	
}

unitTestRenameFileToRoot <-
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
	
	oldPath <- layer$files[1]
	layer <- moveFile(layer, layer$files[1], "/newFileName")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], "newFileName")
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	checkTrue(!file.exists(file.path(layer$files, oldPath)))
}

unitTestMoveFileToRoot <-
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
	
	oldPath <- layer$files[1]
	layer <- moveFile(layer, layer$files[1], "/")
	checkTrue(length(layer$files) == 1L)
	checkEquals(layer$files[1], gsub("^.+[\\\\/]", "", file))
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	checkTrue(!file.exists(file.path(layer$files, oldPath)))
}


unitTestMoveFileOverExisting <-
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
	
	file2 <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file=file2)
	path2 <- "/foo"
	checksum2 <- as.character(tools::md5sum(file2))
	layer <- addFile(layer, file2, path2)
	checkTrue(length(layer$files) == 2L)
	checkEquals(checksum2, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[2]))))
	checkEquals(layer$files[2], sprintf("%s/%s",gsub("^[\\\\/]+", "", path2), gsub("^.+[\\\\/]","", file2)))
	
	checkException(moveFile(layer, layer$files[1], layer$files[2]))
	checkTrue(length(layer$files) == 2L)
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	checkEquals(checksum, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[1]))))
	checkEquals(checksum2, as.character(tools::md5sum(file.path(layer$cacheDir, layer$files[2]))))
	
	checkException(moveFile(layer, layer$files[1], "/foo"))
}