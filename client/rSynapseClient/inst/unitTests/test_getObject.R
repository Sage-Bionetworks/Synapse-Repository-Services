# Unit tests for getObject method
# 
# Author: Matt Furia
###############################################################################


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