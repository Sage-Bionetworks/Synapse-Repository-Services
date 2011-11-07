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

setMethod(
		f = "show",
		signature = "Step",
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