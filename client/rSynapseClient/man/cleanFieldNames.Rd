\name{cleanFieldNames}
\alias{cleanFieldNames}
\title{
Clean field names -internal use only
}
\description{
Removes special characters and changes names to be lower-case.
}
\usage{
cleanFieldNames(field.names)
}
\arguments{
\item{field.names}{An array of characters.}
}
\details{
Substitutes whitespace and punctuation with a period ("."). The substitution is done such that multiple
consecutive special characters are substituted for a single ".". All upper-case characters are changed
to lower case.
	
The regular expression used for substitution is: "[[:punct:][:space:]]+". See the help file for regexp for details
on what characters this regular expression will match.
}
\value{
The list of transformed field names.
}

\author{
Matt Furia
}
\note{
This function is for internal use and is not intended to be called by the user.
}

\examples{
f.names <- c("field 1", "field_2", "Field_-3", "Field/4", "field\5")
cleanFieldNames(f.names)
}
\keyword{ utilities }