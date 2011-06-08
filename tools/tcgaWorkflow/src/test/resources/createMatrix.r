#!/usr/bin/env Rscript

library(synapseClient)

synapseRepoServiceHostName('http://localhost:8080/')
sessionToken('admin')

inputLayerId <- getInputLayerId()
inputDatasetId <- getInputDatasetId()

dataset <- synapseGet(paste('/dataset', inputDatasetId, sep='/'))
layer <- synapseGet(paste('/layer', inputLayerId, sep='/'))

queryResult <- synapseQuery(paste('select * from layer where layer.parentId == ', inputDatasetId, ' and layer.name == "clinical_public_', dataset$name, '"', sep=''))

if(0 == attr(queryResult,'totalNumberOfResults')) {
  warning('did not find the clinical dataset')
}
if(1 < attr(queryResult,'totalNumberOfResults')) {
	stop('found too many clinical datasets, something is busted')
}

data <- read.table(getLocalFilepath(), sep='\t')

setOutputLayerId(inputLayerId)
