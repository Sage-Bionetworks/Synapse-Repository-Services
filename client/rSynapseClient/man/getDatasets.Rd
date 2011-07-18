\name{getDatasets}
\alias{getDatasets}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
	Get Synapse Datasets
}
\description{
	Get annotations for the datasets that are available in Synapse
}
\usage{
	getDatasets(queryParams)
}
\arguments{
  \item{queryParams}{
	named list of query parameters controlling which datasets are returned
	}
}
\value{
	A data frame containing the annotations
}

\author{
	Matt Furia
}

\seealso{
	\code{\link{getDataset}}
}
\examples{
	## Not Run
	\dontrun{
		synapseLogin('demouser@sagebase.org','demouser-pw')
		
		## get first 100 datasets
		dss <- getDatasets(c(limit=100))
		
		## get datasets 10-14
		dss <- getDatasets(c(limit=5, offset=9))
	}
	## end (Not Run)
}
