\name{getClientConfig}
\alias{getClientConfig}
\alias{setClientConfig}
\alias{}
\title{
Retrieve the Client Configuration Object
}
\description{
	
}
\usage{
getClientConfig()
}
%- maybe also 'usage' for other objects documented here.
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
	Matt Furia
}
\note{
	This method is for internal use only.
}

\seealso{
\code{\link{setClientConfig}}
%% ~~objects to See Also as \code{\link{help}}, ~~~
}
\examples{
##---- Should be DIRECTLY executable !! ----
##-- ==>  Define data, use random,
##--	or do  help(data=index)  for the standard data sets.

## The function is currently defined as
function () 
{
    if (!exists(getClientConfigName(), envir = getClientConfigEnvironment())) {
        setClientConfig(new(Class = "ClientConfig"))
    }
    config <- get(getClientConfigName(), envir = getClientConfigEnvironment())
    return(config)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
