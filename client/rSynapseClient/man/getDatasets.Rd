\name{getDatasets}
\alias{getDatasets}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
getDatasets(queryParams, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{queryParams}{
%%     ~~Describe \code{queryParams} here~~
}
  \item{curlHandle}{
%%     ~~Describe \code{curlHandle} here~~
}
  \item{anonymous}{
%%     ~~Describe \code{anonymous} here~~
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
function (queryParams, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous")) 
{
    if (missing(queryParams)) {
        queryParams <- list()
    }
    if (!is.list(queryParams)) {
        stop("params must be a list")
    }
    kService <- "/query?query"
    kQueryRoot <- "select * from dataset"
    query <- kQueryRoot
    for (n in names(queryParams)) {
        query <- paste(query, n, queryParams[n], sep = " ")
    }
    uri <- paste(kService, curlEscape(query), sep = "=")
    json.records <- synapseGet(uri = uri, curlHandle = curlHandle, 
        anonymous = anonymous)
    result <- .parseJSONRecords(json.records$results)
    attr(result, "total_records_available") <- json.records$totalNumberOfResults
    return(result)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
