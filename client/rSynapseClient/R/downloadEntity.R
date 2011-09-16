# TODO: Add comment
# 
# Author: furia
###############################################################################


setMethod(
		f = "downloadEntity",
		signature = "Layer",
		definition = function(entity){
			## check whether user has signed agreement
			if(!hasSignedEula(entity)){
				if(!.promptSignEula())
					stop(sprintf("Visit https://synapse.sagebase.org to sign the EULA for entity %s", propertyValue(entity, "id")))
				if(!.promptEulaAgreement(entity))
					stop("You must sign the EULA to download this dataset. Visit http://synapse.sagebase.org form more information.")
				.signEula(entity)
			}
			entity@location <- .cacheEntity(entity)
			entity
		}
)

setMethod(
		f = "downloadEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			stop("Currently only Layer entities can contain data.")
		}
)
setMethod(
		f = "downloadEntity",
		signature = "character",
		definition = function(entity){
			downloadEntity(getEntity(entity))
		}
)
setMethod(
		f = "downloadEntity",
		signature = "numeric",
		definition = function(entity){
			downloadEntity(as.character(entity))
		}
)
setMethod(
		f = "downloadEntity",
		signature = "list",
		definition = function(entity){
			downloadEntity(getEntity(entity))
		}
)

setMethod(
		f = ".cacheEntity",
		signature = "Layer",
		definition = function(entity){
			if(is.null(propertyValue(entity, "locations"))){
					if(is.null(propertyValue(entity, "id")))
						stop("The entity does not have a 'locations' property or an 'id' property so could not be cached")
					entity <- getEntity(propertyValue(entity, "id"))
					if(is.null(propertyValue(entity, "locations")))
						stop("The entity does not have any locations so could not be downloaded")
			}
			locationPrefs = synapseDataLocationPreferences()
			cacheDir = synapseCacheDir()
			
			## TODO fix this. Shouldn't use global variables this way. Wait for fix on PLFM-568
			if (!all(locationPrefs %in% kSupportedDataLocationTypes)) {
				ind <- which(!(locationPrefs %in% kSupportedDataLocationTypes))
				stop(paste("unsupported repository location(s):", locationPrefs[ind]))
			}
			response <- synapseGet(propertyValue(entity, "locations"))
			if (response$totalNumberOfResults == 0) 
				return(NULL)
			availableLocations <- .jsonListToDataFrame(response$results)
			ind <- match(locationPrefs, availableLocations$type)
			ind <- ind[!is.na(ind)]
			if (length(ind) == 0) {
				stop("Data file was not available in any of the locations specified. Locations available for this layer:", 
						annotations(entity)$locations)
			}
			availableLocations <- availableLocations[ind, ]
			location <- getEntity(availableLocations$id[1])
			destfile = synapseDownloadFile(url = propertyValue(location, "path"), checksum = propertyValue(location, "md5sum"))
			
			return(CachedLocation(location, .unpack(filename = destfile)))
		}
)
