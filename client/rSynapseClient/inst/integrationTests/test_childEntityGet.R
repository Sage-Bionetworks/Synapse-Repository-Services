.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestSawyersDatasetChildEntityGet <- function() {
	datasets <- synapseQuery(query='select * from dataset where dataset.name == "MSKCC Prostate Cancer"')
	layers <- getDatasetLayers(entity=datasets$dataset.id[1])
	checkTrue(5 <= layers$totalNumberOfResults)
	locations <- getLayerLocations(entity=layers$results[[1]])
	checkTrue(1 <= locations$totalNumberOfResults)
	previews <- getLayerPreviews(entity=layers$results[[1]])
	checkTrue(1 <= previews$totalNumberOfResults)
}

integrationTestSageBioCurationProjectChildEntityGet <- function() {
	projects <- synapseQuery(query='select * from project where project.name == "SageBioCuration"')
	project <- getProject(entity=projects$project.id[1])

	datasets <- getProjectDatasets(entity=project)
	checkEquals(100, length(datasets$results))
	checkTrue(115 <= datasets$totalNumberOfResults)

	moreDatasets <- getProjectDatasets(entity=project, limit=200)
	checkTrue(115 <= length(moreDatasets$results))
	checkTrue(115 <= moreDatasets$totalNumberOfResults)
	
}
