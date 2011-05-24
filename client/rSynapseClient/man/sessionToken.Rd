\name{sessionToken}
\alias{sessionToken}
\title{
	get/set the session token
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
	sessionToken(session.token, check.validity = TRUE)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{session.token}{}
  \item{check.validity}{}
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
function (session.token, check.validity = TRUES) 
{
    config <- getClientConfig()
    if (!missing(session.token)) {
        config@session.token <- session.token
        setClientConfig(config)
    }
    else {
        if (is.null(config@session.token)) {
            stop("You must be logged in to do that!")
        }
        return(config@session.token)
    }
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
