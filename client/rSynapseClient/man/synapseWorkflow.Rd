\name{synapseWorkflow}
\alias{finishWorkflowTask}
\alias{skipWorkflowTask}
\alias{getArgVal}
\alias{getInputLayerIdArg}
\alias{getInputDatasetIdArg}
\alias{getPasswordArg}
\alias{getUsernameArg}
\title{
	Functions for Handling Workflow Tasks
}
\description{
	These functions should be used when writing an R workflow that is to be executed by AWS simple workflow service.
}
\usage{
	finishWorkflowTask(outputLayerId)
	skipWorkflowTask(reason = "this script does not want to work on this task")
	getArgVal(argName)
	getInputDatasetIdArg()
	getInputLayerIdArg()
	getPasswordArg()
	getUsernameArg()
}
\arguments{
  \item{outputLayerId}{
		The ID of the output layer
	}
  \item{reason}{
  		String describing why the task was skipped
  	}
  \item{argName}{
  	Name of the workflow constant whose value should be returned
  }
}
\value{
	getArgVal returns the value of the workflow constant that was requested. finishWorkflowTask and skipWorkflowTask
	are called for their side-effects.
}
\author{
	Nicole Deflaux
}

\examples{
	## Not Run
	
	## end(Not Run)
}

