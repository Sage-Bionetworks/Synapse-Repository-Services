\name{loadLayerData}
\alias{loadLayerData}
\title{
	Load Layer Data
}
\description{
	Downloads layer data files to the local cache and, if possible, loads the data into memory.
}
\usage{
	loadLayerData(entity)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{entity}{
		The entity whose data should be loaded
	}
}
\details{
	If the entity type currently has no method for loading data to memory, this function returns a list of file locations.
}
\value{
	List of the files contained in the layer. For supported entity types, the entity data will be loaded into memory.
}

\author{
	Matt Furia
}
\seealso{
	\code{\link{getDatasetLayers}}
}
\examples{
	## Not Run

	## end (Not Run)
}
