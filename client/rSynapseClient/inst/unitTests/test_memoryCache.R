# Unit tests for in-memory cache for global variables
# 
# Author: Matt Furia
###############################################################################

.setUp <- 
		function()
{
	synapseClient:::.setCache("testGetKey", "testGetValue")
	synapseClient:::.setCache("testDeleteKey", "testDeleteValue")
	synapseClient:::.setCache("testSetKey", "testSetValue")
}

.tearDown <- 
		function()
{
	synapseClient:::.deleteCache("testGetKey")
	synapseClient:::.deleteCache("testDeleteKey")
	synapseClient:::.deleteCache("testSetKey")
}

unitTestGetCacheValue <- 
		function()
{
	checkEquals(synapseClient:::.getCache("testGetKey"), "testGetValue")
}

unitTestSetCacheValue <- 
		function()
{
	checkEquals(synapseClient:::.getCache("testSetKey"), "testSetValue")
	synapseClient:::.setCache("testSetKey", "testSetValueNew")
	checkEquals(synapseClient:::.getCache("testSetKey"), "testSetValueNew")
}

unitTestDeleteCacheValue <- 
		function()
{
	synapseClient:::.deleteCache("testDeleteKey")
	checkTrue(is.null(synapseClient:::.getCache("testDeleteKey")))
}