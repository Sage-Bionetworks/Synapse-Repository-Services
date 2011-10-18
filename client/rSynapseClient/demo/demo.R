## load the client
library(synapseClient)

## login to Synapse, this will prompt for username and password
## NOTE: First time users must create a Synapse password, do this on https://synapse-alpha.sagebase.org
synapseLogin()

## get the list of available datasets
datasetTable <- getDatasets()

## pick a couple datasets to explore
indx <- which(datasetTable$name %in% c("MSKCC Prostate Cancer", "Harvard Brain Tissue Resource Center"))

## get the layer annotations for the two datasets
datasetLayerTable <- getDatasetLayers(datasetTable[indx,])

## what phenotype layers are available for these datasets?
datasetLayerTable[indx <- which(datasetLayerTable$layer.type == "C"), ]

## get the phenotype layer for the Sawyers dataset
layer <- getEntity(datasetLayerTable[1, 'layer.id'], "layer")

## !!! Have you signed the use agreement yet???
## go to: https://synapse-alpha.sagebase.org and sign the agreement
## coming soon: sign EULAs from the R client.
phenotypes <- loadLayerData(layer)

## create your own project
projectProperties <- list(
			name =  <Make up a name>,
			description = "A project to demonstrate the R Synapse Client"
		)
project <- Project(projectProperties)

## store the project on the server
## the server will add some values
## NOTE: must catch the return value to keep the local
## entity in sync with the server. 
project <- createEntity(project) 

## create a dataset to add the project
dataset <- new(Class="Dataset", properties = list(
				parentId = propertyValue(project,"id"),
				name = <Make up a name>
				)
			)
annotValue(dataset,"species") <- "mouse"
dataset <- createEntity(dataset)

## After we create it, we can still add more annotations
annotationValues(dataset) <- list(
				source = "WPP Collaboration",
				embargoed = "TRUE"
			)
dataset <- updateEntity(dataset)

## store the layer in Synapse
newPhenotypes <- pData(phenotypes)
layer <- new(Class="Layer", properties = list(parentId=propertyValue(dataset,"id"), name="newLayer"))
propertyValue(layer, "type") <- "C"
layer <- addObject(layer, newPhenotypes)
layer <- storeEntity(layer)

## get the layer back.
loadedLayer <- load(propertyValue(layer, "id"))
loadedLayer
retrievedData <- loadedLayer$objects$newPhenotypes
