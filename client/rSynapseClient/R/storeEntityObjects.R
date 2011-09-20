# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################



setGeneric(
		name = "storeEntityObjects",
		def = function(entity){
			standardGeneric("storeEntityObjects")
		}
)

setMethod(
		f = "storeEntityObjects",
		signature = "Layer",
		def = function(entity){
			stop("Not Yet Implemented")
		}
)