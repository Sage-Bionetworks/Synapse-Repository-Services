CREATE TABLE IF NOT EXISTS `ANNOTATED_EXAMPLE_TEST` (
	`ID` bigint(20) not null AUTO_INCREMENT,
	`NUMBER` bigint(20) not null,
	`NUMBER_OR_NULL` bigint(20),
	`BLOB_ONE` mediumblob,
	`CUSTOM` blob,
	`SERIALIZED` blob,
	`COMMENT` VARCHAR(256) CHARACTER SET latin1 COLLATE latin1_bin default null,
	`ENUM` ENUM( 'aaa', 'bbb', 'ccc'),
	`NAME` CHAR(16) default null,
	`ETAG` CHAR(36) default null,
	`MODIFIED_BY` VARCHAR(256) CHARACTER SET latin1 COLLATE latin1_bin not null,
	`MODIFIED_ON` bigint(20) not null,
	PRIMARY KEY (`ID`)
)
