# Unit test for renaming Layer entity objects
# 
# Author: Matt Furia
###############################################################################

unitTestRename <-
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	
	renameObject(layer, "foo", "bar")
	checkEquals(length(layer$objects), 1L)
	checkEquals(names(layer$objects), "bar")
}

unitTestCatchReturn <-
		function()
{
	layer <- new("Layer", list(name="testLayer"))
	addObject(layer, "foo")
	
	caughtLayer <- renameObject(layer, "foo", "bar")
	checkEquals(length(caughtLayer$objects), 1L)
	checkEquals(names(caughtLayer$objects), "bar")
	checkEquals(propertyValue(layer, "name"), propertyValue(caughtLayer, "name"))
}

unitTestRenameInvalidObject <-
		function()
{
	layer <- new("Layer")
	checkException(renameObject(layer, "foo", "bar"))
	
	addObject(layer, "foo")
	checkException(renameObject(layer, "bar", "boo"))
}

unitTestRenameSameName <-
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	renameObject(layer, "foo", "foo")
	checkEquals(length(layer$objects), 1L)
	checkEquals(names(layer$objects), "foo")
}

unitTestMultipleRename <-
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo", "foo")
	addObject(layer, "goo", "goo")
	addObject(layer, "keepMe", "ok")
	
	renameObject(layer, c("foo", "goo"), c("goo", "foo"))
	checkEquals(length(layer$objects), 3L)
	checkEquals(layer$objects$foo, "goo")
	checkEquals(layer$objects$goo, "foo")
	checkEquals(layer$objects$ok, "keepMe")
	
}

unitTestMoreNamesThanObjects <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	checkException(renameObject(layer, "foo", c("bar", "boo")))
}

unitTestMoreObjectsThanNames <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	addObject(layer, "boo")
	checkException(renameObject(layer, c("foo","boo"), "boo"))
}

unitTestInvalidWhich <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	checkException(renameObject(layer, "boo", "goo"))
}

unitTestOverwrite <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	addObject(layer, "goo")
	renameObject(layer, "foo", "goo")
	checkEquals(length(layer$objects), 1L)
	checkEquals(layer$objects$goo, "foo")
}

unitTestRenameToNew <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	renameObject(layer, "foo", "goo")
	checkEquals(length(layer$objects), 1L)
	checkEquals(layer$objects$goo, "foo")
}

unitTestOverwriteOneRenameToNewAnother <- 
		function()
{
	layer <- new("Layer")
	addObject(layer, "foo")
	addObject(layer, "goo")
	renameObject(layer, c("foo", "goo"), c("goo", "bar"))
	checkEquals(length(layer$objects), 2L)
	checkEquals(layer$objects$goo, "foo")
	checkEquals(layer$objects$bar, "goo")
}


