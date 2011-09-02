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
			return(dataset)
		}
)
