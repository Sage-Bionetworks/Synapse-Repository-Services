CREATE TABLE `CREDENTIAL` (
  `PRINCIPAL_ID` bigint(20) NOT NULL,
  `VALIDATED_ON` datetime DEFAULT NULL,
  `SESSION_TOKEN` varchar(100) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
  `PASS_HASH` varchar(70) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,
  `SECRET_KEY` varchar(100) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  UNIQUE KEY `UNIQUE_SESSION_TOKEN` (`SESSION_TOKEN`), 
  CONSTRAINT `PRINCIPAL_ID_FK` FOREIGN KEY (`PRINCIPAL_ID`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE
)