#!/usr/bin/env Rscript

#----- Load the Synpase R client
library(synapseClient)

#----- Unpack our command line parameters
inputLayerId <- getInputLayerIdArg()
inputDatasetId <- getInputDatasetIdArg()

#----- Log into Synapse
synapseLogin(getUsernameArg(), getPasswordArg())

# DELETE ME
#inputLayerId <- 544
#inputDatasetId <- 543
#inputDatasetId <- 500
#sessionToken('0PuPGsDY84AiezCW0a0j3A00')

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

#----- Read the clinical and expression data into R objects
clinicalData <- read.table(clinicalFile, sep='\t')
expressionData <- read.table(getLocalFilepath(), sep='\t')

#----- Do interesting work
# e.g., make a matrix by combining expression and clinical data
outputData <- t(clinicalData)

#----- Now we have an analysis result, add the metadata for the new layer to Synapse
outputLayer <- list()
outputLayer$parentId <- inputDatasetId
outputLayer$name <- paste(dataset$name, inputLayer$name, clinicalLayer$name, sep='-')
outputLayer$type <- 'E'
storedOutputLayer <- synapsePost('/layer', outputLayer)

#----- Upload the analysis result
outputFilename <- file.path(synapseCacheDir(), paste(outputLayer$name, 'txt', sep='.'))
write.table(outputData, outputFilename, sep='\t')
# TODO zip this

checksum <- md5sum(outputFilename)

outputLocation <- list()
outputLocation$parentId <- storedOutputLayer$id
outputLocation$path <- paste('/tcga', outputFilename, sep="/")
outputLocation$type <- 'awss3'
outputLocation$md5sum <- checksum[[1]]
storedLocation <- synapsePost(uri='/location', entity=outputLocation)

synapseUploadFile(uri=storedLocation$path, srcfile=outputFilename, checksum=checksum[[1]])

finishWorkflowTask(layerId=storedOutputLayer$id)

