CREATE TABLE `ACL` (
  `ID` bigint(20) NOT NULL,
  `OWNER_TYPE`  ENUM('NODE', 'EVALUATION', 'TEAM'),
  `ETAG` char(36) NOT NULL,
  `CREATED_ON` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`, `OWNER_TYPE`)
)
