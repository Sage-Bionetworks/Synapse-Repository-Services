\name{getDataPacketSummaries}
\alias{getDataPacketSummaries}
\alias{getDataPacketSummary}
\title{
	Retrieve Data Packet Summaries
}
\description{
	Returns summary information about the data packets available for download.
}
\usage{
	getDataPacketSummaries(query.params = list(limit = 10, offset = 1))
}
\arguments{
  	\item{query.params}{ A named list of parameters to be passed to the Service API}
}
\details{
	
}
\value{
	An S4 ResultSet object.
}

\author{
	Matt Furia
}
\note{
	A  valid username and password are required to use this funciton.
}

\seealso{
\code{\link{sbnLogin}}
\code{\linkS4class{ResultSet}}
}
\examples{
	## Not run:
	
##	sbnLogin('demouser', 'demouser-pw')
##	rs <- getDataPacketSummaries(list(limit=50))
	
	## End(Not run)
}
\keyword{ database }
