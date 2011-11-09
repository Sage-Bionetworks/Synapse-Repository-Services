unitTestNow <- function() {
	checkTrue(grepl("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.*", .now()))
	checkTrue(grepl("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\dZ", .nowAsString()))
}