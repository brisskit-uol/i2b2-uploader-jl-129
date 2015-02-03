/**
 * 
 */
package org.brisskit.i2b2;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *   
 * @author jeff
 */
public class OntologyBranch {
	
	private static Log log = LogFactory.getLog( OntologyBranch.class ) ;
	
	public static enum Type {
	    NUMERIC, 
	    DATE,
	    STRING ;
	    
	    public String toString() {
	    	
	    	 switch (this) {
	            case NUMERIC:
	                return "NUMERIC" ;    
	            case DATE: 
	                return "DATE" ;
	            case STRING:
	            default:
	                return "STRING" ;
	        }
	    }
	}
	
	//
	// I have my doubts about some of the final columns in the create statement
	// for the ontology tables. The below are missing them.
	public static final String METADATA_SQL_INSERT_COMMAND = 
			"SET SCHEMA '<DB_SCHEMA_NAME>';" +
			"" +
			"INSERT INTO <DB_SCHEMA_NAME>.<PROJECT_METADATA_TABLE>" +
			                "( C_HLEVEL" +
			                ", C_FULLNAME" +
			                ", C_NAME" +
			                ", C_SYNONYM_CD" +
			                ", C_VISUALATTRIBUTES" +
			                ", C_TOTALNUM" +
			                ", C_BASECODE" +
			                ", C_METADATAXML" +
			                ", C_FACTTABLECOLUMN" +
			                ", C_TABLENAME" +
			                ", C_COLUMNNAME" +
			                ", C_COLUMNDATATYPE" +
			                ", C_OPERATOR" +
			                ", C_DIMCODE" +
			                ", C_COMMENT" +
			                ", C_TOOLTIP" +
			                ", M_APPLIED_PATH" +
			                ", UPDATE_DATE" +
			                ", DOWNLOAD_DATE" +
			                ", IMPORT_DATE" +
			                ", SOURCESYSTEM_CD" +
			                ", VALUETYPE_CD ) " +
	         "VALUES( <HLEVEL>" +
	               ", <FULLNAME>" +
	               ", <NAME>" +
	               ", <SYNONYM_CD>" +         // 1 char
	               ", <VISUALATTRIBUTES>" +   // 3 chars
	               ", NULL" +				  // totalnum
	               ", <BASECODE>" +
	               ", <METADATAXML>" +
	               ", 'concept_cd'" +
	               ", 'concept_dimension'" +
	               ", 'concept_path'" +
	               ", <COLUMNDATATYPE>" +
	               ", <OPERATOR>" +
	               ", <DIMCODE>" +
	               ", NULL" +				  // comment
	               ", <TOOLTIP>" +
	               ", '@'" +				  // applied path
	               ", now()" +
	               ", now()" +
	               ", now()" +
	               ", <SOURCESYSTEM_CD>" +
	               ", NULL ) ;" ;			  // valuetype_cd
	
	//
	//
	// In the following, CreationDateTime should be something like 01/26/2011 00:00:00
	public static final String 	METADATAXML = 
			  "<?xml version=\"1.0\"?>"
			+ "<ValueMetadata>" 
			+ " <Version>3.02</Version>"
			+ " <CreationDateTime><date-time-goes-here></CreationDateTime>"
			+ " <TestID><code-name-goes-here></TestID>"
			+ " <TestName><name-goes-here></TestName>"
			+ " <DataType><data-type-goes-here></DataType>" 
			+ " <CodeType/>" 
			+ " <Loinc/>" 
			+ " <Flagstouse></Flagstouse>"
			+ " <Oktousevalues>Y</Oktousevalues>" 
			+ " <MaxStringLength/>" 
			+ " <LowofLowValue></LowofLowValue>" 
			+ " <HighofLowValue></HighofLowValue>" 
			+ " <LowofHighValue></LowofHighValue>" 
			+ " <HighofHighValue></HighofHighValue>" 
			+ " <LowofToxicValue/>" 
			+ " <HighofToxicValue/>" 
			+ " <EnumValues></EnumValues>" 
			+ " <CommentsDeterminingExclusion>" 
			+ "   <Com/>" 
			+ " </CommentsDeterminingExclusion>"
			+ " <UnitValues>" 
			+ "   <NormalUnits><units-go-here></NormalUnits>" 
			+ "   <EqualUnits/>" 
			+ "   <ExcludingUnits/>" 
			+ "   <ConvertingUnits>" 
			+ "     <Units/>" 
			+ "     <MultiplyingFactor/>" 
			+ "   </ConvertingUnits>" 
			+ " </UnitValues>" 
			+ " <Analysis>" 
			+ "   <Enums/>" 
			+ "   <Counts/>" 
			+ "   <New/> " 
			+ " </Analysis>" 
			+ "</ValueMetadata>";
	
	
	//
	// Required query for establishing whether the concept code
	// is already in the database.
	// NB:
	// (1) The ontcode can be extended for enumeric numerations.
	// (2) We are only interested in Leaf nodes.
	public static final String CONCEPT_CODE_SQL_SELECT_COMMAND = 
			"SELECT * FROM <DB_SCHEMA_NAME>.<PROJECT_METADATA_TABLE> " +
			"WHERE C_BASECODE LIKE '<BASECODE>%' AND C_VISUALATTRIBUTES LIKE 'L%';" ;
	

	public static final String CONCEPT_DIM_SQL_INSERT_COMMAND = 
			"SET SCHEMA '<DB_SCHEMA_NAME>';" +
			"" +
			"INSERT INTO <DB_SCHEMA_NAME>.CONCEPT_DIMENSION" +
			       "( CONCEPT_PATH" +    // 	VARCHAR(700) NOT NULL
			       ", CONCEPT_CD" +      // 	VARCHAR(50) NULL
			       ", NAME_CHAR" +	     //  	VARCHAR(2000) NULL
			       ", CONCEPT_BLOB" +    //  	TEXT NULL
			       ", UPDATE_DATE" +     //  	TIMESTAMP NULL
			       ", DOWNLOAD_DATE" +   //  	TIMESTAMP NULL
			       ", IMPORT_DATE" +     //  	TIMESTAMP NULL
			       ", SOURCESYSTEM_CD" + //		VARCHAR(50) NULL
			       ", UPLOAD_ID )" +	 //		INT NULL
			"VALUES ( <CONCEPT_PATH>" +
	               ", <CONCEPT_CD>" +
	               ", <NAME_CHAR>" +
	               ", NULL" +  			// concept blob       
	               ", now()" +
	               ", now()" +
	               ", now()" +
	               ", <SOURCESYSTEM_CD>" +
	               ", NULL ) ;" ;		// upload id
	
	
	//
	// These two prefixes are to enable codes and paths to be stored in the same 
	// set (see pathsAndCodes below) without interfering with each other.
	public static final String CODE_PREFIX = "code->" ;
	public static final String PATH_PREFIX = "path->" ;

	//
	// Project id (required for schema and some column values )
	private String projectId ;
	//
	// (Possibly more important in future more complex spread sheets, but already partly present)
	// The OntologyBranch object holds data for one a basic code plus its ontology path,
	// (or rather, one basic code plus it's potential enumerations, and paths). This means that it
	// is possible for duplication to occur. The obvious one is the root, which is shared
	// by every ontology path, but also every intermediate node down to the leaf could be
	// a possible clash, meaning some repeated inserts would fail.
	// We place a string representation of each node and code here (the collection is passed in to the
	// constructor) and the same collection is present in EVERY OntologyBranch object.
	// This enables us to avoid duplicaton.
	private HashSet<String> pathsAndCodes ;
	//
	// Column name as it appears in the spreadsheet metadata row
	private String colName ;
	//
	// Tooltip as it appears in the spreadsheet metadata row
	private String toolTip ;
	//
	// Ontology code at is appears in the spreadsheet metadata row
	private String ontCode ;
	//
	// Data type for a given column (numeric, string or date) 
	private Type type ;
	//
	// If the type is numeric, this should give the units.
	// A value of blanks (or length 0) means implied units.
	// For enumerated types this is set to the default value of "enum".
	private String units ;
	//
	// 
	private ProjectUtils utils ;
	//
	// The range of values encountered within the spreadsheet for this particular column...
	private HashSet<String> values ;
	//
	// Only required if code lookups are provided 
	private Map<String,String> lookups ;
	
	private OntologyBranch() {}
	
	private OntologyBranch( String projectId
	  		              , String colName
			              , String toolTip
			              , String ontCode
			              , Type type
			              , String units
			              , Map<String,String> lookups
			              , HashSet<String> values
			              , HashSet<String> pathsAndCodes
			              , ProjectUtils utils ) {
		enterTrace( "OntologyBranch()" ) ;
		this.projectId = projectId ;
		this.colName = colName ;
		this.toolTip = toolTip ;
		this.ontCode = ontCode ;
		this.type = type ;
		this.units = units.trim() ;  // ensures this MUST NOT BE null
		this.lookups = lookups ;
		this.pathsAndCodes = pathsAndCodes ;
		this.values = values ;
		this.utils = utils ;
		exitTrace( "OntologyBranch()" ) ;
	}
	
	
	public void serializeToDatabase( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.serializeToDatabase()" ) ;
		try {			
			insertRoot( connection ) ;
			
			switch( type ) {
			case NUMERIC:
				if( units.equalsIgnoreCase( "enum" ) ) {
					insertEnumeratedNumeric( connection ) ;
				}
				else {
					insertNumeric( connection ) ;
				}				
				break ;
			case DATE:
				insertDate( connection ) ;
				break ;
			case STRING:
			default:
				if( units.equalsIgnoreCase( "text" ) ) {
					insertSearchableText( connection ) ;
				}
				else {
					insertEnumeratedString( connection ) ;
				}				
				break;
			}

		}
		finally {
			exitTrace( "OntologyBranch.serializeToDatabase()" ) ;
		}
	}
	
	
	public void serializeDifferencesToDatabase( Connection connection
											  , OntologyBranch that ) throws UploaderException {
		enterTrace( "OntologyBranch.serializeDifferencesToDatabase()" ) ;
		try {	
			//
			// Deal with the nonsensical first...
			if( !this.projectId.equals( that.projectId ) ) {
				String message = "Project ids differ. Project this: [" + this.projectId + 
						         "] whilst Project that: [" + that.projectId + "]" ;
				log.error( message ) ;
				throw new UploaderException( message ) ;
			}
				
			//
			// If we get this far in the process, all additions must be enumerations...
			if( units.equalsIgnoreCase( "enum" ) ) {
				//
				// Merge previous values into the target first...
				this.values.addAll( that.values ) ;	
				//
				// Insert the differences...
				boolean ignoreInsertFailures = true ;
				switch ( this.type ) {
				case NUMERIC:
					insertEnumeratedNumeric( connection, ignoreInsertFailures ) ;
					break;
				case STRING:
					insertEnumeratedString( connection, ignoreInsertFailures ) ;
					break;
				default:
					String message = "Differences must be either enumerated numerics or enumerated strings. Type was: " + this.type ;
					log.error( message ) ;
					throw new UploaderException( message ) ;
				}
			}
			else {
				String message = "Differences must be marked as enumerations. Units were: " + units ;
				log.error( message ) ;
				throw new UploaderException() ;
			}
					
		}
		finally {
			exitTrace( "OntologyBranch.serializeDifferencesToDatabase()" ) ;
		}
	}
	
	
	public boolean equals( OntologyBranch that ) {
		enterTrace( "OntologyBranch.equals()" ) ;
		try {			
			if( !this.projectId.equals( that.projectId ) ) {
				return false ;
			}
			if( !this.colName.equals( that.colName ) ) {
				return false ;
			}
			if( !this.ontCode.equals( that.ontCode ) ) {
				return false ;
			}
			if( !this.units.equals( that.units ) ) {
				return false ;
			}
			if( this.type != that.type ) {
				//
				// What the following is saying is that Type.STRING and Type.DATE
				// are for our purposes compatible (ie: both represent enumerated strings).
				// Any other combinations are incompatible!!!
				if( this.type == Type.NUMERIC || that.type == Type.NUMERIC ) {
					return false ;
				}
			}
			//
			// If either is of Type.DATE then the ontCode is the boss
			// and we have already tested for equality of ontCode...
			if( this.type == Type.DATE || that.type == Type.DATE ) {
				return true ;
			}
			//
			// If the values collections differ in size there must be some difference...
			if( this.values.size() != that.values.size() ) {
				return false ;
			}
			//
			// But if not, then the values must match exactly...
			Iterator<String> it = this.values.iterator() ;
			while( it.hasNext() ) {
				String value = it.next() ;
				if( !that.values.contains( value ) ) {
					return false ;
				}
			}
			//
			// If we get this far, surely they are equal?
			return true ;
		}
		finally {
			exitTrace( "OntologyBranch.equals()" ) ;
		}
	}
	
	
	private void insertRoot( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.insertRoot()" ) ;
		try {
			String fullName = "\\" + projectId + "\\" ;		
			
			if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {

				String sqlCmd = METADATA_SQL_INSERT_COMMAND ;			
					
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
				
				sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 0 ) ) ;
				sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( projectId ) ) ;
				sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
				sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "FA" ) ) ;
				sqlCmd = sqlCmd.replace( "<BASECODE>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
				sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
				sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
				
				Statement st = connection.createStatement();

				st.execute( sqlCmd ) ;

				//
				// Record the path name so we don't try and duplicate it next time...
				pathsAndCodes.add( PATH_PREFIX + fullName ) ;
			}
		}
		catch( SQLException sqlx ) {
			sqlx.printStackTrace() ;
			throw new UploaderException( "OntologyBranch.insertRoot(): Failed to create metadata table root.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertRoot()" ) ;
		}
	}
	
	private void insertNumeric( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.insertNumeric()" ) ;
		try {
			String fullName = "\\" + projectId + "\\" + colName + "\\" ;
			if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {
				
				String sqlCmd = METADATA_SQL_INSERT_COMMAND ;
				
				String date = utils.formatDate( new Date() ) ;
				String metadataxml = METADATAXML ;
				metadataxml = metadataxml.replace( "<date-time-goes-here>", date + " 00:00:00" ) ;
				metadataxml = metadataxml.replace( "<code-name-goes-here>", ontCode ) ;
				metadataxml = metadataxml.replace( "<name-goes-here>", colName ) ;
				metadataxml = metadataxml.replace( "<data-type-goes-here>", "PosFloat" ) ; 
				metadataxml = metadataxml.replace( "<units-go-here>", units ) ;
				
				log.debug( "For ontCode " + ontCode + " numeric units are: " + units ) ;
				
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
				
				sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 1 ) ) ;
				sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName ) ) ;
				sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
				sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "LA" ) ) ;
				sqlCmd = sqlCmd.replace( "<BASECODE>", utils.enfoldString( ontCode ) ) ;
				sqlCmd = sqlCmd.replace( "<METADATAXML>", utils.enfoldNullableString( metadataxml ) ) ;
				sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
				sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
				sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
				
				Statement st = connection.createStatement();
				
				st.execute( sqlCmd ) ;
				//
				// Insert concept into concept dimension...
				insertIntoConceptDimension( st, fullName, ontCode, colName ) ;
				//
				// Record the path name so we don't try and duplicate it next time...
				pathsAndCodes.add( PATH_PREFIX + fullName ) ;
			}
			
		}
		catch( SQLException sqlx ) {
			throw new UploaderException( "Failed to insert decimal branches into metadata table.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertNumeric()" ) ;
		}
	}
	
	
	private void insertEnumeratedNumeric( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.insertEnumeratedNumeric(Connection)" ) ;
		try {
			boolean ignoreInsertFailures = false ;
			insertEnumeratedNumeric( connection, ignoreInsertFailures ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertEnumeratedNumeric(Connection)" ) ;
		}
	}
	
	
	private void insertEnumeratedNumeric( Connection connection
			                            , boolean ignoreInsertFailures ) throws UploaderException {
		enterTrace( "OntologyBranch.insertEnumeratedNumeric(Connection,boolean)" ) ;
		try {
			//
			// Inserts are inserted as enumerations, so on two/three levels:
			// The base code; eg: Age
			// Possible ranges of values; eg: 1-10, 11-20, 21-30 etc
			// End point values; eg: 1, 2, 3, 4, 5 etc
			
			//
			// Insert the base code...
			String fullName = "\\" + projectId + "\\" + colName + "\\" ;
			String sqlCmd = null ;
			Statement st = connection.createStatement();
			
			if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {
				sqlCmd = METADATA_SQL_INSERT_COMMAND ;
				
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
								
				sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 1 ) ) ;
				sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName ) ) ;
				sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
				sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "FA" ) ) ;
				sqlCmd = sqlCmd.replace( "<BASECODE>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
				sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
				sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip ) ) ;
				sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
				
				try { 
					st.execute( sqlCmd ) ;				
				}
				catch( SQLException sqlex ) {
					if( !ignoreInsertFailures ) {
						throw sqlex ;
					}
				}
				//
				// Record the path name so we don't try and duplicate it next time...
				pathsAndCodes.add( PATH_PREFIX + fullName ) ;
			}
					
			//
			// Examine ranges...
			int lowestValue = Integer.MAX_VALUE ;
			int highestValue = Integer.MIN_VALUE ;
			Iterator<String> it = values.iterator() ;
			while( it.hasNext() ) {
				String val = it.next() ;
				int i = Integer.valueOf( val ) ;
				if( i < lowestValue ) {
					lowestValue = i ;
				}
				if( i > highestValue ) {
					highestValue = i ;
				}
			}
			
			//
			// Set lowest and highest in 10's
			// (ie; 3 as a lowest becomes 0, 13 as a highest becomes 20)...
			lowestValue = ( lowestValue / 10 ) * 10 ;
					
			if( highestValue % 10 != 0 ) {
				highestValue = ( ( highestValue / 10 ) * 10 ) + 10;
			}
			//
			// The formatString helps keep data sensibly in collating sequence...
			// (ie: 3 is displayed as 03, 10 as 10, and so on,
			//  otherwise 10 would come before 3 visually (in aphabetical sequence)
			long numberOfDigits = Math.round( Math.log10( highestValue ) / Math.log10( 10 ) ) ;
			if( highestValue % 10 == 0) {
				numberOfDigits++ ;
			}
			String formatString = "%0" + String.valueOf( numberOfDigits ) + "d" ;
			log.debug( "format string is " + formatString ) ;
			
			//
			// If the total range of values is below 21, we use just endpoints
			// and don't bother with ranges...
			if( (highestValue - lowestValue) < 21 ) {
				//
				// Insert the end points...
				String endPointFullName = null ;
				for( int j=lowestValue; j<highestValue+1; j++ ) {
					String paddedValue = String.format( formatString, j ) ;
					endPointFullName = fullName + paddedValue + "\\" ;
					if( !pathsAndCodes.contains( PATH_PREFIX + endPointFullName ) ) {
						sqlCmd = METADATA_SQL_INSERT_COMMAND ;
						
						sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
						sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
						
						sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 2 ) ) ;
						sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( endPointFullName ) ) ;
						sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName + ":" + paddedValue ) ) ;
						sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
						sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "LA" ) ) ;
						sqlCmd = sqlCmd.replace( "<BASECODE>", utils.enfoldString( ontCode + ":" + j ) ) ;
						sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
						sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
						sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
						sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( endPointFullName ) ) ;
						sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip + ":" + paddedValue ) ) ;
						sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
						
						try { 
							st.execute( sqlCmd ) ;				
						}
						catch( SQLException sqlex ) {
							if( !ignoreInsertFailures ) {
								throw sqlex ;
							}
						}
						
						//
						// Insert concept into concept dimension...
						insertIntoConceptDimension( st, endPointFullName, ontCode + ":" + j, colName + ":" + j ) ;
						//
						// Record the path name so we don't try and duplicate it next time...
						pathsAndCodes.add( PATH_PREFIX + fullName ) ;
					}
					
				} // end inner for
			}
			// Otherwise we use ranges of values based on a base range of 10...
			else {
				//
				// Insert the ranges...
				String rangeFullName = null ;
				String rangeShortName = null ;
				for( int i=lowestValue; i<highestValue+1; i=i+10 ) {
					String lower = String.format( formatString, i ) ;
					String upper = String.format( formatString, i+9 ) ;
					rangeShortName = lower + "-" + upper ;
					rangeFullName = fullName + rangeShortName + "\\" ;
					if( !pathsAndCodes.contains( PATH_PREFIX + rangeFullName ) ) {
						sqlCmd = METADATA_SQL_INSERT_COMMAND ;
						
						sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
						sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
						
						sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 2 ) ) ;
						sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( rangeFullName ) ) ;
						sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName + ":" + rangeShortName ) ) ;
						sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
						sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "FA" ) ) ;
						sqlCmd = sqlCmd.replace( "<BASECODE>", "NULL" ) ;
						sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
						sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
						sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
						sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( rangeFullName ) ) ;
						sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip + ": " + rangeShortName ) ) ;
						sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
						
						try { 
							st.execute( sqlCmd ) ;				
						}
						catch( SQLException sqlex ) {
							if( !ignoreInsertFailures ) {
								throw sqlex ;
							}
						}
						//
						// Record the path name so we don't try and duplicate it next time...
						pathsAndCodes.add( PATH_PREFIX + rangeFullName ) ;
					}
					
					//
					// Insert the end points...
					String endPointFullName = null ;
					for( int j=i; j<i+10; j++ ) {
						String paddedValue = String.format( formatString, j ) ;
						endPointFullName = rangeFullName + ":" + paddedValue + "\\" ;
						if( !pathsAndCodes.contains( PATH_PREFIX + endPointFullName ) ) {
							sqlCmd = METADATA_SQL_INSERT_COMMAND ;
							
							sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
							sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
							
							sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 3 ) ) ;
							sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( endPointFullName ) ) ;
							sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName + ":" + paddedValue ) ) ;
							sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
							sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "LA" ) ) ;
							sqlCmd = sqlCmd.replace( "<BASECODE>", utils.enfoldString( ontCode + ":" + j ) ) ;
							sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
							sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
							sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
							sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( endPointFullName ) ) ;
							sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip + ":" + paddedValue ) ) ;
							sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
							
							try { 
								st.execute( sqlCmd ) ;				
							}
							catch( SQLException sqlex ) {
								if( !ignoreInsertFailures ) {
									throw sqlex ;
								}
							}
							
							//
							// Insert concept into concept dimension...
							insertIntoConceptDimension( st, endPointFullName, ontCode + ":" + j, colName + ":" + j ) ;
							//
							// Record the path name so we don't try and duplicate it next time...
							pathsAndCodes.add( PATH_PREFIX + endPointFullName ) ;
						}
						
					} // end inner for
									
				} // end outer for
				
			}	
			
		}
		catch( SQLException sqlx ) {
			throw new UploaderException( "Failed to insert integer branches into metadata table.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertEnumeratedNumeric(Connection,boolean)" ) ;
		}
	}
	
	
	private void insertIntoConceptDimension( Statement statement
			                               , String conceptPath
			                               , String conceptCode
			                               , String conceptName ) throws UploaderException {
		enterTrace( "OntologyBranch.insertIntoConceptDimension()" ) ;
		try {
			boolean ignoreInsertFailures = false ;
			insertIntoConceptDimension( statement, conceptPath, conceptCode, conceptName, ignoreInsertFailures ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertIntoConceptDimension()" ) ;
		}
	}
	
	
	private void insertIntoConceptDimension(  Statement statement
											, String conceptPath
											, String conceptCode
											, String conceptName
											, boolean ignoreInsertFailures ) throws UploaderException {
		enterTrace( "OntologyBranch.insertIntoConceptDimension(ignoreInsertFailures)" ) ;
		try {
			String sqlCmd = CONCEPT_DIM_SQL_INSERT_COMMAND ;

			sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;			

			sqlCmd = sqlCmd.replace( "<CONCEPT_PATH>", utils.enfoldString( conceptPath ) ) ;
			sqlCmd = sqlCmd.replace( "<CONCEPT_CD>", utils.enfoldNullableString( conceptCode ) ) ;
			sqlCmd = sqlCmd.replace( "<NAME_CHAR>", utils.enfoldNullableString( conceptName ) ) ;
			sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;

			statement.execute( sqlCmd ) ;			
		}
		catch( SQLException sqlx ) {
			if( !ignoreInsertFailures ) {
				throw new UploaderException( "Failed to insert concept into concept dimension.", sqlx ) ;
			}
		}
		finally {
			exitTrace( "OntologyBranch.insertIntoConceptDimension(ignoreInsertFailures)" ) ;
		}
	}
	
	
	private void insertDate( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.insertDate()" ) ;
		try {
			//
			// A date is really an instance of an ontological code occurring.
			// The date is the fact start date.
			// So we treat dates like strings, but no enumeration, and 
			// rely upon the start date (of a fact) to distinguish occurrances.
			
			//
			// Insert the base code...
			String fullName = "\\" + projectId + "\\" + colName + "\\" ;
			if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {
				
				String sqlCmd = METADATA_SQL_INSERT_COMMAND ;
				
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
				
				sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 1 ) ) ;
				sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName ) ) ;
				sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
				sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "LA" ) ) ;
				sqlCmd = sqlCmd.replace( "<BASECODE>", utils.enfoldString( ontCode ) ) ;
				sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
				sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
				sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip ) ) ;
				sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
				
				Statement st = connection.createStatement();
				
				st.execute( sqlCmd ) ;
				//
				// Insert concept into concept dimension...
				insertIntoConceptDimension( st, fullName, ontCode, colName ) ;
				//
				// Record the path name so we don't try and duplicate it next time...
				pathsAndCodes.add( PATH_PREFIX + fullName ) ;
			}
			
		}
		catch( SQLException sqlx ) {
			throw new UploaderException( "Failed to insert date branches into metadata table.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertDate()" ) ;
		}
	}
	
	
	private void insertSearchableText( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.insertSearchableText()" ) ;
		try {
			
			Statement st = connection.createStatement() ;
			String sqlCmd = null ;
			String fullName = "\\" + projectId + "\\" + colName + "\\" ;
			if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {
				
				sqlCmd = METADATA_SQL_INSERT_COMMAND ;
				
				String date = utils.formatDate( new Date() ) ;
				String metadataxml = METADATAXML ;
				metadataxml = metadataxml.replace( "<date-time-goes-here>", date + " 00:00:00" ) ;
				metadataxml = metadataxml.replace( "<code-name-goes-here>", ontCode ) ;
				metadataxml = metadataxml.replace( "<name-goes-here>", colName ) ;
				metadataxml = metadataxml.replace( "<data-type-goes-here>", "String" ) ; 
				metadataxml = metadataxml.replace( "<units-go-here>", "" ) ;
				
				log.debug( "For ontCode " + ontCode + " numeric units are: " + units ) ;
				
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
				
				sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 1 ) ) ;
				sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName ) ) ;
				sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
				sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "LA" ) ) ;
				sqlCmd = sqlCmd.replace( "<BASECODE>", utils.enfoldString( ontCode ) ) ;
				sqlCmd = sqlCmd.replace( "<METADATAXML>", utils.enfoldNullableString( metadataxml ) ) ;
				sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
				sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
				sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
				
				st = connection.createStatement();
				
				st.execute( sqlCmd ) ;
				//
				// Insert concept into concept dimension...
				insertIntoConceptDimension( st, fullName, ontCode, colName ) ;
				//
				// Record the path name so we don't try and duplicate it next time...
				pathsAndCodes.add( PATH_PREFIX + fullName ) ;
			}
		}
		catch( SQLException sqlx ) {
			throw new UploaderException( "Failed to insert string branches into metadata table.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertSearchableText()" ) ;
		}
	}

	
	private void insertEnumeratedString( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.insertEnumeratedString(Connection)" ) ;
		try {
			boolean ignoreInsertFailures = false ;
			insertEnumeratedString( connection, ignoreInsertFailures ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertEnumeratedString(Connection)" ) ;
		}
	}
	
	//
	// Need to cater for lookups for bottom leaves
	// The subcategory would be replaced
	private void insertEnumeratedString( Connection connection
			                           , boolean ignoreInsertFailures ) throws UploaderException {
		enterTrace( "OntologyBranch.insertEnumeratedString(Connection,boolean)" ) ;
		try {
			if( colName.equalsIgnoreCase( "CL_STATUS" ) 
				||
				colName.equalsIgnoreCase( "DEATH" ) ) {
					
				log.debug( "About to process " + colName ) ;
					
			}
			//
			// Strings are inserted as enumerations, so on two levels:
			// The base code; eg: marital_status
			// The possible range of values; eg: single, married, widowed, separated, divorced ...
			
			//
			// Insert the base code...
			Statement st = connection.createStatement() ;
			String sqlCmd = null ;
			String fullName = "\\" + projectId + "\\" + colName + "\\" ;
			if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {
				
				sqlCmd = METADATA_SQL_INSERT_COMMAND ;
				
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
				
				sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 1 ) ) ;
				sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( colName ) ) ;
				sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
				sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "FA" ) ) ;
				sqlCmd = sqlCmd.replace( "<BASECODE>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
				sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
				sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
				sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
				sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip ) ) ;
				sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
				
				try { 
					st.execute( sqlCmd ) ;				
				}
				catch( SQLException sqlex ) {
					if( !ignoreInsertFailures ) {
						throw sqlex ;
					}
				}
				//
				// Record the path name so we don't try and duplicate it next time...
				pathsAndCodes.add( PATH_PREFIX + fullName ) ;
			}
			
					
			//
			// Now insert the range of values in the enumeration ...
			Iterator<String> it = values.iterator() ;
			while( it.hasNext() ) {
				String subCategory = it.next() ;
				String lookup = subCategory ; // default to what is there
				
				if( lookups.containsKey( colName ) ) {
					lookup = lookups.get( colName + ":" + subCategory ) ;
				}
				
				fullName = "\\" + projectId + "\\" + colName + "\\" + lookup + "\\" ;
				if( !pathsAndCodes.contains( PATH_PREFIX + fullName ) ) {
					
					sqlCmd = METADATA_SQL_INSERT_COMMAND ;
					
					sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
					sqlCmd = sqlCmd.replace( "<PROJECT_METADATA_TABLE>", projectId ) ;
					
					sqlCmd = sqlCmd.replace( "<HLEVEL>", utils.enfoldInteger( 2 ) ) ;
					sqlCmd = sqlCmd.replace( "<FULLNAME>", utils.enfoldString( fullName ) ) ;
					sqlCmd = sqlCmd.replace( "<NAME>", utils.enfoldString( lookup ) ) ;
					sqlCmd = sqlCmd.replace( "<SYNONYM_CD>", utils.enfoldString( "N" ) ) ;
					sqlCmd = sqlCmd.replace( "<VISUALATTRIBUTES>", utils.enfoldString( "LA" ) ) ;
					sqlCmd = sqlCmd.replace( "<BASECODE>", utils.enfoldString( ontCode + ":" + subCategory ) ) ;
					sqlCmd = sqlCmd.replace( "<METADATAXML>", "NULL" ) ;
					sqlCmd = sqlCmd.replace( "<COLUMNDATATYPE>", utils.enfoldString( "T" ) ) ;
					sqlCmd = sqlCmd.replace( "<OPERATOR>", utils.enfoldString( "LIKE" ) ) ;
					sqlCmd = sqlCmd.replace( "<DIMCODE>", utils.enfoldString( fullName ) ) ;
					sqlCmd = sqlCmd.replace( "<TOOLTIP>", utils.enfoldNullableString( toolTip + ":" + lookup ) ) ;
					sqlCmd = sqlCmd.replace( "<SOURCESYSTEM_CD>", utils.enfoldNullableString( projectId ) ) ;
					
					try { 
						st.execute( sqlCmd ) ;				
					}
					catch( SQLException sqlex ) {
						if( !ignoreInsertFailures ) {
							throw sqlex ;
						}
					}
					//
					// Insert concept into concept dimension...
					insertIntoConceptDimension( st, fullName, ontCode + ":" + subCategory, colName + " " + lookup ) ;
					//
					// Record the path name so we don't try and duplicate it next time...
					pathsAndCodes.add( PATH_PREFIX + fullName ) ;
				}
				
			}
		}
		catch( SQLException sqlx ) {
			throw new UploaderException( "Failed to insert string branches into metadata table.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.insertEnumeratedString(Connection,boolean)" ) ;
		}
	}
	
	
	public boolean existsWithinDataBase( Connection connection ) throws UploaderException {
		enterTrace( "OntologyBranch.existsWithinDataBase()" ) ;
		boolean exists = false ;
		try {
			//
			// See whether the base code exists in the db...
			Statement st = connection.createStatement() ;
			String sqlCmd = CONCEPT_CODE_SQL_SELECT_COMMAND ;
			sqlCmd = sqlCmd.replace( "<BASECODE>", ontCode ) ;
			ResultSet rs = st.executeQuery( sqlCmd ) ;
			if( rs.next() ) {
				exists = true ;
			}			
			rs.close() ;
			return exists ;
		}
		catch( SQLException sqlx ) {
			throw new UploaderException( "Failed to detect whether concept code was in DB or not.", sqlx ) ;
		}
		finally {
			exitTrace( "OntologyBranch.existsWithinDataBase()" ) ;
		}		
	}
	

	public Type getType() {
		return type;
	}	
	
	
	public String getOntCode() {
		return ontCode;
	}
	
	
	public String getUnits() {
		return units ;
	}
	
	private String getColName() {
		return colName;
	}
		
	
	/**
	 * Utility routine to enter a structured message in the trace log that the given method 
	 * has been entered. 
	 * 
	 * @param entry: the name of the method entered
	 */
	public static void enterTrace( String entry ) {
		I2B2Project.enterTrace( log, entry ) ;
	}

    /**
     * Utility routine to enter a structured message in the trace log that the given method 
	 * has been exited. 
	 * 
     * @param entry: the name of the method exited
     */
    public static void exitTrace( String entry ) {
    	I2B2Project.exitTrace( log, entry ) ;
	}
    
    private static Type extractDataTypeFromMetadataXml( String metadataxml ) {
    	enterTrace( "OntologyBranch.extractDataTypeFromMetadataXml()" ) ;
    	try {
    		// <DataType><data-type-goes-here></DataType>
    		int fromIndex = metadataxml.indexOf( "<DataType>" ) + 10 ;
    		int toIndex = metadataxml.indexOf( "</DataType>" ) ;
    		String dataType = metadataxml.substring( fromIndex, toIndex ) ;
    		if( dataType.equalsIgnoreCase( "String" ) ) {
    			return Type.STRING ;
    		}
    		else if( dataType.equalsIgnoreCase( "PosFloat" ) ) {
    			return Type.NUMERIC ;
    		}
    		//
    		// If not, ensure things fall in a heap...
    		return null ;
    	}
    	finally {
    		exitTrace( "OntologyBranch.extractDataTypeFromMetadataXml()" ) ;
    	}
    }
    
    private static String extractUnitsFromMetadataXml( String metadataxml ) {
    	enterTrace( "OntologyBranch.extractUnitsFromMetadataXml()" ) ;
    	try {
    		// <NormalUnits><units-go-here></NormalUnits>
    		int fromIndex = metadataxml.indexOf( "<NormalUnits>" ) + 13 ;
    		int toIndex = metadataxml.indexOf( "</NormalUnits>" ) ;
    		String units = metadataxml.substring( fromIndex, toIndex ) ;
    		return units ;
    	}
    	finally {
    		exitTrace( "OntologyBranch.extractUnitsFromMetadataXml()" ) ;
    	}
    }

    public static class Factory {
    	
    	public static OntologyBranch newInstance( String projectId
								    			, String colName
									            , String toolTip
									            , String ontCode
									            , Type type
									            , String units
									            , Map<String,String> lookups
									            , HashSet<String> values
									            , HashSet<String> pathsAndCodes
									            , ProjectUtils utils ) throws UploaderException {
			enterTrace( "OntologyBranch.Factory.newInstance()" ) ;
			OntologyBranch ob = null ;
			try {
				ob = new OntologyBranch( projectId
						    		   , colName
							           , toolTip
							           , ontCode
							           , type
							           , units
							           , lookups
							           , values
							           , pathsAndCodes
							           , utils ) ;
				return ob ;
			}
			finally {
				exitTrace( "I2B2ProjOntologyBranchect.Factory.newInstance()" ) ;
			}
		}
    	
    	
    	public static OntologyBranch newInstance( String projectId
								    			, String colName
								    			, String ontCode
								    			, Map<String,String> lookups
								    			, HashSet<String> pathsAndCodes
								    			, ProjectUtils utils ) throws UploaderException {
    		enterTrace( "OntologyBranch.Factory.newInstance()" ) ;
    		OntologyBranch ob = null ;
    		try {
    			Connection connection = Base.getSimpleConnectionPG() ;
    			Statement st = connection.createStatement() ;
    			String sqlCmd = CONCEPT_CODE_SQL_SELECT_COMMAND ;
    			sqlCmd = sqlCmd.replace( "<BASECODE>",  ontCode ) ;
    			ResultSet rs = st.executeQuery( sqlCmd ) ;
    			if( rs.next() ) {
    				ob = new OntologyBranch() ;
    				ob.projectId = projectId ;
    				ob.colName = colName ;
    				ob.ontCode = ontCode ;
    				ob.lookups = lookups ;
    				ob.pathsAndCodes = pathsAndCodes ;
    				ob.toolTip = rs.getString( "C_TOOLTIP" ) ;
    				String metadataxml = rs.getString( "C_METADATAXML" ) ;
    				if( metadataxml != null ) {
    					ob.type = extractDataTypeFromMetadataXml( metadataxml ) ;
    					ob.units = extractUnitsFromMetadataXml( metadataxml ) ;
     				}
    				else {
    					ob.units = "enum" ;
    					ob.values = new HashSet<String>() ;
    					String value = rs.getString( "C_BASECODE" ).trim() ;
    					int indexColon = value.indexOf( ':' ) ;
    					if( indexColon > 0 ) {
    						value = value.substring( indexColon+1 ) ;
    					}
    					//
    					// Collect all the "values"...
    					ob.values.add( value ) ;
    					while( rs.next() ) {
    						value = rs.getString( "C_BASECODE" ).trim() ;
    						indexColon = value.indexOf( ':' ) ;
    						value = value.substring( indexColon+1 ) ;
    						ob.values.add( value ) ;
    					}
    					ob.type = null ;
    					if( lookups.containsKey( colName ) ) {
    						// If there is a code lookup,
    						// we must treat this as of type STRING.
    						ob.type = Type.STRING ;
    					}
    					else {
    						//
    						// else we examine all the values
    						// NB: Date types are not applicable here
    						Iterator<String> it = ob.values.iterator() ;
        					while( it.hasNext() ) {
        						value = it.next() ;
        						if( utils.isNumeric( value ) ) {
        							if( ob.type == null ) {
        								ob.type = Type.NUMERIC ;
        							}
        							else if( ob.type == Type.NUMERIC ){
        								; // do nothing
        							}
        							else {
        								ob.type = Type.STRING ;
        							}						
        						}
        						else {
        							ob.type = Type.STRING ;
        						}
        					} // end while
    					}  					
    				}
    			}			
    			rs.close() ;
    			//
    			// NB: Returning null indicates this code does not exist in the database:
    			return ob ;
    		}
    		catch( SQLException sqlx ) {
    			throw new UploaderException( "Failed to detect whether concept code was in DB or not.", sqlx ) ;
    		}
    		finally {
    			exitTrace( "I2B2ProjOntologyBranchect.Factory.newInstance()" ) ;
    		}
    	}
    	
    }


}
