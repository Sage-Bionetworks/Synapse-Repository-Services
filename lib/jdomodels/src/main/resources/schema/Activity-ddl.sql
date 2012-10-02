CREATE TABLE `ACTIVITY` (
  `ID` bigint(20) NOT NULL,
  `ETAG` char(36) NOT NULL,  
  `SERIALIZED_OBJECT` mediumblob,
  PRIMARY KEY (`ID`)
)