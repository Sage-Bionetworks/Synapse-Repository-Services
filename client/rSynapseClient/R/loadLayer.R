setGeneric(
		name = "loadLayer",
		def = function(object){
			standardGeneric("loadLayer")
		}
)

setMethod(
		f = "loadLayer",
		signature = "layerList",
		def = function(object){
			loadLayer(Layer(object))
		}
)

setMethod(
		f = "loadLayer",
		signature = "Layer",
		definition = function(object){
			object@cachedFiles <- .cacheFiles(object@locations)
			return(object)
		}
)


.cacheFiles <-
		function(entity, locationPrefs = dataLocationPrefs(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), cacheDir = synapseCacheDir())
{
	if(!"locations" %in% names(entity)){
		stop("the entity does not have any locations")
	}
	
	## make sure that the location type for this layer is currently supported
	if(!all(locationPrefs %in% .getCache("supportedRepositoryLocationTypes"))){
		ind <- which(!(locationPrefs %in% .getCache("supportedRepositoryLocationTypes")))
		stop(paste("unsupported repository location(s):", locationPrefs[ind]))
	}
	
	#get the available locations for this layer and match to locationPrefs
	response <- synapseGet(entity$locations, curlHandle = curlHandle, anonymous = anonymous)
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
	synapseCheckSum <- response$md5sum
	zipFile <- URL(file.path(cacheDir, s4URL@fullFilePath))
	destDir <- URL(paste(zipFile@url, .getCache("downloadSuffix"), sep="_"))
	localFile <- file.path(cacheDir, s4URL@fullFilePath)
	
	synapseDownloadFile(url = s4URL@url, destfile = s4URL@fullFilePath, checksum = md5sum(localFile), cacheDir = cacheDir)
	
	## unpack the layer file
	## TODO: should the code only unpack the file if the destdir doesn't already exists?
	## TODO: should the dest directory be deleted before unpacking?
	files <- .unpack(filename=zipFile@url, destdir=destDir@url)
	
	class(files) <- "layerFiles"
	attr(files, "rootDir") <- destDir@url
	return(files)
}
