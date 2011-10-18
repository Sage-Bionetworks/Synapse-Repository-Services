\name{addObject-methods}
\docType{methods}
\alias{addObject-methods}
\alias{addObject,Layer,ANY-method}
\alias{addObject,Layer,data.frame-method}
\alias{addObject,Layer,list-method}
\title{ Methods for Function \code{addObject}  }
\description{
 ~~ Methods for function \code{addObject}  ~~
}
\section{Methods}{
\describe{

\item{\code{signature(entity = "Layer", object = "ANY", name="missing", unlist="missing")}}{
	store the object with it's default name. unlist lists.
}

\item{\code{signature(entity = "Layer", object = "data.frame", name="missing", "missing")}}{
	data frames should never be "unlisted".
}
\item{\code{signature(entity = "Layer", object = "data.frame", name="character", "missing")}}{
	data frames should never be "unlisted". specify the name of the data frame object
}

\item{\code{signature(entity = "Layer", object = "list")}}{
%%  ~~describe this method here~~
}

\item{\code{signature(entity = "Layer", object = "list")}}{
%%  ~~describe this method here~~
}

}}
\keyword{methods}
