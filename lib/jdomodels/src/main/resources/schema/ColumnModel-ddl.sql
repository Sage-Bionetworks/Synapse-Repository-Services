CREATE TABLE `COLUMN_MODEL` (
  `ID` bigint(20) NOT NULL,
  `NAME` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `HASH` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `BYTES` mediumblob,
  PRIMARY KEY (`ID`),
  KEY `CM_NAME_INDEX` (`NAME`),
  UNIQUE KEY `UNIQUE_CM_HASH` (`HASH`)
)