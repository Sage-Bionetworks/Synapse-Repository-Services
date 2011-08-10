setMethod(
		f = "loadLayerData",
		signature = "list",
		definition = function(entity)
		{
			.cacheFiles(entity)
		}
)

setMethod(
		f = "loadLayerData",
		signature = "character",
		definition = function(entity)
		{
			.cacheFiles(entity)
		}
)

setMethod(
		f = "loadLayerData",
		signature = "integer",
		definition = function(entity)
		{
			.cacheFiles(entity)
		}
)

