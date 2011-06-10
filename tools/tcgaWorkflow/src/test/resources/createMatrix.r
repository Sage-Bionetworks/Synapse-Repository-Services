#!/usr/bin/env Rscript

#----- Load the Synpase R client
library(synapseClient)

#----- Unpack our command line parameters
inputLayerId <- getInputLayerIdArg()
inputDatasetId <- getInputDatasetIdArg()

#----- Log into Synapse
synapseLogin(getUsernameArg(), getPasswordArg())

#----- Decide whether this script wants to work on this input layer
dataset <- getDataPacketSummary(id = inputDatasetId)
if('coad' != dataset$name) {
  skipWorkflowTask('this script only handles prostate cancer data')
}

inputLayer <- synapseGet(uri=paste('/layer', inputLayerId, sep='/'))
if('E' != inputLayer$type) {
  skipWorkflowTask('this script only handles expression data')
}

#----- Download, unpack, and load the TCGA source data layer
expressionDataFiles <- getLayerData(inputLayer)
# TODO load each of the files into R objects

#----- Download, unpack, and load the clinical layer of this dataset  
#      because we need it as additional input to this script
datasetLayers <- getPacketLayers(id = inputDatasetId)
clinicalLayer <- datasetLayers$C
clinicalLayerLocations <- synapseGet(uri = clinicalLayer$locations)
if(0 == clinicalLayerLocations$totalNumberOfResults) {
	stop('did not find the clinical dataset location')
}

clinicalDataFiles <- getLayerData(clinicalLayer)
clinicalData <- read.table(clinicalDataFiles[[4]], sep='\t')

#----- Do interesting work with the clinical and expression data R objects
#      e.g., make a matrix by combining expression and clinical data
outputData <- t(clinicalData)

#----- Now we have an analysis result, add the metadata for the new layer 
#      to Synapse and upload the analysis result
outputLayer <- list()
outputLayer$parentId <- inputDatasetId
outputLayer$name <- paste(dataset$name, inputLayer$name, clinicalLayer$name, sep='-')
outputLayer$type <- 'E'

storedOutputLayer <- storeLayerData(layerMetadata=outputLayer, layerData=outputData)

finishWorkflowTask(outputLayerId=storedOutputLayer$id)

