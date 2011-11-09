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

setMethod(
		f = "show",
		signature = "Analysis",
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