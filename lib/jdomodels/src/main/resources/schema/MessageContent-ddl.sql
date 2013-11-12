CREATE TABLE `MESSAGE_CONTENT` (
  `ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) NOT NULL,
  `FILE_HANDLE_ID` bigint(20) NOT NULL,
  `IN_REPLY_TO` bigint(20) DEFAULT NULL, 
  `CREATED_ON` bigint(20) NOT NULL,
  `ETAG` char(36) NOT NULL, 
  PRIMARY KEY (`ID`),
  CONSTRAINT `MESSAGE_CONTENT_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `MESSAGE_CONTENT_FILE_HANDLE_ID_FK` FOREIGN KEY (`FILE_HANDLE_ID`) REFERENCES `FILES` (`ID`) ON DELETE CASCADE, 
  CONSTRAINT `MESSAGE_CONTENT_IN_REPLY_TO_ID_FK` FOREIGN KEY (`IN_REPLY_TO`) REFERENCES `MESSAGE_CONTENT` (`ID`) ON DELETE CASCADE
)