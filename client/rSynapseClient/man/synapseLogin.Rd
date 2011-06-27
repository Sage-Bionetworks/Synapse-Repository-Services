\name{synapseLogin}
\alias{synapseLogin}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseLogin(username, password, curlHandle = getCurlHandle(), host = .getAuthEndpointLocation(), path = .getAuthEndpointPrefix())
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{username}{
%%     ~~Describe \code{username} here~~
}
  \item{password}{
%%     ~~Describe \code{password} here~~
}
  \item{curlHandle}{
%%     ~~Describe \code{curlHandle} here~~
}
  \item{host}{
%%     ~~Describe \code{host} here~~
}
  \item{path}{
%%     ~~Describe \code{path} here~~
}
}
\details{
%%  ~~ If necessary, more details than the description above ~~
}
\value{
%%  ~Describe the value returned
%%  If it is a LIST, use
%%  \item{comp1 }{Description of 'comp1'}
%%  \item{comp2 }{Description of 'comp2'}
%% ...
}
\references{
%% ~put references to the literature/web site here ~
}
\author{
%%  ~~who you are~~
}
\note{
%%  ~~further notes~~
}

%% ~Make other sections like Warning with \section{Warning }{....} ~

\seealso{
%% ~~objects to See Also as \code{\link{help}}, ~~~
}
\examples{
##---- Should be DIRECTLY executable !! ----
##-- ==>  Define data, use random,
##--	or do  help(data=index)  for the standard data sets.

## The function is currently defined as
function (username, password, curlHandle = getCurlHandle(), host = .getAuthEndpointLocation(), 
    path = .getAuthEndpointPrefix()) 
{
    kService <- "/session"
    entity <- list()
    entity$email <- username
    entity$password <- password
    response <- synapsePost(uri = kService, entity = entity, 
        host = host, path = path, curl = curlHandle)
    .checkCurlResponse(curlHandle, response)
    synapseSessionToken(response$sessionToken, check.validity = FALSE)
    .setCache("sessionTimestamp", Sys.time())
    cat(paste("Welcome ", response$displayName, "!\n", sep = ""))
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
