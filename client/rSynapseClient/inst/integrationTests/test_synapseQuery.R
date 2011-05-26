.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestForgotToLogin <- function() {
  # Check that this fails as it should
  checkException(synapseQuery('select * from dataset', anonymous=F))
}

integrationTestAnonymousAccess <- function() {
  packets <- synapseQuery('select * from dataset limit 10', anonymous=T)
  # We should get back 10 datasets
  checkEquals(dim(packets)[1], 10)
  # The fields returned by this service API may change over time, but
  # there are a few we should always expect to receive
  checkTrue("dataset.id" %in% names(packets))
  checkTrue("dataset.name" %in% names(packets))
  checkTrue("dataset.version" %in% names(packets))
  checkTrue("dataset.status" %in% names(packets))
  checkTrue("dataset.species" %in% names(packets))
}

integrationTestPaging <- function() {
  firstPagePackets <- synapseQuery('select * from dataset limit 20 offset 1')
  secondPagePackets <- synapseQuery('select * from dataset limit 20 offset 21')
  # We should get back 20 datasets
  checkEquals(dim(firstPagePackets)[1], 20)
  checkEquals(dim(secondPagePackets)[1], 20)
  # And they do not overlap
  checkEquals(length(union(firstPagePackets$dataset.id,
                           secondPagePackets$dataset.id)),
              40)
}

integrationTestQueryForDataset <- function() {
  packets <- synapseQuery('select * from dataset where dataset.name == "MSKCC Prostate Cancer"', anonymous=T)
  # We should get back 1 dataset
  checkEquals(dim(packets)[1], 1)
  # And its name should match the one we searched for
  checkEquals(packets$dataset.name, "MSKCC Prostate Cancer")
}
