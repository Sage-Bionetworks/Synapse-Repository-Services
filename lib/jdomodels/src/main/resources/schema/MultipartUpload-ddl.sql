CREATE TABLE IF NOT EXISTS `MULTIPART_UPLOAD` (
  `ID` BIGINT NOT NULL,
  `REQUEST_HASH` char(100) NOT NULL,
  `ETAG` char(36) NOT NULL,
  `REQUEST_BLOB` BLOB NOT NULL,
  `STARTED_BY` BIGINT NOT NULL,
  `STARTED_ON` TIMESTAMP NOT NULL,
  `UPDATED_ON` TIMESTAMP NOT NULL,
  `FILE_HANDLE_ID` BIGINT DEFAULT NULL,
  `STATE` ENUM('UPLOADING','COMPLETED') NOT NULL,
  `UPLOAD_TOKEN` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL, 
  `UPLOAD_TYPE` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `BUCKET` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `FILE_KEY` varchar(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `NUMBER_OF_PARTS` int NOT NULL,
  `REQUEST_TYPE` ENUM('UPLOAD', 'COPY') NOT NULL,
  `PART_SIZE` BIGINT NOT NULL,
  `SOURCE_FILE_HANDLE_ID` BIGINT DEFAULT NULL,
  `SOURCE_FILE_ETAG` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY (`STARTED_BY`,`REQUEST_HASH`),
  INDEX `MUTI_UPDATED_ON_INDEX` (`UPDATED_ON`),
  CONSTRAINT `MUTI_FILE_HAN_BY_FK` FOREIGN KEY (`FILE_HANDLE_ID`) REFERENCES `FILES` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `MUTI_STARTED_BY_FK` FOREIGN KEY (`STARTED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `MUTI_SOURCE_FILE_HAN_ID_FK` FOREIGN KEY (`SOURCE_FILE_HANDLE_ID`) REFERENCES `FILES` (`ID`) ON DELETE CASCADE
)