CREATE TABLE `ANNOTATED_EXAMPLE_TEST` (
	`ID` bigint(20) not null AUTO_INCREMENT,
	`NUMBER` bigint(20) not null,
	`BLOB_ONE` mediumblob,
	`CUSTOM` blob,
	`COMMENT` VARCHAR(256) CHARACTER SET latin1 COLLATE latin1_bin default null,
	`NAME` CHAR(16) default null,
	`ETAG` CHAR(36) default null,
	`MODIFIED_BY` VARCHAR(256) CHARACTER SET latin1 COLLATE latin1_bin not null,
	`MODIFIED_ON` datetime not null,
	PRIMARY KEY (`ID`)
)
