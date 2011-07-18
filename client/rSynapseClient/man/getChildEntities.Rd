\name{getChildEntities}
\alias{getChildEntities}
\alias{getLayerLocations}
\alias{getLayerPreviews}
\alias{getProjectDatasets}
\alias{getDatasetLayers}
\title{
	Get Annotations for Child Entities
}
\description{
	Returns an array of lists, each of which contain annotations for entities that are children
	of the entity provided.
}
\usage{
	getLayerLocations(entity, offset=1, limit=100)
}
\arguments{
  \item{entity}{
		The parent entity whose child entities should be returned
	}
  \item{offset}{
		How many entities should be skipped
	}
  \item{limit}{
		The maximum number of entities to return
	}
}

\value{
	An array of lists, which each list containing the annotations for a single child entity.
}

\author{
	Nicole Deflaux
}

\examples{
## Not Run

## End Not Run
}

