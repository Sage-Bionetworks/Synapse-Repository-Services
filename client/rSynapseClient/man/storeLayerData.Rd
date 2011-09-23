\name{storeLayerData}
\alias{storeLayerData}
\alias{storeLayerDataFile}
\title{
	Store Layer Data
}
\description{
	Store a layer metadata and associated data file in Synapse.
}
\usage{
	storeLayerData(entity, ...)
	storeLayerDataFile(entity, layerDataFilepath)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{entity}{
	The Entity
	}
  \item{layerDataFilepath}{
  	The path to the file that should be stored
  }
  \item{...}{
		Objects to be stored
	}
}

\value{
	This function is called for it's side effects, which are to create a new layer and add
	the layer data file to Synapse.
}

\author{
	Nicole Deflaux
}

\examples{
	## Not Run
	
	## end(Not Run)
}

