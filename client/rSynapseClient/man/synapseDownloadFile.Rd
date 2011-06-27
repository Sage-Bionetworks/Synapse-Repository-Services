\name{synapseDownloadFile}
\alias{synapseDownloadFile}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseDownloadFile(url, checksum, curlHandle = getCurlHandle(), cacheDir = synapseCacheDir(), opts = .getCache("curlOpts"))
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{url}{
%%     ~~Describe \code{url} here~~
}
  \item{checksum}{
%%     ~~Describe \code{checksum} here~~
}
  \item{curlHandle}{
%%     ~~Describe \code{curlHandle} here~~
}
  \item{cacheDir}{
%%     ~~Describe \code{cacheDir} here~~
}
  \item{opts}{
%%     ~~Describe \code{opts} here~~
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
function (url, checksum, curlHandle = getCurlHandle(), cacheDir = synapseCacheDir(), 
    opts = .getCache("curlOpts")) 
{
    parsedUrl <- .ParsedUrl(url)
    destfile <- file.path(cacheDir, gsub("^/", "", parsedUrl@path))
    destfile <- path.expand(destfile)
    synapseDownloadFileToDestination(url = url, checksum = checksum, 
        destfile = destfile, opts = opts)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
