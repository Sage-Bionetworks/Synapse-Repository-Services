CREATE TABLE IF NOT EXISTS `DOI` (
	`ID`              BIGINT                           NOT NULL,
	`ETAG`            char(36)                             NOT NULL,
	`DOI_STATUS`      ENUM('IN_PROCESS', 'CREATED', 'READY', 'ERROR') NOT NULL,
	`PORTAL_ID`		  BIGINT NOT NULL,
	`OBJECT_ID`       VARCHAR(256)                           NOT NULL,
	`OBJECT_TYPE`     ENUM('ENTITY', 'PORTAL_RESOURCE')         NOT NULL,
	`OBJECT_VERSION`  BIGINT                           NOT NULL,
	`CREATED_BY`      BIGINT                           NOT NULL,
	`CREATED_ON`      TIMESTAMP                            NOT NULL,
  	`UPDATED_BY`      BIGINT                           NOT NULL,
	`UPDATED_ON`      TIMESTAMP                            NOT NULL,
	PRIMARY KEY (`ID`),
	UNIQUE INDEX (PORTAL_ID, OBJECT_ID, OBJECT_TYPE, OBJECT_VERSION),
	CONSTRAINT `DOI_PORTAL_FK` FOREIGN KEY (`PORTAL_ID`) REFERENCES `PORTAL` (`ID`) ON DELETE RESTRICT
)
