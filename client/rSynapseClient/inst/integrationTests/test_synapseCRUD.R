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
	createdDataset <- synapsePost('/dataset', dataset)
	checkEquals(dataset$name, createdDataset$name)
	checkEquals(dataset$status, createdDataset$status)
	
	# Get a dataset
	storedDataset <- synapseGet(createdDataset$uri)
	checkEquals(dataset$name, storedDataset$name)
	checkEquals(dataset$status, storedDataset$status)
	
	# Modify a dataset
	storedDataset$status <- 'test update'
	modifiedDataset <- synapsePut(storedDataset$uri, storedDataset)
	checkEquals(dataset$name, modifiedDataset$name)
	checkTrue(dataset$status != modifiedDataset$status)
	checkEquals('test update', modifiedDataset$status)
	
	# Delete a dataset
	synapseDelete(modifiedDataset$uri)
	
	# Confirm that its gone
	checkException(synapseGet(createdDataset$uri))
}

