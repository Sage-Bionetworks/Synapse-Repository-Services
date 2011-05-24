\name{initializeClientConfig}
\alias{initializeClientConfig}
\alias{sbnHostName}
\alias{sbnSslHostName}
\title{
	Set and Retrieve 
}
\description{
	Resets the client configuration. If no arguments are provided, the hostname and ssl hostname
	are set to the default values. The session token can not be set using this method and
	calling it will require the user to log in again before connecting to the Repository service. 
}
\usage{
	initializeClientConfig(host, sslhost)
	sbnHostName(host)
	sbnSslHostName(sslhost)
}
\arguments{
  \item{host}{URL of the http host}
  \item{sslhost}{URL of the https host}
}
\details{

}
\author{
	Matt Furia
}
\seealso{
	\code{\link{sbnLogin}}
}
\examples{
	sbnHostName()
	sbnHostName("http://new.host.com")
}

\keyword{ connection }

