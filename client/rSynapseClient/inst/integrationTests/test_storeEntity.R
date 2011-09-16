# Integration tests for storing entities
# 
# Author: Matt Furia
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
}

.tearDown <-
		function()
{
	deleteEntity(synapseClient:::.getCache("testProject"))
	synapseClient:::.deleteCache("testProject")
	synapseClient:::.deleteCache("testDataset")
}

integrationTestStoreLayerRbin <- 
		function()
{
	dataset <- synapseClient:::.getCache("testDataset")
	## create a layer
	layer <- Layer(list(parentId=propertyValue(dataset,"id"), type="C", name="myLayer"))
	annotValue(layer, "format") <- "rbin"
	layer@location@files <- "/my_test_packet/phenotypes.rbin"

	phenotypes <- diag(nrow=10, ncol=10)
	if(file.exists(file.path(layer@location@cacheDir, "my_test_packet")))
		unlink(file.path(layer@location@cacheDir, "my_test_packet"), recursive=TRUE)
	dir.create(file.path(layer@location@cacheDir, "my_test_packet"), recursive=TRUE)
	save(phenotypes, file=file.path(layer@location@cacheDir, layer@location@files))
	
	checksum <- as.character(tools::md5sum(file.path(layer@location@cacheDir, layer@location@files)))
	
	storedLayer <- storeEntity(layer)
	checkEquals(propertyValue(storedLayer, "name"), propertyValue(layer, "name"))
	checkEquals(propertyValue(storedLayer, "type"), propertyValue(layer, "type"))
	checkEquals(propertyValue(storedLayer, "parentId"), propertyValue(layer, "parentId"))

	checkEquals(as.character(tools::md5sum(file.path(storedLayer@location@cacheDir, storedLayer@location@files))), checksum)
	downloadedLayer <- downloadEntity(storedLayer)
	checkEquals(propertyValue(downloadedLayer, "name"), propertyValue(storedLayer, "name"))
	checkEquals(propertyValue(downloadedLayer, "id"), propertyValue(storedLayer, "id"))
	checkEquals(propertyValue(downloadedLayer, "parentId"), propertyValue(storedLayer, "parentId"))
	checkEquals(propertyValue(downloadedLayer, "type"), propertyValue(storedLayer, "type"))
	checkEquals(downloadedLayer@location@cacheDir, storedLayer@location@cacheDir)
	checkTrue(all(downloadedLayer@location@files %in% storedLayer@location@files))
	checkTrue(all(storedLayer@location@files %in% downloadedLayer@location@files))
	
	checkTrue(all(file.exists(file.path(storedLayer@location@cacheDir, storedLayer@location@files))))
	
}