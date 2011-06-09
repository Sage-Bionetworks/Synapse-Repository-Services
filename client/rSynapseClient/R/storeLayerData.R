storeLayerData <-
		function(layerMetadata, layerData, locationPrefs = dataLocationPrefs(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), cacheDir = synapseCacheDir())
{

	#----- Create our new layer
	outputLayerMetadata <- synapsePost('/layer', layerMetadata)
	
	#----- Write our analysis result to disk
	outputFilename <- paste(outputLayerMetadata$name, 'txt', sep='.')
	outputFilepath <- file.path(synapseCacheDir(), outputFilename)
	write.table(outputData, outputFilepath, sep='\t')
    # TODO zip this
	
	#----- Compute the provenance checksum
	checksum <- md5sum(outputFilepath)
	
	#----- Create our new location
	locationMetadata <- list()
	locationMetadata$parentId <- outputLayerMetadata$id
	locationMetadata$path <- paste('/tcga', outputLayerMetadata$id, outputFilename, sep="/")
	locationMetadata$type <- 'awss3'
	locationMetadata$md5sum <- checksum[[1]]
	outputLocationMetadata <- synapsePost(uri='/location', entity=locationMetadata)
	
	# Upload the data to S3
	synapseUploadFile(url=outputLocationMetadata$path, srcfile=outputFilepath, checksum=checksum[[1]])
	
	return(outputLayerMetadata)
}
