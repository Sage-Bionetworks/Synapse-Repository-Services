CREATE TABLE IF NOT EXISTS `MESSAGE_STATUS` (
  `MESSAGE_ID` BIGINT NOT NULL,
  `RECIPIENT_ID` BIGINT NOT NULL,
  `STATUS` ENUM('READ', 'UNREAD', 'ARCHIVED') NOT NULL, 
  PRIMARY KEY (`MESSAGE_ID`, `RECIPIENT_ID`),
  CONSTRAINT `MESSAGE_STATUS_ID_FK` FOREIGN KEY (`MESSAGE_ID`) REFERENCES `MESSAGE_CONTENT` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `MESSAGE_STATUS_RECIPIENT_ID_FK` FOREIGN KEY (`RECIPIENT_ID`) REFERENCES `USER_GROUP` (`ID`) ON DELETE RESTRICT
)
