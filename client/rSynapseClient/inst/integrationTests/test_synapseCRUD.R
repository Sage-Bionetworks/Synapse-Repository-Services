.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestCRUD <- function() {

	# Create a project
	project <- list()
	project$name = 'R Integration Test Project'
	createdProject <- createProject(entity=project)
	checkEquals(project$name, createdProject$name)
	
	# Create a dataset
	dataset <- list()
	dataset$name = 'R Integration Test Dataset'
	dataset$status = 'test create'
	dataset$parentId <- createdProject$id
	createdDataset <- synapsePost(uri='/dataset', entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	checkEquals(dataset$status, createdDataset$status)
	
	# Get a dataset
	storedDataset <- synapseGet(uri=createdDataset$uri)
	checkEquals(dataset$name, storedDataset$name)
	checkEquals(dataset$status, storedDataset$status)
	
	# Modify a dataset
	storedDataset$status <- 'test update'
	modifiedDataset <- synapsePut(uri=storedDataset$uri, entity=storedDataset)
	checkEquals(dataset$name, modifiedDataset$name)
	checkTrue(dataset$status != modifiedDataset$status)
	checkEquals('test update', modifiedDataset$status)
	
	# Get dataset annotations
	annotations <- getAnnotations(entity=modifiedDataset)
	annotations$stringAnnotations$myNewAnnotationKey <- 'my new annotation value'
	storedAnnotations <- updateAnnotations(annotations=annotations)
	checkEquals('my new annotation value', storedAnnotations$stringAnnotations$myNewAnnotationKey)
	
	# Delete a dataset
	synapseDelete(uri=modifiedDataset$uri)
	
	# Confirm that its gone
	checkException(synapseGet(uri=createdDataset$uri))
	
 	# Delete a Project
	deleteProject(entity=createdProject)
}

