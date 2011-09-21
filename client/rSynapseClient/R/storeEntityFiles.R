# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################


setMethod(
		f = "storeEntityFiles",
		signature = "SynapseEntity",
		definition = function(entity){
			stop("Only Layer entities can contain stored files")
		}
)

setMethod(
		f = "storeEntityFiles",
		signature = "Layer",
		definition = function(entity){
			if(length(entity$files) == 0)
				stop("Entity has no files to store")
			
			if(!all(mk <- file.exists(file.path(entity@location@cacheDir, entity@location@files))))
				stop("Not all files listed by the entity exist.")
				
			## build the outfile name
			dataFileName <- gsub("^[\\/]+", "", tempfile(fileext=".zip", tmpdir=""))
			if(!is.null(propertyValue(entity, "name")))
				dataFileName <- sprintf("%s%s",gsub("[ ]+", "_", propertyValue(entity, "name")),".zip")
			
			dataFileName <- file.path(tempdir(), dataFileName)
			
			## if zipFile exists delete it before creating
			if(file.exists(dataFileName))
				file.remove(dataFileName)
			
			
			## change directory to the cache directory
			oldDir <- getwd()
			setwd(entity@location@cacheDir)
			suppressWarnings(zipRetVal <- zip(zipfile=normalizePath(dataFileName, mustWork=FALSE), files=gsub("^/","",entity@location@files)))
			setwd(oldDir)
			
			## if zip failes, load uncompressed
			if(zipRetVal != 0L){
				msg <- sprintf("Unable to zip layerData Files. Error code: %i.",zipRetVal)
				if(length(entity@location@files) > 1)
					stop(msg, " Make sure that zip is installed on your computer. Without zip, only one file can be uploaded at a time")
				warning("Zip was not installed on your computer. Uploading layer data uncompressed. Directory structure will not be preserved.")
				dataFileName <- entity@location@files
			}
			
			storeFile(entity, dataFileName)
		}
)

setMethod(
		f = "storeFile",
		signature = signature("Layer", "character"),
		definition = function(entity, filePath){
			if(is.null(propertyValue(entity, "id"))){
				## Create the layer in Synapse
				entity <- createEntity(entity)
			}else{
				if(is.null(propertyValue(entity@location, "id"))){
					## Check if the Layer already has an awss3 location
					locations <- getLayerLocations(entity = propertyValue(entity,"id"))
					
					awss3Location <- grep("awss3", locations$type)
					entity@location <- new(Class="CachedLocation")
					if(1L < length(awss3Location)) {
						stop("there are multiple awss3 locations for this layer")
					} else if(1L == length(awss3Location)) {
						entity@location <- getEntity(locations$id[awss3Location[1]])
					}
				}
			}
			## Compute the provenance checksum
			checksum <- as.character(md5sum(filePath))
			
			## parse out the filename
			filename <- gsub(sprintf("%s%s%s", "^.+",.Platform$file.sep, "+"), "",filePath)
			
			## Create or update the location, as appropriate
			propertyValues(entity@location) <- list(
					path = filename,
					type = "awss3",
					md5sum = checksum
			)
			
			if(is.null(propertyValue(entity@location, "id"))){
				## set the parent ID before creating
				propertyValue(entity@location, "parentId") <- propertyValue(entity, "id")
				## create the Location entity in Synapse
				entity@location <- createEntity(entity = entity@location)
			}else{
				## update the Location entity in Synapse
				entity@location <- updateEntity(entity@location)
			}
			
			contentType <- propertyValue(entity@location, 'contentType')
			if(is.null(contentType)) {
				contentType <- 'application/binary'
			}
			
			## Upload the data file
			tryCatch(
					synapseUploadFile(url = propertyValue(entity@location, "path"),
								srcfile = filePath,
								checksum = propertyValue(entity@location, "md5sum"),
								contentType = contentType
					),
					error = function(e){
						warning(sprintf("failed to upload data file, please try again: %s", e))
						return(entity)
					}
			)
			
			## move the data file from where it is to the local cache directory
			parsedUrl <- .ParsedUrl(propertyValue(entity@location, "path"))
			destdir <- file.path(synapseCacheDir(), gsub("^/", "", parsedUrl@pathPrefix))
			destdir <- path.expand(destdir)
			
			if(file.exists(destdir))
				unlink(destdir, recursive = TRUE)
			dir.create(destdir, recursive = TRUE)
			
			if(!file.copy(filePath, destdir, overwrite = TRUE)){
				warning("Failed to copy file to local cache")
				## unpack into the local cache and update the location entity
				entity@location <- CachedLocation(entity@location, .unpack(filepath))
				
			}else{
				entity@location@cacheDir <- destdir
				## unpack into the local cache and update the location entity
				entity@location <- CachedLocation(entity@location, .unpack(file.path(destdir, filename)))
			}
			
			refreshEntity(entity)
		}
)
