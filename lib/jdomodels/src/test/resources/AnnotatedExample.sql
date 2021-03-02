CREATE TABLE IF NOT EXISTS `ANNOTATED_EXAMPLE_TEST` (
	`ID` BIGINT not null AUTO_INCREMENT,
	`NUMBER` BIGINT not null,
	`NUMBER_OR_NULL` BIGINT,
	`BLOB_ONE` mediumblob,
	`CUSTOM` blob,
	`SERIALIZED` blob,
	`COMMENT` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci default null,
	`ENUM` ENUM( 'aaa', 'bbb', 'ccc'),
	`NAME` CHAR(16) default null,
	`ETAG` CHAR(36) default null,
	`MODIFIED_BY` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci not null,
	`MODIFIED_ON` BIGINT not null,
	`PARENT_ID` BIGINT,
	`FILE_HANDLE` BIGINT,
	PRIMARY KEY (`ID`),
	constraint PARENT_FK foreign key (`PARENT_ID`) references `ANNOTATED_EXAMPLE_TEST` (`ID`) on delete cascade
)
