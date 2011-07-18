\name{entityCRUD}
\alias{createEntity}
\alias{createDataset}
\alias{createLayer}
\alias{createProject}
\alias{createPreview}
\alias{createLocation}
\alias{deleteEntity}
\alias{deleteDataset}
\alias{deleteLayer}
\alias{deleteProject}
\alias{deletePreview}
\alias{deleteLocation}
\alias{updateEntity}
\alias{updateDataset}
\alias{updateLayer}
\alias{updateProject}
\alias{updatePreview}
\alias{updateLocation}
\alias{getEntity}
\alias{getDataset}
\alias{getLayer}
\alias{getProject}
\alias{getPreview}
\alias{getLocation}
\title{
	CRUD operations for Synapse Entities
}
\description{
	Given a list holding metadata, perform a CRUD operation on a Synapse Entity. For get, update and delete
	operations, the entity must contain an element named "id" that contains a valide Synapse entity ID. For
	create operations, the returned entity 
}
\usage{
	createDataset(entity)
	createLayer(entity)
	createProject(entity)
}
\arguments{
  \item{entity}{
		A list containing the entity metadata.
	}
}
\value{
	This function is called for it's side-effect.
}

\author{
	Nicole Deflaux
}

\examples{

	## Not Run
	\dontrun{
		ds <- list(name="aFakeDatasetName")
		createDataset(ds)
	}
	## End (Not Run)
}

