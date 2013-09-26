CREATE TABLE `MEMBERSHIP_INVITATION_SUBMISSION` (
  `ID` bigint(20) NOT NULL,
  `ETAG` char(36) NOT NULL,
  `TEAM_ID` bigint(20) NOT NULL,
  `EXPIRES_ON` bigint(20),
  `PROPERTIES` mediumblob,
  PRIMARY KEY (`ID`),
  CONSTRAINT `MEMBERSHIP_INVITATION_TEAM_FK` FOREIGN KEY (`TEAM_ID`) REFERENCES `TEAM` (`ID`)
)