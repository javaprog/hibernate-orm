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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.annotation.jaxb.XMLAccessType;
import org.hibernate.metamodel.source.annotation.jaxb.XMLAttributes;
import org.hibernate.metamodel.source.annotation.jaxb.XMLEntityListeners;
import org.hibernate.metamodel.source.annotation.jaxb.XMLIdClass;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPostLoad;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPostPersist;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPostRemove;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPostUpdate;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPrePersist;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPreRemove;
import org.hibernate.metamodel.source.annotation.jaxb.XMLPreUpdate;

/**
 * @author Strong Liu
 */
abstract class AbstractEntityObjectMocker extends AnnotationMocker {
	private ListenerMocker listenerParser;
	protected AbstractAttributesBuilder attributesBuilder;
	protected ClassInfo classInfo;

	AbstractEntityObjectMocker(IndexBuilder indexBuilder, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
	}

	private boolean isPreProcessCalled = false;

	/**
	 * Pre-process Entity Objects to find the default {@link javax.persistence.Access} for later attributes processing.
	 */
	final void preProcess() {
		applyDefaults();
		classInfo = indexBuilder.createClassInfo( getClassName() );
		DotName classDotName = classInfo.name();
		if ( isMetadataComplete() ) {
			indexBuilder.metadataComplete( classDotName );
		}
		parserAccessType( getAccessType(), getTarget() );
		isPreProcessCalled = true;
	}

	final void process() {
		if ( !isPreProcessCalled ) {
			throw new AssertionFailure( "preProcess should be called before process" );
		}
		if ( getAccessType() == null ) {
			XMLAccessType accessType = AccessHelper.getEntityAccess( getTargetName(), indexBuilder );
			if ( accessType == null ) {
				accessType = getDefaults().getAccess();
			}
			parserAccessType( accessType, getTarget() );
		}
		processExtra();
		if ( isExcludeDefaultListeners() ) {
			create( EXCLUDE_DEFAULT_LISTENERS );
		}
		if ( isExcludeSuperclassListeners() ) {
			create( EXCLUDE_SUPERCLASS_LISTENERS );
		}
		parserIdClass( getIdClass() );

		if ( getAttributes() != null ) {
			getAttributesBuilder().parser();

		}
		if ( getEntityListeners() != null ) {
			getListenerParser().parser( getEntityListeners() );
		}
		getListenerParser().parser( getPrePersist() );
		getListenerParser().parser( getPreRemove() );
		getListenerParser().parser( getPreUpdate() );
		getListenerParser().parser( getPostPersist() );
		getListenerParser().parser( getPostUpdate() );
		getListenerParser().parser( getPostRemove() );
		getListenerParser().parser( getPostLoad() );

		indexBuilder.finishEntityObject( getTargetName(), getDefaults() );
	}


	abstract protected void processExtra();

	/**
	 * give a chance to the sub-classes to override defaults configuration
	 */
	abstract protected void applyDefaults();

	abstract protected boolean isMetadataComplete();

	abstract protected boolean isExcludeDefaultListeners();

	abstract protected boolean isExcludeSuperclassListeners();

	abstract protected XMLIdClass getIdClass();

	abstract protected XMLEntityListeners getEntityListeners();

	abstract protected XMLAccessType getAccessType();

	abstract protected String getClassName();

	abstract protected XMLPrePersist getPrePersist();

	abstract protected XMLPreRemove getPreRemove();

	abstract protected XMLPreUpdate getPreUpdate();

	abstract protected XMLPostPersist getPostPersist();

	abstract protected XMLPostUpdate getPostUpdate();

	abstract protected XMLPostRemove getPostRemove();

	abstract protected XMLPostLoad getPostLoad();

	abstract protected XMLAttributes getAttributes();

	protected ListenerMocker getListenerParser() {
		if ( listenerParser == null ) {
			listenerParser = new ListenerMocker( indexBuilder, classInfo );
		}
		return listenerParser;
	}

	protected AbstractAttributesBuilder getAttributesBuilder() {
		if ( attributesBuilder == null ) {
			attributesBuilder = new AttributesBuilder(
					indexBuilder, classInfo, getAccessType(), getDefaults(), getAttributes()
			);
		}
		return attributesBuilder;
	}

	protected AnnotationInstance parserIdClass(XMLIdClass idClass) {
		if ( idClass == null ) {
			return null;
		}
		String className = MockHelper.buildSafeClassName( idClass.getClazz(), getDefaults().getPackageName() );
		return create(
				ID_CLASS, MockHelper.classValueArray(
				"value", className, indexBuilder.getServiceRegistry()
		)
		);
	}


	@Override
	protected DotName getTargetName() {
		return classInfo.name();
	}

	@Override
	protected AnnotationTarget getTarget() {
		return classInfo;
	}
}
