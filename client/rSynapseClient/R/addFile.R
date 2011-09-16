# TODO: Add comment
# 
# Author: furia
###############################################################################

setMethod(
		f = "addFile",
		signature = signature("Layer", "character"),
		definition = function(entity, file, path = ""){
			entity@location <- addFile(entity@location, file, path)
			entity
		}
)
setMethod(
		f = "addFile",
		signature = signature("CachedLocation", "character"),
		definition = function(entity, file, path = ""){
			if(length(file) != 1)
				stop("Can only add one file at a time.")
			if(length(path) != 1)
				stop("Only one path is allowed.")
			if(!is.character(path))
				stop("path should be a valid subdirectory path")
			if(!file.exists(file))
				stop(sprintf("File not found: %s", file))
			
			destdir <- file.path(entity@cacheDir, path)
			if(!file.exists(destdir))
				dir.create(destdir, recursive = TRUE)
			
			file.copy(file, destdir, overwrite=T)
			file <- gsub("^.+/", "", file)
			file <- gsub("^[\\\\/]+", "", file.path(path,file))
			file <- gsub("[\\\\/]+", "/", file)
			if(!(file %in% entity@files))
				entity@files <- c(entity@files, file)
			entity
		}
)