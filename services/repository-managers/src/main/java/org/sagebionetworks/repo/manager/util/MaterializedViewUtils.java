package org.sagebionetworks.repo.manager.util;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.TableNameCorrelation;
import org.sagebionetworks.util.ValidateArgument;

import java.util.HashSet;
import java.util.Set;

public class MaterializedViewUtils {

    public static QueryExpression getQuerySpecification(String definingSql) {
        ValidateArgument.requiredNotBlank(definingSql, "The definingSQL of the materialized view");
        try {
            return new TableQueryParser(definingSql).queryExpression();
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static Set<IdAndVersion> getSourceTableIds(QueryExpression query) {
        Set<IdAndVersion> sourceTableIds = new HashSet<>();

        for (TableNameCorrelation table : query.createIterable(TableNameCorrelation.class)) {
            sourceTableIds.add(IdAndVersion.parse(table.getTableName().toSql()));
        }

        return sourceTableIds;
    }

    public static Set<IdAndVersion> getDependencies(String definingSql) {
        return getSourceTableIds(getQuerySpecification(definingSql));
    }
}
