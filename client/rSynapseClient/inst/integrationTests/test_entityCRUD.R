.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestCRUD <- function() {
	# Create a project
	project <- list()
	project$name = paste('R Entity CRUD Integration Test Project', gsub(':', '_', date()))
	createdProject <- createProject(entity=project)
	checkEquals(project$name, createdProject$name)

	# Create a dataset
	dataset <- list()
	dataset$name = 'R Integration Test Dataset'
	dataset$status = 'test create'
	dataset$parentId <- createdProject$id
	createdDataset <- createDataset(entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	checkEquals(dataset$status, createdDataset$status)
	
	# Get a dataset (this is redundant, it should be exactly the same as the one returned by createDataset)
	storedDataset <- getDataset(entity=createdDataset$id)
	checkEquals(dataset$name, storedDataset$name)
	checkEquals(dataset$status, storedDataset$status)
	checkEquals(createdDataset, storedDataset)
	
	# Modify a dataset
	storedDataset$status <- 'test update'
	modifiedDataset <- updateDataset(entity=storedDataset)
	checkEquals(dataset$name, modifiedDataset$name)
	checkEquals(storedDataset$id, modifiedDataset$id)
	checkTrue(dataset$status != modifiedDataset$status)
	checkEquals('test update', modifiedDataset$status)
	
	# Get dataset annotations
	annotations <- getAnnotations(entity=modifiedDataset)
	annotations$stringAnnotations$myNewAnnotationKey <- 'my new annotation value'
	storedAnnotations <- updateAnnotations(annotations=annotations)
	checkEquals('my new annotation value', storedAnnotations$stringAnnotations$myNewAnnotationKey)
	
	# Delete a Project
	deleteProject(entity=createdProject)
	
	# Confirm that its gone
	checkException(getDataset(entity=modifiedDataset))
}

