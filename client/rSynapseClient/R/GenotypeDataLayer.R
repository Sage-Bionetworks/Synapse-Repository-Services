setClass(
		Class = "GenotypeDataLayer",
		contains = "DataLayer",
)

setMethod(
		f = "layerType",
		signature = "GenotypeDataLayer",
		definition = function(object){
			return(.getCache("layerCodeTypeMap")[["GenotypeDataLayer"]])
		}
)
