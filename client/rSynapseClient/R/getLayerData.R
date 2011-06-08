getLayerData <-
		function(layer, locationPrefs = dataLocationPrefs(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	## constants
	kHeader <- c(Accept = "application/json")
	kDownloadMethod <- "curl"
	## end constants
	
	## make sure that the location type for this layer is currently supported
	if(!all(locationPrefs %in% .getCache("supportedRepositoryLocationTypes"))){
		ind <- which(!(locationPrefs %in% .getCache("supportedRepositoryLocationTypes")))
		stop(paste("unsupported repository location(s):", locationPrefs[ind]))
	}
	
	#get the available locations for this layer and match to locationPrefs
	response <- jsonListToDataFrame(synapseGet(layer$locations))
	checkCurlResponse(curlHandle, response)
	availableLocations$results
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
	datapath <- response$path
	zipFile <- gsub("\\?.+$", "", strsplit(datapath, ".com/")[[1]][2])
    
	synapseDownloadFile(url = datapath, destfile = zipFile, method = kDownloadMethod)

	## deflaux: I could not figure out the right set of args to unzip
	## to get it to happily extract the directory structure contained
	## in the zip file
	
	
	extractDirectory <- paste(getwd(), strsplit(zipFile, ".zip")[[1]][1], sep="/")
	unzip(zipFile, exdir=extractDirectory)
	files <- file.path(extractDirectory,list.files(extractDirectory))
	
	class(files) <- "layerData"
	attr(files, "layerType") <- layer$type
	return(files)
}
