CREATE TABLE `DOI` (
	`ID`              bigint(20)                           NOT NULL,
	`ETAG`            char(36)                             NOT NULL,
	`DOI_STATUS`      ENUM('IN_PROCESS', 'READY', 'ERROR') NOT NULL,
	`OBJECT_ID`       bigint(20)                           NOT NULL,
	`OBJECT_TYPE`     ENUM('ENTITY', 'EVALUATION')         NOT NULL,
	`OBJECT_VERSION`  bigint(20)                           DEFAULT NULL,
	`CREATED_BY`      bigint(20)                           NOT NULL,
	`CREATED_ON`      TIMESTAMP                            NOT NULL,
	`UPDATED_ON`      TIMESTAMP                            NOT NULL,
	PRIMARY KEY (`ID`),
	UNIQUE INDEX (OBJECT_ID, OBJECT_TYPE, OBJECT_VERSION)
)
