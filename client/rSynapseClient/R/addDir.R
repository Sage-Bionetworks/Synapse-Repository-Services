# TODO: Add comment
# 
# Author: furia
###############################################################################


setGeneric(
		name = "addDir",
		def = function(entity, path){
			standardGeneric("addDir")
		}
)
setMethod(
		f = "addDir",
		signature = signature("Layer", "character"),
		definition = function(entity, path){
			if(length(path) > 1)
				stop("When selecting files in browse mode, provide only a single path")
			if(is.null(useTk <- .getCache("useTk")))
				useTk <- .hasTk()
			if(!useTk)
				stop("Could not initialize tk widget. Interactive file selection is not available on your computer.")
			files <- 
					tryCatch(
							tcltk:::tk_choose.dir(),
							error = function(e){
								.setCache("useTk", FALSE)
								stop("Could not initialize tk widget. Interactive  file selection is not available on your computer.")
							}
					)
			if(length(files) == 0)
				return(entity)
			addFile(entity, file=files, path=path)
		}
)

setMethod(
		f = "addDir",
		signature = signature("Layer", "missing"),
		definition = function(entity){
			addDir(entity, "")
		}
)