.setUp <- 
		function() 
{
	# Create a project
	createdProject <- createEntity(Project(list(name=paste('R Conditional Get Integration Test Project', gsub(':', '_', date())))))
	synapseClient:::.setCache("rIntegrationTestProject", createdProject)
	
	synapseClient:::.setCache("oldCacheDir", synapseCacheDir())
	synapseCacheDir(file.path(tempdir(), ".conditionalGetCacheDir"))
}

.tearDown <- 
		function() 
{
	deleteEntity(synapseClient:::.getCache("rIntegrationTestProject"))
	synapseClient:::.deleteCache("rIntegrationTestProject")
	
	## delete test cache dir
	unlink(synapseCacheDir(), recursive=TRUE)
	synapseCacheDir(synapseClient:::.getCache("oldCacheDir"))
	synapseClient:::.deleteCache("oldCacheDir")
}

integrationTestConditionalGet <- 
		function() 
{

	# Create a dataset
	dataset <- Dataset(list(
					name='R Integration Test Dataset',
					parentId=propertyValue(synapseClient:::.getCache("rIntegrationTestProject"), 'id')
			))
	createdDataset <- createEntity(dataset)
	
	# Create a layer and store a data R object
	layer <- Layer(list(name='R Integration Test Layer',
					type='C',
					parentId=propertyValue(createdDataset, 'id')
	)) 
	
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = TRUE)
	fileName <- file.path(tempdir(), "data.tab")
	write.table(data, file=fileName, quote=F, sep="\t", row.names=F)
	layer <- addFile(layer, fileName)
	createdLayer <- createEntity(layer)
	checkEquals(propertyValue(layer,"name"), propertyValue(createdLayer, "name"))
	
	# Now download the layer
	loadedLayer1 <- loadEntity(layer)
	fileInfo1 <- file.info(normalizePath(file.path(loadedLayer1$cacheDir, loadedLayer1$files[1]))) 
	
	# Now download the layer again, but this time it should just read from the cache
	loadedLayer2 <- loadEntity(layer)
	fileInfo2 <- file.info(normalizePath(file.path(loadedLayer2$cacheDir, loadedLayer2$files[1]))) 
	
	checkEquals(loadedLayer1$files[1], loadedLayer2$files[1])
	# If the modification time is the same, we did not download it the second time
	checkEquals(fileInfo1$mtime, fileInfo2$mtime)
}