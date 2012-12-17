CREATE TABLE `CHANGES` (
  `CHANGE_NUM` bigint(20) NOT NULL AUTO_INCREMENT,
  `TIME_STAMP` TIMESTAMP NOT NULL,
  `OBJECT_ID` bigint(20) NOT NULL,
  `PARENT_ID` bigint(20) DEFAULT NULL,
  `OBJECT_TYPE` ENUM('ENTITY', 'PRINCIPAL', 'ACTIVITY', 'COMPETITION', 'SUBMISSION') NOT NULL,
  `OBJECT_ETAG` char(36) DEFAULT NULL,
  `CHANGE_TYPE` ENUM('CREATE','UPDATE','DELETE'),
  PRIMARY KEY (`CHANGE_NUM`),
  UNIQUE KEY `CHANGE_UNIQUE_OBJECT_ID` (`OBJECT_ID`)
)