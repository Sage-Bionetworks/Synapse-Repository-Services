setClass(
		Class = "DataLayer",
		representation(
				datasetId = "numeric",
				dataStatus = "character",
				numRecords = "numeric",
				uri = "character"
		),
		prototype = prototype(
				datasetId = NULL,
				dataStatus = NULL,
				numRecords = 0,
				uri = NULL
		)
)

setGeneric(
		name = "layerType",
		def = function(object){
			standardGeneric("layerType")
		}
		
)
