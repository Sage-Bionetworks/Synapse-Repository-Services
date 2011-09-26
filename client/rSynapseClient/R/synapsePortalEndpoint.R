# TODO: Add comment
# 
# Author: furia
###############################################################################


synapsePortalEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("portalEndpoint", endpoint)
	}
	else {
		return(.getCache("portalEndpoint"))
	}
}
