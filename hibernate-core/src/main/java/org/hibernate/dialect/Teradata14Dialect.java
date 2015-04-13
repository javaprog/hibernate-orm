/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.dialect;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.type.StandardBasicTypes;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
/**
 * A dialect for the Teradata database
 *
 */
public class Teradata14Dialect extends TeradataDialect {
	/**
	 * Constructor
	 */
	public Teradata14Dialect() {
		super();
		//registerColumnType data types
		registerColumnType( Types.BIGINT, "BIGINT" );
		registerColumnType( Types.BINARY, "VARBYTE(100)" );
		registerColumnType( Types.LONGVARBINARY, "VARBYTE(32000)" );
		registerColumnType( Types.LONGVARCHAR, "VARCHAR(32000)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE,DEFAULT_BATCH_SIZE );

		registerFunction( "current_time", new SQLFunctionTemplate( StandardBasicTypes.TIME, "current_time" ) );
		registerFunction( "current_date", new SQLFunctionTemplate( StandardBasicTypes.DATE, "current_date" ) );
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getAddColumnString() {
		return "Add";
	}

	/**
	 * Get the name of the database type associated with the given
	 * <tt>java.sql.Types</tt> typecode.
	 *
	 * @param code <tt>java.sql.Types</tt> typecode
	 * @param length the length or precision of the column
	 * @param precision the precision of the column
	 * @param scale the scale of the column
	 *
	 * @return the database type name
	 *
	 * @throws HibernateException
	 */
	 public String getTypeName(int code, int length, int precision, int scale) throws HibernateException {
		/*
		 * We might want a special case for 19,2. This is very common for money types
		 * and here it is converted to 18,1
		 */
		float f = precision > 0 ? ( float ) scale / ( float ) precision : 0;
		int p = ( precision > 38 ? 38 : precision );
		int s = ( precision > 38 ? ( int ) ( 38.0 * f ) : ( scale > 38 ? 38 : scale ) );
		return super.getTypeName( code, length, p, s );
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return false;
	}

	@Override
	public String getIdentityColumnString() {
		return "generated by default as identity not null";
	}

	@Override
	public String getIdentityInsertString() {
		return "null";
	}

	@Override
	public String getDropTemporaryTableString() {
		return "drop temporary table";
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}


	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}


	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}


	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter(col, Types.REF);
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement cs) throws SQLException {
		boolean isResultSet = cs.execute();
		while (!isResultSet && cs.getUpdateCount() != -1) {
			isResultSet = cs.getMoreResults();
		}
		return cs.getResultSet();
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		@Override
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;

			int errorCode = sqle.getErrorCode();
			if (errorCode == 27003) {
				constraintName = extractUsingTemplate("Unique constraint (", ") violated.", sqle.getMessage());
			} else if (errorCode == 2700) {
				constraintName = extractUsingTemplate("Referential constraint", "violation:", sqle.getMessage());
			} else if (errorCode == 5317) {
				constraintName = extractUsingTemplate("Check constraint (", ") violated.", sqle.getMessage());
			}

			if (constraintName != null) {
				int i = constraintName.indexOf('.');
				if (i != -1) {
					constraintName = constraintName.substring(i + 1);
				}
			}
			return constraintName;
		}
	};

	@Override
	public String getWriteLockString(int timeout) {
		String sMsg = " Locking row for write ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public String getReadLockString(int timeout) {
		String sMsg = " Locking row for read  ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map keyColumnNames) {
		return new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString() + " " + sql;
	}

	@Override
	public boolean useFollowOnLocking() {
		return true;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}
}

