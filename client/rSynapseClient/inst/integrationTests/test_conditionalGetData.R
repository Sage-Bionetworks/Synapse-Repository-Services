.setUp <- 
		function() 
{
	# Create a project
	project <- RJSONIO::emptyNamedList
	project$name <- paste('R Conditional Get Integration Test Project', gsub(':', '_', date()))
	createdProject <- synapseClient:::createProject(entity=project)
	synapseClient:::.setCache("rIntegrationTestProject", createdProject)
	
	synapseClient:::.setCache("oldCacheDir", synapseCacheDir())
	synapseCacheDir(file.path(tempdir(), ".conditionalGetCacheDir"))
}

.tearDown <- 
		function() 
{
	synapseClient:::deleteProject(entity=synapseClient:::.getCache("rIntegrationTestProject"))
	synapseClient:::.deleteCache("rIntegrationTestProject")
	synapseClient:::.deleteCache("createdDataset")
	
	## delete test cache dir
	unlink(synapseCacheDir(), recursive=TRUE)
	synapseCacheDir(synapseClient:::.getCache("oldCacheDir"))
	synapseClient:::.deleteCache("oldCacheDir")
}

integrationTestConditionalGet <- 
		function() 
{

	# Create a dataset
	dataset <- RJSONIO::emptyNamedList
	dataset$name <- 'R Integration Test Dataset'
	dataset$parentId <- synapseClient:::.getCache("rIntegrationTestProject")$id
	createdDataset <- synapseClient:::createDataset(entity=dataset)
	synapseClient:::.setCache("createdDataset",createdDataset)
	checkEquals(dataset$name, createdDataset$name)
	
	# Create a layer and store a data R object
	layer <- RJSONIO::emptyNamedList
	layer$name <- 'R Integration Test Layer'
	layer$type <- 'C'
	layer$parentId <- createdDataset$id 
	
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = TRUE)
	fileName <- file.path(tempdir(), "data.tab")
	write.table(data, file=fileName, quote=F, sep="\t", row.names=F)
	layer <- Layer(layer)
	createdLayer <- synapseClient:::storeLayerDataFiles(entity=layer, layerDataFile=fileName)
	checkEquals(propertyValue(layer,"name"), propertyValue(createdLayer, "name"))
	
	# Now download the layer
	locations <- synapseClient:::getLayerLocations(entity=synapseClient:::.extractEntityFromSlots(createdLayer))
	destinationFile1 <- synapseClient:::synapseDownloadFile(url=locations$path[1], checksum=locations$md5sum[1])
	fileInfo1 <- file.info(destinationFile1) 
	
	# Now download the layer again, but this time it should just read from the cache
	destinationFile2 <- synapseClient:::synapseDownloadFile(url=locations$path[1], checksum=locations$md5sum[1])
	fileInfo2 <- file.info(destinationFile2) 
	
	checkEquals(destinationFile1, destinationFile2)
	# If the modification time is the same, we did not download it the second time
	checkEquals(fileInfo1$mtime, fileInfo2$mtime)
	
	## delete the dataset
	synapseClient:::deleteDataset(synapseClient:::.getCache("createdDataset"))
}