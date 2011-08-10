######################################################################################
## GENERAL FUNCTION THAT TAKES A TAB DELIMITED FILE AND PUTS INTO USEFUL R DATA FRAME
## USED BY ALL OTHER SUBSEQUENT FUNCTIONS
######################################################################################
.readTxtFile <- 
		function(file)
{
  tmp <- read.delim(file=file, as.is=TRUE, header = FALSE)
  names(tmp) <- tmp[1,]
  tmp <- tmp[-1, ]
  rownames(tmp) <- tmp[, 1]
  tmp[, -1]
}

#########################################
## EXPRESSION MATRIX TO assayData OBJECT
#########################################
.loadAffyExprDataFromFile <- 
		function(exprsFile)
{
  tmp <- .readTxtFile(exprsFile)
  assayDataNew(exprs = tmp)
}

#################
## FEATURE FILES
#################
.loadFeatureDataFromFile <- 
		function(featureFile)
{
  tmp <- .readTxtFile(featureFile)
  new("AnnotatedDataFrame", data = tmp)
}

## HAVE TO FIND A STANDARD WAY TO DETERMINE ROW NAMES FOR THE PHENOTYPE DATA
## SO IT CAN BE PUT TOGETHER IN AN eSet WITH EXPRESSION DATA
## THIS PROBABLY INCLUDES SOMETHING WITH THE INDIVIDUALS FILE, BUT NAMING CONVENTIONS GET IN THE WAY

#################################################
## PHENOTYPE MATRIX TO AnnotatedDataFrame OBJECT
#################################################
.loadPhenoDataFromFile <- 
		function(phenFile, descFile)
{
  tmpPhen <- as.data.frame(t(.readTxtFile(phenFile)))
  tmpDesc <- .readTxtFile(descFile)
  new("AnnotatedDataFrame", data = tmpPhen, varMetadata = tmpDesc)
}
