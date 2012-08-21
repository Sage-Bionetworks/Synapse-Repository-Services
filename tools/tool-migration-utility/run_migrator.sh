#! /bin/bash
# usage: ./run_migrator <MIGRATER JAR> <MIGRATER PROPERTY FILE>
java -Xms256m -Xmx1g -cp $1 org.sagebionetworks.tool.migration.gui.MigrationConsoleUI $2

