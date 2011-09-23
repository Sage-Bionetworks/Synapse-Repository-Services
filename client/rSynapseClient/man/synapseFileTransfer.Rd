\name{synapseFileTransfer}
\alias{synapseDownloadFile}
\alias{synapseDownloadFileToDestination}
\alias{synapseUploadFile}
\title{
	Upload/Download a File from Synapse
}
\description{
	Functions for uploading and downloading files using the libcurl library that is part of RCurl.
}
\usage{
	synapseDownloadFile(url, checksum, curlHandle = getCurlHandle(), cacheDir = synapseCacheDir(), opts = .getCache("curlOpts"))
	synapseDownloadFileToDestination(url, destfile, checksum, curlHandle = getCurlHandle(), opts = .getCache("curlOpts"))
	synapseUploadFile(url, srcfile, checksum, contentType =
                 "application/binary", method = "curl", quiet = FALSE,
                 mode = "w", cacheOK = TRUE)

}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{url}{
		url of the remote file
	}
  \item{srcfile}{
  		local file to upload
  	}
  \item{checksum}{
		checksum of the local file
	}
  \item{curlHandle}{
  		curlHandle for the request
	}
  \item{cacheDir}{
		local file cache directory where downloaded files will be stored
	}
  \item{opts}{
		curl options
	}
  \item{destfile}{
  		local file location
  	}
  \item{method}{
  		method to be used for transfer. currently only "curl" is supported.
  }
  \item{quiet}{
  		boolean indicated whether function should print output to stdout
  }
  \item{mode}{
  		mode of the file
  }
  \item{cacheOK}{
  		boolean controlling cache
  }
  \item{contentType}{
  		content type
  }
	
}
\details{
	These functions bypass R's memory using c curlWriter and curlReader functions.
}
\value{
	synapseDownloadFile returns a list of the files that were downloaded
}

\author{
	Matt Furia
}

\seealso{
	\code{\link{curlPerform}}
}

