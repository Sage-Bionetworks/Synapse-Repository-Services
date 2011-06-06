setClass(
		Class = "ExpressionDataLayer",
		representation(
				tissue = "character"
		),
		contains = "DataLayer",
		prototype = prototype(
				tissue = NULL
		)
)

setMethod(
		f = "layerType",
		signature = "ExpressionDataLayer",
		definition = function(object){
			return(.getCache("layerCodeTypeMap")[["ExpressionDataLayer"]])
		}
)

setMethod(
		f = "show",
		signature = "ExpressionDataLayer",
		definition = function(object){
			cat("Data Type: ", 
					layerType(object),
					"\nData Status: ",
					object@dataStatus,
					"\tPlatform: ",
					object@platform,
					"\tissue: ",
					object@tissue,
					"\t# Records: ",
					object@numRecords,
					"\n"
			)
		}
)