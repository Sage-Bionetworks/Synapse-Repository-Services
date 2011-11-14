.setUp <- 
		function() 
{
	# Create a project
	project <- RJSONIO::emptyNamedList
	project$name <- paste('R Store Layer Data Integration Test Project', gsub(':', '_', date()))
	createdProject <- createEntity(Project(entity=project))
	synapseClient:::.setCache("rIntegrationTestProject", createdProject)
	
	synapseClient:::.setCache("oldCacheDir", synapseCacheDir())
	synapseCacheDir(file.path(tempdir(), ".storeLayerDataCacheDir"))
}

.tearDown <- function() {
	deleteEntity(entity=synapseClient:::.getCache("rIntegrationTestProject"))
	synapseClient:::.deleteCache("rIntegrationTestProject")
	synapseClient:::.deleteCache("createdLayer")
	
	## delete test cache dir
	unlink(synapseCacheDir(), recursive=TRUE)
	synapseCacheDir(synapseClient:::.getCache("oldCacheDir"))
	synapseClient:::.deleteCache("oldCacheDir")
}

integrationTestStoreLayerData <- 
		function() 
{
	
	## Create a dataset
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = propertyValue(synapseClient:::.getCache("rIntegrationTestProject"), "id")
			)
	)
	
	createdDataset <- createEntity(entity=dataset)
	checkEquals(propertyValue(dataset, "name"), propertyValue(createdDataset, "name"))
	
	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	dataFile <- file.path(tempdir(), "data.tab")
	write.table(data, file=dataFile, sep="\t", quote=F, row.names=F)
	
	##------
	## Create a layer and use the convenience method to store an R object as a tab-delimited file
	layer <- Layer(entity = list(
					name = 'R Integration Test Layer',
					type = 'C',
					parentId = propertyValue(createdDataset, "id")
			)
	)
	
	createdLayer <- synapseClient:::storeLayerDataFiles(entity=layer, layerDataFile = dataFile)
	checkEquals(propertyValue(layer, "name"), propertyValue(createdLayer, "name"))
	
	##------
	## Create a layer and store a tab-delimited file explicity
	layer2 <- RJSONIO::emptyNamedList
	layer2$name <- 'R Integration Test Layer2 (check_special-characters.in0entity+names)'
	layer2$type <- 'C'
	layer2$parentId <- propertyValue(createdDataset, "id")
	
	layer2 <- Layer(entity = layer2)
	
	createdLayer2 <- synapseClient:::storeLayerDataFiles(entity=layer2, layerDataFile = dataFile)
	checkEquals(propertyValue(layer2, "name"), propertyValue(createdLayer2,"name"))
	
	##------
	## Create a layer and store a serialized R object explicity
	layer3 <- RJSONIO::emptyNamedList
	layer3$name <- 'R Integration Test Layer3'
	layer3$type <- 'C'
	layer3$parentId <- propertyValue(createdDataset, "id")
	layer3 <- Layer(layer3)
	
	createdLayer3 <- synapseClient:::storeLayerData(entity=layer3, data)
	checkEquals(propertyValue(layer3,"name"), propertyValue(createdLayer3,"name"))
	
	## Download all three layers and make sure they are equivalent
	layerFiles <- synapseClient:::loadLayerData(entity=synapseClient:::.extractEntityFromSlots(createdLayer))
	layer2Files <- synapseClient:::loadLayerData(entity=synapseClient:::.extractEntityFromSlots(createdLayer2))
	storedLayerData <- read.delim(layerFiles[[1]], sep='\t', stringsAsFactors = FALSE)
	storedLayer2Data <- read.delim(layer2Files[[1]], sep='\t', stringsAsFactors = FALSE)
	checkEquals(storedLayerData, storedLayer2Data)	
	
	layer3Data <- synapseClient:::loadLayerData(entity=synapseClient:::.extractEntityFromSlots(createdLayer3))
	# TODO fixme, see comment below
	# checkEquals(storedLayerData,data)
	checkEquals(data[,1], storedLayer2Data[,1])
	
	# Delete the dataset, can pass the entity or the entity id
synapseClient:::deleteDataset(entity=propertyValue(createdDataset, "id"))
	# Confirm that its gone
	checkException(synapseClient:::getDataset(entity=propertyValue(createdDataset,"id")))
}

# > checkEquals(data, storedLayerData)
# Error in checkEquals(data, storedLayerData) : 
#		Attributes: < Component 2: Modes: numeric, character >
# 		Attributes: < Component 2: target is numeric, current is character >
# 		Component 3: Attributes: < Length mismatch: comparison on first 1 components >
# 		Component 3: Attributes: < Component 1: 1 string mismatch >
# 		Component 3: target is Date, current is factor
# > summary(data)
# a       b           c             
# Min.   :1.0   j:1   Min.   :2004-01-01  
# 1st Qu.:1.5   k:1   1st Qu.:2004-01-04  
# Median :2.0   l:1   Median :2004-01-08  
# Mean   :2.0         Mean   :2004-01-08  
# 3rd Qu.:2.5         3rd Qu.:2004-01-11  
# Max.   :3.0         Max.   :2004-01-15  
# > summary(storedLayer2Data)
# a       b              c    
# Min.   :1.0   j:1   2004-01-01:1  
# 1st Qu.:1.5   k:1   2004-01-08:1  
# Median :2.0   l:1   2004-01-15:1  
# Mean   :2.0                       
# 3rd Qu.:2.5                       
# Max.   :3.0                

## there is a bug in here. I'll fix it later
integrationTestUpdateStoredLayerData <- function() {
	
	# Create a dataset
	dataset <- RJSONIO::emptyNamedList
	dataset$name <- 'R Integration Test Dataset - Update Stored Layer'
	dataset$parentId <- propertyValue(synapseClient:::.getCache("rIntegrationTestProject"), "id")
	createdDataset <- createEntity(Dataset(entity=dataset))
	checkEquals(dataset$name, propertyValue(createdDataset, "name"))
	
	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	##------
	## Create a layer and use the convenience method to store an R object as a tab-delimited file
	layer <- RJSONIO::emptyNamedList
	layer$name <- 'R Integration Test Layer'
	layer$type <- 'C'
	layer$parentId <- propertyValue(createdDataset, "id")
	createdLayer <- createEntity(Layer(layer))
	
	createdLayer <- synapseClient:::storeLayerData(entity=createdLayer, data)
	locations <- synapseClient:::getLayerLocations(entity=synapseClient:::.extractEntityFromSlots(createdLayer))
	checkEquals(1, nrow(locations))
	layerFiles <- synapseClient:::loadLayerData(entity=synapseClient:::.extractEntityFromSlots(createdLayer))
	local({
				load(layerFiles[[1]])
				checkEquals(1, data[1,1])
			})
	
	## Modify the data and store it again
	data[1,1] <- 42
	data2 <- data
	updatedLayer <- synapseClient:::storeLayerData(entity=createdLayer, data2)
	locations <- synapseClient:::getLayerLocations(entity=synapseClient:::.extractEntityFromSlots(createdLayer))
	# The data should be overwritten and we still only have one layer location
	checkEquals(1, nrow(locations))
	layerFiles <- synapseClient:::loadLayerData(entity=synapseClient:::.extractEntityFromSlots(updatedLayer))
	local({
				load(layerFiles[[1]])
				checkEquals(42, data[1,1])
			})
}

integrationTestStoreMediaLayer <- function() {
	
	## Create a dataset
	dataset <- RJSONIO::emptyNamedList
	dataset$name <- 'R Integration Test Dataset - Store Media Layer'
	dataset$parentId <- propertyValue(synapseClient:::.getCache("rIntegrationTestProject"), "id")
	createdDataset <- createEntity(entity=Dataset(dataset))
	checkEquals(dataset$name, propertyValue(createdDataset, "name"))
	
	## Create a layer
	layer <- RJSONIO::emptyNamedList
	layer$name <- 'R Integration Test Layer'
	layer$type <- 'M'
	layer$parentId <- propertyValue(createdDataset, "id")
	
	## Make a jpeg when PLFM-498 is fixed, for now, make a fake one
	filename <- file.path(tempdir(),"r_integration_test_plot.jpg")
#	attach(mtcars)
#	jpeg(filename)
#	plot(wt, mpg) 
#	abline(lm(mpg~wt))
#	title("Regression of MPG on Weight")
#	dev.off()
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	write.table(data, filename)	
	
	createdLayer <- synapseClient:::storeLayerDataFile(Layer(layer), filename)
	layerFiles <- synapseClient:::loadLayerData(createdLayer)
	checkEquals(1, length(layerFiles))
}
integrationTestMultipleBinary <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = propertyValue(synapseClient:::.getCache("rIntegrationTestProject"), "id")
			)
	)
	
	createdDataset <- createEntity(entity=dataset)
	checkEquals(propertyValue(dataset, "name"), propertyValue(createdDataset, "name"))
	
	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	data2 <- diag(100)
	
	layer <- Layer(entity = list(
					name = 'R Integration Test Layer',
					type = 'C',
					parentId = propertyValue(createdDataset, "id")
			)
	)
	
	layer <- synapseClient:::storeLayerData(entity=layer, data, data2)
	layerData <- synapseClient:::loadLayerData(layer)
	
	checkTrue(all(c("data", "data2") %in% ls(layerData)))
	
	checkEquals(data, layerData$data)
	checkEquals(data2, layerData$data2)
}