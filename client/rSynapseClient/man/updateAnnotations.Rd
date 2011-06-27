\name{updateAnnotations}
\alias{updateAnnotations}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
updateAnnotations(annotations, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{annotations}{
%%     ~~Describe \code{annotations} here~~
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
function (annotations, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous")) 
{
    if (!grepl("annotations", annotations$uri)) {
        stop("this is not an annotations entity")
    }
    for (key in names(annotations)) {
        if ("list" == class(annotations[[key]])) {
            for (annotKey in names(annotations[[key]])) {
                if ("list" != class(annotations[[key]][[annotKey]])) {
                  annotations[[key]][[annotKey]] <- list(annotations[[key]][[annotKey]])
                }
            }
        }
    }
    synapsePut(uri = annotations$uri, entity = annotations, curlHandle = curlHandle, 
        anonymous = anonymous)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
