CREATE FUNCTION `aggregateCountBinnedNoise`(countValue BIGINT, binSize BIGINT, scale DOUBLE, threshold BIGINT)
		RETURNS BIGINT
	NOT DETERMINISTIC NO SQL
BEGIN
	DECLARE lowerBound BIGINT;
	DECLARE u DOUBLE DEFAULT RAND() - 0.5;
	DECLARE centered DOUBLE;
	IF countValue <= 0 THEN
		RETURN countValue;
	END IF;
	IF ABS(u) >= 0.5 THEN
		SET u = 0;
	END IF;
	SET lowerBound = (countValue DIV binSize) * binSize;
	SET centered = lowerBound + (binSize - 1) / 2 - scale * SIGN(u) * LN(1 - 2 * ABS(u));
	RETURN LEAST(lowerBound + binSize - 1, GREATEST(lowerBound, threshold, FLOOR(centered + 0.5)));
END
