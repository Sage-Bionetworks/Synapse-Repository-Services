# TODO: Add comment
# 
# Author: furia
###############################################################################


.setUp <- function(){
	## stub-out zip file to return 127
	myZip <- function(zipfile, files){
		return(127L)
	}
	attr(myZip, "origFcn") <- utils:::zip
	## detach packages so their functions can be overridden
	detach('package:utils', force=TRUE)
	utils:::assignInNamespace("zip", myZip, "utils")
	#reload detached packages
	library(utils, quietly=TRUE)
	
	## create project and add to cache
	project <- list()
	project$name <- paste('R noZip Integration Test Project', gsub(':', '_', date()))
	createdProject <- createProject(entity=project)
	.setCache("rIntegrationTestProject", createdProject)
}

.tearDown <- function(){
	## put back method
	detach('package:utils', force = TRUE)
	utils:::assignInNamespace("zip", attr(utils:::zip, "origFcn"), "utils")
	library(utils, quietly = TRUE)
	
	## delete project 
	deleteProject(entity=.getCache("rIntegrationTestProject"))
	.deleteCache("rIntegrationTestProject")
}

integrationTestText <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = .getCache("rIntegrationTestProject")$id
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
	
	layer <- Layer(entity = list(
					name = 'R Integration Test Layer',
					type = 'C',
					parentId = propertyValue(createdDataset, "id")
			)
	)
	
	createdLayer <- storeLayerDataFiles(entity=layer, layerDataFile = dataFile)
	checkEquals(propertyValue(layer, "name"), propertyValue(createdLayer, "name"))
	
	layerData <- loadLayerData(createdLayer)
	checkTrue(grepl(sprintf("%s$", "data.tab"), layerData[1]))
}

integrationTestMultipleText <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = .getCache("rIntegrationTestProject")$id
			)
	)
	
	createdDataset <- createEntity(entity=dataset)
	checkEquals(propertyValue(dataset, "name"), propertyValue(createdDataset, "name"))
	
	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	dataFile <- file.path(tempdir(), "data.tab")
	dataFile2 <- file.path(tempdir(), "data2.tab")
	write.table(data, file=dataFile, sep="\t", quote=F, row.names=F)
	write.table(data, file=dataFile2, sep="\t", quote=F, row.names=F)
	
	layer <- Layer(entity = list(
					name = 'R Integration Test Layer',
					type = 'C',
					parentId = propertyValue(createdDataset, "id")
			)
	)
	
	checkException(storeLayerDataFiles(entity=layer, layerDataFile = c(dataFile, dataFile2)))
}

integrationTestBinary <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = .getCache("rIntegrationTestProject")$id
			)
	)
	
	createdDataset <- createEntity(entity=dataset)
	checkEquals(propertyValue(dataset, "name"), propertyValue(createdDataset, "name"))
	
	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	layer <- Layer(entity = list(
					name = 'R Integration Test Layer',
					type = 'C',
					parentId = propertyValue(createdDataset, "id")
			)
	)
	
	createdLayer <- storeLayerData(entity=layer, data)
	checkEquals(propertyValue(layer, "name"), propertyValue(createdLayer, "name"))
	files <- .cacheFiles(propertyValue(createdLayer,"id"))
	checkTrue(grepl(sprintf("%s$", "data.rbin"), files[1]))
	
	layerData <- loadLayerData(createdLayer)
	checkEquals(data, layerData$data)
}


integrationTestMultipleBinary <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = .getCache("rIntegrationTestProject")$id
			)
	)
	
	createdDataset <- createEntity(entity=dataset)
	checkEquals(propertyValue(dataset, "name"), propertyValue(createdDataset, "name"))
	
	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	data2 <- data
	
	layer <- Layer(entity = list(
					name = 'R Integration Test Layer',
					type = 'C',
					parentId = propertyValue(createdDataset, "id")
			)
	)
	
	checkException(storeLayerData(entity=layer, dataFile, dataFile2))
}