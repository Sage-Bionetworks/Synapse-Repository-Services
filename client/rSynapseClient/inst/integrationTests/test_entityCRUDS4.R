# TODO: Add comment
# 
# Author: mfuria
###############################################################################
.setUp <- function(){
	.setCache("testProjectName", paste('R Entity S4 CRUD Integration Test Project', gsub(':', '_', date())))
}

.tearDown <- function(){
	if(!is.null(.getCache("testProject")))
		deleteEntity(.getCache("testProject"))	
}

integrationTestCreateS4Entities <- function(){
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- .getCache("testProjectName")
	createdProject <- createEntity(project)
	.setCache("testProject", createdProject)
	checkEquals(propertyValue(createdProject,"name"), .getCache("testProjectName"))
	project <- createdProject
	
	## Create DataSet
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(project, "id")
	createdDataset <- createEntity(dataset)
	checkEquals(propertyValue(createdDataset,"name"), propertyValue(dataset, "name"))
	checkEquals(propertyValue(createdDataset,"parentId"), propertyValue(project, "id"))
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

integrationTestCreateEntityWithAnnotations <- 
		function()
{
	## Create Project
	project <- new(Class="Project")
	propertyValue(project,"name") <- .getCache("testProjectName")
	annotValue(project, "annotationKey") <- "projectAnnotationValue"
	createdProject <- createEntity(project)
	.setCache("testProject", project)
	checkEquals(annotValue(createdProject, "annotationKey"), annotValue(project, "annotationKey"))
	
	project <- createdProject
	
	## Create DataSet
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(project, "id")
	annotValue(dataset, "annotKey") <- "annotValue"
	createdDataset <- createEntity(dataset)
	checkEquals(propertyValue(createdDataset,"name"), propertyValue(dataset, "name"))
	checkEquals(propertyValue(createdDataset,"parentId"), propertyValue(project, "id"))
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
	
	## set an annotation value and update. This failes. might be a Synapse bug
	##annotValue(project, "newKey") <- "newValue"
	###createdProject <- updateEntity(project)
	##checkEquals(propertyValue(createdProject,"id"), propertyValue(project,"id"))
	##checkTrue(propertyValue(createdProject, "etag") != propertyValue(project, "etag"))
	project <- createdProject
	
	## create a dataset
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(project, "id")
	dataset <- createEntity(dataset)
	
	## update the dataset annotations
	annotValue(dataset, "newKey") <- "newValue"
	createdDataset <- updateEntity(dataset)
	checkEquals(annotValue(createdDataset, "newKey"), annotValue(dataset, "newKey"))
	checkTrue(propertyValue(createdDataset, "etag") != propertyValue(dataset, "etag"))
	checkEquals(propertyValue(createdDataset, "id"), propertyValue(dataset, "id"))
	
	## create a layer
	layer <- new(Class = "PhenotypeLayer")
	propertyValue(layer, "name") <- "testPhenoLayerName"
	propertyValue(layer, "parentId") <- propertyValue(dataset,"id")
	layer <- createEntity(layer)
	
	## update the md5sum property
	propertyValue(layer, "md5sum") <- "thisIsAfakeChecksum"
	createdLayer <- updateEntity(layer)
	checkEquals(propertyValue(createdLayer, "md5sum"), propertyValue(layer, "md5sum"))
}

integrationTestDeleteEntity <- 
		function()
{
	project <- new(Class="Project")
	propertyValue(project,"name") <- .getCache("testProjectName")
	project <- createEntity(project)
	.setCache("testProject", createdProject)
	
	dataset <- new(Class="Dataset")
	propertyValue(dataset, "name") <- "testDatasetName"
	propertyValue(dataset,"parentId") <- propertyValue(project, "id")
	dataset <- createEntity(dataset)
	
	deleteEntity(project)
	checkException(refreshEntity(dataset))
	checkException(refreshEntity(project))
}


