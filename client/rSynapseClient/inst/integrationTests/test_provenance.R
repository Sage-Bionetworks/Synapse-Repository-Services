# Integration tests for provenance
# 
# Author: deflaux
###############################################################################
.setUp <- function(){
	synapseClient:::.setCache("testProjectName", paste('Provenance Integration Test Project', gsub(':', '_', date())))
}

.tearDown <- function(){
	if(!is.null(synapseClient:::.getCache("testProject"))) {
		deleteEntity(synapseClient:::.getCache("testProject"))	
		synapseClient:::.deleteCache("testProject")
	}
}

integrationTestProvenance <- function() {

	## Create Project
	project <- createEntity(
		Project(
			list(
				name=synapseClient:::.getCache("testProjectName")
				)))
	synapseClient:::.setCache("testProject", project)
	checkEquals(propertyValue(project,"name"), synapseClient:::.getCache("testProjectName"))
	
	## Create Dataset
	dataset <- createEntity(
		Dataset(
			list(
				name="testDatasetName",
				parentId=propertyValue(project, "id")
				)))
	checkEquals(propertyValue(dataset,"name"), "testDatasetName")
	checkEquals(propertyValue(dataset,"parentId"), propertyValue(project, "id"))
	
	## Create Layer
	layer <- new(Class = "PhenotypeLayer")
	propertyValue(layer, "name") <- "testPhenoLayerName"
	propertyValue(layer, "parentId") <- propertyValue(dataset,"id")
	checkEquals(propertyValue(layer,"type"), "C")
	layer <- createEntity(layer)
	checkEquals(propertyValue(layer,"name"), "testPhenoLayerName")
	checkEquals(propertyValue(layer,"parentId"), propertyValue(dataset, "id"))
	inputLayer <- layer

	## Start a new step
	step <- startStep()

	## The command line used to invoke this should be stored in the commandLine field
	checkEquals(paste(commandArgs(), collapse=" "), propertyValue(step, 'commandLine'))
	
	## Get a layer, it will be added as input
	layer <- getEntity(inputLayer)
	step <- getStep()
	checkEquals(propertyValue(inputLayer, "id"), propertyValue(step, "input")[[1]]$targetId)
 	
	## Create a layer, it will be added as output
	layer <- new(Class = "ExpressionLayer")
	propertyValue(layer, "name") <- "testExprLayerName"
	propertyValue(layer, "parentId") <- propertyValue(dataset,"id")
	checkEquals(propertyValue(layer,"type"), "E")
	layer <- createEntity(layer)
	checkEquals(propertyValue(layer,"name"), "testExprLayerName")
	checkEquals(propertyValue(layer,"parentId"), propertyValue(dataset, "id"))
	outputLayer <- layer
	step <- getStep()
	checkEquals(propertyValue(outputLayer, "id"), propertyValue(step, "output")[[1]]$targetId)
	
	## Create an Analysis, it will become the parent of the step
	analysis <- createEntity(Analysis(list(parentId=propertyValue(project, "id"),
																				 name='test analysis name',
																				 description='test analysis description')))
	step <- getStep()
	checkEquals(propertyValue(analysis, "id"), propertyValue(step, "parentId"))
	
	step <- stopStep()
	checkTrue(0 < propertyValue(step, 'endDate'))
	checkTrue(10 < length(propertyValue(step, 'environmentDescriptors')))
	checkTrue(0 < length(annotValue(step, 'rHistory')))
}
