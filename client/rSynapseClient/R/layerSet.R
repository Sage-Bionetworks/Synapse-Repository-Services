## layerSet class description and methods
## Holds set of layers for a particular data packet
## primary key is dataset id
## will hold set of dataLayer instances
## 
## methods:
##	* getQCd layers
##	* getCurated layers
##	* getRaw layers
##	* getExpression layers
##		- ...
##	* show: clean print out of layers contained in set. possibly also data packet name and description

setClass(
		Class = "LayerSet",
		representation(
				layers = "list"
		),
		prototype = prototype(
				layers = NULL
		)
)

