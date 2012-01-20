#!/bin/sh
# Launch the migration utility UI.
java -cp target/tool-migration-utility-0.10-SNAPSHOT-jar-with-dependencies.jar org.sagebionetworks.tool.migration.gui.MigrationConsoleUI "$@"