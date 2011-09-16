# TODO: Add comment
# 
# Author: furia
###############################################################################

.setUp <- 
		function()
{
	## create a project
	project <- new(Class="Project")
	propertyValues(project) <- list(
			name = paste("myProject", synapseClient:::sessionToken())
	)
	project <- createEntity(project)
	synapseClient:::.setCache("testProject", project)
	
	## create a dataset
	dataset <- Dataset(list(name="MyDataSet", parentId=propertyValue(project, "id")))
	dataset <- createEntity(dataset)
	synapseClient:::.setCache("testDataset", dataset)
	synapseClient:::.setCache("oldCacheDir", synapseCacheDir())
	synapseCacheDir(tempfile(pattern="tempSynapseCache"))
}

.tearDown <-
		function()
{
	deleteEntity(synapseClient:::.getCache("testProject"))
	synapseClient:::.deleteCache("testProject")
	synapseClient:::.deleteCache("testDataset")
	
	unlink(synapseCacheDir(), recursive=T)
	synapseCacheDir(synapseClient:::.getCache("oldCacheDir"))
	
}

integrationTestAddToNewLayer <-
		function()
{
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- new(Class="Layer", properties=list(parentId=propertyValue(dataset, "id"), type="C"))
	
	file <- "file1.rbin"
	path <- "/apath"
	d <- diag(nrow=10, ncol=10)
	save(d, file=file.path(tempdir(), file))
	## add a file in a subdirectory
	checkTrue(!file.exists(file.path(layer$cacheDir, path, file)))
	checkTrue(!grepl(synapseClient:::synapseCacheDir(), layer$cacheDir))
	checkEquals(length(layer$files), 0L)
	layer <- addFile(layer, file.path(tempdir(), file), path)
	checkEquals(length(layer$files), 1L)
	
	checkTrue(file.exists(file.path(layer$cacheDir, layer$files)))
	checkEquals(layer$files, gsub(sprintf("^%s", .Platform$file.sep), "", file.path(path, file)))
	
	layer <- storeEntity(layer)
	checkTrue(grepl(synapseClient:::synapseCacheDir(), layer$cacheDir))
	checkEquals(length(layer$files), 1L)
	checkTrue(file.exists(file.path(layer$cacheDir, layer$files)))
	
	##update the layer
	file <- "file2.rbin"
	path <- "/apath2"
	d <- diag(x=2,nrow=10, ncol=10)
	save(d, file=file.path(tempdir(), file))
	layer <- addFile(layer, file.path(tempdir(), file), path)
	checkEquals(length(layer$files), 2L)
	checkEquals(layer$files[2], gsub(sprintf("^%s", .Platform$file.sep), "", file.path(path, file)))
	checkTrue(grepl(synapseClient:::synapseCacheDir(), layer$cacheDir))
	
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	
	layer <- storeEntity(layer)
	checkTrue(all(file.remove(file.path(layer$cacheDir, layer$files))))
	layer2 <- downloadEntity(propertyValue(layer,"id"))
	checkEquals(layer$cacheDir, layer2$cacheDir)
	checkEquals(propertyValue(layer,"id"), propertyValue(layer2, "id"))
	checkTrue(all(file.exists(file.path(layer2$cacheDir, layer2$files))))
	checkTrue(all(layer$files == layer2$files))
	
	## add a file to the rood direcotory
	layer <- layer2
	file <- "file3.rbin"
	path <- "/"
	d <- diag(x=3,nrow=10, ncol=10)
	save(d, file=file.path(tempdir(), file))
	layer <- addFile(layer, file.path(tempdir(), file), path)
	checkEquals(length(layer$files), 3L)
	checkEquals(layer$files[3], gsub(sprintf("^%s+", .Platform$file.sep), "", file.path(path, file)))
	checkTrue(grepl(synapseClient:::synapseCacheDir(), layer$cacheDir))
	
	layer <- storeEntity(layer)
	checkEquals(length(layer$files), 3L)
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	checkTrue(all(file.remove(file.path(layer$cacheDir, layer$files))))
	
	layer2 <- downloadEntity(propertyValue(layer,"id"))
	checkEquals(layer$cacheDir, layer2$cacheDir)
	checkEquals(propertyValue(layer,"id"), propertyValue(layer2, "id"))
	checkTrue(all(file.exists(file.path(layer2$cacheDir, layer2$files))))
	checkTrue(all(layer$files == layer2$files))
	
	## add a file to the rood direcotory
	layer <- layer2
	file <- "file4.rbin"
	path <- ""
	d <- diag(x=4,nrow=10, ncol=10)
	save(d, file=file.path(tempdir(), file))
	layer <- addFile(layer, file.path(tempdir(), file), path)
	checkEquals(length(layer$files), 4L)
	checkEquals(layer$files[4], gsub(sprintf("^%s+", .Platform$file.sep), "", file.path(path, file)))
	checkTrue(grepl(synapseClient:::synapseCacheDir(), layer$cacheDir))
	
	layer <- storeEntity(layer)
	checkEquals(length(layer$files), 4L)
	checkTrue(all(file.exists(file.path(layer$cacheDir, layer$files))))
	checkTrue(all(file.remove(file.path(layer$cacheDir, layer$files))))
	
	layer2 <- downloadEntity(propertyValue(layer,"id"))
	checkEquals(layer$cacheDir, layer2$cacheDir)
	checkEquals(propertyValue(layer,"id"), propertyValue(layer2, "id"))
	checkTrue(all(file.exists(file.path(layer2$cacheDir, layer2$files))))
	checkTrue(all(layer$files == layer2$files))
	checkTrue(all(file.remove(file.path(layer$cacheDir, layer$files))))
	
}

