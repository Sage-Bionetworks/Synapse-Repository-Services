# Unit Tests for deleting objects from Layer entities
# 
# Author: Matt Furia
###############################################################################


unitTestDelete <-
		function()
{
	layer <- new("Layer")
	addObject(layer,"foo", "bar")
	
	checkEquals(length(objects(layer@objects)), 1L)
	
	deleteObject(layer, "bar")
	checkEquals(length(objects(layer@objects)), 0L)
}

unitTestDeleteOneOfTwo <-
		function()
{
	layer <- new("Layer")
	addObject(layer,"foo")
	checkEquals(length(objects(layer@objects)), 1L)
	
	addObject(layer,"goo")
	checkEquals(length(objects(layer@objects)), 2L)
	
	deleteObject(layer, "goo")
	checkEquals(length(objects(layer@objects)), 1L)
	checkEquals(names(layer$objects), "foo")
}

unitTestDeleteAll <-
		function()
{
	layer <- new("Layer")
	addObject(layer,"foo")
	addObject(layer,"goo")
	checkEquals(length(objects(layer@objects)), 2L)
	
	deleteObject(layer, c("goo", "foo"))
	checkEquals(length(objects(layer@objects)), 0L)
}

unitTestDeleteNonExisting <-
		function()
{
	layer <- new("Layer")
	
	## this should warn, but not fail
	deleteObject(layer, "fakeObject")
}

unitTestCatchReturnValue <-
		function()
{
	layer <- new("Layer",list(name="testLayer"))
		
	addObject(layer, "foo")
	caughtLayer <- deleteObject(layer, "foo")
	checkEquals(length(layer$objects), 0L)
	
	checkEquals(propertyValue(layer, "name"), propertyValue(caughtLayer, "name"))	
}
