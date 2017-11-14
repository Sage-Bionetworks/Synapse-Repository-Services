CREATE TABLE IF NOT EXISTS JDOSUBMISSION (
    ID bigint(20) NOT NULL,
    NAME varchar(256) CHARACTER SET latin1 COLLATE latin1_bin,
    EVALUATION_ID bigint(20) NOT NULL,
    USER_ID bigint(20) NOT NULL,
    SUBMITTER_ALIAS varchar(256) CHARACTER SET latin1 COLLATE latin1_bin,
    ENTITY_ID bigint(20) NOT NULL,
    ENTITY_BUNDLE mediumblob,
    ENTITY_VERSION bigint(20) NOT NULL,
    CREATED_ON bigint(20) NOT NULL,
    TEAM_ID bigint(20),
    DOCKER_REPO_NAME varchar(400) CHARACTER SET latin1 COLLATE latin1_bin,
    DOCKER_DIGEST varchar(200) CHARACTER SET latin1 COLLATE latin1_bin,
    PRIMARY KEY (ID),
    KEY (DOCKER_REPO_NAME),
    FOREIGN KEY (EVALUATION_ID) REFERENCES JDOEVALUATION (ID) ON DELETE CASCADE,
   	FOREIGN KEY (USER_ID) REFERENCES JDOUSERGROUP (ID)
);
