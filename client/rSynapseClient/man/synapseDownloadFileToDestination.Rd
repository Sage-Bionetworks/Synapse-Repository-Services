\name{synapseDownloadFileToDestination}
\alias{synapseDownloadFileToDestination}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseDownloadFileToDestination(url, checksum, destfile, curlHandle = getCurlHandle(), opts = .getCache("curlOpts"))
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{url}{
%%     ~~Describe \code{url} here~~
}
  \item{checksum}{
%%     ~~Describe \code{checksum} here~~
}
  \item{destfile}{
%%     ~~Describe \code{destfile} here~~
}
  \item{curlHandle}{
%%     ~~Describe \code{curlHandle} here~~
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
function (url, checksum, destfile, curlHandle = getCurlHandle(), 
    opts = .getCache("curlOpts")) 
{
    if (file.exists(destfile)) {
        localFileChecksum <- md5sum(destfile)
        if (checksum == localFileChecksum) {
            return(destfile)
        }
    }
    splits <- strsplit(destfile, .Platform$file.sep)
    downloadDir <- path.expand(paste(splits[[1]][-length(splits[[1]])], 
        collapse = .Platform$file.sep))
    if (!file.exists(downloadDir)) {
        dir.create(downloadDir, recursive = TRUE)
    }
    writeBin(getBinaryURL(url, curl = curlHandle, .opts = opts), 
        con = destfile)
    tryCatch(.checkCurlResponse(curlHandle, paste(readLines(con = destfile, 
        warn = FALSE), collapse = "")), error = function(ex) {
        file.remove(destfile)
        stop(ex)
    })
    return(destfile)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
