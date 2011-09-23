\name{synapseAuthServiceEndpoint}
\alias{synapseAuthServiceEndpoint}
\alias{synapseRepoServiceEndpoint}
\alias{synapseResetEndpoints}
\title{
	Synapse REST Endpoints
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseAuthServiceEndpoint(endpoint)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{endpoint}{
%%     ~~Describe \code{endpoint} here~~
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
function (endpoint) 
{
    if (!missing(endpoint)) {
        .setCache("authserviceEndpoint", endpoint)
        url <- .ParsedUrl(url = endpoint)
        .setCache("authserviceEndpointLocation", paste(url@protocol, 
            "://", url@authority, sep = ""))
        .setCache("authserviceEndpointPrefix", url@path)
    }
    else {
        return(.getCache("authserviceEndpoint"))
    }
  }
}
