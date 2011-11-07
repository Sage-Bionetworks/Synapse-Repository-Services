\name{startStep}
\alias{startStep}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
Start a new provenance Step
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
call with no arguments to start a new current step with the default parent

call with a parent Analysis entity to start that Step in the specified Analysis

if another provenance Step is already in progress as the current Step, it will be stopped and this new one will be started
}
\usage{
startStep(parentEntity)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{parentEntity}{
%%     ~~Describe \code{parentEntity} here~~
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
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
