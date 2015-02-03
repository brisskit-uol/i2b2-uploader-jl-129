/**
 * 
 */
package org.brisskit.i2b2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.brisskit.i2b2.OntologyBranch.Type;

/**
 * @author jeff
 *
 */
/**
 * @author jeff
 *
 */
public class I2B2Project {
		
	public static final String BREAKDOWNS_SQL_INSERT_COMMAND = 
			"SET SCHEMA '<DB_SCHEMA_NAME>';" +
			"" +
			"INSERT INTO <DB_SCHEMA_NAME>.QT_BREAKDOWN_PATH" +
			                "( NAME" +
			                ", VALUE" +
			                ", CREATE_DATE" +
			                ", UPDATE_DATE" +
			                ", USER_ID ) " +
	         "VALUES( <LONG_NAME>" +
	               ", <PATH>" +
	               ", now()" +
	               ", now()" +
	               ", NULL ) ;" ;	
	
	/*
	 * This set of commands will delete a project AND ALL OF ITS DATA!!!
	 */
	public static final String COMPLETELY_DELETE_PROJECT_SQL_COMMAND =
			"DROP SCHEMA <DB_SCHEMA_NAME> CASCADE; " +
	        "DROP USER <DB_USER_NAME> ;" +
			"DELETE FROM I2B2HIVE.CRC_DB_LOOKUP WHERE C_PROJECT_PATH = '/<PROJECT_ID>/' ;" +
			"DELETE FROM I2B2HIVE.ONT_DB_LOOKUP WHERE C_PROJECT_PATH = '<PROJECT_ID>/' ;" +
			"DELETE FROM I2B2HIVE.WORK_DB_LOOKUP WHERE C_PROJECT_PATH = '<PROJECT_ID>/' ;" +
			"DELETE FROM I2B2HIVE.IM_DB_LOOKUP WHERE C_PROJECT_PATH = '<PROJECT_ID>/' ;" +
			"DELETE FROM I2B2PM.PM_PROJECT_DATA WHERE PROJECT_ID = '<PROJECT_ID>' ;" +
			"DELETE FROM I2B2PM.PM_PROJECT_USER_ROLES WHERE PROJECT_ID = '<PROJECT_ID>' ;" ;
		
	public static final int DATA_SHEET_INDEX = 0 ;
	public static final int LOOKUP_SHEET_INDEX = 1 ;
	
	//
	// Data sheet values...
	public static final int COLUMN_NAME_ROW_INDEX = 0 ;
	public static final int TOOLTIPS_ROW_INDEX = 1 ;
	public static final int ONTOLOGY_CODES_ROW_INDEX = 2 ;
	public static final int FIRST_DATA_ROW_INDEX = 3 ;
	
	//
	// Code lookup sheet values...
	public static final int LOOKUP_HEADINGS_ROW_INDEX = 0 ;
	public static final int CONCEPT_COLUMN_INDEX = 0 ;
	public static final int CODE_COLUMN_INDEX = 1 ;
	public static final int DESCRIPTION_COLUMN_INDEX = 2 ;
	
	public static final String CONCEPT_COLNAME = "concept" ;
	public static final String CODE_COLNAME = "code" ;
	public static final String DESCRIPTION_COLNAME = "description" ;
	
	//
	// Breakdowns sheet values...
	public static final int BREAKDOWNS_HEADINGS_ROW_INDEX = 0 ;
	public static final int BREAKDOWN_COLUMN_INDEX = 0 ;
	public static final int NAME_COLUMN_INDEX = 1 ;
	
	public static final String BREAKDOWN_COLNAME = "breakdown" ;
	public static final String NAME_COLNAME = "column heading" ;

	public static final String[][] STANDARD_BREAKDOWNS = {
		{ "Age", "PATIENT_AGE_COUNT_XML" },
		{ "Gender", "PATIENT_GENDER_COUNT_XML" },
		{ "Race", "PATIENT_RACE_COUNT_XML" },
		{ "Vital Status", "PATIENT_VITALSTATUS_COUNT_XML" } 
	} ;
	
	private static Log log = LogFactory.getLog( I2B2Project.class ) ;
	
	private static StringBuffer logIndent = null ;
	
	private String projectId ;
    private File spreadsheetFile ;
    private boolean spreadsheetHasStartDateColumn = false ;
    private Workbook workbook ;
    private Sheet dataSheet ;
    private Sheet lookupSheet ;
    private Sheet breakdownsSheet ;
    private Row columnNames ;
    private Row toolTips ;
    private Row ontologyCodes ;
    private int numberColumns ;
    
    private boolean newProject = true ;
    
    //
    // Initial encounter number
//    private int encounterNumber = 1 ;
    
    private Map<String,Integer> patientMappings = new HashMap<String,Integer>() ;
    
    private Map<String,String> lookups = new HashMap<String,String>() ;
    
    private Map<String,String> breakdowns = new HashMap<String,String>() ;
    
    private Map<String,OntologyBranch> ontBranches = new HashMap<String,OntologyBranch>() ;
//    private ArrayList<PatientDimension> patientDims = new ArrayList<PatientDimension>() ;
//    private ArrayList<PatientMapping> patientMaps = new ArrayList<PatientMapping>() ;
    private ArrayList<ObservationFact> observatonFacts = new ArrayList<ObservationFact>() ;
    
    private ProjectUtils utils = new ProjectUtils() ;
    
    @SuppressWarnings("unused")
	protected I2B2Project() {}
    
    //
    // Removed admin userid and password.
    // We are defaulting to the demo system setup.
    // The recommendation should be the user changes passwords immediately.
    // And creates whatever other users are required.
    //
    // projectId must be alpha-numeric starting with an alpha,
    // and with no spaces.
    private I2B2Project( String projectId ) throws UploaderException {
    	enterTrace( "I2B2Project()" ) ;
    	this.projectId = projectId ;
    	CreateDBPG.setUp() ;
    	exitTrace( "I2B2Project()" ) ;
    }
    
    
    public synchronized void processSpreadsheet( File spreadSheetFile ) throws UploaderException {
    	enterTrace( "I2B2Project.processSpreadsheet(File)" ) ;
    	try {
    		//
    		// Default observation date set to run date...
    		processSpreadsheet( spreadSheetFile, new Date() ) ;
    	}
    	finally {
    		exitTrace( "I2B2Project.processSpreadsheet(File)" ) ;
    	}
    }
    
    
    public synchronized void processSpreadsheet( File spreadSheetFile
    										   , Date defaultObservationDate ) throws UploaderException {
    	enterTrace( "I2B2Project.processSpreadsheet(File,Date)" ) ;
    	try {
    		this.spreadsheetFile = spreadSheetFile ;
    		if( !newProject ) {
    			log.debug( "=====================================================" ) ;
    			log.debug( "Processing spreadsheet for an existing project: start" ) ;
    			log.debug( "=====================================================" ) ;
    			readSpreadsheet() ;
    			produceOntology() ;
    			producePatientMapping() ;
    			producePatientDimension() ;		
    			produceFacts( defaultObservationDate ) ;
    			log.debug( "===================================================" ) ;
    			log.debug( "Processing spreadsheet for an existing project: end" ) ;
    			log.debug( "===================================================" ) ;
    		}
    		else {
    			readSpreadsheet() ;
    			produceOntology() ;
    			producePatientMapping() ;
    			producePatientDimension() ;		
    			produceFacts( defaultObservationDate ) ;
    			newProject = false ;
    		}    		
    	}
    	finally {
    		exitTrace( "I2B2Project.processSpreadsheet(File,Date)" ) ;
    	}
    }
	
	
	protected void readSpreadsheet() throws UploaderException {
		enterTrace( "readSpreadsheet()" ) ;
		try {
			//
			// We read the file and create the workbook
			InputStream inp = new FileInputStream( spreadsheetFile ) ;
		    workbook = WorkbookFactory.create( inp ) ;
		    //
		    // Check we have enough sheets...
		    int noSheets = workbook.getNumberOfSheets() ;
		    if( noSheets == 0 ) {
		    	throw new UploaderException( "Spreadsheet has no contents" ) ;
		    }
		    //
	    	// Get the data sheet, which must be the first sheet... 
	    	dataSheet = workbook.getSheetAt( DATA_SHEET_INDEX ) ;
	    	//
	    	// Injest any lookups and breakdowns described in additional sheets...
		    if( noSheets > 1 ) {
		    	
		    	for( int i=1; i<noSheets ; i++ ) {
		    		Sheet sheet = workbook.getSheetAt( i ) ;
		    		if( isLookupSheet( sheet ) ) {
		    			lookupSheet = sheet ;
		    			injestLookupTables() ;
		    		}
		    		else if( isBreakdownsSheet( sheet ) ) {
		    			breakdownsSheet = sheet ;
		    			//
		    			// We only injest the breakdown info here (if it exists!)
		    			// The actual metadata is built elsewhere outside of this loop.
		    			injestBreakdowns() ;
		    		}
		    	}
		 		       	
		    }		    
		    //
		    // Check we have sufficient data rows...
		    int numberDataRows = dataSheet.getLastRowNum() - FIRST_DATA_ROW_INDEX + 1;
			if( numberDataRows < 1 ) {
				throw new UploaderException( "The workbook has insufficient data rows: " + numberDataRows ) ;
			}
		    //
		    // The first three rows contain required metadata...
			// (Perhaps in future one more row for ontology tree structure? 
			//  ie: a path statement)
		    columnNames = dataSheet.getRow( COLUMN_NAME_ROW_INDEX ) ;
		    toolTips = dataSheet.getRow( TOOLTIPS_ROW_INDEX ) ;
		    ontologyCodes = dataSheet.getRow( ONTOLOGY_CODES_ROW_INDEX ) ;	
		    //
		    // Check on the existence (or not) of observation start date column:
		    Iterator<Cell> it = columnNames.cellIterator() ;
		    spreadsheetHasStartDateColumn = false ;
		    while( it.hasNext() ) {
		    	Cell cell = it.next() ;
		    	String value = utils.getValueAsString( cell ) ;
		    	if( !utils.isNull( value ) ) {
		    		if( value.equalsIgnoreCase( "OBS_START_DATE" ) ) {
		    			spreadsheetHasStartDateColumn = true ;
		    			break ;
		    		}
		    	}
		    }
		    //
		    // Could do with some basic checks to see all rows have the same number of columns!
		    numberColumns = columnNames.getLastCellNum() ;
		    
		    //
		    // Alter spreadsheet if necessary to add dummy data for missing breakdowns
		    // ( This is in memory only. Changes do not get written back to the file ).
		    // First, we pack out the breakdowns collection, if need be with a 
		    // complete set of defaults...
		    for( int i=0; i<STANDARD_BREAKDOWNS.length; i++ ) {
				if( !breakdowns.containsKey( STANDARD_BREAKDOWNS[i][0] ) ){
					breakdowns.put( STANDARD_BREAKDOWNS[i][0], STANDARD_BREAKDOWNS[i][0] ) ;
				}
			}
		    //
		    // Then we check the spreadsheet. 
		    // It may not have have the required columns for suitable breakdowns:
		    // If the latter is the case, we create a column with all its values "unknown"...
		    String columnName = null ;
		    for( int i=0; i<STANDARD_BREAKDOWNS.length; i++ ) {
		    	columnName = breakdowns.get( STANDARD_BREAKDOWNS[i][0] ) ;
		    	if( !breakdownExists( columnName ) ) {
		    		addDefaultBreakdown( columnName ) ;
		    	}
		    }
		    		    
		}
		catch( Exception ex ) {
			throw new UploaderException( ex ) ;
		}
		finally {
			exitTrace( "readSpreadsheet()" ) ;
		}
	}
	
	
	private boolean breakdownExists( String columnName ) {
		enterTrace( "i2b2Project.breakdownExists()" ) ;
		try {
			Iterator<Cell> it = columnNames.cellIterator() ;
			while( it.hasNext() ) {
				Cell cell = it.next() ;
				String value = utils.getValueAsString( cell ) ;
				if( value.equals( columnName ) ) {
					return true ;
				}
			}			
			return false ;
		}
		finally {
			exitTrace( "i2b2Project.breakdownExists()" ) ;
		}
	}
	
	
	private void addDefaultBreakdown( String columnName ) {
		enterTrace( "i2b2Project.addDefaultBreakdown()" ) ;
		try {
			columnNames.createCell( numberColumns, Cell.CELL_TYPE_STRING ).setCellValue( columnName ) ;
			toolTips.createCell( numberColumns, Cell.CELL_TYPE_STRING ).setCellValue( columnName ) ;
			ontologyCodes.createCell( numberColumns, Cell.CELL_TYPE_STRING ).setCellValue( columnName ) ;
			Iterator<Row> it = dataSheet.iterator() ;
			it.next() ;
			it.next() ;
			it.next() ;
			while( it.hasNext() ) {
				Row row = it.next() ;
				row.createCell( numberColumns, Cell.CELL_TYPE_STRING ).setCellValue( "unknown" ) ;
			}
			numberColumns++ ;
		}
		finally {
			exitTrace( "i2b2Project.addDefaultBreakdown()" ) ;
		}
	}
	
	
	private boolean isLookupSheet( Sheet sheet ) {
		enterTrace( "i2b2Project.isLookupSheet()" ) ;
		try {
			//
		    // Check we have sufficient rows...
		    int numberRows = sheet.getLastRowNum() + 1 ;
			if( numberRows < 2 ) {
				return false ;
			}
			//
			// Check we have sufficient columns...
			Row columnNameRow = sheet.getRow( I2B2Project.LOOKUP_HEADINGS_ROW_INDEX ) ;
			int numberCols = columnNameRow.getLastCellNum() ;
			if( numberCols != 3 ) {
				return false ;
			}
			//
			// Check the format of the first row...
			String conceptHeading = utils.getValueAsString( columnNameRow.getCell( I2B2Project.CONCEPT_COLUMN_INDEX ) ) ;
			String codeHeading = utils.getValueAsString( columnNameRow.getCell( I2B2Project.CODE_COLUMN_INDEX ) ) ;
			String descriptionHeading = utils.getValueAsString( columnNameRow.getCell( I2B2Project.DESCRIPTION_COLUMN_INDEX ) ) ;
			if( !conceptHeading.equalsIgnoreCase( I2B2Project.CONCEPT_COLNAME ) 
				||
				!codeHeading.equalsIgnoreCase( I2B2Project.CODE_COLNAME )
				||
				!descriptionHeading.equalsIgnoreCase( I2B2Project.DESCRIPTION_COLNAME ) ) {
				
				return false ;
			}
			
			return true ;
		}
		finally {
			exitTrace( "i2b2Project.isLookupSheet()" ) ;
		}
	}
	
	private boolean isBreakdownsSheet( Sheet sheet ) {
		enterTrace( "i2b2Project.isBreakdownsSheet()" ) ;
		try {
			//
		    // Check we have sufficient rows...
		    int numberRows = sheet.getLastRowNum() + 1 ;
			if( numberRows < 2 ) {
				return false ;
			}
			//
			// Check we have sufficient columns...
			Row columnNameRow = sheet.getRow( I2B2Project.BREAKDOWNS_HEADINGS_ROW_INDEX ) ;
			int numberCols = columnNameRow.getLastCellNum() ;
			if( numberCols != 2 ) {
				return false ;
			}
			//
			// Check the format of the first row...
			String breakdownHeading = utils.getValueAsString( columnNameRow.getCell( I2B2Project.BREAKDOWN_COLUMN_INDEX ) ) ;
			String nameHeading = utils.getValueAsString( columnNameRow.getCell( I2B2Project.NAME_COLUMN_INDEX ) ) ;
			if( !breakdownHeading.equalsIgnoreCase( I2B2Project.BREAKDOWN_COLNAME ) 
				||
				!nameHeading.equalsIgnoreCase( I2B2Project.NAME_COLNAME ) ) {
				
				return false ;
			}
			
			return true ;
		}
		finally {
			exitTrace( "i2b2Project.isBreakdownsSheet()" ) ;
		}
	}
	
	
	private void injestLookupTables() throws UploaderException {
		enterTrace( "i2b2Project.injestLookupTables()" ) ;
		try {			

			Iterator<Row> rowIt = lookupSheet.rowIterator() ;
			//
			// Tab past column headings' row...
			rowIt.next() ;
			//
			// Process code lookup rows...
			while( rowIt.hasNext() ) {
				Row lookupRow = rowIt.next() ;	
				String name = utils.getValueAsString( lookupRow.getCell( I2B2Project.CONCEPT_COLUMN_INDEX ) ) ;
				String code = utils.getValueAsString( lookupRow.getCell( I2B2Project.CODE_COLUMN_INDEX ) ) ;
				String description = utils.getValueAsString( lookupRow.getCell( I2B2Project.DESCRIPTION_COLUMN_INDEX ) ) ;
				//
				// Place a special singular name tag within the collection.
				// (Really to make it easier to see whether the collection contains mappings for one column)
				if( !lookups.containsKey( name ) ) {
					lookups.put( name, name ) ;
				}
				//
				// Place a suitable name/code to description mapping in the collection...
				lookups.put( name + ":" + code, description ) ;
			}
			
		}
		finally {
			exitTrace( "i2b2Project.injestLookupTables()" ) ;
		}
	}
	
	
	private void injestBreakdowns() throws UploaderException {
		enterTrace( "i2b2Project.injestBreakdowns()" ) ;
		try {			

			Iterator<Row> rowIt = breakdownsSheet.rowIterator() ;
			//
			// Tab past headings' row...
			rowIt.next() ;
			//
			// Process code breakdown rows...
			Row breakdownRow = null ;
			String breakdown = null ;
			String name = null ;
			while( rowIt.hasNext() ) {
				breakdownRow = rowIt.next() ;	
				breakdown = utils.getValueAsString( breakdownRow.getCell( I2B2Project.BREAKDOWN_COLUMN_INDEX ) ) ;
				name = utils.getValueAsString( breakdownRow.getCell( I2B2Project.NAME_COLUMN_INDEX ) ) ;
				//
				// Place a suitable mapping in the collection...				
				breakdowns.put( mapToStandardBreakdownName( breakdown ), name ) ;
			}
			for( int i=0; i<STANDARD_BREAKDOWNS.length; i++ ) {
				if( !breakdowns.containsKey( STANDARD_BREAKDOWNS[i][0] ) ){
					breakdowns.put( STANDARD_BREAKDOWNS[i][0], STANDARD_BREAKDOWNS[i][0] ) ;
				}
			}
			
		}
		finally {
			exitTrace( "i2b2Project.injestBreakdowns()" ) ;
		}
	}
	
	
	private String mapToStandardBreakdownName( String breakdown ) throws UploaderException {
		enterTrace( "i2b2Project.mapToStandardBreakdownName()" ) ;
		try {
			for( int i=0; i<STANDARD_BREAKDOWNS.length; i++ ) {
				if( breakdown.equalsIgnoreCase( STANDARD_BREAKDOWNS[i][0] ) ) {
					return STANDARD_BREAKDOWNS[i][0] ;
				}
			}
			throw new UploaderException( "Non standard breakdown name encountered: " + breakdown ) ;
		}
		finally {
			exitTrace( "i2b2Project.mapToStandardBreakdownName()" ) ;
		}
	}
	
	
	private void buildBreakdowns() throws UploaderException {
		enterTrace( "i2b2Project.buildBreakdowns()" ) ;
		try {
			String breakdownName = null ;
			String headingName = null ;
			String path = null ;
			String sqlCmd = null ;
			Statement st = Base.getSimpleConnectionPG().createStatement() ;
			for( int i=0; i<STANDARD_BREAKDOWNS.length; i++ ) {
				breakdownName = STANDARD_BREAKDOWNS[i][0] ;
				//
				// The following conditional caters for the situation where
				// no breakdown sheet has been provided at all...
				if( !breakdowns.containsKey( breakdownName ) ) {
					breakdowns.put( STANDARD_BREAKDOWNS[i][0], STANDARD_BREAKDOWNS[i][0] ) ;
				}
				if( !newProject ) {
					continue ;
				}
				//
				// Only write them to the DB if a new project...
				headingName = breakdowns.get( breakdownName ) ;
				path = "\\\\" + projectId + "\\" + projectId + "\\" + headingName + "\\" ;
				sqlCmd = BREAKDOWNS_SQL_INSERT_COMMAND ;				
				sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", projectId ) ;
				sqlCmd = sqlCmd.replace( "<LONG_NAME>", utils.enfoldString( STANDARD_BREAKDOWNS[i][1] ) ) ;
				sqlCmd = sqlCmd.replace( "<PATH>", utils.enfoldString( path ) ) ;				
				st.execute( sqlCmd ) ;				
			}			
		}
		catch( SQLException sqlex ) {
			throw new UploaderException( sqlex ) ;
		}
		finally {
			exitTrace( "i2b2Project.buildBreakdowns()" ) ;
		}
	}
	
	
	protected void producePatientMapping() throws UploaderException {
		enterTrace( "producePatientMapping()" ) ;
		String value = null ;
		String name = null ;
		try {
			Iterator<Row> rowIt = dataSheet.rowIterator() ;
			//
			// Tab past metadata rows...
			rowIt.next() ;
			rowIt.next() ;
			rowIt.next() ;
			//
			// Process data rows...
			while( rowIt.hasNext() ) {
				Row dataRow = rowIt.next() ;	
				
				PatientMapping	pMap = new PatientMapping( utils ) ;
				pMap.setSchema_name( projectId ) ;
				pMap.setSourcesystem_id( projectId ) ;
				
				Iterator<Cell> cellIt = dataRow.cellIterator() ;
				Iterator<Cell> namesIt = columnNames.cellIterator() ;
				//
				// We process each cell according to its code...
				while( cellIt.hasNext() ) {
					value = utils.getValueAsString( cellIt.next() ) ;
					name = utils.getValueAsString( namesIt.next() ) ;
					if( name.equalsIgnoreCase( "id" ) ) {
							pMap.setPatient_ide( value ) ;
							pMap.setPatient_ide_source( projectId ) ;
							pMap.setProject_id( projectId ) ;
							pMap.setPatient_ide_status( "?" ) ;
							break ;
					}
					
				} // end of inner while - processing cell	
				
				//
				// Write mapping to i2b2
				pMap.serializeToDatabase( Base.getSimpleConnectionPG() ) ;
				//
				// Record the mapping between external id and internal id...
				this.patientMappings.put( pMap.getPatient_ide(), pMap.getPatient_num() ) ;
				
			} // end of outer while - processing row
			
		}
		finally {
			exitTrace( "producePatientMapping()" ) ;
		}		
	}
	
	
	protected void producePatientDimension() throws UploaderException {
		enterTrace( "producePatientDimension()" ) ;
		String value = null ;
		String code = null ;
		String name = null ;

		try {
			Iterator<Row> rowIt = dataSheet.rowIterator() ;
			//
			// Tab past metadata rows...
			rowIt.next() ;
			rowIt.next() ;
			rowIt.next() ;
			//
			// Process data rows...
			while( rowIt.hasNext() ) {
				
				Row dataRow = rowIt.next() ;
				
				PatientDimension pDim = new PatientDimension( utils ) ;
				pDim.setSchema_name( projectId ) ;
				pDim.setSourcesystem_cd( projectId ) ;
				
				//
				// We process each cell according to its code...
				Iterator<Cell> cellIt = dataRow.cellIterator() ;
				Iterator<Cell> codeIt = ontologyCodes.cellIterator() ;
				Iterator<Cell> namesIt = columnNames.cellIterator() ;
								
				while( cellIt.hasNext() ) {
					value = utils.getValueAsString( cellIt.next() ) ;
					code = utils.getValueAsString( codeIt.next() ) ;
					name = utils.getValueAsString( namesIt.next() ) ;
					if( code.startsWith( "p_dim:" ) ) {
						String[] parts = code.split( ":" ) ;
						if( parts[1].equalsIgnoreCase( "age" ) ) {
							pDim.setAge_in_years( Integer.valueOf( value ) ) ;
						}
						else if( parts[1].equalsIgnoreCase( "vital_status" ) ) {
							pDim.setVital_status_cd( value ) ;
						}
						else if( parts[1].equalsIgnoreCase( "birth_date" ) ) {
							pDim.setBirth_date( utils.parseDate( value ) ) ;
							Date presentDate = new Date() ;
							Date birthDate = utils.parseDate( value ) ;
							Calendar birthdateCalendar = Calendar.getInstance() ;
							birthdateCalendar.setTime( birthDate ) ;
							Calendar presentCalendar = Calendar.getInstance() ;	
							presentCalendar.setTime( presentDate ) ;
							int age_in_years = 
									  presentCalendar.get( Calendar.YEAR ) 
									- birthdateCalendar.get( Calendar.YEAR ) ;
							if( presentCalendar.get( Calendar.DAY_OF_YEAR ) 
								<
								birthdateCalendar.get( Calendar.DAY_OF_YEAR ) ) {
								age_in_years-- ;
							}
							pDim.setAge_in_years( age_in_years ) ;
						}
						else if( parts[1].equalsIgnoreCase( "death_date" ) ) {
							pDim.setDeath_date( utils.parseDate( value ) ) ;
						}
						else {
							//
							// And so on. However,
							// for the moment we are not concentrating on queries using patient dimension.
						}
					}
					else if( name.equalsIgnoreCase( "id" ) ) {
						//
						// We do not expect the spreadsheet to contain the i2b2 internal patient number,
						// but we can use the source-system id to retrieve the internal number generated
						// at patient mapping time...
						String sourceSystemPatientID = value ;
						pDim.setPatient_num( patientMappings.get( sourceSystemPatientID ) ) ;
						//
						// Dirty  hack to enable short-term identification of participant in brisskit portal
						// (PLEASE REMOVE - Jeff)
						pDim.setZip_cd( sourceSystemPatientID ) ;
					}
					
				} // end of inner while - processing cell	
				
				//
				// Write patient dimension to i2b2...
				pDim.serializeToDatabase( Base.getSimpleConnectionPG() ) ;
								
			} // end of outer while - processing row
		}
		catch( ParseException pex ) {
			throw new UploaderException( "Failed to parse date: " + value, pex ) ;
		}
		finally {
			exitTrace( "producePatientDimension()" ) ;
		}		
	}
	
	
	protected void produceOntology() throws UploaderException {
		enterTrace( "produceOntology()" ) ;
		try {
			Row codesRow = dataSheet.getRow( ONTOLOGY_CODES_ROW_INDEX ) ;
			//
			// pathsAndCodes is a collection to help us write out
			// hierarchical ontology paths in the DB without
			// attempting to (erroneously) insert duplicated nodes.
			// For example: path root-node/demographics/age involves
			// writing three nodes, but the first two will be repeated
			// for some other path, for example: root-node/demographics/marital-status
			HashSet<String> pathsAndCodes = new HashSet<String>() ;
			String colName = null ;
			String toolTip = null ;
			String ontCode = null ;

			//
			// Values is for the range of values contained in a column.
			HashSet<String> values = null ;
			//
			// We process each cell in the code row.
			//
			// But (!) we need to know the type of the column value(s)
			// eg: whether these represent a numeric, date or string.
			// In order to do this we examine each data value in a given column,
			// so we end up iterating over rows to gather the values of a given column.
			Iterator<Cell> cellIt = codesRow.cellIterator() ;
			while( cellIt.hasNext() ) {
				Cell codeCell = cellIt.next() ;
				int colIndex = codeCell.getColumnIndex() ;
				colName = utils.getValueAsString( dataSheet.getRow( I2B2Project.COLUMN_NAME_ROW_INDEX ).getCell( colIndex ) ) ;
				toolTip = utils.getValueAsString( dataSheet.getRow( I2B2Project.TOOLTIPS_ROW_INDEX ).getCell( colIndex ) ) ;
				//
				// The source patient id should not be processed as a fact (and therefore no ontological data)...
				if( colName.equalsIgnoreCase( "ID" ) ) {
					continue ;
				}
				//
				// Units are derived from the ontCode value in the spreadsheet.
				// Square brackets, eg: [cms] will contain the unit measure.
				// If the brackets are empty, the units are an implied value (eg: age[] would imply years)
				// An implied value can be whatever the user wishes to interpret it as.
				// 
				// NB: The special value "enum" is used internally to indicate enumerated values.
				//     Currently for numerics or strings, enums are auto-generated
				//     dependent upon the range of values encountered in the columns.
				//
				// Thus, age[] would be a numeric field searched on by value.
				// Whereas, age without the square bracket, would be an enumeration of ages
				String units = "enum" ;
				//
				// Get the ontology code and make adjustments for units
				ontCode = utils.getValueAsString( codeCell ) ;
				if( ontCode.contains( "[" ) ) {
					log.debug( "concept column value: " + ontCode ) ;
					int firstBracket = ontCode.indexOf( "[" ) ;
					int secondBracket = ontCode.indexOf( "]" ) ;
					units = ontCode.substring( firstBracket+1, secondBracket ) ;
					ontCode = ontCode.substring( 0, firstBracket ) ;
					log.debug( "which yields concept: " + ontCode + " with units: " + units ) ;
				}
								
				//
				// We gather values and attempt to diagnose type...
				values = new HashSet<String>() ;
				log.debug( "Processing column with colName: [" + colName + "] toolTip: [" + toolTip + "] ontCode: [" + ontCode + "]" ) ;

				Iterator<Row> rowIt = dataSheet.rowIterator() ;
				rowIt.next() ; // tab past column names
				rowIt.next() ; // tab past tool tips
				rowIt.next() ; // tab past codes

				while( rowIt.hasNext() ) {
					Row row = rowIt.next() ;
					Cell dataCell = row.getCell( colIndex ) ;
					String value = utils.getValueAsString( dataCell ) ;
					if( utils.isNull( value ) ) {
						log.debug( "Encountered a cell with null value" ) ;
						continue ;
					}
					//
					// Add to the range of values encountered...
					values.add( value ) ;

				} // end inner while

				//
				// Decide whether numeric, date or string...
				//
				Type type = null ;
				if( lookups.containsKey( colName ) ) {
					// If there is a code lookup,
					// we must treat this as of type STRING.
					type = Type.STRING ;
					units = "enum" ;
				}
				else {
					Iterator<String> it = values.iterator() ;				
					while( it.hasNext() ) {
						String value = it.next() ;
						if( utils.isNumeric( value ) ) {
							if( type == null ) {
								type = Type.NUMERIC ;
							}
							else if( type == Type.NUMERIC ){
								; // do nothing
							}
							else {
								type = Type.STRING ;
							}						
						}
						else if( utils.isDate( value ) ) {
							if( type == null ) {
								type = Type.DATE ;
							}
							else if( type == Type.DATE ){
								; // do nothing
							}
							else {
								type = Type.STRING ;
							}	
						}
						else {
							type = Type.STRING ;
						}
					} // end while
				}
				
				//
				// We build each branch in memory and save it in a collection 
				OntologyBranch 
					branch = OntologyBranch.Factory.newInstance( projectId
											                   , colName
											                   , toolTip
											                   , ontCode
											                   , type
											                   , units
											                   , lookups
											                   , values
											                   , pathsAndCodes
											                   , utils ) ;
				
				ontBranches.put( branch.getOntCode(), branch ) ;
				
			} // end outer while 
			
			
			//
			// Process ontology branches into the database ...
			OntologyBranch obThis = null ;
			OntologyBranch obThat = null ;
			Iterator<OntologyBranch> itOb = ontBranches.values().iterator() ;
			while( itOb.hasNext() ) {
				obThis = itOb.next() ;
				//
				// Are we creating the project's ontology
				// here for the very first time, as derived from the spreadsheet?
				obThat = OntologyBranch.Factory.newInstance( projectId
														   , colName
														   , ontCode
														   , lookups
														   , utils ) ;
				if( obThat == null ) {				
					obThis.serializeToDatabase( Base.getSimpleConnectionPG() ) ;
				}
				//
				// But if this is an existing ontology, 
				// we need to check new against old metadata...
				else {
					//
					// If there's a difference, we need to write the differences...
					if( !obThis.equals( obThat ) ) {
						obThis.serializeDifferencesToDatabase( Base.getSimpleConnectionPG()
								                             , obThat ) ;
					}
				}
			}
			
			//
	    	// Breakdowns may or may not be there as a separate sheet.
	    	// In any case, we must create some breakdown metadata
	    	// in order for breakdowns not to raise errors.
	    	// So we build all breakdowns here (defaulting if need be)...
	    	buildBreakdowns() ;
			
		}
		finally {
			exitTrace( "produceOntology()" ) ;
		}		
	}
	
	
	protected void _produceOntology() throws UploaderException {
		enterTrace( "produceOntology()" ) ;
		try {
			Row codesRow = dataSheet.getRow( ONTOLOGY_CODES_ROW_INDEX ) ;
			//
			// pathsAndCodes is a collection to help us write out
			// hierarchical ontology paths in the DB without
			// attempting to (erroneously) insert duplicated nodes.
			// For example: path root-node/demographics/age involves
			// writing three nodes, but the first two will be repeated
			// for some other path, for example: root-node/demographics/marital-status
			HashSet<String> pathsAndCodes = new HashSet<String>() ;
			String colName = null ;
			String toolTip = null ;
			String ontCode = null ;

			//
			// Values is for the range of values contained in a column.
			HashSet<String> values = null ;
			//
			// We process each cell in the code row.
			//
			// But (!) we need to know the type of the column value(s)
			// eg: whether these represent a numeric, date or string.
			// In order to do this we examine each data value in a given column,
			// so we end up iterating over rows to gather the values of a given column.
			Iterator<Cell> cellIt = codesRow.cellIterator() ;
			while( cellIt.hasNext() ) {
				Cell codeCell = cellIt.next() ;
				int colIndex = codeCell.getColumnIndex() ;
				colName = utils.getValueAsString( dataSheet.getRow( I2B2Project.COLUMN_NAME_ROW_INDEX ).getCell( colIndex ) ) ;
				toolTip = utils.getValueAsString( dataSheet.getRow( I2B2Project.TOOLTIPS_ROW_INDEX ).getCell( colIndex ) ) ;
				//
				// The source patient id should not be processed as a fact (and therefore no ontological data)...
				if( colName.equalsIgnoreCase( "ID" ) ) {
					continue ;
				}
				//
				// The default value is String
				OntologyBranch.Type type = Type.STRING ;
				//
				// Units are derived from the ontCode value in the spreadsheet.
				// Square brackets, eg: [cms] will contain the unit measure.
				// If the brackets are empty, the units are an implied value (eg: age[] would imply years)
				// An implied value can be whatever the user wishes to interpret it as.
				// 
				// NB: The special value "enum" is used internally to indicate enumerated values.
				//     Currently for numerics or strings, enums are auto-generated
				//     dependent upon the range of values encountered in the columns.
				//
				// Thus, age[] would be a numeric field searched on by value.
				// Whereas, age without the square bracket, would be an enumeration of ages
				String units = "enum" ;
				//
				// Get the ontology code and make adjustments for units
				ontCode = utils.getValueAsString( codeCell ) ;
				if( ontCode.contains( "[" ) ) {
					log.debug( "concept column value: " + ontCode ) ;
					int firstBracket = ontCode.indexOf( "[" ) ;
					int secondBracket = ontCode.indexOf( "]" ) ;
					units = ontCode.substring( firstBracket+1, secondBracket ) ;
					ontCode = ontCode.substring( 0, firstBracket ) ;
					log.debug( "which yields concept: " + ontCode + " with units: " + units ) ;
				}
								
				//
				// We gather values and attempt to diagnose type...
				values = new HashSet<String>() ;
				log.debug( "Processing column with colName: [" + colName + "] toolTip: [" + toolTip + "] ontCode: [" + ontCode + "]" ) ;

				Iterator<Row> rowIt = dataSheet.rowIterator() ;
				rowIt.next() ; // tab past column names
				rowIt.next() ; // tab past tool tips
				rowIt.next() ; // tab past codes

				while( rowIt.hasNext() ) {
					Row row = rowIt.next() ;
					Cell dataCell = row.getCell( colIndex ) ;
					String value = utils.getValueAsString( dataCell ) ;
					if( utils.isNull( value ) ) {
						log.debug( "Encountered a cell with null value" ) ;
						continue ;
					}
					//
					// Add to the range of values encountered...
					values.add( value ) ;
					//
					// Decide whether numeric, date or string...
					if( utils.isNumeric( value ) ) {
						log.debug( "Cell with numeric value: " + value ) ;
						if( type == Type.STRING ) {
							type = Type.NUMERIC ;
						}
						else if( type == Type.NUMERIC ) {
							// If type has already been established as a decimal, do nothing
						}
					}
					else if( utils.isDate( value ) ) {
						log.debug( "Cell with date value: " + value ) ;
						if( type == Type.STRING ) {
							type = Type.DATE ;
						}
						else if( type != Type.DATE ) {
							log.error( "Cells with incompatible values; current value: " + value ) ;
						}
					}
					else {
						// We assume a string
						log.debug( "Cell with string value: " + value ) ;
					}

				} // end inner while

				
				//
				// If there is a code lookup, however,
				// we must treat this as of type STRING.
				if( lookups.containsKey( colName ) ) {
					type = Type.STRING ;
					units = "enum" ;
				}
													
				//
				// We build each branch in memory and save it in a collection 
				OntologyBranch 
					branch = OntologyBranch.Factory.newInstance( projectId
											                   , colName
											                   , toolTip
											                   , ontCode
											                   , type
											                   , units
											                   , lookups
											                   , values
											                   , pathsAndCodes
											                   , utils ) ;
				
				ontBranches.put( branch.getOntCode(), branch ) ;
				
			} // end outer while 
			
			
			//
			// Process ontology branches into the database ...
			OntologyBranch obThis = null ;
			OntologyBranch obThat = null ;
			Iterator<OntologyBranch> itOb = ontBranches.values().iterator() ;
			while( itOb.hasNext() ) {
				obThis = itOb.next() ;
				//
				// Are we creating the project's ontology
				// here for the very first time, as derived from the spreadsheet?
				if( !obThis.existsWithinDataBase( Base.getSimpleConnectionPG() ) ) {				
					obThis.serializeToDatabase( Base.getSimpleConnectionPG() ) ;
				}
				//
				// But if this is an existing ontology, 
				// we need to check new against old metadata...
				else {
					obThat = OntologyBranch.Factory.newInstance( projectId
															   , colName
							                                   , ontCode
							                                   , lookups
							                                   , utils ) ;
					//
					// If there's a difference, we need to write the differences...
					if( !obThis.equals( obThat ) ) {
						obThis.serializeDifferencesToDatabase( Base.getSimpleConnectionPG()
								                             , obThat ) ;
					}
				}
			}
			
			//
	    	// Breakdowns may or may not be there as a separate sheet.
	    	// In any case, we must create some breakdown metadata
	    	// in order for breakdowns not to raise errors.
	    	// So we build all breakdowns here (defaulting if need be)...
	    	buildBreakdowns() ;
			
		}
		finally {
			exitTrace( "produceOntology()" ) ;
		}		
	}
	
	
	protected void produceFacts( Date defaultObservationStartDate ) throws UploaderException {
		enterTrace( "I2B2Project.produceFacts()" ) ;
		try {
			Iterator<Row> rowIt = dataSheet.iterator() ;
			Date observationStartDate = null ;
			//
			// Tab past metadata rows...
			rowIt.next() ;
			rowIt.next() ;
			rowIt.next() ;
			//
			// Process data rows...
			while( rowIt.hasNext() ) {
				Row dataRow = rowIt.next() ;
				int patientNumber = getPatientNumber( dataRow ) ;
				if( spreadsheetHasStartDateColumn ) {
					observationStartDate = getObservationStartDate( dataRow ) ;
					//
					// It might still return null if the column is empty...
					if( observationStartDate == null ) {
						observationStartDate = defaultObservationStartDate ;
					}
				}
				else {
					observationStartDate = defaultObservationStartDate ;
				}
				//
				// Process the cells for each row...
				Iterator<Cell> cellIt = dataRow.cellIterator() ;
				while( cellIt.hasNext() ) {
					Cell cell = cellIt.next() ;		
					if( utils.isNull( utils.getValueAsString( cell ) ) ) {
						continue ;
					}
					String ontCode = getOntCode( cell ) ;
					//
					// We bypass any columns which are not connected to ontological facts
					if( utils.isNull( ontCode ) ) {
						continue ;
					}
					else if( ontCode.startsWith( "p_map:" ) || ontCode.startsWith( "p_dim:" ) ) {
						continue ;
					}
					else if( ontCode.equalsIgnoreCase( "OBS_START_DATE" ) ) {
						continue ;
					}
					else {
						OntologyBranch ontBranch = getOntologyBranch( ontCode ) ;
						OntologyBranch.Type type = ontBranch.getType() ;
						String units = ontBranch.getUnits() ;
						ObservationFact of = null ;						
						switch ( type ) {
						case DATE:
							of = produceDateFact( patientNumber, ontCode, cell ) ;
							break ;
						case NUMERIC:
							of = produceNumericFact( patientNumber
									               , ontCode
									               , units
									               , cell 
									               , observationStartDate ) ;
							break ;
						case STRING:
						default:
							of = produceStringFact( patientNumber
									              , ontCode
									              , units
									              , cell 
									              , observationStartDate ) ;
							break;
						}
						
						//
						// Write fact to i2b2...
						of.serializeToDatabase( Base.getSimpleConnectionPG() ) ;
						
					}
					
				} // end of inner while - processing cell	
				
			} // end of outer while - processing row
		}
		finally {
			exitTrace( "I2B2Project.produceFacts()" ) ;
		}		
	}
	
	
	private ObservationFact produceDateFact( int patientNumber
			                               , String ontCode
			                               , Cell cell ) throws UploaderException {
		enterTrace( "I2B2Project.produceDateFact()" ) ;
		try {
			ObservationFact of = new ObservationFact( utils ) ;				
//			of.setEncounter_num( encounterNumber++ ) ;
			of.setPatient_num( patientNumber ) ;
			
			of.setConcept_cd( ontCode ) ;
			of.setProvider_id( "@" ) ;

			String value = utils.getValueAsString( cell ) ;	
			//
			// For dates, the value becomes the start date of the observation...
			of.setStart_date( utils.parseDate( value ) ) ;			
			of.setValtype_cd( "T" ) ;
			//
			// For dates, the value is a choice between "yes" and "no"
			// (it could be "true" and "false")
			of.setTval_char( "yes" ) ;
			
			of.setSourcesystem_cd( projectId ) ;
			of.setSchema_name( projectId ) ;
			return of ;
		}
		catch( ParseException pex ) {
			throw new UploaderException( "Could not parse start date", pex ) ;
		}
		finally {
			exitTrace( "I2B2Project.produceDateFact()" ) ;
		}
	}
	
	
	private ObservationFact produceNumericFact( int patientNumber
                                              , String ontCode
                                              , String units
                                              , Cell cell
                                              , Date observationStartDate ) throws UploaderException {
		enterTrace( "I2B2Project.produceNumericFact()" ) ;
		try {
			ObservationFact of = new ObservationFact( utils ) ;				
//			of.setEncounter_num( encounterNumber++ ) ;
			of.setPatient_num( patientNumber ) ;
				
			of.setProvider_id( "@" ) ;
			of.setStart_date( observationStartDate ) ;
			
			String value = utils.getValueAsString( cell ) ;			
			//
			// Numeric facts can be stored as 
			// (1) a plain numeric value, or
			// (2) as an enumeration
			if( units.equalsIgnoreCase( "enum" ) ) {
				of.setConcept_cd( ontCode + ":" + value ) ;	// this is liable to error in padding!!!
				of.setValtype_cd( "T" ) ;						
				of.setTval_char( value ) ;
			}
			else {
				of.setConcept_cd( ontCode ) ;
				of.setValtype_cd( "N" ) ;
				of.setTval_char( "E" ) ;
				of.setNval_num( Double.valueOf( value ) ) ;
			}
			
			of.setSourcesystem_cd( projectId ) ;
			of.setSchema_name( projectId ) ;
			return of ;
		}
		finally {
			exitTrace( "I2B2Project.produceNumericFact()" ) ;
		}
	}
	
	
	private ObservationFact produceStringFact( int patientNumber
                                             , String ontCode
                                             , String units
                                             , Cell cell
                                             , Date observationStartDate ) throws UploaderException {
		enterTrace( "I2B2Project.produceStringFact()" ) ;
		try {
			ObservationFact of = new ObservationFact( utils ) ;				
//			of.setEncounter_num( encounterNumber++ ) ;
			of.setPatient_num( patientNumber ) ;
			
			
			of.setProvider_id( "@" ) ;
			of.setStart_date( observationStartDate ) ;
		
			of.setValtype_cd( "T" ) ;					
			String value = utils.getValueAsString( cell ) ;	
			of.setTval_char( value ) ;
			
			if( units.equalsIgnoreCase( "enum" ) ) {
				of.setConcept_cd( ontCode + ":" + value ) ;
			}
			else {
				of.setConcept_cd( ontCode ) ;
			}
			
			of.setSourcesystem_cd( projectId ) ;
			of.setSchema_name( projectId ) ;
			return of ;
		}
		finally {
			exitTrace( "I2B2Project.produceStringFact()" ) ;
		}
	}

	
	protected OntologyBranch getOntologyBranch( String ontCode) {
		return ontBranches.get( ontCode ) ;
	}
	
	protected String getOntCode( Cell dataCell ) {
		String ontCodeCellValue = 
				utils.getValueAsString( dataSheet.getRow( I2B2Project.ONTOLOGY_CODES_ROW_INDEX ).getCell( dataCell.getColumnIndex() ) ) ;
		int indexFirstUnitsBracket = ontCodeCellValue.indexOf( '[' ) ;
		if( indexFirstUnitsBracket != -1 ) {
			ontCodeCellValue = ontCodeCellValue.substring( 0, indexFirstUnitsBracket ) ;			
		}
		return ontCodeCellValue ;
	}
	
	
	public int getPatientNumber( Row dataRow ) {
		Iterator<Cell> cellIt = dataRow.getSheet().getRow( I2B2Project.COLUMN_NAME_ROW_INDEX ).cellIterator() ;
		int patientNumberIndex = -1 ;
		while( cellIt.hasNext() ) {
			Cell cell = cellIt.next() ;
			String value = utils.getValueAsString( cell ) ;
			//
			// Search for the source systems id...
			if( value.equalsIgnoreCase( "id" ) ) {
				patientNumberIndex =  cell.getColumnIndex() ;
				break ;
			}			
		}
		String sourcePatientNoAsString = utils.getValueAsString( dataRow.getCell( patientNumberIndex ) )  ;		
		//
		// Given the source system patient identifier (as a string), 
		// we use the mappings to get the i2b2 internal id...		
		return this.patientMappings.get( sourcePatientNoAsString ) ;
	}
	
	
	public Date getObservationStartDate( Row dataRow ) throws UploaderException {
		enterTrace( "I2B2Project.getObservationStartDate()" ) ;
		Date obsStartDate = null ;
		String dateAsString = null ;
		try {
			Iterator<Cell> cellIt = dataRow.getSheet().getRow( I2B2Project.COLUMN_NAME_ROW_INDEX ).cellIterator() ;			
			int startDateIndex = -1 ;
			while( cellIt.hasNext() ) {
				Cell cell = cellIt.next() ;
				String value = utils.getValueAsString( cell ) ;
				//
				// Search for the source systems id...
				if( value.equalsIgnoreCase( "OBS_START_DATE" ) ) {
					startDateIndex = cell.getColumnIndex() ;
					break ;
				}			
			}
			if( startDateIndex != -1 ) {
				dateAsString = utils.getValueAsString( dataRow.getCell( startDateIndex ) ) ;
				if( utils.isNull( dateAsString ) ) {
					dateAsString = null ;
				}
				else {
					obsStartDate = utils.parseDate( dateAsString )  ;	
				}
			}
			return obsStartDate ;
		}
		catch( ParseException  pex) {
			String message = "Failed to parse column value as a observation start date: " + dateAsString ;
			log.error( message, pex ) ;
			throw new UploaderException( message, pex ) ;
		}
		finally {
			exitTrace( "I2B2Project.getObservationStartDate()" ) ;
		}		
	}
	

	
	/**
	 * Utility routine to enter a structured message in the trace log that the given method 
	 * has been entered. 
	 * 
	 * @param entry: the name of the method entered
	 */
	public static void enterTrace( String entry ) {
		enterTrace( log, entry ) ;
	}
	
	public static void enterTrace( Log log, String entry ) {
		log.trace( getIndent().toString() + "enter: " + entry ) ;
		indentPlus() ;
	}

    /**
     * Utility routine to enter a structured message in the trace log that the given method 
	 * has been exited. 
	 * 
     * @param entry: the name of the method exited
     */
    public static void exitTrace( String entry ) {
    	exitTrace( log, entry ) ;
	}
    
	public static void exitTrace( Log log, String entry ) {
		indentMinus() ;
		log.trace( getIndent().toString() + "exit: " + entry ) ;
	}
	
    /**
     * Utility method used to maintain the structured trace log.
     */
    public static void indentPlus() {
		getIndent().append( ' ' ) ;
	}
	
    /**
     * Utility method used to maintain the structured trace log.
     */
    public static void indentMinus() {
        if( logIndent.length() > 0 ) {
            getIndent().deleteCharAt( logIndent.length()-1 ) ;
        }
	}
	
    /**
     * Utility method used for indenting the structured trace log.
     */
    public static StringBuffer getIndent() {
	    if( logIndent == null ) {
	       logIndent = new StringBuffer() ;	
	    }
	    return logIndent ;	
	}
    
    @SuppressWarnings("unused")
	private static void resetIndent() {
        if( logIndent != null ) { 
            if( logIndent.length() > 0 ) {
               logIndent.delete( 0, logIndent.length() )  ;
            }
        }   
    }
	
	public String getProjectId() {
		return projectId;
	}


	protected void setSpreadsheetFile( File file )  {
		this.spreadsheetFile = file ;
	}
	
	public File getSpreadsheetFile() {
		return spreadsheetFile;
	}


	public Workbook getWorkbook() {
		return workbook;
	}


	public Sheet getSheetOne() {
		return dataSheet;
	}


	public Row getColumnNames() {
		return columnNames;
	}


	public Row getToolTips() {
		return toolTips;
	}


	public Row getOntologyCodes() {
		return ontologyCodes;
	}
	
	public Row getDataRow( int rowOffset ) {
		return dataSheet.getRow( rowOffset ) ;
	}


	public ArrayList<ObservationFact> getObservatonFacts() {
		return observatonFacts;
	}
	
	public static class Factory {
		
		public static I2B2Project newInstance( String projectId ) throws UploaderException {
			enterTrace( "I2B2Project.Factory.newInstance()" ) ;
			I2B2Project project = new I2B2Project( projectId ) ;
			try {
				if( projectExists( project ) ) {
					project.newProject = false ;
				}
				else {
					CreateDBPG.createI2B2Database( projectId );
				}
				return project ;
			}
			finally {
				exitTrace( "I2B2Project.Factory.newInstance()" ) ;
			}
		}
		
		
		public static void delete( String projectId ) throws UploaderException {
			enterTrace( "I2B2Project.Factory.delete(String)" ) ;
			try {
				I2B2Project project = new I2B2Project( projectId ) ;
				delete( project ) ;
			}	
			finally {
				exitTrace( "I2B2Project.Factory.delete(String)" ) ;
			}
		}
		
		
		public static void delete( I2B2Project project ) throws UploaderException {
			enterTrace( "I2B2Project.Factory.delete(I2B2Project)" ) ;
			Connection connection = null ;
			try {
				if( projectExists( project.getProjectId() ) ) {
					connection = Base.getSimpleConnectionPG() ;
					connection.setTransactionIsolation( Connection.TRANSACTION_SERIALIZABLE ) ;
					connection.setAutoCommit( false ) ;
					String sqlCmd = COMPLETELY_DELETE_PROJECT_SQL_COMMAND ;							
					sqlCmd = sqlCmd.replaceAll( "<DB_SCHEMA_NAME>", project.getProjectId() ) ;
					sqlCmd = sqlCmd.replace( "<DB_USER_NAME>", project.getProjectId() ) ;
					sqlCmd = sqlCmd.replace( "<PROJECT_ID>", project.getProjectId() ) ;
					Statement st = connection.createStatement() ;
					st.execute( sqlCmd ) ;
					connection.commit() ;
					connection.setTransactionIsolation( Connection.TRANSACTION_SERIALIZABLE ) ;
					connection.setAutoCommit( true ) ;
					CreateDBPG.undeployFromJBoss( project.getProjectId() ) ;
				}	
				else {
					throw new UploaderException( "Cannot delete non-existent project: " + project.getProjectId() ) ;
				}
			}
			catch( SQLException sqlex ) {
				if( connection != null ) try { connection.rollback() ; } catch( SQLException ex ) { ; }
				throw new UploaderException( "Failed to delete project: " + project.getProjectId(), sqlex ) ;
			}	
			finally {
				exitTrace( "I2B2Project.Factory.delete(I2B2Project)" ) ;
			}
		}
		
		
		public static boolean projectExists( I2B2Project project ) throws UploaderException {
			enterTrace( "I2B2Project.Factory.projectExists(I2B2Project)" ) ;
			boolean exists = false ;
			Connection connection = null ;
			try {
				//
				// We make two attempts to see whether a project exists
				// First by querying the pm table
				// Second by seeing whether the schema exists
				// (If either of these returns true, the project exists in our terms)...
				String sqlPMCmd = "select * from i2b2pm.pm_project_data where project_id = '<PROJECT_ID>' ;" ;
				sqlPMCmd = sqlPMCmd.replaceAll( "<PROJECT_ID>", project.getProjectId() ) ;
				connection = Base.getSimpleConnectionPG() ;
//				connection.setTransactionIsolation( Connection.TRANSACTION_SERIALIZABLE ) ;
//				connection.setAutoCommit( false ) ;
				Statement st = connection.createStatement() ;
				ResultSet rs = st.executeQuery( sqlPMCmd ) ;
				if( rs.next() ) {
					exists = true ;
				}
				else {
					String sqlSchemaCmd = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '<SCHEMA_NAME>';" ;
					sqlSchemaCmd = sqlSchemaCmd.replaceAll( "<SCHEMA_NAME>", project.getProjectId() ) ;
					rs = st.executeQuery( sqlSchemaCmd ) ;
					if( rs.next() ) {
						exists = true ;
					}
				}				
				rs.close() ;
//				connection.commit() ;
//				connection.setTransactionIsolation( Connection.TRANSACTION_SERIALIZABLE ) ;
//				connection.setAutoCommit( true ) ;
				return exists ;
			}
			catch( SQLException sqlx ) {
				String message =  "Could not confirm project existed or not. Project id: " + project.getProjectId() ;
				log.error( message, sqlx ) ;
				if( connection != null ) try { connection.rollback() ; } catch( SQLException ex ) { ; }
				throw new UploaderException( message, sqlx ) ;
			}
			finally {
				exitTrace( "I2B2Project.Factory.projectExists(I2B2Project)" ) ;
			}
		}
		
		
		public static boolean projectExists( String projectId ) throws UploaderException {
			enterTrace( "I2B2Project.Factory.projectExists(I2B2Project)" ) ;		
			try {
				I2B2Project project = new I2B2Project( projectId ) ;
				return projectExists( project ) ;
			}
			finally {
				exitTrace( "I2B2Project.Factory.projectExists(I2B2Project)" ) ;
			}
		}
		
	} // end of Factory class

	
}
