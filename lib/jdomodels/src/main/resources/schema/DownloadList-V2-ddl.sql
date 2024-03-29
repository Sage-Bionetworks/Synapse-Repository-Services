CREATE TABLE IF NOT EXISTS `DOWNLOAD_LIST_V2` (
  `PRINCIPAL_ID` BIGINT NOT NULL,
  `UPDATED_ON` TIMESTAMP(3) NULL,
  `ETAG` char(36) NOT NULL,
  PRIMARY KEY (`PRINCIPAL_ID`),
  CONSTRAINT FOREIGN KEY (`PRINCIPAL_ID`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)
