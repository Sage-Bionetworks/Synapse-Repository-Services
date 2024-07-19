CREATE TABLE IF NOT EXISTS `VERIFICATION_STATE` (
  `ID` bigint NOT NULL,
  `VERIFICATION_ID` bigint NOT NULL,
  `CREATED_BY` bigint NOT NULL,
  `CREATED_ON` bigint NOT NULL,
  `STATE` enum('SUBMITTED','APPROVED','REJECTED','SUSPENDED') NOT NULL,
  `REASON` blob,
  `NOTES` blob,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNIQUE_VS_VERFCTN_AND_CREATED_ON` (`VERIFICATION_ID`,`CREATED_ON`),
  KEY `VERI_STATE_USER_ID` (`CREATED_BY`),
  CONSTRAINT `VERI_STATE_USER_ID` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE RESTRICT,
  CONSTRAINT `VERI_STATE_VERI_ID` FOREIGN KEY (`VERIFICATION_ID`) REFERENCES `VERIFICATION_SUBMISSION` (`ID`) ON DELETE CASCADE
)