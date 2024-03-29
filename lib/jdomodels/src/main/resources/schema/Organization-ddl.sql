CREATE TABLE IF NOT EXISTS `ORGANIZATION` (
  `ID` BIGINT NOT NULL,
  `NAME` varchar(250) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE (`NAME`),
  CONSTRAINT `ORG_CREATOR_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE RESTRICT
)
