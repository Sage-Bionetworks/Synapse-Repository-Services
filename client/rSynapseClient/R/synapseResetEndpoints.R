synapseResetEndpoints <-
		function()
{
	synapseAuthServiceEndpoint("https://auth-staging.sagebase.org/auth/v1")
	synapseRepoServiceEndpoint("https://repo-staging.sagebase.org/repo/v1")
	synapsePortalEndpoint("http://synapse-staging.sagebase.org")
}
