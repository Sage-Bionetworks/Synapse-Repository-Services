
setMethod(
		f = "Location",
		signature = "character",
		definition = function(entity){
			Location(.getEntity(entity = entity))
		}
)

setMethod(
		f = "Location",
		signature = "list",
		definition = function(entity){
			## superclass constructor
			s4Entity <- SynapseEntity(entity=entity)
			## coerce the type to Location
			class(s4Entity) <- "Location"
			synapseEntityKind(s4Entity) <- synapseEntityKind(new(Class="Location"))
			return(s4Entity)
		}
)

setMethod(
		f = "Location",
		signature = "numeric",
		definition = function(entity){
			Location(as.character(entity))
		}
)
