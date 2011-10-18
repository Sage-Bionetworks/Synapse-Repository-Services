\name{addFile-methods}
\docType{methods}
\alias{addFile-methods}
\alias{addFile,CachedLocation,character,character-method}
\alias{addFile,Layer,character,ANY-method}
\alias{addFile,Layer,character,character-method}
\alias{addFile,Layer,missing,character-method}
\alias{addFile,Layer,missing,missing-method}
\title{Methods for Function \code{addFile}}
\description{
	Methods for function \code{addFile}
}
\section{Methods}{
\describe{

\item{\code{signature(entity = "CachedLocation", file = "character", path = "character")}}{
	This method is not intended to be called by the user.
}

\item{\code{signature(entity = "Layer", file = "character", path = "character")}}{
	Add a file and specify the path.
}

\item{\code{signature(entity = "Layer", file = "missing", path = "character")}}{
Calling this method will open a file select dialog and will place the selected file in the subdirectory specified by path.
}

\item{\code{signature(entity = "Layer", file = "missing", path = "missing")}}{
	Calling this method will open a file select dialog and will place the selected file in the root directory of the zip file.
}
}}
\keyword{methods}
