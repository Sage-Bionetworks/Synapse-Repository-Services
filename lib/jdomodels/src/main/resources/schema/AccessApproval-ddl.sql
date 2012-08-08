CREATE TABLE `ACCESS_APPROVAL` (
  `ID` bigint(20) NOT NULL,
  `ETAG` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) NOT NULL,
  `CREATED_ON` bigint(20) NOT NULL,
  `MODIFIED_BY` bigint(20) NOT NULL,
  `MODIFIED_ON` bigint(20) NOT NULL,
  `REQUIREMENT_ID` bigint(20) NOT NULL,
  `ACCESSOR_ID`  bigint(20) NOT NULL,
  `ENTITY_TYPE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `SERIALIZED_ENTITY` mediumblob,
  PRIMARY KEY (`ID`),
  CONSTRAINT `ACCESS_APPROVAL_REQUIREMENT_ID_FK` FOREIGN KEY (`REQUIREMENT_ID`) REFERENCES `ACCESS_REQUIREMENT` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `ACCESS_APPROVAL_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `JDOUSERGROUP` (`ID`),
  CONSTRAINT `ACCESS_APPROVAL_MODIFIED_BY_FK` FOREIGN KEY (`MODIFIED_BY`) REFERENCES `JDOUSERGROUP` (`ID`),
  CONSTRAINT `ACCESS_APPROVAL_ACCESSOR_ID_FK` FOREIGN KEY (`ACCESSOR_ID`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE
)