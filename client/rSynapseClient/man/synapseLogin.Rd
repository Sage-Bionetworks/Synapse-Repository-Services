\name{synapseLogin}
\alias{synapseLogin}
\alias{synapseLogout}
\alias{.getPassword}
\alias{.getUsername}
\title{
	Authentication
}
\description{
	Functions for handling Synapse authentication.
}
\usage{
	synapseLogin(username, password)
	synapseLogout(localOnly=FALSE)
	.getUsername
	.getPassword()
}

\arguments{
  \item{username}{
		Synapse username
	}
  \item{password}{
		API password associated with the username (see details)
	}
  \item{localOnly}{
  		boolean controlling whether logout is done locally only, or propagated to Synapse
	}
}
\details{
	If username and/or password arguments are not provided, the user will be prompted for input. If logout is propagated to
	Synapse (ie. localOnly = TRUE), it is likely that the user will be logged out for all clients currently in use
}

\author{
	Matt Furia
}