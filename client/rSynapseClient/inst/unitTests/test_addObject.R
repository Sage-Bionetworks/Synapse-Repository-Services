# Unit tests for adding objects to Layer entity
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

unitTestDefaultName <-
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(layer@objects), 0L)
	
	id <- 1234
	addObject(layer, id)
	
	checkEquals(length(layer@objects), 1L)
	checkEquals(objects(layer@objects), "id")
	checkEquals(layer$objects$id, id)
}

unitTestDefaultNameStringConstant <-
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(layer@objects), 0L)
	
	addObject(layer, "foo")
	
	checkEquals(length(layer@objects), 1L)
	checkEquals(objects(layer@objects), "foo")
	checkEquals(layer$objects$foo, "foo")
}

unitTestDefaultNameNumericExpression <-
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(layer@objects), 0L)
	
	addObject(layer, 1:5)
	
	checkEquals(length(layer@objects), 1L)
	
	checkEquals(objects(layer@objects), "1:5")
	checkTrue(all(layer$objects[["1:5"]] == 1:5))
}

unitTestSpecifyName <-
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(layer@objects), 0L)
	
	name <- "aName"
	
	id <- 1234
	addObject(layer, id, name)
	
	checkEquals(length(layer@objects), 1L)
	checkEquals(objects(layer@objects), name)
	checkEquals(layer$objects[[name]], id)
}

unitTestSpecifyNameStringConstant <-
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(layer@objects), 0L)
	
	name <- "aName"
	addObject(layer, "foo", name)
	
	checkEquals(length(layer@objects), 1L)
	checkEquals(objects(layer@objects), name)
	checkEquals(layer$objects[[name]], "foo")
	
}

unitTestCatchReturnValue <-
		function()
{
	layer <- new("Layer",list(name="testLayer"))
	
	caughtLayer <- addObject(layer, "foo")
	checkEquals(names(caughtLayer$objects), "foo")
	
	checkEquals(propertyValue(layer, "name"), propertyValue(caughtLayer, "name"))
}

unitTestAddSingleAsList <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, list(foo="bar"))
	checkEquals(layer$objects$foo, "bar")
	checkEquals(length(layer$objects), 1L)
}

unitTestAddMultipleAsList <-
		function()
{
	layer <- new("Layer")
	addObject(layer, list(foo="bar", boo="goo"))
	checkEquals(layer$objects$foo, "bar")
	checkEquals(layer$objects$boo, "goo")
	checkEquals(length(layer$objects), 2L)
}

unitTestAddSingleAsListNoName <-
		function()
{
	layer <- new("Layer")
	checkException(addObject(layer, list("bar")))
}

unitTestAddMultipleAsListNoName <-
		function()
{
	layer <- new("Layer")
	checkException(addObject(layer, list("bar", foo="goo")))
}

unitTestAddMatrixSpecifyName <-
		function()
{
	layer <- new("Layer")
	addObject(layer, diag(nrow=10, ncol=10), "diag")
	checkTrue(all(layer@objects$diag == diag(nrow=10, ncol=10)))
}

unitTestAddMatrixNoName <-
		function()
{
	layer <- new("Layer")
	addObject(layer, diag(nrow=10, ncol=10))
	checkEquals(length(layer$objects), 1L)
	checkTrue(all(layer$objects[[1]] == diag(nrow=10, ncol=10)))
}

unitTestAddDataFrameNoName <-
		function()
{
	layer <- new(Class="Layer")
	data <- data.frame(x=1:4, y=c("a", "b", "c", "d"))
	addObject(layer, data)
	checkEquals(length(objects(layer@objects)), 1L)
	checkTrue(all(layer$objects$data == data))
}

unitTestAddDataFrameWithName <-
		function()
{
	layer <- new(Class="Layer")
	data <- data.frame(x=1:4, y=c("a", "b", "c", "d"))
	addObject(layer, data, "newName")
	checkEquals(length(objects(layer@objects)), 1L)
	checkTrue(all(get("newName", envir=layer@objects)== data))
}

unitTestAddListOfDataFramesNoName <-
		function()
{
	layer <- new(Class="Layer")
	checkException(addObject(layer, list(data.frame(x=1:4, y=c("a", "b", "c", "d")), data.frame(y=c("a", "b", "c", "d"), z=5:8))))
}

