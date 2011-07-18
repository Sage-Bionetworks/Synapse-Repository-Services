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
	storeLayerData(layerMetadata, layerData)
	storeLayerDataFile(layerMetadata, layerDataFilepath)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{layerMetadata}{
	list containing metadata as name/value pairs.
}
  \item{layerData}{
		path to the layer data file.
	}
  \item{layerDataFilepath}{
  		file path to the data file
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

