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
	suppressWarnings(detach('package:utils', force=TRUE))
	utils:::assignInNamespace("zip", myZip, "utils")
	#reload detached packages
	library(utils, quietly=TRUE)
	
	## create project and add to cache
	project <- list()
	project$name <- paste('R noZip Integration Test Project', gsub(':', '_', date()))
	createdProject <- synapseClient:::createProject(entity=project)
	synapseClient:::.setCache("rIntegrationTestProject", createdProject)
}

.tearDown <- function(){
	## put back method
	suppressWarnings(detach('package:utils', force = TRUE))
	utils:::assignInNamespace("zip", attr(utils:::zip, "origFcn"), "utils")
	library(utils, quietly = TRUE)
	
	## delete project 
	synapseClient:::deleteProject(entity=synapseClient:::.getCache("rIntegrationTestProject"))
	synapseClient:::.deleteCache("rIntegrationTestProject")
}

integrationTestNoZipFile <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = synapseClient:::.getCache("rIntegrationTestProject")$id
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
	layer <- createEntity(layer)
	layer <- addFile(layer, dataFile)
	layer <- updateEntity(layer)
	
	loadedLayer <- loadEntity(layer)
	checkTrue(grepl(sprintf("%s$", "data.tab"), loadedLayer$files[1]))
}

integrationTestNoZipMultipleFiles <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = synapseClient:::.getCache("rIntegrationTestProject")$id
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
	layer <- createEntity(layer)
	layer <- addFile(layer, c(dataFile, dataFile2))
	# This should fail because we've added two files to the layer but we don't have a zip utility
	checkException(storeEntity(layer))
}

integrationTestNoZipBinary <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = synapseClient:::.getCache("rIntegrationTestProject")$id
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
	
	layer <- createEntity(layer)
	layer <- addObject(layer, data)
	layer <- storeEntity(layer)
	
	loadedLayer <- loadEntity(layer)
	checkEquals(data, loadedLayer$objects$data)
}


integrationTestNoZipMultipleBinary <- 
		function()
{
	dataset <- Dataset(entity = list(
					name = 'R Integration Test Dataset',
					parentId = synapseClient:::.getCache("rIntegrationTestProject")$id
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
	layer <- createEntity(layer)
	layer <- addObject(layer, c(data, data2))
	# This should fail because we've added two objectss to the layer but we don't have a zip utility
	checkException(storeEntity(layer))
}