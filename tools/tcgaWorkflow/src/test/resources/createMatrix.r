#!/usr/bin/env Rscript


# TODO move this code into an R package again like it was here:
# http://sagebionetworks.jira.com/source/browse/PLFM/branches/Synapse-0.5/client/rSynapseClient/R/synapseWorkflow.R?r=3380
library(methods)
source('./src/test/resources/synapseWorkflow.R')

#----- Load the Synpase R client
library(synapseClient)

#----- Log into Synapse
synapseAuthServiceEndpoint(getAuthEndpointArg())
synapseRepoServiceEndpoint(getRepoEndpointArg())
synapseLogin(getUsernameArg(), getPasswordArg())

#----- Unpack the rest of our command line parameters
inputLayerId <- getInputLayerIdArg()
inputDatasetId <- getInputDatasetIdArg()

#----- Decide whether this script wants to work on this input layer
dataset <- getEntity(inputDatasetId)
if('Colon Adenocarcinoma TCGA' != propertyValue(dataset, "name")) {
  skipWorkflowTask('this script only handles TCGA colon cancer data')
}

inputLayer <- getEntity(inputLayerId)
if('E' != propertyValue(inputLayer, "type")) {
  skipWorkflowTask('this script only handles expression data')
}

if('Level_2' != annotValue(inputLayer, "tcgaLevel")) {
	skipWorkflowTask('this script ony handles level 2 expression data from TCGA')
}

# Need a better way to handle this case
synapseClient:::.signEula(dataset)

#----- Download, unpack, and load the expression layer
inputLayerData <- loadEntity(inputLayer)
# TODO load each of the files into R objects

#----- Download, unpack, and load the clinical layer of this TCGA dataset  
#      because we need it as additional input to this script
datasetLayers <- getDatasetLayers(dataset)
clinicalLayerIndex <- which(datasetLayers$layer.type == 'C')
clinicalLayer <- loadEntity(datasetLayers[clinicalLayerIndex[2], 'layer.id'])
clinicalData <- read.table(paste(clinicalLayer$cacheDir, clinicalLayer$files[[4]], sep='/'), sep='\t')

#----- Do interesting work with the clinical and expression data R objects
#      e.g., make a matrix by combining expression and clinical data
outputData <- t(clinicalData)

#----- Now we have an analysis result, add the metadata for the new layer 
#      to Synapse and upload the analysis result

project <- Project(list(
                name=paste('TCGA Test Project', gsub(':', '_', date()))
                ))
project <- createEntity(project)
dataset <- Dataset(list(
                name = 'Analysis Result',
                parentId = propertyValue(project, "id") ## dataset is a child of the project
            ))
dataset <- createEntity(dataset)
layer <- Layer(list(
                name="Analysis Result Expression Data",
                type="E",
                status="QCed",
                parentId=propertyValue(dataset, "id") ## layer is a child of the dataset
            ))
layer <- addObject(layer, outputData)
annotValue(layer, "sourceLayer") <- propertyValue(clinicalLayer, 'name')
layer <- storeEntity(layer)

finishWorkflowTask(outputLayerId=propertyValue(layer, 'id'))

