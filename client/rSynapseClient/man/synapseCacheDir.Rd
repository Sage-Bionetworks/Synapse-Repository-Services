\name{synapseCacheDir}
\alias{synapseCacheDir}
\title{
	Synapse Local File Cache Directory
}
\description{
	Set and retrieve the local directory path to be used for caching synapse data.
}
\usage{
	synapseCacheDir(cacheDir)
}
\arguments{
  \item{cacheDir}{
		A valid directory path on the local filesystem.
	}
}

\value{
	If called with no arguments, returns the path to the root of the local file cache.
}
\author{
	Matt Furia
}
\seealso{
	\code{\link{synapseDownloadFile}}
}
\examples{
##---- Should be DIRECTLY executable !! ----
##-- ==>  Define data, use random,
##--	or do  help(data=index)  for the standard data sets.

## The function is currently defined as
function (cacheDir) 
{
    if (missing(cacheDir)) {
        return(.getCache("synapseCacheDir"))
    }
    .setCache("synapseCacheDir", cacheDir)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
