# Unit tests for getObject method
# 
# Author: Matt Furia
###############################################################################
.setUp <-
		function()
{
	synapseClient:::.setCache("oldSynapseCacheDir", synapseClient:::.getCache("synapseCacheDir"))
	synapseClient:::.setCache("synapseCacheDir", tempfile())
}

.tearDown <-
		function()
{
	unlink(synapseClient:::.getCache("synapseCacheDir"), recursive = TRUE)
	synapseClient:::.setCache("synapseCacheDir", synapseClient:::.getCache("oldSynapseCacheDir"))
	synapseClient:::.deleteCache("oldSynapseCacheDir")
}

unitTestGet <-
		function()
{
	layer <- new(Class="Layer")
	addObject(layer, "foo", "bar")
	checkEquals(getObject(layer, "bar"), "foo")
}

unitTestGetInvalidObject <-
		function()
{
	layer <- new(Class="Layer")
	checkException(getObject(layer, "bar"))
}