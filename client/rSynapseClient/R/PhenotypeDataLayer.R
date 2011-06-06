setClass(
		Class = "PhenotypeDataLayer",
		contains = "DataLayer",
		prototype = prototype(
				datasetId = NULL,
				dataStatus = NULL,
				numRecords = 0
		)
)

setMethod(
		f = "layerType",
		signature = "PhenotypeDataLayer",
		definition = function(object){
			return(.getCache("layerCodeTypeMap")[["PhenotypeDataLayer"]])
		}
)

setMethod(
		f = "show",
		signature = "PhenotypeDataLayer",
		definition = function(object){
			cat("Data Type: ", 
					layerType(object),
					"\nData Status: ",
					object@dataStatus,
					"\t# Records: ",
					object@numRecords,
					"\n"
			)
		}
)