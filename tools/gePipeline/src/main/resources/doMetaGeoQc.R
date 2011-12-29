# TODO: Add comment
# 
# Author: mfuria
###############################################################################

doMetaGeoQc <-
    function(sourceLayerId, destDataset, timestamp, deleteDataFiles=TRUE)
{
	library(metaGEO)
	geoData <- downloadEntity(sourceLayerId)
	sourceLayerName <- propertyValue(geoData, "name")
	
	locations<-synapseQuery(sprintf('select * from location where parentId=="%s"', sourceLayerId))
	if (is.null(locations) || dim(locations)[1]<1) stop(paste("No location for layer", sourceLayerName, "(", sourceLayerId, ")"))
	md5sum = locations[1,"location.md5sum"]
	
	tryCatch({
		result <- runWorkflow(geoData$cacheDir)
	
		exprLayers <- NULL
		metadataLayers <- NULL
		for(cdfname in names(result)){
			exprLayers <- c(exprLayers, .storeExprResults(destDataset, sourceLayerName, cdfname, result[[cdfname]], deleteDataFiles)$layerId)
		}
		
		# 'destDataset' object may be 'stale', so refresh it
		maxretries<-10
		for (i in 1:maxretries) { # may get an error during update, so try several times
			destDataset <- getEntity(propertyValue(destDataset, "id"))
			annotValue(destDataset, lastUpdateAnnotName(sourceLayerId)) <- as.character(timestamp)
			annotValue(destDataset, md5sumAnnotName(sourceLayerId)) <- md5sum
			ans <- try(updateEntity(destDataset))
			if (class(ans)=="try-error") {
				if (i==maxretries) stop(paste("Failed to update entity:", ans[[1]], propertyValue(destDataset, "id")))
				Sys.sleep(runif(1,1,4)) # sleep for 1-4 seconds before trying again
			} else {
				destDataset <<- ans
				break
			}
		}
		},
		finally = {
			if(deleteDataFiles){
				cat(sprintf("\n\ndeleting files for: %s\n\n",  propertyValue(destDataset, "name")))
				ret <- unlink(gsub(sprintf("%s/.+$", propertyValue(destDataset, "name")), propertyValue(destDataset,"name"), geoData$cacheDir), recursive=TRUE)
				if(ret != 0L)
				    stop(sprintf("could not delete files for: %s",  propertyValue(destDataset, "name")))
				unlink(tempdir(), recursive=TRUE)
			}
		}
	)
	list(exprLayers=exprLayers)
}


.storeExprResults <- 
		function(geoDataset, sourceLayerName, cdfname, data, deleteDataFiles)
{
	
	destLayerName <- sprintf("QCd Expression Data %s %s", sourceLayerName, cdfname)
	
	result <- synapseQuery(sprintf('select * from layer where layer.name == "%s" and layer.parentId == "%s"', destLayerName, propertyValue(geoDataset, "id")))
	if(!is.null(result)){
		## get the layer from synapse. it will be updated
		layer <- downloadEntity(result$layer.id)
	}else{
		## create the layer that will hold the QCd expression data
		layer <- Layer(list(
						name=destLayerName,
						type="E",
						status="QCed",
						parentId=propertyValue(geoDataset, "id"), ## layer is a child of the dataset
            platform=cdfname
				))
		## set the studyId annotation before storing the layer and its data in Synapse
		annotValue(layer, "Dataset") <- propertyValue(geoDataset, "name")
	}
	## add the data to the layer. Each of the data objects named in storeFields
	## will be stored as a separate binary object in layer
	layer <- addObject(layer, data)
	layer <- storeEntity(layer)
	if(deleteDataFiles)
		unlink(gsub(sprintf("%s/.+$", propertyValue(layer@location, "id")), propertyValue(layer@location,"id"), layer$cacheDir), recursive=T)	
	list(layerId = propertyValue(layer, "id"))	
}


.storeMetaData <- 
		function(geoDataset, destLayerName, cdfname, data, deleteDataFiles)
{
	storeFields <- "metadata"
	dsName <- sprintf("Metadata %s", cdfname)
	
	result <- synapseQuery(sprintf('select * from layer where layer.name == "%s" and layer.parentId == "%s"', dsName, propertyValue(geoDataset, "id")))
	if(!is.null(result)){
		## get the layer from synapse. it will be updated
		layer <- downloadEntity(result$layer.id)
	}else{
		
		## create the layer that will hold the metadata
		layer <- Layer(list(
						name=dsName,
						type="C",
						status="Raw",
						parentId=propertyValue(geoDataset, "id") ## layer is a child of the dataset
				))
		## set the studyId annotation before storing the layer and it's data in Synapse
		annotValue(layer, "StudyId") <- propertyValue(geoDataset, "name")
	}
	## add the data to the layer. Each of the data objects named in storeFields
	## will be stored as a separate binary object in layer
	layer <- addObject(layer, data)
	layer <- storeEntity(layer)
	if(deleteDataFiles)
		unlink(gsub(sprintf("%s/.+$", propertyValue(layer@location, "id")), propertyValue(layer@location,"id"), layer$cacheDir), recursive=T)	

	list(layerId = propertyValue(layer, "id"))	
}
