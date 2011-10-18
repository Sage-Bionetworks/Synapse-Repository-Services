# TODO: Add comment
# 
# Author: furia
###############################################################################

setGeneric(
		name = "addObject",
		def = function(entity, object, name, unlist){
			standardGeneric("addObject")
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer", "ANY", "missing", "missing"),
		definition = function(entity, object){
			name = deparse(substitute(object, env=parent.frame()))
			name <- gsub("\\\"", "", name)
			addObject(entity, object, name)
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer", "ANY", "character", "missing"),
		definition = function(entity, object, name){
			assign(name, object, envir = entity@objects)
			tryCatch(
					.cacheObject(entity, name),
					error = function(e){
						deleteObject(entity, name)
						stop(e)
					}
			)
			invisible(entity)
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer", "list", "character", "missing"),
		definition = function(entity, object, name){
			assign(name, object, envir = entity@objects)
			tryCatch(
					.cacheObject(entity, name),
					error = function(e){
						deleteObject(entity, name)
						stop(e)
					}
			)
			invisible(entity)
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer", "list", "missing", "missing"),
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
		signature = signature("Layer", "list", "missing", "logical"),
		definition = function(entity, object, unlist){
			if(unlist)
				return(addObject(entity, object))
			name = deparse(substitute(object, env=parent.frame()))
			name <- gsub("\\\"", "", name)
			addObject(entity, object, name, unlist)
		}
)

setMethod(
		f = "addObject",
		signature = signature("Layer", "list", "character", "logical"),
		definition = function(entity, object, name, unlist){
			if(unlist)
				stop("cannot specify the object name when unlisting")
			assign(name, object, envir = entity@objects)
			tryCatch(
					.cacheObject(entity, name),
					error = function(e){
						deleteObject(entity, name)
						stop(e)
					}
			)
			invisible(entity)
		}
)


setMethod(
		f = "addObject",
		signature = signature("Layer", "data.frame", "missing", "missing"),
		definition = function(entity, object){
			name = deparse(substitute(object, env=parent.frame()))
			name <- gsub("\\\"", "", name)
			if(!is.character(name))
				stop("name must be a character")
			assign(name, object, envir = entity@objects)
			tryCatch(
					.cacheObject(entity, name),
					error = function(e){
						deleteObject(entity, name)
						stop(e)
					}
			)
			invisible(entity)
		}
)





