CREATE TABLE IF NOT EXISTS `DOI` (
	`ID`              BIGINT                           NOT NULL,
	`ETAG`            char(36)                             NOT NULL,
	`DOI_STATUS`      ENUM('IN_PROCESS', 'CREATED', 'READY', 'ERROR') NOT NULL,
	`OBJECT_ID`       BIGINT                           NOT NULL,
	`OBJECT_TYPE`     ENUM('ENTITY', 'EVALUATION')         NOT NULL,
	`OBJECT_VERSION`  BIGINT                           NOT NULL,
	`CREATED_BY`      BIGINT                           NOT NULL,
	`CREATED_ON`      TIMESTAMP                            NOT NULL,
  `UPDATED_BY`      BIGINT                           NOT NULL,
	`UPDATED_ON`      TIMESTAMP                            NOT NULL,
	PRIMARY KEY (`ID`),
	UNIQUE INDEX (OBJECT_ID, OBJECT_TYPE, OBJECT_VERSION)
)
