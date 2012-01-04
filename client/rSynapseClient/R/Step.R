# S4 Class definition, constructors and associated methods for Synapse steps
# 
# Author: Matt Furia
###############################################################################
setMethod(
		f = "Step",
		signature = "numeric",
		definition = function(entity){
			Step(as.character(entity))
		}
)

setMethod(
		f = "Step",
		signature = "character",
		definition = function(entity){
			entity <- getStep(entity = entity)
			Step(entity)
		}
)

setMethod(
		f = "Step",
		signature = "list",
		definition = function(entity){
			## call the superClass constructor
			s4Entity <- SynapseEntity(entity)
			class(s4Entity) <- "Step"
			synapseEntityKind(s4Entity) <- synapseEntityKind(new(Class="Step"))
			return(s4Entity)
		}
)

setMethod(
		f = "Step",
		signature = "missing",
		definition = function(entity){
			Step(list())
		}
)

