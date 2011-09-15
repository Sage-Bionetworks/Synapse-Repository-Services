.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestCRUD <- function() {

	# Create a project
	project <- RJSONIO::emptyNamedList
	project$name = paste('R Synapse CRUD Integration Test Project', gsub(':', '_', date()))
	createdProject <- synapseClient:::createProject(entity=project)
	checkEquals(project$name, createdProject$name)
	
	# Create a dataset
	dataset <- RJSONIO::emptyNamedList
	dataset$name = 'R Integration Test Dataset'
	dataset$status = 'test create'
	dataset$parentId <- createdProject$id
	createdDataset <- synapseClient:::synapsePost(uri='/dataset', entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	checkEquals(dataset$status, createdDataset$status)
	
	# Get a dataset
	storedDataset <- synapseClient:::synapseGet(uri=createdDataset$uri)
	checkEquals(dataset$name, storedDataset$name)
	checkEquals(dataset$status, storedDataset$status)
	
	# Modify a dataset
	storedDataset$status <- 'test update'
	modifiedDataset <- synapseClient:::synapsePut(uri=storedDataset$uri, entity=storedDataset)
	checkEquals(dataset$name, modifiedDataset$name)
	checkTrue(dataset$status != modifiedDataset$status)
	checkEquals('test update', modifiedDataset$status)
	
	# Get dataset annotations
	annotations <- synapseClient:::getAnnotations(entity=modifiedDataset)
	annotations$stringAnnotations$myNewAnnotationKey <- 'my new annotation value'
	storedAnnotations <- synapseClient:::updateAnnotations(annotations=annotations)
	checkEquals('my new annotation value', storedAnnotations$stringAnnotations$myNewAnnotationKey)
	
	# Delete a dataset
	synapseClient:::synapseDelete(uri=modifiedDataset$uri)
	
	# Confirm that its gone
	checkException(synapseClient:::synapseGet(uri=createdDataset$uri))
	
 	# Delete a Project
	synapseClient:::deleteProject(entity=createdProject)
}

