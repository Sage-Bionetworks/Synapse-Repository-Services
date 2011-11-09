unitTestNow <- function() {
	checkTrue(grepl("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.*", synapseClient:::.now()))
	checkTrue(grepl("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\dZ", synapseClient:::.nowAsString()))
}