# TODO: Add comment
# 
# Author: mfuria
###############################################################################
.setUp <- function(){
	synapseClient:::.setCache("testProjectName", paste('R Entity S4 CRUD Integration Test Project', gsub(':', '_', date())))
}

.tearDown <- function(){
	if(!is.null(synapseClient:::.getCache("testProject"))) {
		deleteEntity(synapseClient:::.getCache("testProject"))	
		synapseClient:::.deleteCache("testProject")
	}
}

integrationTestCreateS4Entities <- function(){
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- synapseClient:::.getCache("testProjectName")
	createdProject <- createEntity(project)
	synapseClient:::.setCache("testProject", createdProject)
	checkEquals(propertyValue(createdProject,"name"), synapseClient:::.getCache("testProjectName"))
	
	## Create DataSet
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(createdProject, "id")
	createdDataset <- createEntity(dataset)
	checkEquals(propertyValue(createdDataset,"name"), propertyValue(dataset, "name"))
	checkEquals(propertyValue(createdDataset,"parentId"), propertyValue(createdProject, "id"))
	dataset <- createdDataset
	
	## Create Layer
	layer <- new(Class = "PhenotypeLayer")
	propertyValue(layer, "name") <- "testPhenoLayerName"
	propertyValue(layer, "parentId") <- propertyValue(dataset,"id")
	checkEquals(propertyValue(layer,"type"), "C")
	createdLayer <- createEntity(layer)
	checkEquals(propertyValue(createdLayer,"name"), propertyValue(layer, "name"))
	checkEquals(propertyValue(createdLayer,"parentId"), propertyValue(dataset, "id"))
	
	## expression
	layer <- new(Class = "ExpressionLayer")
	propertyValue(layer, "name") <- "testExprLayerName"
	propertyValue(layer, "parentId") <- propertyValue(dataset,"id")
	checkEquals(propertyValue(layer,"type"), "E")
	createdLayer <- createEntity(layer)
	checkEquals(propertyValue(createdLayer,"name"), propertyValue(layer, "name"))
	checkEquals(propertyValue(createdLayer,"parentId"), propertyValue(dataset, "id"))
	
	## genotype
	layer <- new(Class = "GenotypeLayer")
	propertyValue(layer, "name") <- "testGenoLayerName"
	propertyValue(layer, "parentId") <- propertyValue(dataset,"id")
	checkEquals(propertyValue(layer,"type"), "G")
	createdLayer <- createEntity(layer)
	checkEquals(propertyValue(createdLayer,"name"), propertyValue(layer, "name"))
	checkEquals(propertyValue(createdLayer,"parentId"), propertyValue(dataset, "id"))
	
	## Create Location
	location <- new(Class = "Location")
	propertyValue(location, "parentId") <- propertyValue(createdLayer,"id")
	propertyValue(location, "type") <- "awss3"
	propertyValue(location, "path") <- "fakeFile.txt"
	propertyValue(location, "md5sum") <- "80ca8c7c1c83310e471b8c4b19a86cc9"
	createdLocation <- createEntity(location)
	checkEquals(propertyValue(createdLocation,"md5sum"), propertyValue(location, "md5sum"))
	checkEquals(propertyValue(createdLocation,"parentId"), propertyValue(createdLayer, "id"))
	checkEquals(propertyValue(createdLocation,"type"), propertyValue(location, "type"))
	
}

integrationTestCreateEntityWithAnnotations <- 
		function()
{
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- synapseClient:::.getCache("testProjectName")
	annotValue(project, "annotationKey") <- "projectAnnotationValue"
	createdProject <- createEntity(project)
	synapseClient:::.setCache("testProject", createdProject)
	checkEquals(annotValue(createdProject, "annotationKey"), annotValue(project, "annotationKey"))
	
	## Create Dataset
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(createdProject, "id")
	annotValue(dataset, "annotKey") <- "annotValue"
	createdDataset <- createEntity(dataset)
	checkEquals(propertyValue(createdDataset,"name"), propertyValue(dataset, "name"))
	checkEquals(propertyValue(createdDataset,"parentId"), propertyValue(createdProject, "id"))
	checkEquals(annotValue(createdDataset,"annotKey"), annotValue(dataset, "annotKey"))
	
}

integrationTestUpdateS4Entity <-
		function()
{
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- synapseClient:::.getCache("testProjectName")
	createdProject <- createEntity(project)
	synapseClient:::.setCache("testProject", createdProject)
	checkEquals(propertyValue(createdProject,"name"), propertyValue(project,"name"))
	
	## set an annotation value and update. 
	annotValue(createdProject, "newKey") <- "newValue"
	updatedProject <- updateEntity(createdProject)
	checkEquals(propertyValue(updatedProject,"id"), propertyValue(createdProject,"id"))
	checkTrue(propertyValue(updatedProject, "etag") != propertyValue(createdProject, "etag"))
	
	## create a dataset
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(createdProject, "id")
	createdDataset <- createEntity(dataset)
	
	## update the dataset annotations
	annotValue(createdDataset, "newKey") <- "newValue"
	updatedDataset <- updateEntity(createdDataset)
	checkEquals(annotValue(createdDataset, "newKey"), annotValue(updatedDataset, "newKey"))
	checkTrue(propertyValue(createdDataset, "etag") != propertyValue(updatedDataset, "etag"))
	checkEquals(propertyValue(createdDataset, "id"), propertyValue(updatedDataset, "id"))
	
	## create a layer
	layer <- new(Class = "PhenotypeLayer")
	propertyValue(layer, "name") <- "testPhenoLayerName"
	propertyValue(layer, "parentId") <- propertyValue(createdDataset,"id")
	createdLayer <- createEntity(layer)
  	checkEquals(propertyValue(createdLayer,"name"), propertyValue(layer,"name"))

	
	## update the description property
	propertyValue(createdLayer, "description") <- "This is a description"
	updatedLayer <- updateEntity(createdLayer)
	checkEquals(propertyValue(createdLayer, "description"), propertyValue(updatedLayer, "description"))
	
	## update the description property on a project
	createdProject <- refreshEntity(createdProject)
	propertyValue(createdProject, "description") <- "This is a new description"
	updatedProject <- updateEntity(createdProject)
	checkEquals(propertyValue(createdProject, "description"), propertyValue(updatedProject, "description"))
	
	## Create Location
	location <- new(Class = "Location")
	propertyValue(location, "parentId") <- propertyValue(createdLayer,"id")
	propertyValue(location, "type") <- "awss3"
	propertyValue(location, "path") <- "fakeFile.txt"
	propertyValue(location, "md5sum") <- "80ca8c7c1c83310e471b8c4b19a86cc9"
	createdLocation <- createEntity(location)
	checkEquals(propertyValue(createdLocation,"md5sum"), propertyValue(location, "md5sum"))
	checkEquals(propertyValue(createdLocation,"parentId"), propertyValue(createdLayer, "id"))
	checkEquals(propertyValue(createdLocation,"type"), propertyValue(location, "type"))
	
	## update the location
	propertyValue(createdLocation, "md5sum") <- "f0d66fb8fd48901050b32d060c4de3c9"
	annotValue(createdLocation, "anAnnotation") <- "anAnnotationValue"
	updatedLocation <- updateEntity(createdLocation)
	checkEquals(propertyValue(createdLocation, "md5sum"), propertyValue(updatedLocation, "md5sum"))
	checkTrue(is.null(annotValue(updatedLocation, "anAnnotation")))
	checkEquals(propertyValue(createdLocation, "id"), propertyValue(updatedLocation, "id"))
}

integrationTestDeleteEntity <- 
		function()
{
	project <- new(Class="Project")
	propertyValue(project,"name") <- synapseClient:::.getCache("testProjectName")
	createdProject <- createEntity(project)
	synapseClient:::.setCache("testProject", createdProject)
	
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(createdProject, "id")
	createdDataset <- createEntity(dataset)
	
	deleteEntity(createdProject)
	checkException(refreshEntity(createdDataset))
	checkException(refreshEntity(createdProject))
	synapseClient:::.deleteCache("testProject")
}

integrationTestGetEntity <-
		function()
{
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- synapseClient:::.getCache("testProjectName")
	createdProject <- createEntity(project)
	synapseClient:::.setCache("testProject", createdProject)
	checkEquals(propertyValue(createdProject,"name"), synapseClient:::.getCache("testProjectName"))
	
	fetchedProject <- getEntity(as.numeric(propertyValue(createdProject, "id")))
	checkEquals(propertyValue(fetchedProject, "id"), propertyValue(createdProject, "id"))
	checkEquals(propertyValue(fetchedProject,"name"), synapseClient:::.getCache("testProjectName"))
	
	fetchedProject <- getEntity(as.character(propertyValue(createdProject, "id")))
	checkEquals(propertyValue(fetchedProject, "id"), propertyValue(createdProject, "id"))
	checkEquals(propertyValue(fetchedProject,"name"), synapseClient:::.getCache("testProjectName"))
	
	fetchedProject <- getEntity(synapseClient:::.extractEntityFromSlots(createdProject))
	checkEquals(propertyValue(fetchedProject, "id"), propertyValue(createdProject, "id"))
	checkEquals(propertyValue(fetchedProject,"name"), synapseClient:::.getCache("testProjectName"))
	
	fetchedProject <- getEntity(createdProject)
	checkEquals(propertyValue(fetchedProject, "id"), propertyValue(createdProject, "id"))
	checkEquals(propertyValue(fetchedProject,"name"), synapseClient:::.getCache("testProjectName"))
}

