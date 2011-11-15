# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################
.setUp <- 
		function()
{
	## create a project
	project <- new(Class="Project")
	propertyValues(project) <- list(
			name = paste("myProject", gsub(':', '_', date()))
	)
	project <- createEntity(project)
	synapseClient:::.setCache("testProject", project)
	
	## create a dataset
	dataset <- Dataset(list(name="MyDataSet", parentId=propertyValue(project, "id")))
	dataset <- createEntity(dataset)
	synapseClient:::.setCache("testDataset", dataset)
}

.tearDown <-
		function()
{
	deleteEntity(synapseClient:::.getCache("testProject"))
	synapseClient:::.deleteCache("testProject")
	synapseClient:::.deleteCache("testDataset")
}


integrationTestStore <-
		function()
{
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(list(name="Test Layer", parentId = propertyValue(dataset, "id"), type="C"))
	addObject(layer, list(foo="bar"))
	checkEquals(length(layer$files), 0L)
	
	storedLayer <- storeEntityObjects(layer)
	checkEquals(length(storedLayer$files), 0L)
	checkEquals(length(storedLayer$objects), 1L)
	checkEquals(layer$objects$foo, storedLayer$objects$foo)
	checkTrue(file.exists(file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"), "foo.rbin")))
}

integrationTestDownload <-
		function()
{
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(list(name="Test Layer", parentId = propertyValue(dataset, "id"), type="C"))
	addObject(layer, list(foo="bar"))
	checkEquals(length(layer$files), 0L)
	
	storedLayer <- storeEntityObjects(layer)
	
	downloadedLayer <- downloadEntity(propertyValue(storedLayer, "id"))
	checkEquals(length(downloadedLayer$files), 0L)
	checkEquals(length(downloadedLayer$objects), 0L)
	checkTrue(file.exists(file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"), "foo.rbin")))
}

integrationTestLoad <-
		function()
{
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(list(name="Test Layer", parentId = propertyValue(dataset, "id"), type="C"))
	addObject(layer, list(foo="bar"))
	checkEquals(length(layer$files), 0L)
	
	storedLayer <- storeEntityObjects(layer)
	downloadedLayer <- downloadEntity(propertyValue(storedLayer, "id"))
	
	loadedLayer <- loadEntity(propertyValue(storedLayer, "id"))
	checkTrue(file.exists(file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"), "foo.rbin")))
	checkEquals(length(loadedLayer$files), 0L)
	checkEquals(length(loadedLayer$objects), 1L)
	checkEquals(layer$objects$foo, loadedLayer$objects$foo)
}

integrationTestDownloadFilesAndObjects <-
		function()
{
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(list(name="Test Layer", parentId = propertyValue(dataset, "id"), type="C"))
	addObject(layer, list(foo="bar"))
	checkEquals(length(layer$files), 0L)
	
	assign("diag", diag(nrow=10, ncol=10), envir = layer@objects)
	checkEquals(length(layer$objects), 2L)
	storedLayer <- storeEntityObjects(layer)
	checkEquals(length(layer$files), 0L)
	checkEquals(length(layer$objects), 2L)
	
	downloadedLayer <- downloadEntity(propertyValue(storedLayer, "id"))
	checkEquals(length(downloadedLayer$files), 0L)
	checkEquals(length(downloadedLayer$objects), 0L)
	
	loadedLayer <- loadEntity(downloadedLayer)
	checkEquals(length(downloadedLayer$files), 0L)
	checkEquals(length(downloadedLayer$objects), 1L)
	
	loadedLayer <- loadEntity(propertyValue(storedLayer,"id"))
	checkEquals(length(downloadedLayer$files), 0L)
	checkEquals(length(downloadedLayer$objects), 1L)
}



