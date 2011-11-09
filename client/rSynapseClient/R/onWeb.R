# TODO: Add comment
# 
# Author: furia
###############################################################################


setGeneric(
		name = "onWeb",
		def = function(entity){
			standardGeneric("onWeb")
		}
)

setMethod(
		f = "onWeb",
		signature = "numeric",
		definition = function(entity){
			onWeb(as.character(entity))
		}
)

setMethod(
		f = "onWeb",
		signature = "character",
		definition = function(entity){
			utils::browseURL(.buildSynapseUrl(entity))
		}
)

setMethod(
		f = "onWeb",
		signature = signature("SynapseEntity"),
		definition = function(entity){
			stop(sprintf("%s Entities do not have a web representation"), class(entity))
		}
)

setMethod(
		f = "onWeb",
		signature = signature("Dataset"),
		definition = function(entity){
			.doOnWeb(entity)
		}
)

setMethod(
		f = "onWeb",
		signature = signature("Layer"),
		definition = function(entity){
			.doOnWeb(entity)
		}
)

setMethod(
		f = "onWeb",
		signature = signature("Project"),
		definition = function(entity){
			.doOnWeb(entity)
		}
)

setMethod(
	f = "onWeb",
	signature = signature("Analysis"),
	definition = function(entity){
		.doOnWeb(entity)
	}
	)

setMethod(
	f = "onWeb",
	signature = signature("Step"),
	definition = function(entity){
		.doOnWeb(entity)
	}
)
.doOnWeb <- function(entity) {
	if(entity@synapseWebUrl == "")
		stop("This Entity has not been saved to Synapse yet. Use createEntity() to save it and then try again.")
	tryCatch(
		utils::browseURL(entity@synapseWebUrl),
		error = function(e){
			warning("Unable to launch the web browser. Paste this url into your web browser: %s", entity@synapseWebUrl)
			warning(e)
		}
		)
	invisible(entity@synapseWebUrl)
}