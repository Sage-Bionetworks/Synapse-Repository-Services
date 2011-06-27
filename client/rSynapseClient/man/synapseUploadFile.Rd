\name{synapseUploadFile}
\alias{synapseUploadFile}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseUploadFile(url, srcfile, checksum, method = "curl", quiet = FALSE, mode = "w", cacheOK = TRUE)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{url}{
%%     ~~Describe \code{url} here~~
}
  \item{srcfile}{
%%     ~~Describe \code{srcfile} here~~
}
  \item{checksum}{
%%     ~~Describe \code{checksum} here~~
}
  \item{method}{
%%     ~~Describe \code{method} here~~
}
  \item{quiet}{
%%     ~~Describe \code{quiet} here~~
}
  \item{mode}{
%%     ~~Describe \code{mode} here~~
}
  \item{cacheOK}{
%%     ~~Describe \code{cacheOK} here~~
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
function (url, srcfile, checksum, method = "curl", quiet = FALSE, 
    mode = "w", cacheOK = TRUE) 
{
    if (method == "curl") {
        extra <- if (quiet) 
            " -s -S "
        else if (.getCache("debug")) {
            "-v"
        }
        else ""
        status <- system(paste("curl", extra, " -k ", " -H Content-Type:application/binary", 
            " -H Content-MD5:", .hexMD5ToBase64MD5(checksum), 
            " -H x-amz-acl:bucket-owner-full-control", " --data-binary ", 
            "@", path.expand(srcfile), " -X PUT ", shQuote(url), 
            sep = ""))
    }
    else {
        stop("unsupported method:", method)
    }
    invisible(status)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
