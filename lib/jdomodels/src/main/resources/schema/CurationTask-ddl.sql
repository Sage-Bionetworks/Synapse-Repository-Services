CREATE TABLE IF NOT EXISTS `CURATION_TASK`
(
    `ID`              BIGINT                                                        NOT NULL,
    `DATA_TYPE`       VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `PROJECT_ID`      BIGINT                                                        NOT NULL,
    `INSTRUCTIONS`    MEDIUMTEXT DEFAULT NULL,
    `ETAG`            CHAR(36)                                                      NOT NULL,
    `CREATED_BY`      BIGINT                                                        NOT NULL,
    `CREATED_ON`      TIMESTAMP(3)                                                  NOT NULL,
    `MODIFIED_BY`     BIGINT                                                        NOT NULL,
    `MODIFIED_ON`     TIMESTAMP(3)                                                  NOT NULL,
    `TASK_PROPERTIES` JSON                                                          NOT NULL,
    `ASSIGNEE`		  BIGINT DEFAULT NULL,
    PRIMARY KEY (`ID`),
    CONSTRAINT `CURATION_TASK_DATA_TYPE_PROJECT_ID` UNIQUE (`DATA_TYPE`, `PROJECT_ID`),
    CONSTRAINT `CURATION_TASK_PROJECT_FK` FOREIGN KEY (`PROJECT_ID`) REFERENCES `NODE` (`ID`) ON DELETE CASCADE,
    CONSTRAINT `CURATION_TASK_ASSIGNEE_FK` FOREIGN KEY (`ASSIGNEE`) REFERENCES `USER_GROUP` (`ID`) ON DELETE CASCADE
)
