CREATE TABLE IF NOT EXISTS `DOWNLOAD_ORDER` (
  `ORDER_ID` BIGINT NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  `FILE_NAME` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TOTAL_SIZE_BYTES` BIGINT NOT NULL,
  `TOTAL_NUM_FILES` BIGINT NOT NULL,
  `FILES_BLOB` blob,
  PRIMARY KEY (`ORDER_ID`),
  INDEX (`CREATED_BY`),
  CONSTRAINT `CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)
