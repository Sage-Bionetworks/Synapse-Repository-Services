.setUp <- function() {
	# Perhaps do some setup stuff here like read an override for the respository and auth services urls to use
}
.tearDown <- function() {
	# Do some test cleanup stuff here, if applicable
}

integrationTestQuery <- function() {
	datasets <- synapseQuery(query='select * from dataset limit 10')
	# We should get back 10 datasets
	checkEquals(nrow(datasets), 10)
	# The fields returned by this service API may change over time, but
	# there are a few we should always expect to receive
	checkTrue("dataset.id" %in% names(datasets))
	checkTrue("dataset.name" %in% names(datasets))
	checkTrue("dataset.version" %in% names(datasets))
	checkTrue("dataset.status" %in% names(datasets))
	checkTrue("dataset.Species" %in% names(datasets))
}

integrationTestPaging <- function() {
	firstPageDatasets <- synapseQuery(query='select * from dataset limit 20 offset 1')
	secondPageDatasets <- synapseQuery(query='select * from dataset limit 20 offset 21')
	# We should get back 20 datasets
	checkEquals(nrow(firstPageDatasets), 20)
	checkEquals(nrow(secondPageDatasets), 20)
	# And they do not overlap
	checkEquals(length(union(firstPageDatasets$dataset.id,
							secondPageDatasets$dataset.id)),
			40)
}

integrationTestQueryForDataset <- function() {
	datasets <- synapseQuery(query='select * from dataset where dataset.name == "MSKCC Prostate Cancer"')
	# We should get back 1 dataset
	checkEquals(nrow(datasets), 1)
	# And its name should match the one we searched for
	checkEquals(datasets$dataset.name, "MSKCC Prostate Cancer")
}

integrationTestLotsOQueries <- function() {

	datasetsOrderBy <- synapseQuery(query='select * from dataset order by Number_of_Samples DESC limit 3')
	checkTrue(3 == nrow(datasetsOrderBy))

	datasetsMultiWhere <- synapseQuery(query='select * from dataset where dataset.Species == "Human" and dataset.Number_of_Samples > 100 limit 3 offset 1')
	checkTrue(3 == nrow(datasetsMultiWhere))

	datasetsSingleWhere <- synapseQuery(query='select * from dataset where name == "MSKCC Prostate Cancer"')
	checkTrue(1 == nrow(datasetsSingleWhere))
	
	mskccDatasetId <- datasetsSingleWhere$dataset.id[1]

	layers <- synapseQuery(query=paste('select * from layer where layer.parentId ==', mskccDatasetId, sep=' '))
	checkTrue(6 <= nrow(layers))

	layersOrderBy <- synapseQuery(query=paste('select * from layer where layer.parentId == ', mskccDatasetId, 'ORDER BY type', sep=' '))
	checkTrue(6 <= nrow(layersOrderBy))
}
