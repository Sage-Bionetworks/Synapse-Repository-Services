# TODO: Add comment
# 
# Author: furia
###############################################################################

setGeneric(
		name = "renameObject",
		def = function(entity, which, name){
			standardGeneric("renameObject")
		}
)

setMethod(
		f = "renameObject",
		signature = signature("Layer", "character", "character"),
		definition = function(entity, which, name){
			if(length(which) != length(name))
				stop("Must supply the same number of names as objects")
			
			## make a copy of the objects that will be moved and delete them from
			## the entity
			## TODO : make this more performant by only making copies of objects
			## when absolutely necessary
			tmpEnv <- new.env()
			lapply(which, FUN = function(key){
						assign(key, getObject(entity, key), envir = tmpEnv)
						deleteObject(entity, key)
					}
			)
			
			lapply(1:length(which), FUN=function(i){
						addObject(entity, get(which[i], envir=tmpEnv), name[i])
					}
			)
			rm(tmpEnv)
			invisible(entity)
		}
)

