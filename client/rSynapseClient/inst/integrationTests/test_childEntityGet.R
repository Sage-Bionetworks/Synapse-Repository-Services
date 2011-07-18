.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestSawyersDatasetChildEntityGet <- function() {
	datasets <- synapseQuery(query='select * from dataset where dataset.name == "MSKCC Prostate Cancer"')
	layers <- getDatasetLayers(entity=datasets$dataset.id[1], includeParentAnnot = FALSE)
	checkTrue(5 <= dim(layers)[1])
	layer <- getLayer(entity=layers$id[1])
	locations <- getLayerLocations(entity=layer)
	checkTrue(1 <= dim(locations)[1])
	previews <- getLayerPreviews(entity=layer)
	checkTrue(1 <= dim(previews)[1])
}

integrationTestSageBioCurationProjectChildEntityGet <- function() {
	projects <- synapseQuery(query='select * from project where project.name == "SageBioCuration"')
	project <- getProject(entity=projects$project.id[1])

	datasets <- getProjectDatasets(entity=project)
	checkEquals(100, dim(datasets)[1])

	moreDatasets <- getProjectDatasets(entity=project, limit=200)
	checkTrue(115 <= dim(moreDatasets)[1])
}
