getLayerData <-
		function(layer, locationPrefs = dataLocationPrefs(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), cacheDir = synapseCacheDir())
{
	## make sure that the location type for this layer is currently supported
	if(!all(locationPrefs %in% .getCache("supportedRepositoryLocationTypes"))){
		ind <- which(!(locationPrefs %in% .getCache("supportedRepositoryLocationTypes")))
		stop(paste("unsupported repository location(s):", locationPrefs[ind]))
	}
	
	#get the available locations for this layer and match to locationPrefs
	response <- synapseGet(layer$locations, curlHandle = curlHandle, anonymous = anonymous)
	checkCurlResponse(curlHandle, response)
	availableLocations <- jsonListToDataFrame(response$results)
	ind <- match(locationPrefs, availableLocations$type)
	
	if(length(ind) == 0){
		stop("Data file was not available in any of the locations specified. Locations available for this layer:", layer@locations)
	}
	
	#order the list of available locations and take the first one
	availableLocations <- availableLocations[ind,]
	
	uri <- availableLocations$uri[1]
	
	## get result. path is an empty string since the path is already included in 
	## the uri element of layer
	response <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous, path="")
	checkCurlResponse(curlHandle, response)
	s4URL <- URL(response$path)
	zipFile <- URL(file.path(cacheDir, s4URL@fullFilePath))
	destDir <- URL(paste(zipFile@url, .getCache("downloadSuffix"), sep="_"))
	
	synapseDownloadFile(url = s4URL@url, destfile = s4URL@fullFilePath, cacheDir = cacheDir)
	unzip(zipFile@url, exdir=destDir@url)
	files <- file.path(destDir@url,list.files(destDir@url))
	
	class(files) <- "layerData"
	attr(files, "layerType") <- layer$type
	return(files)
}
