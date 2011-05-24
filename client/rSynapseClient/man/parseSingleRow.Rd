\name{parseSingleRow}
\alias{parseSingleRow}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
parse a single row of tabular data in JSON format
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
parseSingleRow(row.list)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{row.list}{
%%     ~~Describe \code{row.list} here~~
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
function (row.list) 
{
    kNaValue <- NA
    names(row.list) <- cleanFieldNames(names(row.list))
    row <- data.frame()
    for (field.name in names(row.list)) {
        this.value <- row.list[[field.name]]
        if (is.null(this.value)) {
            this.value <- kNaValue
        }
        row[1, field.name] <- this.value
    }
    return(row)
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line
