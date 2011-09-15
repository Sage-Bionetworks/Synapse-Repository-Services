# Annotation S4 class unit tests
# 
# Author: matt furia
###############################################################################


.setUp <- function(){
	synapseClient:::.setCache("datasetJSON", "{\"name\":\"Mouse Cultured Bone marrow derived Macrophage\",\"annotations\":\"/repo/v1/dataset/3773/annotations\",\"id\":\"3773\",\"version\":\"1.0.0\",\"description\":\"A powerful way to identify genes for complex traits it to combine genetic and genomic methods. Many trait quantitative trait loci (QTLs) for complex traits are sex specific, but the reason for this is not well understood. RNA was prepared from bone marrow derived macrophages of 93 female and 114 male F(2) mice derived from a strain intercross between apoE-deficient mice on the AKR and DBA/2 genetic backgrounds, and was subjected to transcriptome profiling using microarrays. A high density genome scan was performed using a mouse SNP chip, and expression QTLs (eQTLs) were located for expressed transcripts. Using suggestive and significant LOD score cutoffs of 3.0 and 4.3, respectively, thousands of eQTLs in the female and male cohorts were identified. At the suggestive LOD threshold the majority of the eQTLs were trans eQTLs, mapping unlinked to the position of the gene. Cis eQTLs, which mapped to the location of the gene, had much higher LOD scores than trans eQTLs, indicating their more direct effect on gene expression. The majority of cis eQTLs were common to both males and females, but only approximately 1% of the trans eQTLs were shared by both sexes. At the significant LOD threshold, the majority of eQTLs were cis eQTLs, which were mostly sex-shared, while the trans eQTLs were overwhelmingly sex-specific. Pooling the male and female data, 31% of expressed transcripts were expressed at different levels in males vs. females after correction for multiple testing.These studies demonstrate a large sex effect on gene expression and trans regulation, under conditions where male and female derived cells were cultured ex vivo and thus without the influence of endogenous sex steroids. These data suggest that eQTL data from male and female mice should be analyzed separately, as many effects, such as trans regulation are sex specific. \",\"status\":\"Future\",\"creationDate\":1312573633063,\"parentId\":\"3731\",\"etag\":\"3\",\"eulaId\":\"3732\",\"uri\":\"/repo/v1/dataset/3773\",\"creator\":\"Smith\",\"accessControlList\":\"/repo/v1/dataset/3773/acl\",\"locations\":\"/repo/v1/dataset/3773/location\",\"releaseDate\":1292025600000,\"hasExpressionData\":true,\"hasGeneticData\":true,\"hasClinicalData\":false,\"layers\":\"/repo/v1/dataset/3773/layer\"}")
	synapseClient:::.setCache("datasetAnnotationJSON", "{\"id\":\"3773\",\"creationDate\":1312573633063,\"etag\":\"3\",\"stringAnnotations\":{\"status\":[\"Future\"],\"eulaId\":[\"3732\"],\"Posting_Restriction\":[\"unspecified\"],\"citation\":[\"Sex specific gene regulation and expression QTLs in mouse macrophages from a strain intercross. Bhasin JM, Chakrabarti E, Peng DQ, Kulkarni A, Chen X, Smith JD. PLoS One. 2008 Jan 16;3(1):e1435. \"],\"Disease\":[\"Healthy\"],\"QC_statistician\":[\"\"],\"Species\":[\"Mouse\"],\"version\":[\"1.0.0\"],\"Internal_Name\":[\"Cleveland_Macrophages\"],\"Tissue_Tumor\":[\"Marcrophage\"],\"uri\":[\"/repo/v1/dataset/3773\"],\"Type\":[\"Other\"],\"Institution\":[\"Cleaveland Clinic\"],\"curator\":[\"\"]},\"longAnnotations\":{\"number_of_downloads\":[66],\"number_of_followers\":[29],\"Number_of_Samples\":[207],\"pubmed_id\":[15121029]},\"dateAnnotations\":{\"releaseDate\":[1292025600000],\"last_modified_date\":[1368144000000]},\"doubleAnnotations\":{},\"blobAnnotations\":{},\"uri\":\"/repo/v1/dataset/3773/annotations\"}")
}

.tearDown <- function(){
	synapseClient:::.deleteCache("datasetJSON")
	synapseClient:::.deleteCache("datasetAnnotationJSON")
}

unitTestConstructor <- 
		function()
{
	synapseClient:::SynapseAnnotation(entity = as.list(RJSONIO::fromJSON(synapseClient:::.getCache("datasetAnnotationJSON"))))
}

unitTestStringAnnotation <- 
		function()
{
	ann <- new(Class = "SynapseAnnotation")
	annotValue(ann, "thisName") <- "thisIsAString"
	checkEquals(ann@stringAnnotations$thisName, "thisIsAString")
}

unitTestLongAnnotation <-
		function()
{
	ann <- new(Class = "SynapseAnnotation")
	annotValue(ann, "thisName") <- 1L
	## all annotations are stored as strings
	checkEquals(ann@longAnnotations$thisName, "1")
	
	## getter should return them as the correct type
	## checkEquals(annotValue(ann,"thisName"), 1L)
}

unitTestDoubleAnnotation <-
		function()
{
	ann <- new(Class = "SynapseAnnotation")
	annotValue(ann, "thisName") <- 1.0
	## all annotations are stored as strings
	checkEquals(ann@doubleAnnotations$thisName, "1")
	
	## getter should return them as the correct type
	## checkEquals(annotValue(ann,"thisName"), 1.0)
}

# TODO write these tests
#unitTestDateAnnotation <-
#		function()
#{
#	## test setting various non-date types with a name containing "date"
#}
#
#unitTestDateProperty <-
#		function()
#{
#	## date annotations should be stored internally as longs, but printed in a human readable form
#	
#
#}

unitTestAnnotationWithMultipleValues <-
		function()
{
	val <- 1L:10L
	ann <- new("SynapseAnnotation")
	annotValue(ann, "multValues") <- val
	## uncomment this once the getter returns typed values
	##checkTrue(all(annotValue(ann,"multValues") == val))
	checkTrue(all(annotValue(ann,"multValues") == as.character(val)))
	checkTrue(all(ann@longAnnotations$multValues == as.character(val)))
}

unitTestAnnotationWithMultipleTypes <-
		function()
{
	## this should be an exception condition. test in integration tests that an exception is thrown when
	## an annotation has more than one type
	ann <- new("SynapseAnnotation")
	ann@stringAnnotations$aDuplicateKey <- "aValue"
	ann@longAnnotations$aDuplicateKey <- "1"

	## this should warn and return a single value
	checkEquals(annotValue(ann,"aDuplicateKey"), "1")
}

unitTestPropertyNamedProperties <-
		function()
{
	kValue <- "thisIsAValue"
	kNewValue <- "thisIsANewValue"
	entity <- as.list(RJSONIO::fromJSON(synapseClient:::.getCache("datasetAnnotationJSON")))
	entity$properties <- kValue

	s4Entity <- new("SynapseAnnotation")
	s4Entity <- synapseClient:::.populateSlotsFromEntity(s4Entity, entity)
	checkEquals(s4Entity@properties$properties, kValue)
	
	propertyValue(s4Entity, "properties") <- kNewValue
	checkEquals(propertyValue(s4Entity, "properties"), kNewValue)
}

unitTestAnnotationNamedProperties <-
		function()
{
	kValue <- "thisIsAValue"
	kNewValue <- "thisIsANewValue"
	entity <- as.list(RJSONIO::fromJSON(synapseClient:::.getCache("datasetAnnotationJSON")))
	entity$properties <- kValue
	
	s4Entity <- new("SynapseAnnotation")
	s4Entity <- synapseClient:::.populateSlotsFromEntity(s4Entity, entity)
	checkEquals(s4Entity@properties$properties, kValue)
}

uniTestProperties <-
		function()
{
	## annotations object
	ann <- new(Class="SynapseAnnotation")
	
	## date valued property
	dd <- Sys.Date()
	propertyValue(ann,"date") <- dd
	
	## all other property types
	propertyValue(ann,"string") <- "string"
	propertyValue(ann,"long") <- 1L
	propertyValue(ann,"double") <- 2.0
	
	## TODO: remove the type coersion one getters return properly typed values
	checkEquals(propertyValue(ann,"date"), as.Date(dd))
	checkEquals(propertyValue(ann,"string"), "string")
	checkEquals(as.integer(propertyValue(ann,"long")), 1L)
	checkEquals(as.double(propertyValue(ann,"double")), 2.0)
	
}

# todo write these tests
#unitTestDoubleValueToInteger <-
#		function()
#{
#	## throw error if trying to set an integer annotation with a double value
#	## this will only happen if the annotation name already exists and holds an int
#}
#
#unitTestDateAnnotationWithNonDateType <-
#		function()
#{
#	## similar to previous test, but with dates
#}

unitTestPropertyAnnotationWithSameName <-
		function()
{
	ann <- new(Class= "SynapseAnnotation")
	## create annotation and property value with same name
	propertyValue(ann, "dupName") <- "property"
	annotValue(ann, "dupName") <- "annotation"

	## make sure they wind up in the right place
	checkEquals(propertyValue(ann, "dupName"), "property")
	checkEquals(annotValue(ann, "dupName"),"annotation")
	
	ann <- new(Class= "SynapseAnnotation")
	## create annotation and property value with same name
	annotValue(ann, "dupName") <- "annotation"
	propertyValue(ann, "dupName") <- "property"
	
	## make sure they wind up in the right place
	checkEquals(propertyValue(ann, "dupName"), "property")
	checkEquals(annotValue(ann, "dupName"),"annotation")
}

## TODO: write a unit test for blobs