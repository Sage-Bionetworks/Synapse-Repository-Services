.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestCRUD <- function() {
	# Create a dataset
	dataset <- list()
	dataset$name = 'R Integration Test Dataset'
	dataset$status = 'test create'
	createdDataset <- createDataset(entity=dataset)
	checkEquals(dataset$name, createdDataset$name)
	checkEquals(dataset$status, createdDataset$status)
	
	# Get a dataset
	storedDataset <- getDatasetById(id=createdDataset$id)
	checkEquals(dataset$name, storedDataset$name)
	checkEquals(dataset$status, storedDataset$status)
	
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
	
	# Delete a dataset
	deleteDataset(entity=modifiedDataset)
	
	# Confirm that its gone
	checkException(getDatasetById(id=modifiedDataset$id))
}

