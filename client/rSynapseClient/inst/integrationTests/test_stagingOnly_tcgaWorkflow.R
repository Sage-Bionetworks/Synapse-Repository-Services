.setUp <- function() {
	# Override commandArgs
	myCommandArgs <- function (trailingOnly = TRUE) {
		c('--args', '--datasetId', '1076', 
				'--layerId', '1078' )
	}
	
	attr(myCommandArgs, "origFCN") <- base:::commandArgs
	assignInNamespace("commandArgs", myCommandArgs, "base")
	
	# this test can only be run against staging
	.setCache("orig.authservice.endpoint", synapseAuthServiceEndpoint())
	.setCache("orig.reposervice.endpoint", synapseRepoServiceEndpoint())
	synapseAuthServiceEndpoint("https://staging-auth.elasticbeanstalk.com/auth/v1")
	synapseRepoServiceEndpoint("https://staging-reposervice.elasticbeanstalk.com/repo/v1")
}

.tearDown <- function() {
	synapseAuthServiceEndpoint(.getCache("orig.authservice.endpoint"))
	synapseRepoServiceEndpoint(.getCache("orig.reposervice.endpoint"))
	.deleteCache("orig.authservice.endpoint")
	.deleteCache("orig.reposervice.endpoint")
	assignInNamespace("commandArgs", attr(base:::commandArgs, "origFCN"), "base")
}

integrationTestTcgaWorkflow <- function() {
	
	#----- Unpack our command line parameters
	inputLayerId <- getInputLayerIdArg()
	inputDatasetId <- getInputDatasetIdArg()
	
	
	#----- Decide whether this script wants to work on this input layer
	dataset <- getDataset(id=inputDatasetId)
	if('coad' != dataset$name) {
		skipWorkflowTask('this script only handles TCGA colon cancer data')
	}
	
	inputLayer <- getLayer(id=inputLayerId)
	if('E' != inputLayer$type) {
		skipWorkflowTask('this script only handles expression data')
	}
	
	layerAnnotations <- getAnnotations(entity=inputLayer)
	if('Level_2' != layerAnnotations$stringAnnotations$format) {
		skipWorkflowTask('this script ony handles level 2 expression data from TCGA')
	}
	
	#----- Download, unpack, and load the expression layer
	expressionDataFiles <- synapseClient:::.cacheFiles(entity=inputLayer)
	# TODO load each of the files into R objects
	
	#----- Download, unpack, and load the clinical layer of this TCGA dataset  
	#      because we need it as additional input to this script
	datasetLayers <- getDatasetLayers(entity=dataset)
	####
	## TODO fix this hack. m.furia
	layerTypes <- NULL
	for(i in 1:length(datasetLayers$results)){
		layerTypes[i] <- datasetLayers$results[[i]]$type
	}
	ind <- which(layerTypes == "C")
	checkTrue(length(ind) > 0)
	## end hack
	####
	clinicalLayer <- datasetLayers$results[[ind[1]]]
	clinicalDataFiles <- synapseClient:::.cacheFiles(entity=clinicalLayer)
	clinicalData <- read.table(clinicalDataFiles[[4]], sep='\t')
	
	#----- Do interesting work with the clinical and expression data R objects
	#      e.g., make a matrix by combining expression and clinical data
	outputData <- t(clinicalData)
	
	#----- Now we have an analysis result, add the metadata for the new layer 
	#      to Synapse and upload the analysis result
	outputLayer <- list()
	outputLayer$parentId <- '1894' # a dataset in a project created for these tests
	outputLayer$name <- paste(dataset$name, inputLayer$name, clinicalLayer$name, sep='-')
	outputLayer$type <- 'E'
	
	storedOutputLayer <- storeLayerData(layerMetadata=outputLayer, layerData=outputData)
	
	#----- Add some annotations to our newly stored output layer
	outputLayerAnnotations <- getAnnotations(entity=storedOutputLayer)
	outputLayerAnnotations$stringAnnotations$format <- 'sageMatrix'
	storedOutputLayerAnnotations <- updateAnnotations(annotations=outputLayerAnnotations)
	
	checkEquals('sageMatrix', storedOutputLayerAnnotations$stringAnnotations$format)
	
	finishWorkflowTask(outputLayerId=storedOutputLayer$id)
}
