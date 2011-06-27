\name{synapseSessionToken}
\alias{synapseSessionToken}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
TODO write documentation
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
synapseSessionToken(sessionToken, check.validity = FALSE, refresh.duration = .getCache("sessionRefreshDurationMin"))
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{sessionToken}{
%%     ~~Describe \code{sessionToken} here~~
}
  \item{check.validity}{
%%     ~~Describe \code{check.validity} here~~
}
  \item{refresh.duration}{
%%     ~~Describe \code{refresh.duration} here~~
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
function (sessionToken, check.validity = FALSE, refresh.duration = .getCache("sessionRefreshDurationMin")) 
{
    if (!missing(sessionToken)) {
        if (is.null(sessionToken)) 
            sessionToken <- ""
        if (check.validity) {
            synapseRefreshSessionToken(sessionToken)
        }
        .setCache("sessionToken", sessionToken)
    }
    else {
        sessionToken <- .getCache("sessionToken")
        elapsed.time.min <- (as.numeric(Sys.time()) - as.numeric(.getCache("sessionTimestamp")))/60
        if (!is.null(.getCache("sessionTimestamp")) & (check.validity || 
            elapsed.time.min >= refresh.duration)) {
            synapseRefreshSessionToken(sessionToken)
        }
        if (is.null(sessionToken)) 
            session.toke <- ""
        return(sessionToken)
    }
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
