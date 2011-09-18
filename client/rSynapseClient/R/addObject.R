# TODO: Add comment
# 
# Author: furia
###############################################################################

setGeneric(
		name = "addObject",
		def = function(entity, object, ...){
			standardGeneric("addObject")
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer"),
		definition = function(entity, object, name = deparse(substitute(object, env=parent.frame()))){
			name <- gsub("\\\"", "", name)
			assign(name, object, envir = entity@objects)
			invisible(entity)
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer", "list"),
		definition = function(entity, object){
			if(any(names(object) == "") || is.null(names(object)))
				stop("all elements of the list must be named")
			lapply(names(object), FUN=function(nm){
						addObject(entity, object[[nm]], nm)
					}
			)
			invisible(entity)
		}
)


setMethod(
		f = "addObject",
		signature = signature("Layer", "data.frame"),
		definition = function(entity, object, name = deparse(substitute(object, env=parent.frame()))){
			name <- gsub("\\\"", "", name)
			if(!is.character(name))
				stop("name must be a character")
			assign(name, object, envir = entity@objects)
			invisible(entity)
		}
)