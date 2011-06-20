.setUp <- function() {
	# Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
	# Do some test cleanup stuff here, if applicable
}

integrationTestGetDatasets <- function() {
	datasets <- getDatasets()
	# The fields returned by this service API may change over time, but
	# there are a few we should always expect to receive
	checkTrue("dataset.id" %in% names(datasets))
	checkTrue("dataset.name" %in% names(datasets))
	checkTrue("dataset.version" %in% names(datasets))
	checkTrue("dataset.status" %in% names(datasets))
	checkTrue("dataset.Species" %in% names(datasets))
}

integrationTestPaging <- function() {
	firstPageDatasets <- getDatasets(list(limit=20, offset=1))
	secondPageDatasets <- getDatasets(list(limit=20, offset=21))
	# We should get back 20 datasets
	checkEquals(dim(firstPageDatasets)[1], 20)
	checkEquals(dim(secondPageDatasets)[1], 20)
	# And they do not overlap
	checkEquals(length(union(firstPageDatasets$dataset.id,
							secondPageDatasets$dataset.id)),
			40)
}

integrationTestQueryForDataset <- function() {
	datasets <- getDatasets(list(where='dataset.name == "MSKCC Prostate Cancer"'))
	# We should get back 1 dataset
	checkEquals(dim(datasets)[1], 1)
	# And its name should match the one we searched for
	checkEquals(datasets$dataset.name, "MSKCC Prostate Cancer")
}
