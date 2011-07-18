\name{synapseAuthServiceEndpoint}
\alias{synapseAuthServiceEndpoint}
\alias{.getAuthEndpointLocation}
\alias{.getAuthEndpointPrefix}
\alias{synapseRepoServiceEndpoint}
\alias{.getRepoEndpointLocation}
\alias{.getRepoEndpointPrefix}
\alias{synapseResetEndpoints}
\title{
	Synapse Service Endpoints
}
\description{
	Functions for setting and retrieving the authentication and repository service endpoints
	for Synapse.
}
\usage{
	synapseAuthServiceEndpoint(endpoint)
	synapseRepoServiceEndpoint(endpoint)
	synapseResetEndpoints()
}
\arguments{
  \item{endpoint}{
		the new service endpoint
	}
}
\details{
	By default, the auth service endpoint is set to https://auth-alpha.sagebase.org/auth/v1 and the repo
	service endpoint is set to https://reposvc-alpha.sagebase.org/repo/v1. The endpoints can be reset to the
	default values by calling synapseResetEndpoints.
}
\value{
	If called with no arguments, synapseAuthServiceEndpoint and synapseRepoServiceEndpoint return the current value
	of the Auth and Repo service endpoints, respectively.
}

\author{
	Matt Furia
}

\examples{
	## Not Run
	
	
	## End (Not Run)
}

