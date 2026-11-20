CREATE FUNCTION `aggregateCountRoundedNoise`(countValue BIGINT, binSize BIGINT, scale DOUBLE, threshold BIGINT,
		label TEXT) RETURNS BIGINT
	DETERMINISTIC NO SQL
BEGIN
	DECLARE lowerBound BIGINT;
	DECLARE u DOUBLE;
	DECLARE centered DOUBLE;
	IF countValue <= 0 THEN
		RETURN countValue;
	END IF;
	SET lowerBound = (countValue DIV binSize) * binSize;
	SET u = (CONV(LEFT(SHA2(CONCAT_WS(CHAR(31 USING utf8mb4), label, lowerBound), 256), 13), 16, 10) + 0.5)
		/ 4503599627370496;
	SET centered = lowerBound + (binSize - 1) / 2 - scale * SIGN(u - 0.5) * LN(1 - 2 * ABS(u - 0.5));
	RETURN LEAST(lowerBound + binSize - 1, GREATEST(lowerBound, threshold, FLOOR(centered + 0.5)));
END
