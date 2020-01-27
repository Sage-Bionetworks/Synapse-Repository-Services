CREATE TABLE IF NOT EXISTS `OAUTH_SECTOR_IDENTIFIER` (
  `ID` BIGINT NOT NULL,
  `URI` varchar(256) NOT NULL UNIQUE,
  `SECRET` varchar(256) NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` BIGINT NOT NULL,
  PRIMARY KEY (`ID`),
  CONSTRAINT `OAUTH_SECTOR_IDENTIFIER_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `JDOUSERGROUP` (`ID`)
)
