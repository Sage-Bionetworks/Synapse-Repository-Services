CREATE TABLE IF NOT EXISTS `USER_TWO_FA_STATUS` (
  `PRINCIPAL_ID` BIGINT NOT NULL,
  `ENABLED` BOOLEAN NOT NULL,
  PRIMARY KEY (`PRINCIPAL_ID`),
  CONSTRAINT `USER_TWO_FA_STATUS_PRINCIPAL_ID_FK` FOREIGN KEY (`PRINCIPAL_ID`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)