CREATE FUNCTION `aggregateCountRound`(countValue BIGINT, roundTo BIGINT) RETURNS BIGINT
	DETERMINISTIC NO SQL
	RETURN (countValue DIV roundTo) * roundTo
