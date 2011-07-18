.setUp <- function() {
	# Override commandArgs
	myCommandArgs <- function (trailingOnly = TRUE) {
		c('--args', '--datasetId', '1028', 
				'--layerId', '1031' )
	}
	
	attr(myCommandArgs, "origFCN") <- base:::commandArgs
	assignInNamespace("commandArgs", myCommandArgs, "base")
	
	# this test can only be run against staging
	.setCache("orig.authservice.endpoint", synapseAuthServiceEndpoint())
	.setCache("orig.reposervice.endpoint", synapseRepoServiceEndpoint())
	synapseAuthServiceEndpoint("https://staging-auth.elasticbeanstalk.com/auth/v1")
	synapseRepoServiceEndpoint("https://staging-reposervice.elasticbeanstalk.com/repo/v1")
	
	# Create a project and a dataset
	project <- list()
	project$name <- 'R Integration Test Project'
	createdProject <- createProject(entity=project)
	.setCache("rIntegrationTestProject", createdProject)
	dataset <- list()
	dataset$name <- 'R Integration Test Dataset'
	dataset$parentId <- createdProject$id
	createdDataset <- createDataset(entity=dataset)
	.setCache("rIntegrationTestDataset", createdDataset)
}

.tearDown <- function() {
	synapseAuthServiceEndpoint(.getCache("orig.authservice.endpoint"))
	synapseRepoServiceEndpoint(.getCache("orig.reposervice.endpoint"))
	.deleteCache("orig.authservice.endpoint")
	.deleteCache("orig.reposervice.endpoint")
	assignInNamespace("commandArgs", attr(base:::commandArgs, "origFCN"), "base")

	deleteProject(entity=.getCache("rIntegrationTestProject"))
	.deleteCache("rIntegrationTestProject")
}

integrationTestSageBioTCGACurationProjectChildEntityGet <- function() {
	projects <- synapseQuery(query='select * from project where project.name == "SageBio TCGA Curation"')
	project <- getProject(entity=projects$project.id[1])
	datasets <- getProjectDatasets(entity=project)
	checkTrue(1 <= dim(datasets)[1])
	checkTrue('coad' %in% datasets$dataset.name)
}

integrationTestTcgaWorkflow <- function() {
	
	#----- Unpack our command line parameters
	inputLayerId <- getInputLayerIdArg()
	inputDatasetId <- getInputDatasetIdArg()
	
	
	#----- Decide whether this script wants to work on this input layer
	dataset <- getDataset(entity=inputDatasetId)
	if('coad' != dataset$name) {
		skipWorkflowTask('this script only handles TCGA colon cancer data')
	}
	
	inputLayer <- getLayer(entity=inputLayerId)
	if('E' != inputLayer$type) {
		skipWorkflowTask('this script only handles expression data')
	}
	
	layerAnnotations <- getAnnotations(entity=inputLayer)
	if('Level_2' != layerAnnotations$stringAnnotations$format) {
		skipWorkflowTask('this script ony handles level 2 expression data from TCGA')
	}
	
	#----- Download, unpack, and load the expression layer
	expressionDataFiles <- loadLayerData(entity=inputLayer)
	# TODO load each of the files into R objects
	
	#----- Download, unpack, and load the clinical layer of this TCGA dataset  
	#      because we need it as additional input to this script
	datasetLayers <- getDatasetLayers(entity=dataset, includeParentAnnot=FALSE)
	ind <- which(datasetLayers$type == "C")
	checkTrue(length(ind) > 0)
	
	clinicalLayer <- getLayer(entity=datasetLayers$id[ind[1]])
	clinicalDataFiles <- loadLayerData(entity=clinicalLayer)
	clinicalData <- read.delim(clinicalDataFiles[[4]], as.is=TRUE)
	
	#----- Do interesting work with the clinical and expression data R objects
	#      e.g., make a matrix by combining expression and clinical data
	outputData <- t(clinicalData)
	
	#----- Now we have an analysis result, add the metadata for the new layer 
	#      to Synapse and upload the analysis result
	outputLayer <- list()
	outputLayer$parentId <- .getCache("rIntegrationTestDataset")$id # a dataset in a project created for these tests
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
