# delete a file from a Location entity
# 
# Author: Matt Furia
###############################################################################


setMethod(
		f = "deleteFile",
		signature = signature("Layer", "character"),
		definition = function(entity, file){
			entity@location <- deleteFile(entity@location, file)
			entity
		}
)

setMethod(
		f = deleteFile,
		signature = signature("CachedLocation", "character"),
		definition = function(entity, file){
			file <- gsub("^[\\\\/]+","", file)
			
			## delete from the local cache
			if(!(file %in% entity@files))
				stop(sprintf("Invalid file: %s", file))
			tryCatch(
					file.remove(file.path(entity@cacheDir, file)),
					error = function(e){
						warning(sprintf("Unable to remove file from local cache: %s", e))
					}
			)
			
			## remove from the list of files
			entity@files <- setdiff(entity@files, file)
			
			##if the path is empty, remove it
			tail <- gsub("^.+/", "", file)
			path <- gsub(sprintf("/?%s",tail), "", file)
			numContents <- length(setdiff(dir(file.path(entity@cacheDir, path), all.files = TRUE, include.dirs=TRUE), c( "." , "..")))
			while(numContents == 0 && length(path > 0) && path != ""){
				file.remove(file.path(entity@cacheDir, path))
				tail <- gsub("^.+[\\\\/]", "", path)
				path <- gsub(sprintf("[\\\\/]?%s",tail), "", path)
				numContents <- length(setdiff(dir(file.path(entity@cacheDir, path), all.files = TRUE, include.dirs=TRUE), c( "." , "..")))
			}
			
			entity
		}
)