# Test copy entity method
# 
# Author: Matt Furia
###############################################################################

unitTestCopyLoadedObjects <- 
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(objects(layer@objects)), 0)
	
	layer@objects$boo <- "blah"
	
	attachedCopy <- layer
	checkEquals(length(objects(attachedCopy@objects)), 1)
	
	newLayer <- new(Class="Layer")
	checkEquals(length(objects(newLayer@objects)), 0)
	
	layer@objects$foo <- "bar"
	checkTrue(length(objects(layer@objects)) == 2)
	checkTrue(length(objects(attachedCopy@objects)) == 2)
	checkTrue(all(objects(attachedCopy@objects) == objects(layer@objects)))
	checkEquals(layer@objects$foo, attachedCopy@objects$foo)
	checkEquals(layer@objects$boo, attachedCopy@objects$boo)
	
	detachedCopy <- copyEntity(layer)
	checkTrue(length(objects(detachedCopy@objects)) == 2)
	checkTrue(all(objects(detachedCopy@objects) == objects(layer@objects)))
	checkEquals(layer@objects$foo, detachedCopy@objects$foo)
	
	layer@objects$bar <- "foo"
	checkTrue(length(objects(layer@objects)) == 3)
	checkTrue(length(objects(attachedCopy@objects)) == 3)
	checkTrue(length(objects(detachedCopy@objects)) == 2)
	
}

unitTestCopyLocation <-
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(layer@location@files), 0)
	
	layer@location@files[1] <- "/fakeDir/fakefile.txt"
	checkEquals(length(layer@location@files), 1)
	
	copy <- copyEntity(layer)
	checkEquals(length(copy@location@files), 1)
	checkTrue(all(copy@location@files == layer@location@files))
}