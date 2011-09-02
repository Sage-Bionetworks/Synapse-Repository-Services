
# TODO: Add comment
# 
# Author: mfuria
###############################################################################

.cache <- new.env(parent=emptyenv())

kCertBundle <- "certificateBundle/cacert.pem"

kSupportedLayerCodeMap <- list(
		C = "PhenotypeLayer",
		E = "ExpressionLayer",
		G = "GenotypeLayer",
		M = "Layer"

	)
kSupportedLayerStatus <- c("Curated", "QCd", "Raw")
kSupportedDataLocationTypes <- c("external", "awss3")
kSupportedPlatforms <- list(
		phenotype.data = c("Custom"),
		expression.data = c("affymetrix", "agilent", "illumina", "custom"),
		genotype.data = c("affymetrix", "illumina", "perlegen", "nimblegen", "custom")
	)
	
kSynapseRAnnotationTypeMap <- list(
		stringAnnotations = "character",
		longAnnotations = "integer",
		doubleAnnotations = "numeric",
		dateAnnotations = "POSIXt"
	)
	
kLayerSubtypeMap <- list(
		ExpressionLayer = list(
					affymetrix = "AffyExpressionLayer",
					agilent = "AgilentExpressionLayer",
					illumina = "IlluminaExpressionLayer"
				)
)

## package-local 'getter'
.getCache <-
		function(key)
{
	.cache[[key]]
}

## package-local 'setter'
.setCache <-
		function(key, value)
{
	.cache[[key]] <- value
}

.deleteCache <-
		function(keys)
{
	indx <- which(keys %in% ls(.cache))
	if(length(indx) > 0)
		rm(list=keys[indx], envir=.cache)
}

.onLoad <-
		function(libname, pkgname)
{
	synapseResetEndpoints()
	synapseDataLocationPreferences(kSupportedDataLocationTypes)
	.setCache("synapseCacheDir", gsub("[\\]+", "/", path.expand("~/.synapseCache")))
	.setCache("layerCodeTypeMap", kSupportedLayerCodeMap)
	.setCache("layerSubtypeMap", kLayerSubtypeMap)
	.setCache("supportedLayerStatus", kSupportedLayerStatus)
	.setCache("supportedPlatforms", kSupportedPlatforms)
	.setCache("sessionRefreshDurationMin", 60)
	.setCache("curlOpts", list(ssl.verifypeer=TRUE, verbose = FALSE, cainfo=file.path(libname, pkgname, kCertBundle)))
	.setCache("curlHeader", c('Content-Type'="application/json", Accept = "application/json"))
	.setCache("anonymous", FALSE)
	.setCache("downloadSuffix", "unpacked")
	.setCache("debug", FALSE)
	.setCache("curlWriter", getNativeSymbolInfo("_writer_write", PACKAGE="synapseClient")$address)
	.setCache("curlReader", getNativeSymbolInfo("_reader_read", PACKAGE="synapseClient")$address)
	.setCache("annotationTypeMap", kSynapseRAnnotationTypeMap)
}
