source("http://bioconductor.org/biocLite.R")

major <- as.numeric(R.Version()$major)
minor <- as.numeric(R.Version()$minor)

if (major == 2 && (minor >= 13 && (minor < 14))) {
  ##ok
} else {
  stop("You must be running R version 2.13")
}

rversion <- paste(R.Version()$major, as.integer(R.Version()$minor), sep=".")
sageRepos <- paste("http://sage.fhcrc.org/sagebioCRAN/",
  rversion, sep="")
allRepos <- c(sageRepos, biocinstallRepos())

pkgInstall <- function (package, ...) {
        install.packages(package, repos = allRepos, ...)
}

