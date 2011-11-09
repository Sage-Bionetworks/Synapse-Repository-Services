###############################################################################
## 
## Class definitions
## 
## Author: Matt Furia
###############################################################################


## SynapseAnnotation class definition
setClass(
		Class = "SynapseAnnotation",
		representation = representation(
				properties = "list",
				blobAnnotations = "list",
				dateAnnotations = "list",
				doubleAnnotations = "list",
				longAnnotations = "list",
				stringAnnotations = "list"),
		prototype = prototype(
				properties = emptyNamedList,
				blobAnnotations = emptyNamedList,
				dateAnnotations = emptyNamedList,
				doubleAnnotations = emptyNamedList,
				longAnnotations = emptyNamedList,
				stringAnnotations = emptyNamedList
		)
)

#####
## SynapseEntity Class definition
#####
setClass(
		Class = "SynapseEntity",
		representation = representation(
				synapseEntityKind = "character",
				properties = "list",
				annotations = "SynapseAnnotation",
				synapseWebUrl = "character"
		),
		prototype = prototype(
				properties = emptyNamedList,
				annotations = new(Class="SynapseAnnotation"),
				synapseWebUrl = ""
		)
)

setClass(
		Class = "Project",
		contains = "SynapseEntity",
		prototype = prototype(
				synapseEntityKind = "project"
		)
)


setClass(
		Class = "Dataset",
		contains = "SynapseEntity",
		prototype = prototype(
				synapseEntityKind = "dataset"
		)
)

setClass(
		Class = "Location",
		contains = "SynapseEntity",
		prototype = prototype(
				synapseEntityKind = "location"
		)
)

setClass(
		Class = "CachedLocation",
		contains = "Location",
		representation = representation(
				cacheDir = "character",
				files = "character"
		)
)

#####
### Layer Class definitions
#####
setClass(
		Class = "Layer",
		contains = "SynapseEntity",
		representation = representation(
				location = "CachedLocation",
				objects = "environment"
		),
		prototype = prototype(
				synapseEntityKind = "layer"
		)
)

setClass(
		Class = "ExpressionLayer",
		contains = "Layer",
		prototype = prototype(
				properties = list(type="E")
		)
)

setClass(
		Class = "AffyExpressionLayer",
		contains = "ExpressionLayer"
)

setClass(
		Class = "AgilentExpressionLayer",
		contains = "ExpressionLayer"
)

setClass(
		Class = "IlluminaExpressionLayer",
		contains = "ExpressionLayer"
)

setClass(
		Class = "GenotypeLayer",
		contains = "Layer",
		prototype = prototype(
				properties = list(type="G")
		)
)

setClass(
		Class = "PhenotypeLayer",
		contains = "Layer",
		prototype = prototype(
				properties = list(type="C")
		)
)

setClass(
		Class = "Media",
		contains = "Layer",
		prototype = prototype(
				synapseEntityKind = "layer"
		)
)

setClass(
		Class = "Eula",
		contains = "SynapseEntity",
		prototype = prototype(
				synapseEntityKind = "eula"
		)
)

setClass(
		Class = "Code",
		contains = "Layer",
		representation = representation(
				location = "CachedLocation",
				objects = "environment"
		),
		prototype = prototype(
				synapseEntityKind = "code"
		)
)

setClass(
	Class = "Analysis",
	contains = "SynapseEntity",
	prototype = prototype(
		synapseEntityKind = "analysis"
		)
	)

setClass(
	Class = "Step",
	contains = "SynapseEntity",
	prototype = prototype(
		synapseEntityKind = "step"
		)
	)

