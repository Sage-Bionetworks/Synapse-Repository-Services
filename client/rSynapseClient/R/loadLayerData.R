loadLayerData <-
		function(layer, locationType='awsS3Location')
{
	kAttrName <- "layerType"
	if(class(layer) != 'layer'){
		stop("input must be of class 'layer'")
	}
	
	layerData <- getLayerData(layer, locationType)
	
 	switch(attr(layerData, kAttrName),
 			"Curated phenotypes" = .loadPhenotypeLayer(layerData),
 			stop(paste("layerType", attr(layerData, kAttrName), "is not supported"))
 	)
}
