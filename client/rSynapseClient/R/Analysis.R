# S4 Class definition, constructors and associated methods for Synapse analyses
# 
# Author: Matt Furia
###############################################################################
setMethod(
		f = "Analysis",
		signature = "numeric",
		definition = function(entity){
			Analysis(as.character(entity))
		}
)

setMethod(
		f = "Analysis",
		signature = "character",
		definition = function(entity){
			entity <- getAnalysis(entity = entity)
			Analysis(entity)
		}
)

setMethod(
		f = "Analysis",
		signature = "list",
		definition = function(entity){
			## call the superClass constructor
			s4Entity <- SynapseEntity(entity)
			class(s4Entity) <- "Analysis"
			synapseEntityKind(s4Entity) <- synapseEntityKind(new(Class="Analysis"))
			return(s4Entity)
		}
)

setMethod(
		f = "Analysis",
		signature = "missing",
		definition = function(entity){
			Analysis(list())
		}
)
