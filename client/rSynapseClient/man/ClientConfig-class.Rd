\name{ClientConfig-class}
\Rdversion{1.1}
\docType{class}
\alias{ClientConfig-class}
\alias{setClientConfig,ClientConfig-method}
\alias{show,ClientConfig-method}

\title{Class "ClientConfig"}
\description{
Holds the host URLs and session token for the data repository.
}
\section{Objects from the Class}{
Objects can be created by calls of the form \code{new("ClientConfig", ...)}.
When called without arguments, the constructor will return and object having the 
correct host names for both the http and https service. The default value of the session token
is NULL, which indicates that the user is not logged in.
}
\section{Slots}{
  \describe{
    \item{\code{host}:}{Object of class \code{"character"} The URL for the http service.}
    \item{\code{sslhost}:}{Object of class \code{"character"} The URL for the https servic }
    \item{\code{session.token}:}{Object of class \code{"character"} The session token passed to the service 
    	for authentication.}
  }
}
\section{Methods}{
  \describe{
    \item{setClientConfig}{\code{signature(object = "ClientConfig")}: ... }
    \item{show}{\code{signature(object = "ClientConfig")}: ... }
	 }
}

\author{
Matt Furia
}


\seealso{
\code{\link{sbnLogin}}
\code{\link{sbnSslHostName}}
\code{\link{sbnHostName}}
}
\examples{
showClass("ClientConfig")
}
\keyword{ classes }
