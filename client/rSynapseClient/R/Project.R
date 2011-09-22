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
		f = "show",
		signature = "Project",
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