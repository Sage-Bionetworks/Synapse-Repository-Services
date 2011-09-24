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
		f = "deleteFile",
		signature = signature("CachedLocation", "character"),
		definition = function(entity, file){
			file <- gsub("^[\\/]+","", file)
			file <- gsub("[\\]+","/", file)
			
			## delete from the local cache
			if(!all(mk <- (file %in% entity@files)))
				stop(sprintf("Invalid file: %s\n", file[!mk]))
			tryCatch(
					file.remove(file.path(entity@cacheDir, file), recursive=TRUE),
					error = function(e){
						warning(sprintf("Unable to remove file from local cache: %s", e))
					}
			)
			
			## remove from the list of files
			entity@files <- setdiff(entity@files, file)
			
			## clean up empty directories
			.recursiveDeleteEmptyDirs(entity@cacheDir)
			
			entity
		}
)

setMethod(
		f = "deleteFile",
		signature = signature("Layer", "missing"),
		definition = function(entity){
			if(!file.exists(entity$cacheDir))
				stop("entity has no files to delete")
			if(length(setdiff(list.files(entity$cacheDir, all.files=TRUE), c(".", ".."))) == 0L)
				stop("entity has no files to delete")
			if(!.hasTk())
				stop("Your computer does not appear to have Tk installed. Cannot launch interactive file selection widget")
			files <- 
					tryCatch(
							tcltk:::tk_choose.files(entity$cacheDir),
							error = function(e){
								.setCache("useTk", FALSE)
								stop("Could not initialize tk widget. Interactive  file selection is not available on your computer.")
							}
					)
			if(length(files) == 0)
				return(entity)
			
			splits <- strsplit(files[1], "[\\\\/]+")[[1]]
			cacheDir <- paste(splits[-length(splits)], collapse="/")
			files <- files[-1]
			files <- gsub(cacheDir, "", files, fixed=TRUE)
			deleteFile(entity, files)
		}
)
