.loadPhenotypeLayer <- 
		function(layerData)
{
	kAttrName <- "layerType"
	kIndividualsPattern <- "/individuals.txt$"
	kPhenotypePattern <- "phenotype/phenotype.txt$"
	kDictPattern <- "phenotype/description.txt$"

	if(class(layerData) != "layerData"){
		stop("argument must be of class 'layerData'")
	}
	if(attr(layerData, kAttrName) != "C"){
		stop("layerData must be of sub-class type 'Curated phenotypes'")
	}	
	
	retVal <- NULL
	
	tmp <- read.delim(layerData[grep(kPhenotypePattern, layerData)], header=F, as.is=T, colClasses='character')
	names(tmp) <- tmp[1,]
	tmp <- tmp[-1,]
	retVal$data <- tmp
	
	tmp <- read.delim(layerData[grep(kDictPattern, layerData)], as.is=T)
	retVal$dictionary <- tmp
	
	tmp <- read.delim(layerData[grep(kIndividualsPattern, layerData)], as.is=T)
	retVal$individuals <- tmp
	
	class(retVal) <- "clinicalDataLayer"

	return(retVal)
	
}