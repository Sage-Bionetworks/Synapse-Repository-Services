setGeneric(
		name = "loadLayer",
		def = function(object){
			standardGeneric("loadLayer")
		}
)

setMethod(
		f = "loadLayer",
		signature = "list",
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
		function(entity, locationPrefs = synapseDataLocationPreferences(), cacheDir = synapseCacheDir())
{
	if(!is.list(entity)){
		entity <- getLayer(entity=entity)
	}
	
	if(!"locations" %in% names(entity)){
		stop("the entity does not have any locations")
	}
	
	## Make sure that the location type for this layer is currently supported
	if(!all(locationPrefs %in% kSupportedDataLocationTypes)){
		ind <- which(!(locationPrefs %in% kSupportedDataLocationTypes))
		stop(paste("unsupported repository location(s):", locationPrefs[ind]))
	}
	
	## Get the available locations for this layer and match to locationPrefs
	response <- synapseGet(entity$locations)
	availableLocations <- .jsonListToDataFrame(response$results)
	ind <- match(locationPrefs, availableLocations$type)
	ind <- ind[!is.na(ind)]
	
	if(length(ind) == 0){
		stop("Data file was not available in any of the locations specified. Locations available for this layer:", annotations(entity)$locations)
	}
	
	## Order the list of available locations and take the first one
	availableLocations <- availableLocations[ind,]
	
	uri <- availableLocations$uri[1]
	
	## Get result. path is an empty string since the path is already included in 
	## the uri element of layer
	response <- synapseGet(uri = uri)
	
	destfile = synapseDownloadFile(url = response$path, checksum = response$md5sum)
	destDir <- paste(destfile, .getCache("downloadSuffix"), sep="_")
	
	## Unpack the layer file
	## TODO: should the code only unpack the file if the destdir doesn't already exists?
	## TODO: should the dest directory be deleted before unpacking?
	files <- .unpack(filename=destfile, destdir=destDir)
	
	class(files) <- "layerFiles"
	attr(files, "rootDir") <- destDir
	return(files)
}
