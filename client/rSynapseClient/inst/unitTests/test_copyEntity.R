# Test copy entity method
# 
# Author: Matt Furia
###############################################################################

unitTestCopyLoadedObjects <- 
		function()
{
	layer <- new(Class="Layer")
	checkEquals(length(objects(layer@loadedObjects)), 0)
	
	layer@loadedObjects$boo <- "blah"
	
	attachedCopy <- layer
	checkEquals(length(objects(attachedCopy@loadedObjects)), 1)
	
	newLayer <- new(Class="Layer")
	checkEquals(length(objects(newLayer@loadedObjects)), 0)
	
	layer@loadedObjects$foo <- "bar"
	checkTrue(length(objects(layer@loadedObjects)) == 2)
	checkTrue(length(objects(attachedCopy@loadedObjects)) == 2)
	checkTrue(all(objects(attachedCopy@loadedObjects) == objects(layer@loadedObjects)))
	checkEquals(layer@loadedObjects$foo, attachedCopy@loadedObjects$foo)
	checkEquals(layer@loadedObjects$boo, attachedCopy@loadedObjects$boo)
	
	detachedCopy <- copyEntity(layer)
	checkTrue(length(objects(detachedCopy@loadedObjects)) == 2)
	checkTrue(all(objects(detachedCopy@loadedObjects) == objects(layer@loadedObjects)))
	checkEquals(layer@loadedObjects$foo, detachedCopy@loadedObjects$foo)
	
	layer@loadedObjects$bar <- "foo"
	checkTrue(length(objects(layer@loadedObjects)) == 3)
	checkTrue(length(objects(attachedCopy@loadedObjects)) == 3)
	checkTrue(length(objects(detachedCopy@loadedObjects)) == 2)
	
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