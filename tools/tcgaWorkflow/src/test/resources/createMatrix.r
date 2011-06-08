#!/usr/bin/env Rscript

library(synapseClient)

synapseRepoServiceHostName('http://localhost:8080/services-repository-0.4-SNAPSHOT')
sessionToken('admin')

inputLayerId <- getInputLayerId()
inputDatasetId <- getInputDatasetId()

datasetObj <- getDataPacketSummary(inputDatasetId)
layersObj <- getPacketLayers(inputDatasetId)

dataset <- synapseGet(paste('/dataset', inputDatasetId, sep='/'))
expressionLayer <- synapseGet(paste('/layer', inputLayerId, sep='/'))

queryResult <- synapseQuery(paste('select * from layer where layer.parentId == ', inputDatasetId, ' and layer.name == "clinical_public_', dataset$name, '"', sep=''))
clinicalLayerId <- queryResult[[1]]$layer.id
clinicalLayer <- synapseGet(paste('/layer', clinicalLayerId, sep='/'))

clinicalLayerLocations <- synapseGet(paste(clinicalLayer$locations, sep='/'))


if(0 == attr(queryResult,'totalNumberOfResults')) {
  warning('did not find the clinical dataset')
}
if(1 < attr(queryResult,'totalNumberOfResults')) {
	stop('found too many clinical datasets, something is busted')
}

data <- read.table(getLocalFilepath(), sep='\t')

setOutputLayerId(inputLayerId)
