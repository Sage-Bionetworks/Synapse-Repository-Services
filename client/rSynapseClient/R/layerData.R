setClass(
		Class = "LayerData",
		representation = representation(
				layer = "Layer",
				files = "layerFiles",
				data = "data.frame",
				rowDict = "data.frame",
				colDict = "data.frame"
		),
		prototype = NULL ##this is a virtual class
)

setMethod(
		f = "show",
		definition = function(object){
			show(Layer)
			show(LayerFiles)
		}
)

setClass(
		Class = "PhenotypeLayerData",
		contains = "LayerData",
		prototype = prototype(
				layer = NULL,
				files = NULL,
				data = NULL,
				rowDict = NULL,
				colDict = NULL
		)
)

PhenotypeLayerData <- function(Layer){
	loadLayerData(Layer)
}


