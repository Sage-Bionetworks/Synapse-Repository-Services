\name{getLayers}
\alias{getLayers}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
getLayers(entity = entity, returnS4Objects = FALSE, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{entity}{
%%     ~~Describe \code{entity} here~~
}
  \item{returnS4Objects}{
%%     ~~Describe \code{returnS4Objects} here~~
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
function (entity = entity, returnS4Objects = FALSE, curlHandle = getCurlHandle(), 
    anonymous = .getCache("anonymous")) 
{
    if (!"layers" \%in\% names(entity)) {
        stop("the entity does not have layers")
    }
    result <- synapseGet(uri = entity$layers, curlHandle = curlHandle, 
        anonymous = anonymous)
    layers <- result$results
    returnVal <- NULL
    for (i in 1:length(layers)) {
        names(layers)[i] <- layers[[i]]$type
        class(layers[[i]]) <- "layerList"
        if (returnS4Objects) {
            returnVal <- c(returnVal, Layer(layers[[i]]))
        }
    }
    if (returnS4Objects) {
        return(returnVal)
    }
    return(layers)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
