source("http://bioconductor.org/biocLite.R")

pkgInstall <- function (package, stack='prod', ...) {
  major <- as.numeric(R.Version()$major)
  minor <- as.numeric(R.Version()$minor)

  if (major == 2 && minor >= 13) {
    ##ok
  } else {
    stop("You must be running R version 2.13 or higher")
  }

  rVersion <- paste(R.Version()$major, as.integer(R.Version()$minor), sep=".")
  sageRepos <- paste("http://sage.fhcrc.org/CRAN",
                     stack, rVersion, sep="/")
  allRepos <- c(biocinstallRepos(), sageRepos)

  install.packages(package, repos=allRepos, ...)
}

