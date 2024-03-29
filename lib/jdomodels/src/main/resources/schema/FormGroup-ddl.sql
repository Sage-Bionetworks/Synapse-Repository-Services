CREATE TABLE IF NOT EXISTS `FORM_GROUP` (
  `GROUP_ID` BIGINT NOT NULL,
  `NAME` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `CREATED_BY` BIGINT NOT NULL,
  `CREATED_ON` TIMESTAMP(3) NOT NULL,
  PRIMARY KEY (`GROUP_ID`),
  UNIQUE (`NAME`),
  CONSTRAINT `F_G_CREATOR_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `USER_GROUP` (`ID`) ON DELETE RESTRICT
)
