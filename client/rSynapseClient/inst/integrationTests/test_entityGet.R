.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestGet <- function() {
	packets <- synapseQuery('select * from dataset where dataset.name == "MSKCC Prostate Cancer"')
	
	dataset <- getDataset(packets$dataset.id[1])
	# The fields returned by this service API may change over time, but
	# there are a few we should always expect to receive
	checkTrue("id" %in% names(dataset))
	checkTrue("name" %in% names(dataset))
	checkTrue("version" %in% names(dataset))
	checkTrue("status" %in% names(dataset))
	checkTrue("id" %in% names(dataset))
	checkTrue("uri" %in% names(dataset))
}

