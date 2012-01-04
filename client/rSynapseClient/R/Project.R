# S4 Class definition, constructors and associated methods for Synapse projects
# 
# Author: Matt Furia
###############################################################################
setMethod(
		f = "Project",
		signature = "numeric",
		definition = function(entity){
			Project(as.character(entity))
		}
)

setMethod(
		f = "Project",
		signature = "character",
		definition = function(entity){
			entity <- getProject(entity = entity)
			Project(entity)
		}
)

setMethod(
		f = "Project",
		signature = "list",
		definition = function(entity){
			## call the superClass constructor
			s4Entity <- SynapseEntity(entity)
			class(s4Entity) <- "Project"
			synapseEntityKind(s4Entity) <- synapseEntityKind(new(Class="Project"))
			return(s4Entity)
		}
)

setMethod(
		f = "Project",
		signature = "missing",
		definition = function(entity){
			Project(list())
		}
)
