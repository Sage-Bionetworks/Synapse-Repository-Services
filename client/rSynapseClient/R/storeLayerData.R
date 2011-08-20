setMethod(
		f = "storeLayerDataFiles",
		signature = "Layer",
		definition = function(entity, layerDataFile){
			if(class(layerDataFile) != "character")
				stop("was expecting an array of file paths")
			
			## if rbin format and only one file, don't need to zip
			if(length(layerDataFile) == 1L && !is.null(annotValue(entity,"format")) && annotValue(entity,"format") == "rbin")
				return(storeLayerDataFile(entity = entity, layerDataFilepath = layerDataFile))
			
			## build the outfile name
			zipFileName <- gsub("^[\\/]+", "", tempfile(fileext=".zip", tmpdir=""))
			if(!is.null(propertyValue(entity, "name")))
				zipFileName <- sprintf("%s%s",gsub("[ ]+", "_", propertyValue(entity, "name")),".zip")
			
			zipFileName <- file.path(tempdir(), zipFileName)
			
			## change working directory to tempdir()
			## TODO: figure out how to zip files without including directory tree
			##		 some way other than changing directory
			oldDir <- getwd()
			setwd(tempdir())
			zipRetVal <- zip(zipfile=normalizePath(zipFileName, mustWork=FALSE), files=normalizePath(layerDataFile))
			setwd(oldDir)
			
			## if zip failes, load uncompressed
			if(zipRetVal != 0L){
				msg <- sprintf("Unable to zip layerData Files. Error code: %i.",zipRetVal)
				if(length(layerDataFile) > 1)
					stop(msg, " Make sure that zip is installed on your computer. Without zip, only one file can be uploaded at a time")
				warning("Zip was not installed on your computer. Uploading layer data uncompressed")
				return(storeLayerDataFile(entity = entity, layerDataFilepath = layerDataFile))
			}
			
			## upload the zipFile
			storeLayerDataFile(entity = entity, layerDataFilepath = zipFileName)
		}
)

setMethod(
		f = "storeLayerData",
		signature = "Layer",
		definition = function(entity, ...){
			## get names of elipsis args
			##argNames <- names(list(...))
			objectNames <- as.character(as.list(substitute(list(...)))[-1L])
			
			## save the objects to rbin objects in the tempdir
			files <- lapply(1:length(objectNames), FUN=function(indx){
						object <- objectNames[indx]
						assign(object,list(...)[[indx]])
						thisFile <- file.path(tempdir(), sprintf("%s%s",object, ".rbin"))
						save(list=object, file = thisFile, compression_level = 9)
						thisFile
					}
			)
			files <- unlist(files)
			## set an annotation on the Layer object to specify the storage format as R binary
			annotValue(entity, "format") <- "rbin"
			
			if(is.null(propertyValue(entity, "id"))){
				entity <- createEntity(entity)
			} else{
				entity <- updateEntity(entity)
			}
			## pass the files to be packaged and uploaded
			storeLayerDataFiles(entity, files)
		}
)

#setMethod(
#		f = "storeLayerData",
#		signature = "list",
#		definition = function(entity, layerDataFile){
#			storeLayerData(entity= getEntity(entity), layerDataFile)
#		}
#)

setMethod(
		f = "storeLayerData",
		signature = "list",
		definition = function(entity, ...){
			storeLayerData(Layer(entity=entity), ...)
		}
)

setMethod(
		f = "storeLayerDataFile",
		signature = signature("list", "character"),
		definition = function(entity, layerDataFilepath){
			storeLayerDataFile(getEntity(kind = "layer", entity=entity), layerDataFilepath)
		}
)

setMethod(
		f = "storeLayerDataFile",
		signature = signature("character", "character"),
		definition = function(entity, layerDataFilepath){
			storeLayerDataFile(getEntity(type="layer", entity=entity))
		}
)

setMethod(
		f = "storeLayerDataFile",
		signature = signature("numeric", "character"),
		definition = function(entity, layerDataFilepath){
			storeLayerDataFile(getEntity(type="layer", entity=entity))
		}
)

setMethod(
		f = "storeLayerDataFile",
		signature = signature("Layer", "character"),
		definition = function(entity, layerDataFilepath){
			if(!file.exists(layerDataFilepath)) {
				stop(sprintf("file %s does not exist", layerDataFilepath))
			}
			
			## Create the layer if it doesn't already exist in Synapse
			## TODO: create a convenience method to check if object has ID (i.e. exists in Synapse)
			## TODO: possibly refresh object immediately before upload
			if(is.null(propertyValue(entity, "id"))){
				## Create the layer in Synapse
				entity <- createEntity(entity)
				
				## Create a new location entity and persist in Synapse
				location <- new(Class="Location")
			}else{
				## Check if the Layer already has an awss3 location
				locations <- getLayerLocations(entity = propertyValue(entity,"id"))
				
				awss3Location <- grep("awss3", locations$type)
				location <- new(Class="Location")
				if(1L < length(awss3Location)) {
					stop("there are multiple awss3 locations for this layer")
				} else if(1L == length(awss3Location)) {
					location <- Location(entity=locations$id[awss3Location[1]])
				}
			}
			
			## Compute the provenance checksum
			checksum <- as.character(md5sum(layerDataFilepath))
			
			## parse out the filename
			filename <- gsub(sprintf("%s%s%s", "^.+",.Platform$file.sep, "+"), "",layerDataFilepath)
						
			## Create or update the location, as appropriate
			## TODO: write convenience method for setting child entities
			propertyValues(location) <- list(
					path = filename,
					type = "awss3",
					md5sum = checksum
			)
			
			if(is.null(propertyValue(location, "id"))){
				## set the parent ID before creating
				propertyValue(location, "parentId") <- propertyValue(entity, "id")
				## create the Location entity in Synapse
				location <- createEntity(entity = location)
			}else{
				## update the Location entity in Synapse
				location <- updateEntity(location)
			}
			
			contentType <- propertyValue(location, 'contentType')
			if(is.null(contentType)) {
				contentType <- 'application/binary'
			}
			
			## Upload the data file
			synapseUploadFile(url = propertyValue(location, "path"),
						srcfile = layerDataFilepath,
						checksum = propertyValue(location, "md5sum"),
						contentType = contentType
					)
			refreshEntity(entity)
		}		
)

