# TODO: Add comment
# 
# Author: mfuria
###############################################################################


setUp <- 
		function()
{
	entity <- new(Class="SynapseEntity")
	entity@properties$stringproperty1 <- "stringvalue1"
	entity@properties$stringproperty2 <- "stringvalue2"
	entity@properties$longproperty3 <- 3L
	entity@properties$doubleproperty4.0 <- 4.0
	
	annotations <- new(Class="SynapseAnnotation")
	annotations@properties$annotationPropertyX <- "annotationProperty"
	annotations@properties$annotationPropertyAnother <- "anotherAnnotationProperty"
	annotations@stringAnnotations$stringAnnotation5 <- "stringValue5"
	annotations@doubleAnnotations$doubleAnnotation6.0 <- 6.0
	annotations@longAnnotations$longAnnotation7 <- 7L
	annotations@stringAnnotations$multiValuedAnnotation <- c("one", "two")
	.setCache("entity", entity)
	.setCache("annotations", annotations)
}

tearDown <- 
		function()
{
	.deleteCache("entity")
	.deleteCache("annotation")
}

testExtractListFromSlots <-
		function()
{
	## check the property values of the entity
	entity <- .getCache("entity")
	entityList <- .extractEntityFromSlots(entity)
	checkTrue(all(names(entity@properties) %in% names(entityList)))
	checkEquals(length(entity@properties), length(entityList))
	for(n in names(entity@properties)){	
		checkTrue(all(entity@properties[[n]] == entityList[[n]]))
	}
	
	## check the annotation values
	annotations <- .getCache("annotations")
	annotationsList <- .extractEntityFromSlots(annotations)
	checkTrue(all(setdiff(slotNames(annotations), "properties") %in% names(annotationsList)[-grep("Property", names(annotationsList))]))
	checkTrue(all(names(annotations@properties) %in% names(annotationsList)))
	checkEquals(length(annotations@properties) + length(slotNames(annotations)) - 1, length(annotationsList))
	for(n in names(annotations@properties)){	
		checkTrue(all(annotations@properties[[n]] == annotationsList[[n]]))
	}
	
	## add annotations to entity and try again
	annotations(entity) <- annotations
	entityList <- .extractEntityFromSlots(entity)
	checkTrue(all(names(entity@properties) %in% names(entityList)))
	checkEquals(length(entity@properties), length(entityList))
	for(n in names(entity@properties)){	
		checkTrue(all(entity@properties[[n]] == entityList[[n]]))
	}
	
	annotations <- annotations(entity)
	annotationsList <- .extractEntityFromSlots(annotations)
	checkTrue(all(setdiff(slotNames(annotations), "properties") %in% names(annotationsList)[-grep("Property", names(annotationsList))]))
	checkTrue(all(names(annotations@properties) %in% names(annotationsList)))
	checkEquals(length(annotations@properties) + length(slotNames(annotations)) - 1, length(annotationsList))
	for(n in names(annotations@properties)){	
		checkTrue(all(annotations@properties[[n]] == annotationsList[[n]]))
	}
}

testPopulateSlotsFromEntity <-
		function()
{
	## extract entity list
	entity <- .getCache("entity")
	entityList <- .extractEntityFromSlots(entity)
	
	## instantiate a new entity and populate the slots
	entity <- new(Class="SynapseEntity")
	entity <- .populateSlotsFromEntity(entity, entityList)
	
	## check the property values of the entity
	checkTrue(all(names(entity@properties) %in% names(entityList)))
	checkEquals(length(entity@properties), length(entityList))
	for(n in names(entity@properties)){	
		checkTrue(all(entity@properties[[n]] == entityList[[n]]))
	}
	
	## extract annotation list
	## check the annotation values
	annotations <- .getCache("annotations")
	annotationsList <- .extractEntityFromSlots(annotations)
	
	## instantiate a new annotation object and poulate the slots
	annotations <- new(Class = "SynapseAnnotation")
	annotations <- .populateSlotsFromEntity(annotations, annotationsList)
	
	## check the annotation values
	checkTrue(all(setdiff(slotNames(annotations), "properties") %in% names(annotationsList)[-grep("Property", names(annotationsList))]))
	checkTrue(all(names(annotations@properties) %in% names(annotationsList)))
	checkEquals(length(annotations@properties) + length(slotNames(annotations)) - 1, length(annotationsList))
	for(n in names(annotations@properties)){	
		checkTrue(all(annotations@properties[[n]] == annotationsList[[n]]))
	}
}

unitTestConstructors <-
		function()
{
	## test the various constructors
}

