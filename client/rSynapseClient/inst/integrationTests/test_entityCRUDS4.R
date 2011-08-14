# TODO: Add comment
# 
# Author: mfuria
###############################################################################
.setUp <- function(){
	.setCache("testProjectName", paste('R Entity S4 CRUD Integration Test Project', gsub(':', '_', date())))
}

.tearDown <- function(){
	if(!is.null(.getCache("testProject"))) {
		deleteEntity(.getCache("testProject"))	
		.deleteCache("testProject")
	}
}

integrationTestCreateS4Entities <- function(){
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- .getCache("testProjectName")
	createdProject <- createEntity(project)
	.setCache("testProject", createdProject)
	checkEquals(propertyValue(createdProject,"name"), .getCache("testProjectName"))
	
	## Create DataSet
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(createdProject, "id")
	createdDataset <- createEntity(dataset)
	checkEquals(propertyValue(createdDataset,"name"), propertyValue(dataset, "name"))
	checkEquals(propertyValue(createdDataset,"parentId"), propertyValue(createdProject, "id"))
	dataset <- createdDataset
	
	## Create Layer
	## phenotype
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
#	location <- new(Class = "Location")
#	propertyValue(layer, "name") <- "testLocationName"
#	propertyValue(layer, "parentId") <- propertyValue(layer,"id")
#	createdLayer <- createEntity(layer)
#	checkEquals(propertyValue(createdLayer,"name"), propertyValue(layer, "name"))
#	checkEquals(propertyValue(createdLayer,"parentId"), propertyValue(layer, "id"))
	
}

# THIS TEST IS BROKEN
DISABLEintegrationTestCreateEntityWithAnnotations <- 
		function()
{
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- .getCache("testProjectName")
	annotValue(project, "annotationKey") <- "projectAnnotationValue"
	createProject <- createEntity(project)
	.setCache("testProject", createdProject)
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
	propertyValue(project,"name") <- .getCache("testProjectName")
	createdProject <- createEntity(project)
	.setCache("testProject", createdProject)
	checkEquals(propertyValue(createdProject,"name"), propertyValue(project,"name"))
	
	## set an annotation value and update. 
  ## R CLIENT UPDATES ARE BROKEN, THIS IS NOT A SYNAPSE BUG
	##annotValue(createdProject, "newKey") <- "newValue"
	###updatedProject <- updateEntity(createdProject)
	##checkEquals(propertyValue(updatedProject,"id"), propertyValue(createdProject,"id"))
	##checkTrue(propertyValue(updatedProject, "etag") != propertyValue(createdProject, "etag"))
	
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
  # UPDATES ARE BROKEN
#	checkEquals(propertyValue(createdLayer, "description"), propertyValue(updatedLayer, "description"))
}

integrationTestDeleteEntity <- 
		function()
{
	project <- new(Class="Project")
	propertyValue(project,"name") <- .getCache("testProjectName")
	createdProject <- createEntity(project)
	.setCache("testProject", createdProject)
	
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(createdProject, "id")
	createdDataset <- createEntity(dataset)
	
	deleteEntity(createdProject)
	checkException(refreshEntity(createdDataset))
	checkException(refreshEntity(createdProject))
	.deleteCache("testProject")
}


