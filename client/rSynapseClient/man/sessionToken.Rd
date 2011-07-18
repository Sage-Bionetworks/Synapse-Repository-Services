\name{sessionToken}
\alias{refreshSessionToken}
\alias{sessionToken}

\title{
	Refresh the Auth Service Session Token
}
\description{
	Resets the counter in the Synapse auth service that determines when the token expires. 
}
\usage{
	sessionToken(sessionToken, checkValidity = FALSE, refreshDuration = .getCache("sessionRefreshDurationMin"))
	refreshSessionToken(sessionToken)
}
\arguments{
  \item{sessionToken}{
		Session token returned by the Synapse auth service
	}
  \item{checkValidity}{
  		boolean indicating whether the session token's validity should be checked before setting it
  		in the memory cache and before returning it to the caller
  }
  \item{refreshDuration}{
  		duration that should be allowed to elapse before refreshing the session token
  }
}

\value{
	refreshSessionToken has no return value, but is called for it's side-effects. The first side effect is that the expiration counter maintained by 
	the Synapse auth service is reset. The second is that the local timestamp cache value is reset.
	
	sessionToken returns the current value of the session token. If the user is not logged in to
	Synapse, the function returns an empty string.
}
\author{
	Matt Furia
}
\note{
	This function will throw an exception if the session token is invalid or is already expired.
}


\seealso{
	\code{\link{synapseLogin}}
}
\examples{

	## Not run:
	\dontrun{
		synapseLogin('demouser@sagebase.org','demouser-pw')
		refreshSessionToken(sessionToken())
	}
	## End (Not run)
}
