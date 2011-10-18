# Definition, Methods and Constructors for the Dataset S4 object, which represents
# a Synapse Dataset Entity
#
# Author: mfuria
###############################################################################

## Dataset constructor for character entity id
setMethod(
	f = "Dataset",
	signature = signature("character"),
	definition = function(entity){
		entity <- getDataset(entity = entity)
		Dataset(entity)
	}
)

## Dataset constructor for integer entity id
setMethod(
		f = "Dataset",
		signature = signature("numeric"),
		definition = function(entity){
			Dataset(as.character(entity))
		}
)

## Dataset constructor for entity list
setMethod(
		f = "Dataset",
		signature = signature("list"),
		definition = function(entity){
			## call the superclass constructor
			dataset <- SynapseEntity(entity=entity)
			
			## coerce to Dataset
			class(dataset) <- "Dataset"
			synapseEntityKind(dataset) <- synapseEntityKind(new(Class="Dataset"))
			dataset
		}
)

setMethod(
		f = "Dataset",
		signature = "missing",
		definition = function(entity){
			Dataset(list())
		}
)

setMethod(
		f = "show",
		signature = "Dataset",
		definition = function(object){
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
			cat(sprintf("To view this Entity on the Synapse website use the 'onWeb()' function\nor paste this url into your browser: %s\n", object@synapseWebUrl))
			
		}
)