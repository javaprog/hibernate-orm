package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

public class IlikeAuditExpression implements AuditCriterion {

	private PropertyNameGetter propertyNameGetter;
	private String value;

	public IlikeAuditExpression(PropertyNameGetter propertyNameGetter, String value) {
		this.propertyNameGetter = propertyNameGetter;
		this.value = value;
	}

	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader, String entityName,
			QueryBuilder qb, Parameters parameters) {

		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);
		CriteriaTools.checkPropertyNotARelation( enversService, entityName, propertyName );

		parameters.addWhereWithFunction( propertyName, " lower ", " like ", value.toLowerCase() );
	}

}
