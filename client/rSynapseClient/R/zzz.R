# TODO: Add comment
# 
# Author: mfuria
###############################################################################

.cache <- new.env(parent=emptyenv())

kSupportedLayerCodeMap <- list(
		PhenotypeDataLayer = "C",
		ExpressionDataLayer = "E",
		GenotypeDataLayer = "G"

	)
kSupportedLayerStatus <- c("Curated", "QCd", "Raw")
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

.onLoad <-
		function(libname, pkgname)
{
	resetSynapseHostConfig()
	.setCache("supportedRepositoryLocationTypes", c("awsS3Location", "sageLocation"))
	.setCache("layerCodeTypeMap", kSupportedLayerCodeMap)
	.setCache("supportedLayerStatus", kSupportedLayerStatus)
	.setCache("supportedPlatforms", kSupportedPlatforms)
	.setCache("sessionRefreshDurationMin", 60)
	.setCache("repoServicePath", "repo/v1")
	.setCache("authServicePath", "auth/v1")
	.setCache("curlOpts", list(ssl.verifypeer = FALSE))
	.setCache("curlHeader", c('Content-Type'="application/json", Accept = "application/json"))
	.setCache("anonymous", FALSE)
}