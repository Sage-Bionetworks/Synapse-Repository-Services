.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestForgotToLogin <- function() {
  # Check that this fails as it should
  checkException(synapseGet('/dataset/0', anonymous=F))
}

integrationTestAnonymousAccess <- function() {
  dataset <- synapseGet('/dataset/0', anonymous=T)
  # The fields returned by this service API may change over time, but
  # there are a few we should always expect to receive
  checkTrue("id" %in% names(dataset))
  checkTrue("name" %in% names(dataset))
  checkTrue("version" %in% names(dataset))
  checkTrue("status" %in% names(dataset))
  checkTrue("id" %in% names(dataset))
  checkTrue("uri" %in% names(dataset))
}

