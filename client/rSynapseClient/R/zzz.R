
# TODO: Add comment
# 
# Author: mfuria
###############################################################################

.cache <- new.env(parent=emptyenv())

kSupportedLayerCodeMap <- list(
		PhenotypeLayer = "C",
		ExpressionLayer = "E",
		GenotypeLayer = "G"

	)
kSupportedLayerStatus <- c("Curated", "QCd", "Raw")
kSupportedDataLocationTypes <- c("external", "awss3")
kSupportedPlatforms <- list(
		phenotype.data = c("Custom"),
		expression.data = c("Affymetrix", "Agilent", "Illumina", "Custom"),
		genotype.data = c("Affymetrix", "Illumina", "Perlegen", "Nimblegen", "Custom")
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
	.setCache("supportedLayerStatus", kSupportedLayerStatus)
	.setCache("supportedPlatforms", kSupportedPlatforms)
	.setCache("sessionRefreshDurationMin", 60)
	.setCache("curlOpts", list(ssl.verifypeer = FALSE, verbose = FALSE))
	.setCache("curlHeader", c('Content-Type'="application/json", Accept = "application/json"))
	.setCache("anonymous", FALSE)
	.setCache("downloadSuffix", "unpacked")
	.setCache("debug", FALSE)
}