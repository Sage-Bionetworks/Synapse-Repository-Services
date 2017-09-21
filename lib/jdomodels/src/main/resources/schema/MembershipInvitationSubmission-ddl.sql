CREATE TABLE IF NOT EXISTS `MEMBERSHIP_INVITATION_SUBMISSION` (
  `ID` bigint(20) NOT NULL,
  `ETAG` char(36),
  `TEAM_ID` bigint(20) NOT NULL,
  `INVITEE_ID` bigint(20),
  `INVITEE_EMAIL` varchar(320),
  `CREATED_ON` bigint(20) NOT NULL,
  `EXPIRES_ON` bigint(20),
  `PROPERTIES` mediumblob,
  PRIMARY KEY (`ID`),
  CONSTRAINT `MEMBERSHIP_INVITATION_INVITEE_FK` FOREIGN KEY (`INVITEE_ID`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `MEMBERSHIP_INVITATION_TEAM_FK` FOREIGN KEY (`TEAM_ID`) REFERENCES `TEAM` (`ID`) ON DELETE CASCADE
)
