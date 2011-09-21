# Unit tests for S4 entities
# 
# Author: matt furia
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
	synapseClient:::.setCache("entity", entity)
	synapseClient:::.setCache("annotations", annotations)
}

tearDown <- 
		function()
{
	synapseClient:::.deleteCache("entity")
	synapseClient:::.deleteCache("annotation")
}

testExtractListFromSlots <-
		function()
{
	## check the property values of the entity
	entity <- .getCache("entity")
	entityList <- synapseClient:::.extractEntityFromSlots(entity)
	checkTrue(all(names(entity@properties) %in% names(entityList)))
	checkEquals(length(entity@properties), length(entityList))
	for(n in names(entity@properties)){	
		checkTrue(all(entity@properties[[n]] == entityList[[n]]))
	}
	
	## check the annotation values
	annotations <- synapseClient:::.getCache("annotations")
	annotationsList <- synapseClient:::.extractEntityFromSlots(annotations)
	checkTrue(all(setdiff(slotNames(annotations), "properties") %in% names(annotationsList)[-grep("Property", names(annotationsList))]))
	checkTrue(all(names(annotations@properties) %in% names(annotationsList)))
	checkEquals(length(annotations@properties) + length(slotNames(annotations)) - 1, length(annotationsList))
	for(n in names(annotations@properties)){	
		checkTrue(all(annotations@properties[[n]] == annotationsList[[n]]))
	}
	
	## add annotations to entity and try again
	annotations(entity) <- annotations
	entityList <- synapseClient:::.extractEntityFromSlots(entity)
	checkTrue(all(names(entity@properties) %in% names(entityList)))
	checkEquals(length(entity@properties), length(entityList))
	for(n in names(entity@properties)){	
		checkTrue(all(entity@properties[[n]] == entityList[[n]]))
	}
	
	annotations <- annotations(entity)
	annotationsList <- synapseClient:::.extractEntityFromSlots(annotations)
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
	entity <- synapseClient:::.getCache("entity")
	entityList <- synapseClient:::.extractEntityFromSlots(entity)
	
	## instantiate a new entity and populate the slots
	entity <- new(Class="SynapseEntity")
	entity <- synapseClient:::.populateSlotsFromEntity(entity, entityList)
	
	## check the property values of the entity
	checkTrue(all(names(entity@properties) %in% names(entityList)))
	checkEquals(length(entity@properties), length(entityList))
	for(n in names(entity@properties)){	
		checkTrue(all(entity@properties[[n]] == entityList[[n]]))
	}
	
	## extract annotation list
	## check the annotation values
	annotations <- synapseClient:::.getCache("annotations")
	annotationsList <- synapseClient:::.extractEntityFromSlots(annotations)
	
	## instantiate a new annotation object and poulate the slots
	annotations <- new(Class = "SynapseAnnotation")
	annotations <- synapseClient:::.populateSlotsFromEntity(annotations, annotationsList)
	
	## check the annotation values
	checkTrue(all(setdiff(slotNames(annotations), "properties") %in% names(annotationsList)[-grep("Property", names(annotationsList))]))
	checkTrue(all(names(annotations@properties) %in% names(annotationsList)))
	checkEquals(length(annotations@properties) + length(slotNames(annotations)) - 1, length(annotationsList))
	for(n in names(annotations@properties)){	
		checkTrue(all(annotations@properties[[n]] == annotationsList[[n]]))
	}
}

unitTestAnnotationSetters <-
		function()
{
	entity <- new(Class="SynapseEntity")
	dateVal <- Sys.Date()
	POSIXVal <- as.POSIXct(Sys.Date())
	
	annotValue(entity, "stringKey") <- "aStringValue"
	annotValue(entity, "longKey") <- 1L
	annotValue(entity, "doubleKey") <- 1.1
	annotValue(entity, "booleanKeyTrue1") <- T
	annotValue(entity, "booleanKeyTrue2") <- TRUE
	annotValue(entity, "booleanKeyFalse1") <- F
	annotValue(entity, "booleanKeyFalse2") <- FALSE
#	annotValue(entity, "dateKey") <- dateVal
#	annotValue(entity, "POSIXdateKey") <- POSIXVal
	
	annotationsObject <- annotations(entity)
	
	checkEquals(annotationsObject@stringAnnotations$stringKey, "aStringValue")
	checkEquals(annotationsObject@longAnnotations$longKey, "1")
	checkEquals(annotationsObject@doubleAnnotations$doubleKey, "1.1")
	checkEquals(annotationsObject@stringAnnotations$booleanKeyTrue1, "TRUE")
	checkEquals(annotationsObject@stringAnnotations$booleanKeyTrue2, "TRUE")
	checkEquals(annotationsObject@stringAnnotations$booleanKeyFalse1, "FALSE")
	checkEquals(annotationsObject@stringAnnotations$booleanKeyFalse2, "FALSE")
#	checkEquals(annotationsObject@dateAnnotations$dateKey, as.POSIXct(dateVal))
#	checkEquals(annotationsObject@dateAnnotations$POSIXdateKey, POSIXVal)
	
	checkEquals(length(annotationsObject@properties), 0L)
	checkEquals(length(annotationsObject@stringAnnotations), 5L)
	checkEquals(length(annotationsObject@longAnnotations), 1L)
	checkEquals(length(annotationsObject@doubleAnnotations), 1L)
	checkEquals(length(annotationsObject@dateAnnotations), 0L)
	
	## date value annotations must have date in the name
	checkException(annotValue(entity, "value") <- Sys.Date())
	checkException(annotValue(entity, "value") <- as.POSIXct(Sys.Date()))

}

unitTestAnnotationGetters <-
		function()
{
	entity <- new(Class="SynapseEntity")
	dateVal <- Sys.Date()
	POSIXVal <- as.POSIXct(Sys.Date())
	
	annotValue(entity, "stringKey") <- "aStringValue"
	annotValue(entity, "longKey") <- 1L
	annotValue(entity, "doubleKey") <- 1.1
	annotValue(entity, "booleanKeyTrue1") <- T
	annotValue(entity, "booleanKeyTrue2") <- TRUE
	annotValue(entity, "booleanKeyFalse1") <- F
	annotValue(entity, "booleanKeyFalse2") <- FALSE
#	annotValue(entity, "dateKey") <- dateVal
#	annotValue(entity, "POSIXdateKey") <- POSIXVal
	
	checkEquals(annotValue(entity, "stringKey"), "aStringValue")
	checkEquals(annotValue(entity, "longKey"), "1")
	checkEquals(annotValue(entity, "doubleKey"), "1.1")
	checkEquals(annotValue(entity, "booleanKeyTrue1"), "TRUE")
	checkEquals(annotValue(entity, "booleanKeyTrue2"), "TRUE")
	checkEquals(annotValue(entity, "booleanKeyFalse1"), "FALSE")
	checkEquals(annotValue(entity, "booleanKeyFalse2"), "FALSE")
#	checkEquals(annotValue(entity, "dateKey"), as.character(as.numeric(as.POSIXct(dateVal))))
#	checkEquals(annotValue(entity, "POSIXdateKey"), as.character(as.numeric(POSIXVal)))	
}

unitTestMultipleValueSetters <-
		function()
{
	entity <- new(Class="SynapseEntity")
	dateVal <- Sys.time()
	POSIXVal <- as.POSIXct(Sys.time(), origin=ISOdatetime(1970,1,1,0,0,0))
	
	valueList <- list(
				stringKey = "aStringValue",
				longKey = 1L,
				doubleKey = 1.1,
				booleanKeyTrue1 = T,
				booleanKeyTrue2 = TRUE,
				booleanKeyFalse1 = F,
				booleanKeyFalse2 = FALSE#,Dates aren't working
#				dateKey = dateVal,
#				POSIXdateKey =POSIXVal
			)
	
	annotationValues(entity) <- valueList
	
	checkEquals(annotValue(entity, "stringKey"), "aStringValue")
	checkEquals(annotValue(entity, "longKey"), "1")
	checkEquals(annotValue(entity, "doubleKey"), "1.1")
	checkEquals(annotValue(entity, "booleanKeyTrue1"), "TRUE")
	checkEquals(annotValue(entity, "booleanKeyTrue2"), "TRUE")
	checkEquals(annotValue(entity, "booleanKeyFalse1"), "FALSE")
	checkEquals(annotValue(entity, "booleanKeyFalse2"), "FALSE")
#	checkEquals(annotValue(entity, "dateKey"), as.POSIXct(dateVal, origin=ISOdatetime(1970,1,1,0,0,0)))
#	checkEquals(annotValue(entity, "POSIXdateKey"), POSIXVal)
	
	entity <- new(Class="SynapseEntity")
	propertyValues(entity) <- valueList
	
	checkEquals(propertyValue(entity, "stringKey"), "aStringValue")
	checkEquals(propertyValue(entity, "longKey"), 1L)
	checkEquals(propertyValue(entity, "doubleKey"), 1.1)
	checkEquals(propertyValue(entity, "booleanKeyTrue1"), TRUE)
	checkEquals(propertyValue(entity, "booleanKeyTrue2"), TRUE)
	checkEquals(propertyValue(entity, "booleanKeyFalse1"), FALSE)
	checkEquals(propertyValue(entity, "booleanKeyFalse2"), FALSE)
#	checkEquals(propertyValue(entity, "dateKey"), dateVal)
#	checkEquals(propertyValue(entity, "POSIXdateKey"), POSIXVal)
}

unitTestPropertySetters <-
		function()
{
	
}

unitTestConstructors <-
		function()
{
	## test the various constructors
}

unitTestCachedLocationEntity <-
		function()
{
	location <- new("CachedLocation")
	checkTrue(file.exists(location@cacheDir))
	
	layer <- new("Layer")
	checkTrue(file.exists(layer@location@cacheDir))
}


