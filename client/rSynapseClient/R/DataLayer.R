setClass(
		Class = "DataLayer",
		representation(
				layerId = "numeric",
				parentId = "numeric",
				dataStatus = "character",
				numRecords = "numeric",
				uri = "character"
		),
		prototype = prototype(
				layerId = NULL,
				parentId = NULL,
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
