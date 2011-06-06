setClass(
		Class = "MicroArrayDataLayer",
		representation(
				vendor = "character",
				platform = "character"
		),
		contains = "DataLayer",
		prototype = prototype(
				vendor = NULL,
				platform = NULL
		)
)

setMethod(
		f = "layerType",
		signature = "MicroArrayDataLayer",
		definition = function(object){
			return(.getCache("layerCodeTypeMap")[["MicroArrayDataLayer"]])
		}
)

setMethod(
		f = "show",
		signature = "MicroArrayDataLayer",
		definition = function(object){
			cat("Data Type: ", 
					layerType(object),
					"\nData Status: ",
					object@dataStatus,
					"\tPlatform: ",
					object@platform,
					"\t# Records: ",
					object@numRecords,
					"\n"
			)
		}
)