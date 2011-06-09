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

#----- Download the clinical layer of this dataset because we need it 
#      as additional input to this script
datasetLayers <- getPacketLayers(id = inputDatasetId)

# This won't work right now
#data <- getLayerData(layersObj$C)

clinicalLayer <- datasetLayers$C
clinicalLayerLocations <- synapseGet(uri = clinicalLayer$locations)
if(0 == clinicalLayerLocations$totalNumberOfResults) {
	stop('did not find the clinical dataset location')
}

location <- clinicalLayerLocations$results[[1]]
path <- location$path
download <- synapseDownloadFile(url = path, destfile='fooNow3.zip')

#synapseCacheDir()

# TODO unpack archive
clinicalFile <- file.path(synapseCacheDir(), 'clinical_patient_public_coad.txt')

#----- Read the clinical and expression data into R objects and do interesting work

clinicalData <- read.table(clinicalFile, sep='\t')

# FIXME
#expressionData <- read.table(getLocalFilepath(), sep='\t')

# e.g., make a matrix by combining expression and clinical data
outputData <- t(clinicalData)

#----- Now we have an analysis result, add the metadata for the new layer 
#      to Synapse and upload the analysis result
outputLayer <- list()
outputLayer$parentId <- inputDatasetId
outputLayer$name <- paste(dataset$name, inputLayer$name, clinicalLayer$name, sep='-')
outputLayer$type <- 'E'

storedOutputLayer <- storeLayerData(layerMetadata=outputLayer, layerData=outputData)

finishWorkflowTask(outputLayerId=storedOutputLayer$id)

