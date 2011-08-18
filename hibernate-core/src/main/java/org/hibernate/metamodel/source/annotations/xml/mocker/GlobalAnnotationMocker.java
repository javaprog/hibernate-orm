/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.hibernate.metamodel.source.annotation.jaxb.XMLColumnResult;
import org.hibernate.metamodel.source.annotation.jaxb.XMLEntityResult;
import org.hibernate.metamodel.source.annotation.jaxb.XMLFieldResult;
import org.hibernate.metamodel.source.annotation.jaxb.XMLNamedNativeQuery;
import org.hibernate.metamodel.source.annotation.jaxb.XMLNamedQuery;
import org.hibernate.metamodel.source.annotation.jaxb.XMLQueryHint;
import org.hibernate.metamodel.source.annotation.jaxb.XMLSequenceGenerator;
import org.hibernate.metamodel.source.annotation.jaxb.XMLSqlResultSetMapping;
import org.hibernate.metamodel.source.annotation.jaxb.XMLTableGenerator;

/**
 * @author Strong Liu
 */
class GlobalAnnotationMocker extends AbstractMocker {
	private GlobalAnnotations globalAnnotations;

	GlobalAnnotationMocker(IndexBuilder indexBuilder, GlobalAnnotations globalAnnotations) {
		super( indexBuilder );
		this.globalAnnotations = globalAnnotations;
	}


	void process() {
		if ( !globalAnnotations.getTableGeneratorMap().isEmpty() ) {
			for ( XMLTableGenerator generator : globalAnnotations.getTableGeneratorMap().values() ) {
				parserTableGenerator( generator );
			}
		}
		if ( !globalAnnotations.getSequenceGeneratorMap().isEmpty() ) {
			for ( XMLSequenceGenerator generator : globalAnnotations.getSequenceGeneratorMap().values() ) {
				parserSequenceGenerator( generator );
			}
		}
		if ( !globalAnnotations.getNamedQueryMap().isEmpty() ) {
			Collection<XMLNamedQuery> namedQueries = globalAnnotations.getNamedQueryMap().values();
			if ( namedQueries.size() > 1 ) {
				parserNamedQueries( namedQueries );
			}
			else {
				parserNamedQuery( namedQueries.iterator().next() );
			}
		}
		if ( !globalAnnotations.getNamedNativeQueryMap().isEmpty() ) {
			Collection<XMLNamedNativeQuery> namedQueries = globalAnnotations.getNamedNativeQueryMap().values();
			if ( namedQueries.size() > 1 ) {
				parserNamedNativeQueries( namedQueries );
			}
			else {
				parserNamedNativeQuery( namedQueries.iterator().next() );
			}
		}
		if ( !globalAnnotations.getSqlResultSetMappingMap().isEmpty() ) {
			parserSqlResultSetMappings( globalAnnotations.getSqlResultSetMappingMap().values() );
		}
		indexBuilder.finishGlobalConfigurationMocking( globalAnnotations );
	}

	private AnnotationInstance parserSqlResultSetMappings(Collection<XMLSqlResultSetMapping> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( Iterator<XMLSqlResultSetMapping> iterator = namedQueries.iterator(); iterator.hasNext(); ) {
			AnnotationInstance annotationInstance = parserSqlResultSetMapping( iterator.next() );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				SQL_RESULT_SET_MAPPINGS, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "values", values ) }

		);
	}


	//@SqlResultSetMapping
	private AnnotationInstance parserSqlResultSetMapping(XMLSqlResultSetMapping mapping) {

		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", mapping.getName(), annotationValueList );
		nestedEntityResultList( "entities", mapping.getEntityResult(), annotationValueList );
		nestedColumnResultList( "columns", mapping.getColumnResult(), annotationValueList );
		return
				create(
						SQL_RESULT_SET_MAPPING, null, annotationValueList

				);
	}


	//@EntityResult
	private AnnotationInstance parserEntityResult(XMLEntityResult result) {

		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue(
				"discriminatorColumn", result.getDiscriminatorColumn(), annotationValueList
		);
		nestedFieldResultList( "fields", result.getFieldResult(), annotationValueList );
		MockHelper.classValue(
				"entityClass", result.getEntityClass(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		return
				create(
						ENTITY_RESULT, null, annotationValueList

				);
	}

	private void nestedEntityResultList(String name, List<XMLEntityResult> entityResults, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( entityResults ) ) {
			AnnotationValue[] values = new AnnotationValue[entityResults.size()];
			for ( int i = 0; i < entityResults.size(); i++ ) {
				AnnotationInstance annotationInstance = parserEntityResult( entityResults.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	//@ColumnResult
	private AnnotationInstance parserColumnResult(XMLColumnResult result) {
		return create( COLUMN_RESULT, null, MockHelper.stringValueArray( "name", result.getName() ) );
	}

	private void nestedColumnResultList(String name, List<XMLColumnResult> columnResults, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( columnResults ) ) {
			AnnotationValue[] values = new AnnotationValue[columnResults.size()];
			for ( int i = 0; i < columnResults.size(); i++ ) {
				AnnotationInstance annotationInstance = parserColumnResult( columnResults.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	//@FieldResult
	private AnnotationInstance parserFieldResult(XMLFieldResult result) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", result.getName(), annotationValueList );
		MockHelper.stringValue( "column", result.getColumn(), annotationValueList );
		return create( FIELD_RESULT, null, annotationValueList );
	}


	private void nestedFieldResultList(String name, List<XMLFieldResult> fieldResultList, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( fieldResultList ) ) {
			AnnotationValue[] values = new AnnotationValue[fieldResultList.size()];
			for ( int i = 0; i < fieldResultList.size(); i++ ) {
				AnnotationInstance annotationInstance = parserFieldResult( fieldResultList.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}

	private AnnotationInstance parserNamedNativeQueries(Collection<XMLNamedNativeQuery> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( Iterator<XMLNamedNativeQuery> iterator = namedQueries.iterator(); iterator.hasNext(); ) {
			AnnotationInstance annotationInstance = parserNamedNativeQuery( iterator.next() );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				NAMED_NATIVE_QUERIES, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "values", values ) }

		);
	}

	//@NamedNativeQuery
	private AnnotationInstance parserNamedNativeQuery(XMLNamedNativeQuery namedNativeQuery) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", namedNativeQuery.getName(), annotationValueList );
		MockHelper.stringValue( "query", namedNativeQuery.getQuery(), annotationValueList );
		MockHelper.stringValue(
				"resultSetMapping", namedNativeQuery.getResultSetMapping(), annotationValueList
		);
		MockHelper.classValue(
				"resultClass", namedNativeQuery.getResultClass(), annotationValueList, indexBuilder.getServiceRegistry()
		);
		nestedQueryHintList( "hints", namedNativeQuery.getHint(), annotationValueList );
		return
				create(
						NAMED_NATIVE_QUERY, null, annotationValueList

				);
	}


	private AnnotationInstance parserNamedQueries(Collection<XMLNamedQuery> namedQueries) {
		AnnotationValue[] values = new AnnotationValue[namedQueries.size()];
		int i = 0;
		for ( Iterator<XMLNamedQuery> iterator = namedQueries.iterator(); iterator.hasNext(); ) {
			AnnotationInstance annotationInstance = parserNamedQuery( iterator.next() );
			values[i++] = MockHelper.nestedAnnotationValue(
					"", annotationInstance
			);
		}
		return create(
				NAMED_QUERIES, null,
				new AnnotationValue[] { AnnotationValue.createArrayValue( "values", values ) }

		);
	}


	//@NamedQuery
	private AnnotationInstance parserNamedQuery(XMLNamedQuery namedQuery) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", namedQuery.getName(), annotationValueList );
		MockHelper.stringValue( "query", namedQuery.getQuery(), annotationValueList );
		MockHelper.enumValue( "lockMode", LOCK_MODE_TYPE, namedQuery.getLockMode(), annotationValueList );
		nestedQueryHintList( "hints", namedQuery.getHint(), annotationValueList );
		return create( NAMED_QUERY, null, annotationValueList );
	}

	//@QueryHint
	private AnnotationInstance parserQueryHint(XMLQueryHint queryHint) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", queryHint.getName(), annotationValueList );
		MockHelper.stringValue( "value", queryHint.getValue(), annotationValueList );
		return create( QUERY_HINT, null, annotationValueList );

	}

	private void nestedQueryHintList(String name, List<XMLQueryHint> constraints, List<AnnotationValue> annotationValueList) {
		if ( MockHelper.isNotEmpty( constraints ) ) {
			AnnotationValue[] values = new AnnotationValue[constraints.size()];
			for ( int i = 0; i < constraints.size(); i++ ) {
				AnnotationInstance annotationInstance = parserQueryHint( constraints.get( i ) );
				values[i] = MockHelper.nestedAnnotationValue(
						"", annotationInstance
				);
			}
			MockHelper.addToCollectionIfNotNull(
					annotationValueList, AnnotationValue.createArrayValue( name, values )
			);
		}
	}


	//@SequenceGenerator
	private AnnotationInstance parserSequenceGenerator(XMLSequenceGenerator generator) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", generator.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", generator.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", generator.getSchema(), annotationValueList );
		MockHelper.stringValue( "sequenceName", generator.getSequenceName(), annotationValueList );
		MockHelper.integerValue( "initialValue", generator.getInitialValue(), annotationValueList );
		MockHelper.integerValue( "allocationSize", generator.getAllocationSize(), annotationValueList );
		return
				create(
						SEQUENCE_GENERATOR, null, annotationValueList

				);
	}

	//@TableGenerator
	private AnnotationInstance parserTableGenerator(XMLTableGenerator generator) {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.stringValue( "name", generator.getName(), annotationValueList );
		MockHelper.stringValue( "catalog", generator.getCatalog(), annotationValueList );
		MockHelper.stringValue( "schema", generator.getSchema(), annotationValueList );
		MockHelper.stringValue( "table", generator.getTable(), annotationValueList );
		MockHelper.stringValue( "pkColumnName", generator.getPkColumnName(), annotationValueList );
		MockHelper.stringValue( "valueColumnName", generator.getValueColumnName(), annotationValueList );
		MockHelper.stringValue( "pkColumnValue", generator.getPkColumnValue(), annotationValueList );
		MockHelper.integerValue( "initialValue", generator.getInitialValue(), annotationValueList );
		MockHelper.integerValue( "allocationSize", generator.getAllocationSize(), annotationValueList );
		nestedUniqueConstraintList( "uniqueConstraints", generator.getUniqueConstraint(), annotationValueList );
		return
				create(
						TABLE_GENERATOR, null, annotationValueList

				);
	}

	@Override
	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null ) {
			return globalAnnotations.push( annotationInstance.name(), annotationInstance );
		}
		return null;
	}
}
