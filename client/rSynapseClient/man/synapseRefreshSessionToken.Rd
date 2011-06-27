\name{synapseRefreshSessionToken}
\alias{synapseRefreshSessionToken}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseRefreshSessionToken(sessionToken, host = .getAuthServiceEndpointLocation())
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{sessionToken}{
%%     ~~Describe \code{sessionToken} here~~
}
  \item{host}{
%%     ~~Describe \code{host} here~~
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
function (sessionToken, host = .getAuthServiceEndpointLocation()) 
{
    kService <- "/session"
    entity <- list()
    entity$sessionToken <- sessionToken
    uri <- kService
    response <- synapsePut(uri = uri, entity = entity, path = .getAuthEndpointPrefix(), 
        host = host, anonymous = TRUE)
    .setCache("sessionTimestamp", Sys.time())
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
