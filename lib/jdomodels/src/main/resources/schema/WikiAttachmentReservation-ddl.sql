CREATE TABLE IF NOT EXISTS `V2_WIKI_ATTACHMENT_RESERVATION` (
  `WIKI_ID` bigint NOT NULL,
  `FILE_HANDLE_ID` bigint NOT NULL,
  `TIME_STAMP` timestamp NOT NULL,
  PRIMARY KEY (`WIKI_ID`,`FILE_HANDLE_ID`),
  UNIQUE KEY `V2_WIKI_UNIQUE_FILE_HANDLE_ID` (`WIKI_ID`,`FILE_HANDLE_ID`),
  KEY `V2_WIKI_FILE_HAND_RESERVE_FK` (`FILE_HANDLE_ID`),
  CONSTRAINT `V2_WIKI_ATTACH_RESERVE_FK` FOREIGN KEY (`WIKI_ID`) REFERENCES `V2_WIKI_PAGE` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `V2_WIKI_FILE_HAND_RESERVE_FK` FOREIGN KEY (`FILE_HANDLE_ID`) REFERENCES `FILES` (`ID`) ON DELETE RESTRICT
)