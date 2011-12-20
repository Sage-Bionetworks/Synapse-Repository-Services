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
		signature = "Code",
		definition = function(entity){
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
		definition = function(entity) {
			
			## Get the download locations for this entity
			locations <- propertyValue(entity, "locations")
			if (is.null(locations)) {
				if(is.null(propertyValue(entity, "id")))
					stop("The entity does not have a 'locations' property or an 'id' property so could not be cached")
				entity <- getEntity(propertyValue(entity, "id"))
				locations <- propertyValue(entity, "locations")
				if (is.null(locations)) {
					stop("The entity does not have any locations so could not be downloaded")
				}
			}

			## Note that we just use the first location, to future-proof this we would use the location preferred
			## by the user, but we're gonna redo this in java so no point in implementing that here right now
			destfile = synapseDownloadFile(url = locations[[1]]['path'], checksum = propertyValue(entity, "md5"))
			
			## Locations are no longer entities in synapse, but they still exist here in the R client
			location <- Location(list(path=locations[[1]]['path'], type=locations[[1]]['type']))
			return(CachedLocation(location, .unpack(filename = destfile)))
		}
)
