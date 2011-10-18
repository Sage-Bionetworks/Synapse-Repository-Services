\name{addFile}
\alias{addFile}
\alias{deleteFile}
\alias{moveFile}
\alias{addDir}
\alias{edit}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
	Manipulate Entity Files
}
\description{
	Functions for manipulating the files belonging to an entity.
}
\usage{
	addFile(entity, file, path)
	deleteFile(entity, file)
	moveFile(entity, src, dest)
	addDir(entity, src)
	edit(entity)
}
\arguments{
  \item{entity}{
		The entity whose files will be modified
}
  \item{file}{
  		A path to the file on the local file system, when adding, or the relative path to the file when deleting (see Details)
}
  \item{path}{
		The relative path where the file should be placed.
}
}
\details{
	The file paths give by entity$files are relative to the root directory of zip file that will store the entity files.
}
\value{
	The modified entity. 
}
\author{
	Matt Furia
}
\note{
	If the return value is not captured, the entity will not be modified. Currently edit is only supported for Code entity types.
}


\seealso{
	\code{\link{addObject}}
}
