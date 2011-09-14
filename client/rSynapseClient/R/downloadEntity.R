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
			files <- .cacheFiles(propertyValue(entity, "id"))
			files <- gsub(attr(files, "rootDir"), "", files)
			entity@cachedFiles <- list(cacheDir= attr(files,"rootDir"), files=as.character(files))
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
