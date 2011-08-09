.setUp <- function() {
	# this test can only be run against staging
	.setCache("orig.authservice.endpoint", synapseAuthServiceEndpoint())
	.setCache("orig.reposervice.endpoint", synapseRepoServiceEndpoint())
	synapseAuthServiceEndpoint("https://staging-auth.elasticbeanstalk.com/auth/v1")
	synapseRepoServiceEndpoint("https://staging-reposervice.elasticbeanstalk.com/repo/v1")
	
	# Create a project
	project <- list()
	project$name <- paste('R Conditional Get Integration Test Project', gsub(':', '_', date()))
	createdProject <- createProject(entity=project)
	.setCache("rIntegrationTestProject", createdProject)
}

.tearDown <- function() {
	synapseAuthServiceEndpoint(.getCache("orig.authservice.endpoint"))
	synapseRepoServiceEndpoint(.getCache("orig.reposervice.endpoint"))
	.deleteCache("orig.authservice.endpoint")
	.deleteCache("orig.reposervice.endpoint")
	
	deleteProject(entity=.getCache("rIntegrationTestProject"))
	.deleteCache("rIntegrationTestProject")
}

integrationTestConditionalGet <- function() {

	# Create a dataset
	dataset <- list()
	dataset$name <- 'R Integration Test Dataset'
	dataset$parentId <- .getCache("rIntegrationTestProject")$id
	createdDataset <- createDataset(entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	
	# Create a layer and store a data R object
	layer <- list()
	layer$name <- 'R Integration Test Layer'
	layer$type <- 'C'
	layer$parentId <- createdDataset$id 
	
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = TRUE)
	
	createdLayer <- storeLayerData(layerMetadata=layer, layerData=data)
	checkEquals(layer$name, createdLayer$name)
	
	# Now download the layer
	locations <- getLayerLocations(entity=createdLayer)
	destinationFile1 <- synapseDownloadFile(url=locations$path[1], checksum=locations$md5sum[1])
	fileInfo1 <- file.info(destinationFile1) 
	
	# Now download the layer again, but this time it should just read from the cache
	destinationFile2 <- synapseDownloadFile(url=locations$path[1], checksum=locations$md5sum[1])
	fileInfo2 <- file.info(destinationFile2) 
	
	checkEquals(destinationFile1, destinationFile2)
	# If the modification time is the same, we did not download it the second time
	checkEquals(fileInfo1$mtime, fileInfo2$mtime)
	
	# Delete the dataset
	deleteDataset(entity=createdDataset)
}