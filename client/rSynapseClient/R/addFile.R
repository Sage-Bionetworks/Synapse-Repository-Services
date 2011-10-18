# TODO: Add comment
# 
# Author: furia
###############################################################################

setMethod(
		f = "addFile",
		signature = signature("Layer", "character", "character"),
		definition = function(entity, file, path){
			entity@location <- addFile(entity@location, file, path)
			entity
		}
)

setMethod(
		f = "addFile",
		signature = signature("Layer", "character", "missing"),
		definition = function(entity, file){
			entity@location <- addFile(entity@location, file, path="/")
			entity
		}
)

setMethod(
		f = "addFile",
		signature = signature("Layer", "missing", "character"),
		definition = function(entity, path){
			if(length(path) > 1)
				stop("When selecting files in browse mode, provide only a single path")
			if(is.null(useTk <- .getCache("useTk")))
				useTk <- .hasTk()
			if(!useTk)
				stop("Could not initialize tk widget. Interactive file selection is not available on your computer.")
			files <- 
					tryCatch(
							tcltk:::tk_choose.files(),
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
		f = "addFile",
		signature = signature("Layer", "missing", "missing"),
		definition = function(entity){
			addFile(entity, path="")	
		}
)

setMethod(
		f = "addFile",
		signature = signature("CachedLocation", "character", "character"),
		definition = function(entity, file, path){
			if(length(path) > 1 && (length(path) != length(file)))
				stop("Must provide either a single path, or one path for each file")
			if(any(file.info(file)$isdir) && length(path) > 1)
				stop("when adding directories, provide only a single path")
			
			if(!all(mk<-file.exists(file)))
				stop(sprintf("File not found: %s", file[!mk]))
			
			path <- gsub("[\\/]+$", "", path)
			path <- gsub("[\\/]+", "/", path)
			path <- gsub("^[/]+", "", path)
			mk <- path %in% c("")
			destdir <- rep(entity@cacheDir, length(path))
			if(any(!mk))
				destdir[!mk] <- file.path(entity@cacheDir,  path[!mk])
			
			if(!all(mk <- file.exists(destdir)))
				lapply(destdir[!mk], function(d) dir.create(d, recursive = TRUE))
			
			if(length(file) == length(path)){
				for(i in 1:length(file)){
					if(file.info(file[i])$isdir){
						recursive = TRUE
					}else{
						recursive = FALSE
					}
					file.copy(file[i], destdir[i], overwrite = TRUE, recursive=recursive)
				}
			}else{
				file.copy(file, destdir, overwrite=TRUE, recursive=TRUE)
			}
			files <- dir(destdir, recursive=TRUE, all.files=T, full.names=T)
			## drop directories
			if(any(mk <- file.info(files)$isdir))
				files <- files[!mk]
			
			## clip out the cacheDir bit
			files <- gsub(entity@cacheDir, "", files, fixed=TRUE)
			
			files <- gsub("^[\\/]+", "", files)
			files <- gsub("[\\/]+", "/", files)
			if(any(!(mk <- files %in% entity@files)))
				entity@files <- c(entity@files, files[!mk])
			entity
		}
)