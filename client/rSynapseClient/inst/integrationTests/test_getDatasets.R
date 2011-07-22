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
	checkTrue("id" %in% names(datasets))
	checkTrue("name" %in% names(datasets))
	checkTrue("version" %in% names(datasets))
	checkTrue("status" %in% names(datasets))
	checkTrue("Species" %in% names(datasets))
}

integrationTestPaging <- function() {
	firstPageDatasets <- getDatasets(queryParams=list(limit=20, offset=1))
	secondPageDatasets <- getDatasets(queryParams=list(limit=20, offset=21))
	# We should get back 20 datasets
	checkEquals(nrow(firstPageDatasets), 20)
	checkEquals(nrow(secondPageDatasets), 20)
	# And they do not overlap
	checkEquals(length(union(firstPageDatasets$id,
							secondPageDatasets$id)),
			40)
}

integrationTestQueryForDataset <- function() {
	datasets <- getDatasets(queryParams=list(where='dataset.name == "MSKCC Prostate Cancer"'))
	# We should get back 1 dataset
	checkEquals(nrow(datasets), 1)
	# And its name should match the one we searched for
	checkEquals(datasets$name, "MSKCC Prostate Cancer")
}

