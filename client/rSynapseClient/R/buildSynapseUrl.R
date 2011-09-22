# TODO: Add comment
# 
# Author: furia
###############################################################################

.buildSynapseUrl <- 
		function(entity)
{
	if(grepl("staging", synapseRepoServiceEndpoint())){
		portal <- kServicePortalUrlMap[["staging"]]
	}else if(grepl("alpha", synapseRepoServiceEndpoint())){
		portal <- kServicePortalUrlMap[["alpha"]]
	}else{
		return("")
	}
	url <- sprintf("%s/#Lookup:%s", portal, entity)
}
