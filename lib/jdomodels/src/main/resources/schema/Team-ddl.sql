CREATE TABLE IF NOT EXISTS `TEAM` (
  `ID` BIGINT NOT NULL,
  `ETAG` char(36) NOT NULL,
  `ICON` BIGINT DEFAULT NULL,
  `PROPERTIES` mediumblob,
  `STATE` ENUM('OPEN', 'CLOSED', 'PUBLIC') NOT NULL,
  PRIMARY KEY (`ID`),
  CONSTRAINT `TEAM_PRINCIPAL_FK` FOREIGN KEY (`ID`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `TEAM_ICON_FK` FOREIGN KEY (`ICON`) REFERENCES `FILES` (`ID`) ON DELETE RESTRICT
)
