storeLayerData <-
		function(layerMetadata, layerData)
{

	## Write our analysis result to disk
	kRegularExpression <- "[[:punct:][:space:]]+"

	outputFilepath <- paste(tolower(gsub(kRegularExpression, "_", layerMetadata$name)), 'txt', sep='.')
	write.table(layerData, outputFilepath, sep='\t')

	outputZipFilepath <- paste(tolower(gsub(kRegularExpression, "_", layerMetadata$name)), 'zip', sep='.')
	zip(outputZipFilepath, c(outputFilepath))
	
	storeLayerDataFile(layerMetadata=layerMetadata, layerDataFilepath=outputZipFilepath)
}

storeLayerDataFile <-
		function(layerMetadata, layerDataFilepath)
{
	
	if(!is.list(layerMetadata)) {
		stop("layerMetadata must be supplied of R type list")
	}
	
	if(!file.exists(layerDataFilepath)) {
		stop(sprintf("file %s does not exist", layerDataFilepath))
	}
	
	## Create or update the layer, as appropriate
	locationMetadata <- list()
	if("id" %in% names(layerMetadata)) {
		outputLayerMetadata <- updateLayer(entity=layerMetadata)
		locations <- getLayerLocations(entity=outputLayerMetadata)
		awss3Location <- grep("awss3", locations$type)
		if(1 < length(awss3Location)) {
			stop("there are multiple awss3 locations for this layer")
		} else if(1 == length(awss3Location)) {
			locationMetadata <- getLocation(entity=locations$id[awss3Location[1]])
		}
	} else {
		outputLayerMetadata <- createLayer(entity=layerMetadata)
	}
	
	## Compute the provenance checksum
	checksum <- md5sum(layerDataFilepath)
	
	## Form the S3 key for this file
	if(grepl(layerDataFilepath, "/")) {
		## Unix
		splits <- strsplit(layerDataFilepath, "/")
		filename <- splits[[1]][length(splits[[1]])]
	} else {
		## Windows
		splits <- strsplit(layerDataFilepath, "/")
		filename <- splits[[1]][length(splits[[1]])]
	}
	## TODO get rid of this path prefix once PLFM-212 is done
	s3Key = paste('/rClient', outputLayerMetadata$id, filename, sep="/")
	
	## Create or update the location, as appropriate
	locationMetadata$parentId <- outputLayerMetadata$id
	locationMetadata$path <- s3Key
	locationMetadata$type <- 'awss3'
	locationMetadata$md5sum <- checksum[[1]]
	if("id" %in% names(locationMetadata)) {
		outputLocationMetadata <- updateLocation(entity=locationMetadata)
	} else {
		outputLocationMetadata <- createLocation(entity=locationMetadata)
	}
	
	## Upload the data to S3
	synapseUploadFile(url=outputLocationMetadata$path, 
			srcfile=layerDataFilepath, 
			checksum=checksum[[1]])
	
	return(outputLayerMetadata)
}
