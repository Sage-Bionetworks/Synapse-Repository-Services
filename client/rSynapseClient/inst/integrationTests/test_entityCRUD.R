.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestCRUD <- function() {
	# Create a project
	project <- RJSONIO::emptyNamedList
	project$name = paste('R Entity CRUD Integration Test Project', gsub(':', '_', date()))
	createdProject <- synapseClient:::createProject(entity=project)
	checkEquals(project$name, createdProject$name)

	# Create a dataset
	dataset <- RJSONIO::emptyNamedList
	dataset$name = 'R Integration Test Dataset'
	dataset$status = 'test create'
	dataset$parentId <- createdProject$id
	createdDataset <- synapseClient:::createDataset(entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	checkEquals(dataset$status, createdDataset$status)
	
	# Get a dataset (this is redundant, it should be exactly the same as the one returned by createDataset)
	storedDataset <- synapseClient:::getDataset(entity=createdDataset$id)
	checkEquals(dataset$name, storedDataset$name)
	checkEquals(dataset$status, storedDataset$status)
	checkEquals(createdDataset, storedDataset)
	
	# Modify a dataset
	storedDataset$status <- 'test update'
	modifiedDataset <- synapseClient:::updateDataset(entity=storedDataset)
	checkEquals(dataset$name, modifiedDataset$name)
	checkEquals(storedDataset$id, modifiedDataset$id)
	checkTrue(dataset$status != modifiedDataset$status)
	checkEquals('test update', modifiedDataset$status)
	
	# Get dataset annotations
	annotations <- synapseClient:::getAnnotations(entity=modifiedDataset)
	annotations$stringAnnotations$myNewAnnotationKey <- 'my new annotation value'
	storedAnnotations <- synapseClient:::updateAnnotations(annotations=annotations)
	checkEquals('my new annotation value', storedAnnotations$stringAnnotations$myNewAnnotationKey)
	
	# Modify a project
	createdProject$name <- 'R Entity CRUD Integration Test Project NEW NAME'
	modifiedProject <- synapseClient:::updateProject(entity = createdProject)
	checkEquals(createdProject$id, modifiedProject$id)
	checkTrue(project$name != modifiedProject$name)
	checkEquals("R Entity CRUD Integration Test Project NEW NAME", modifiedProject$name)
	
	# Delete a Project
	synapseClient:::deleteProject(entity=createdProject)
	
	# Confirm that its gone
	checkException(synapseClient:::getDataset(entity=modifiedDataset))
}
