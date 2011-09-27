# TODO: Add comment
# 
# Author: furia
###############################################################################

.buildSynapseUrl <- 
		function(entity)
{
	url <- sprintf("%s/#Lookup:%s", synapsePortalEndpoint(), entity)
}
