.setUp <- function() {
  # Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

integrationTestSawyersDatasetChildEntityGet <- function() {
	datasets <- synapseQuery(query='select * from dataset where dataset.name == "MSKCC Prostate Cancer"')
	layers <- getDatasetLayers(entity=datasets$dataset.id[1], includeParentAnnot = FALSE)
	checkTrue(5 <= nrow(layers))
	layer <- synapseClient:::getLayer(entity=layers$id[1])
	previews <- synapseClient:::getLayerPreviews(entity=layer)
	checkTrue(1 <= nrow(previews))
	# TODO this user needs to agree to the use agreement before he/she can view the locations
	# locations <- synapseClient:::getLayerLocations(entity=layer)
	# checkTrue(1 <= nrow(locations))
}

integrationTestSageBioCurationProjectChildEntityGet <- function() {
	projects <- synapseQuery(query='select * from project where project.name == "SageBioCuration"')
	project <- synapseClient:::getProject(entity=projects$project.id[1])

	datasets <- synapseClient:::getProjectDatasets(entity=project)
	checkEquals(100, nrow(datasets))

	moreDatasets <- synapseClient:::getProjectDatasets(entity=project, limit=200)
	checkTrue(115 <= nrow(moreDatasets))
}
