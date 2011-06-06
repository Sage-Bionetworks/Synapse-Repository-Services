loadLayerData <-
		function(layer, locationType='awsS3Location')
{
	kAttrName <- "layerType"
	if(class(layer) != 'layer'){
		stop("input must be of class 'layer'")
	}
	
	layerData <- getLayerData(layer, locationType)
	
	layerType <- attr(layerData, kAttrName)
	
	if(layerType == .getCache("layerCodeTypeMap")[["phenotype.data"]]){
		d <- .loadPhenotypeLayer(layerData)
	}else{
		stop(paste("layerType", layerType, "is not supported"))
	}
	
 	return(d)
}
