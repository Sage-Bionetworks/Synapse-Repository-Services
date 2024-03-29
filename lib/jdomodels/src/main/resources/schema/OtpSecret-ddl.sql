CREATE TABLE IF NOT EXISTS `OTP_SECRET` (
  `ID` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `PRINCIPAL_ID` BIGINT NOT NULL,
  `CREATED_ON` TIMESTAMP(3) NOT NULL,
  `SECRET` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `ACTIVE` BOOLEAN NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `SECRET_ENABLED_INDEX` (`PRINCIPAL_ID`, `ACTIVE`),
  CONSTRAINT `TOTP_SECRET_PRINCIPAL_ID_FK` FOREIGN KEY (`PRINCIPAL_ID`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)