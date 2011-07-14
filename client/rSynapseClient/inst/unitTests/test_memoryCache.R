# Unit tests for in-memory cache for global variables
# 
# Author: Matt Furia
###############################################################################

.setUp <- 
		function()
{
	.setCache("testGetKey", "testGetValue")
	.setCache("testDeleteKey", "testDeleteValue")
	.setCache("testSetKey", "testSetValue")
}

.tearDown <- 
		function()
{
	.deleteCache("testGetKey")
	.deleteCache("testDeleteKey")
	.deleteCache("testSetKey")
}

unitTestGetCacheValue <- 
		function()
{
	checkEquals(.getCache("testGetKey"), "testGetValue")
}

unitTestSetCacheValue <- 
		function()
{
	checkEquals(.getCache("testSetKey"), "testSetValue")
	.setCache("testSetKey", "testSetValueNew")
	checkEquals(.getCache("testSetKey"), "testSetValueNew")
}

unitTestDeleteCacheValue <- 
		function()
{
	.deleteCache("testDeleteKey")
	checkTrue(is.null(.getCache("testDeleteKey")))
}