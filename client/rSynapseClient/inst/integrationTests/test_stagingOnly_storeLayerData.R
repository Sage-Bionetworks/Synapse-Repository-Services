.setUp <- function() {
	# this test can only be run against staging
	.setCache("orig.authservice.endpoint", synapseAuthServiceEndpoint())
	.setCache("orig.reposervice.endpoint", synapseRepoServiceEndpoint())
	synapseAuthServiceEndpoint("https://staging-auth.elasticbeanstalk.com/auth/v1")
	synapseRepoServiceEndpoint("https://staging-reposervice.elasticbeanstalk.com/repo/v1")
}

.tearDown <- function() {
	synapseAuthServiceEndpoint(.getCache("orig.authservice.endpoint"))
	synapseRepoServiceEndpoint(.getCache("orig.reposervice.endpoint"))
	.deleteCache("orig.authservice.endpoint")
	.deleteCache("orig.reposervice.endpoint")
}

integrationTestStoreLayerData <- function() {

	# Create a dataset
	dataset <- list()
	dataset$name = 'R Integration Test Dataset'
	dataset$parentId <- '1893' # a project created for these tests
	createdDataset <- createDataset(entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	
	# Create a layer and store a data R object
	layer <- list()
	layer$name = 'R Integration Test Layer'
	layer$type = 'C'
	layer$parentId = createdDataset$id 
	
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = TRUE)
	
	createdLayer <- storeLayerData(layerMetadata=layer, layerData=data)
	checkEquals(layer$name, createdLayer$name)

	# Create a layer and store a data file
	layer2 <- list()
	layer2$name = 'R Integration Test Layer2'
	layer2$type = 'C'
	layer2$parentId = createdDataset$id 
	
	dataFilepath <- 'integrationTestData.txt'
	write.table(data, dataFilepath, sep='\t')
	zippedDataFilepath <- 'integrationTestData.zip'
	zip(zippedDataFilepath, c(dataFilepath))
	
	createdLayer2 <- storeLayerDataFile(layerMetadata=layer2, layerDataFile=zippedDataFilepath)
	checkEquals(layer2$name, createdLayer2$name)

	# Download both layers and make sure they are equivalent
	layerFiles <- synapseClient:::.cacheFiles(entity=createdLayer)
	layer2Files <- synapseClient:::.cacheFiles(entity=createdLayer2)
	storedLayerData <- read.table(layerFiles[[1]], sep='\t')
	storedLayer2Data <- read.table(layer2Files[[1]], sep='\t')
	checkEquals(storedLayerData, storedLayer2Data)	
	
	# Delete the dataset
	deleteDataset(id=createdDataset$id)
}