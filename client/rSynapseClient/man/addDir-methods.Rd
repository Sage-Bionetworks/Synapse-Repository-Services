\name{addDir-methods}
\docType{methods}
\alias{addDir-methods}
\alias{addDir,Layer,character-method}
\alias{addDir,Layer,missing-method}
\title{ Methods for Function \code{addDir}  }
\description{
	Methods for adding a directory containing files to an entity.
}
\section{Methods}{
\describe{
	\item{\code{signature(entity = "Layer", path = "character")}}{
		Recursively adds the files and directories under path to the root directory of the zip file.
	}

	\item{\code{signature(entity = "Layer", path = "missing")}}{
		Opens a file select dialog and recursively adds the files selected under the root directory of the zip file.
	}
	
}}
\keyword{methods}

