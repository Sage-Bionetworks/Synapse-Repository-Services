.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestSawyersDatasetChildEntityGet <- function() {
	datasets <- synapseQuery(query='select * from dataset where dataset.name == "MSKCC Prostate Cancer"')
	dataset <- getDataset(id=datasets$dataset.id[1])
	layers <- getDatasetLayers(entity=dataset)
	checkTrue(5 <= layers$totalNumberOfResults)
	locations <- getLayerLocations(entity=layers$results[[1]])
	checkTrue(1 <= locations$totalNumberOfResults)
	previews <- getLayerPreviews(entity=layers$results[[1]])
	checkTrue(1 <= previews$totalNumberOfResults)
}

integrationTestSageBioCurationProjectChildEntityGet <- function() {
	projects <- synapseQuery('select * from project where project.name == "SageBioCuration"')
	project <- getProject(id=projects$project.id[1])
	datasets <- getProjectDatasets(entity=project)
	checkTrue(115 <= datasets$totalNumberOfResults)
}