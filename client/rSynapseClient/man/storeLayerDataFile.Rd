\name{storeLayerDataFile}
\alias{storeLayerDataFile}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
storeLayerDataFile(layerMetadata, layerDataFilepath)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{layerMetadata}{
%%     ~~Describe \code{layerMetadata} here~~
}
  \item{layerDataFilepath}{
%%     ~~Describe \code{layerDataFilepath} here~~
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
function (layerMetadata, layerDataFilepath) 
{
    if (!is.list(layerMetadata)) {
        stop("layerMetadata must be supplied of R type list")
    }
    locationMetadata <- list()
    if ("id" \%in\% names(layerMetadata)) {
        outputLayerMetadata <- updateLayer(entity = layerMetadata)
        locations <- getLayerLocations(entity = outputLayerMetadata)
        awss3Location <- match("awss3", locations$type, nomatch = 0)
        if (1 < length(awss3Location)) {
            stop("there are multiple awss3 locations for this layer")
        }
        else if (0 != awss3Location[1]) {
            locationMetadata <- locations[awss3Location[1]]
        }
    }
    else {
        outputLayerMetadata <- createLayer(entity = layerMetadata)
    }
    checksum <- md5sum(layerDataFilepath)
    if (grepl(layerDataFilepath, "/")) {
        splits <- strsplit(layerDataFilepath, "/")
        filename <- splits[[1]][length(splits[[1]])]
    }
    else {
        splits <- strsplit(layerDataFilepath, "/")
        filename <- splits[[1]][length(splits[[1]])]
    }
    s3Key = paste("/rClient", outputLayerMetadata$id, filename, 
        sep = "/")
    locationMetadata$parentId <- outputLayerMetadata$id
    locationMetadata$path <- s3Key
    locationMetadata$type <- "awss3"
    locationMetadata$md5sum <- checksum[[1]]
    if ("id" \%in\% names(locationMetadata)) {
        outputLocationMetadata <- updateLocation(entity = locationMetadata)
    }
    else {
        outputLocationMetadata <- createLocation(entity = locationMetadata)
    }
    synapseUploadFile(url = outputLocationMetadata$path, srcfile = layerDataFilepath, 
        checksum = checksum[[1]])
    return(outputLayerMetadata)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
