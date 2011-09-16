# TODO: Add comment
# 
# Author: furia
###############################################################################


setMethod(
		f = "addFile",
		signature = signature("Layer", "character"),
		definition = function(entity, file, path = ""){
			if(length(file) != 1)
				stop("Can only add one file at a time.")
			if(length(path) != 1)
				stop("Only one path is allowed.")
			if(!is.character(path))
				stop("path should be a valid subdirectory path")
			if(!file.exists(file))
				stop(sprintf("File not found: %s", file))

			destdir <- file.path(entity@location@cacheDir, path)
			if(!file.exists(destdir))
				dir.create(destdir, recursive = TRUE)
			
			file.copy(file, destdir, overwrite=T)
			file <- gsub("^.+/", "", file)
			file <- gsub(sprintf("^%s+", .Platform$file.sep), "", file.path(path,file))
			entity@location@files <- c(entity@location@files, file)
			entity
		}
)
