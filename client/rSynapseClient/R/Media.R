# TODO: Add comment
# 
# Author: furia
###############################################################################


setMethod(
		f = "Media",
		signature = "character",
		definition = function(entity){
			getEntity(entity)
		}
)

setMethod(
		f = "Media",
		signature = "numeric",
		definition = function(entity){
			Media(as.character(entity))
		}
)

setMethod(
		f = "Media",
		signature = "list",
		definition = function(entity){
			media <- Layer(entity=entity)
			
			## coerce to Media
			class(media) <- "Media"
			media <- initialize(media)
			synapseEntityKind(media) <- synapseEntityKind(new(Class="Media"))
			media
		}
)

setMethod(
		f = "Media",
		signature = "missing",
		definition = function(entity){
			Media(list())
		}
)

setMethod(
		f = "initialize",
		signature = "Media",
		definition = function(.Object, ...){
			propertyValue(.Object, "type") <- "M"
			.Object
		}
)

setMethod(
		f = "show",
		signature = "Media",
		definition = function(object){
			if(tolower(.Platform$GUI) == "rstudio")
				lapply(object$files, function(f) file.show(file.path(object$cacheDir, f), title=f))
			
			cat('An object of class "', class(object), '"\n', sep="")
			
			cat("Synapse Entity Name : ", properties(object)$name, "\n", sep="")
			cat("Synapse Entity Id   : ", properties(object)$id, "\n", sep="")
			
			if (!is.null(properties(object)$parentId))
				cat("Parent Id           : ", properties(object)$parentId, "\n", sep="")
			if (!is.null(properties(object)$type))
				cat("Type                : ", properties(object)$type, "\n", sep="")
			if (!is.null(properties(object)$version))
				cat("Version             : ", properties(object)$version, "\n", sep="")
			
			
			cat("\nFor complete list of annotations, please use the annotations() function.\n")

		}
		
)
