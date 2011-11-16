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
	
	storedLayer <- storeEntityFiles(layer)
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

integrationTestStoreLayerZip <-
		function()
{
	data <- data.frame(a=1:3, b=letters[10:12],
			c=c('hello', 'world', 'have a nice day'),
			stringsAsFactors = FALSE)
	
	dataFile <- file.path(tempdir(), "data.tab")
	write.table(data, file=dataFile, sep="\t", quote=F, row.names=F)
	zipFile <- file.path(tempdir(), "data.zip")
	zip(zipfile=zipFile, files=dataFile)
	
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(list(parentId=propertyValue(dataset,"id"), type="C", name="myZippedLayer"))
	layer <- addFile(layer, zipFile)
	
	storedLayer <- storeEntity(layer)
	checkEquals(propertyValue(storedLayer, "name"), propertyValue(layer, "name"))
	checkEquals(propertyValue(storedLayer, "type"), propertyValue(layer, "type"))
	checkEquals(propertyValue(storedLayer, "parentId"), propertyValue(dataset, "id"))
	
	loadedLayer <- loadEntity(storedLayer)
	storedLayerData <- read.delim(normalizePath(file.path(loadedLayer$cacheDir, loadedLayer$files[1])), sep='\t', stringsAsFactors = FALSE)
	checkEquals(storedLayerData, data)	
}

integrationTestStoreLayerCode <-
		function()
{
	#dataset <- synapseClient:::.getCache("testDataset")
	project<- synapseClient:::.getCache("testProject") # check set up method
	code <- Code(list(name="a code layer", parentId=propertyValue(project, "id")))
	codeFile <- tempfile(fileext=".R")
	cat('run <- function(){return("executing test function")}',file=codeFile)
	code <- addFile(code, codeFile)
	code <- storeEntity(code)
	code <- loadEntity(propertyValue(code, "id"))
	checkEquals(code$objects$run(), "executing test function")
}

integrationTestStoreMediaLayer <- function() {
	
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(list(parentId=propertyValue(dataset,"id"), type="M", name="myZippedLayer"))
	
	## Make a jpeg when PLFM-498 is fixed, for now, make a fake one
	filename <- "r_integration_test_plot.jpg"
	filepath <- file.path(tempdir(), filename)
#	attach(mtcars)
#	jpeg(filename)
#	plot(wt, mpg) 
#	abline(lm(mpg~wt))
#	title("Regression of MPG on Weight")
#	dev.off()
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	write.table(data, filepath)	

	layer <- addFile(layer, filepath)
	createdLayer <- storeEntity(layer)
	loadedLayer <- loadEntity(createdLayer)
	checkEquals(1, length(loadedLayer$files))
	checkEquals(filename, loadedLayer$files[1])
}

integrationTestMultipleBinary <- 
		function()
{

	## Make an R data object that we will store in a couple different ways
	data <- data.frame(a=1:3, b=letters[10:12],
			c=seq(as.Date("2004-01-01"), by = "week", len = 3),
			stringsAsFactors = FALSE)
	
	data2 <- diag(100)
	
	dataset <- synapseClient:::.getCache("testDataset")
	layer <- Layer(entity = list(
					type = 'E',
					parentId = propertyValue(dataset, "id")
			)
	)
	
	layer <- addObject(layer, data)
    layer <- addObject(layer, data2)
	createdLayer <- storeEntity(layer)
	loadedLayer <- loadEntity(createdLayer)
	
	checkTrue(all(c("data", "data2") %in% names(loadedLayer$objects)))
	
	checkEquals(data, loadedLayer$objects$data)
	checkEquals(data2, loadedLayer$objects$data2)
}