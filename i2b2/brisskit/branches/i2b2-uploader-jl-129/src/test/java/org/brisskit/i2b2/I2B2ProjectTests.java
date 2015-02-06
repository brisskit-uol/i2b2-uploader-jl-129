package org.brisskit.i2b2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import junit.framework.TestCase;

public class I2B2ProjectTests extends TestCase {
	
	private static Log log = LogFactory.getLog( I2B2ProjectTests.class ) ;

	public I2B2ProjectTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception { 
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	public void _test01_CreateNewProject_WithEmptyRow() { 
		enterTrace( "==>>test01_CreateNewProject_WithEmptyRow()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/test-01-with-empty-row.xls").getFile());		
		try {
			if( I2B2Project.Factory.projectExists( "test01_with_empty_row" ) ) {
				I2B2Project.Factory.delete( "test01_with_empty_row" ) ;
			}	
			I2B2Project project = I2B2Project.Factory.newInstance( "test01_with_empty_row" ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test01_CreateNewProject_WithEmptyRow()" ) ;
		}
		
	}
	
	public void test02_SpreadsheetBeyondMaxRows() { 
		enterTrace( "==>>test02_SpreadsheetBeyondMaxRows()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1_more_than_maxrows.xlsx").getFile());		
		String projectId = "maxrowsexceeded" ;
		try {
			if( I2B2Project.Factory.projectExists( projectId ) ) {
				I2B2Project.Factory.delete( projectId ) ;
			}	
			I2B2Project project = I2B2Project.Factory.newInstance( projectId ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
			fail( "Created project with greater than max rows." ) ;
		}
		catch( UploaderException cex ) {			
			log.debug( cex.getLocalizedMessage() ) ;			
		}
		finally {
			exitTrace( "==>>test02_SpreadsheetBeyondMaxRows()" ) ;
		}	
	}

	
	public void _test03_CreateNewLaHeartProject() { 
		enterTrace( "==>>test03_CreateNewLaHeartProject()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());		
		String projectId = "laheart" ;
		try {
			if( I2B2Project.Factory.projectExists( projectId ) ) {
				I2B2Project.Factory.delete( projectId ) ;
			}	
			I2B2Project project = I2B2Project.Factory.newInstance( projectId ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test03_CreateNewLaHeartProject()" ) ;
		}		
	}
	
	
	public void _test04_CreateNewTest01Project() { 
		enterTrace( "==>>test04_CreateNewTest01Project()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());		
		String projectId = "test01" ;
		try {
			if( I2B2Project.Factory.projectExists( projectId ) ) {
				I2B2Project.Factory.delete( projectId ) ;
			}	
			I2B2Project project = I2B2Project.Factory.newInstance( projectId ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test04_CreateNewTest01Project()" ) ;
		}		
	}
	
	
	public void _test05_CreateNewProjectWithStartDateColumn() { 
		enterTrace( "==>>test05_CreateNewProjectWithStartDateColumn()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/test-03-startdatecol.xls").getFile() ) ;	
		try {
			if( I2B2Project.Factory.projectExists( "test03_startdatecols" ) ) {
				I2B2Project.Factory.delete( "test03_startdatecols" ) ;
			}	
			I2B2Project project = I2B2Project.Factory.newInstance( "test03_startdatecols" ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test05_CreateNewProjectWithStartDateColumn()" ) ;
		}
		
	}

	
	public void _test06_SupplementingExistingProject() { 
		enterTrace( "==>>test06_SupplementingExistingProject()" ) ;
		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
		File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());		
		try {
			//
			// Delete project if it already exists...
			if( I2B2Project.Factory.projectExists( "testnn" ) ) {
				I2B2Project.Factory.delete( "testnn" ) ;				
			}
			//
			// Create new project with all it db artifacts
			I2B2Project project = I2B2Project.Factory.newInstance( "testnn" ) ;
			//
			// Process the first spreadsheet...
			project.processSpreadsheet( spreadsheetFile1 ) ;
			//
			// Get a new instance of the project...
			project = I2B2Project.Factory.newInstance( "testnn" ) ;
			//
			// And attempt to process subsequent spreadsheet...
			project.processSpreadsheet( spreadsheetFile2 ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test06_SupplementingExistingProject()" ) ;
		}
		
	}
	
	public void _test07_SupplementingExistingProject_AdditionalData() { 
		enterTrace( "==>>test07_SupplementingExistingProject_AdditionalData()" ) ;
		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
		File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01-samepeople.xls").getFile());	
		try {
			//
			// Delete project if it already exists...
			if( I2B2Project.Factory.projectExists( "testaddmoredata" ) ) {
				I2B2Project.Factory.delete( "testaddmoredata" ) ;				
			}
			//
			// Create new project with all it db artifacts
			I2B2Project project = I2B2Project.Factory.newInstance( "testaddmoredata" ) ;
			//
			// Process the first spreadsheet...
			project.processSpreadsheet( spreadsheetFile1 ) ;
			//
			// Get a new instance of the project...
			project = I2B2Project.Factory.newInstance( "testaddmoredata" ) ;
			//
			// And attempt to process subsequent spreadsheet...
			project.processSpreadsheet( spreadsheetFile2 ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test07_SupplementingExistingProject_AdditionalData()" ) ;
		}
	}
	
	
	
	public void _test08_SupplementingExistingProject_AddionalMetadata() { 
		enterTrace( "==>>test08_SupplementingExistingProject_AddionalMetadata()" ) ;
		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
		File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-02-additionalmetadata.xls").getFile());		
		try {
			//
			// Delete project if it already exists...
			if( I2B2Project.Factory.projectExists( "testaddnewmeta" ) ) {
				I2B2Project.Factory.delete( "testaddnewmeta" ) ;				
			}
			//
			// Create new project with all it db artifacts
			I2B2Project project = I2B2Project.Factory.newInstance( "testaddnewmeta" ) ;
			//
			// Process the first spreadsheet...
			project.processSpreadsheet( spreadsheetFile1 ) ;
			//
			// Get a new instance of the project...
			project = I2B2Project.Factory.newInstance( "testaddnewmeta" ) ;
			//
			// And attempt to process subsequent spreadsheet...
			project.processSpreadsheet( spreadsheetFile2 ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test08_SupplementingExistingProject_AddionalMetadata()" ) ;
		}
		
	}
		
		
		public void _test09_SupplementingExistingProject_SameInstance() { 
			enterTrace( "==>>test09_SupplementingExistingProject_SameInstance()" ) ;
			File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
			File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());	
			String projectId = "testnnsi" ;
			try {
				//
				// Delete project if it already exists...
				if( I2B2Project.Factory.projectExists( projectId ) ) {
					I2B2Project.Factory.delete( projectId ) ;				
				}
				//
				// Create new project with all it db artifacts
				I2B2Project project = I2B2Project.Factory.newInstance( projectId ) ;
				//
				// Process the first spreadsheet...
				project.processSpreadsheet( spreadsheetFile1 ) ;
				//
				// And attempt to process subsequent spreadsheet...
				project.processSpreadsheet( spreadsheetFile2 ) ;
			}
			catch( UploaderException cex ) {			
				cex.printStackTrace( System.out ) ;
				fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
			}
			finally {
				exitTrace( "==>>test09_SupplementingExistingProject_SameInstance()" ) ;
			}
		
	}
	
	
	public void _test10_DeletionOfProject() { 
		enterTrace( "==>>test10_DeletionOfProject()" ) ;		
		try {
			//
			// First - if need be - create the project and deploy to JBoss...			
			if( !I2B2Project.Factory.projectExists( "test02dele" ) ) {
				I2B2Project.Factory.newInstance( "test02dele" ) ;
			}
			//
			// Now delete it...
			I2B2Project.Factory.delete( "test02dele" ) ;
			
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "CreationException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test10_DeletionOfProject()" ) ;
		}
	}

	
	public void _test11_DeletionOfNonExistentProject() {
		enterTrace( "==>>test11_DeletionOfNonExistentProject()" ) ;
		try {
			I2B2Project.Factory.delete( "projectX" ) ;
			fail( "Should not be able to delete a non-existent project: projectX" ) ;
		}
		catch( UploaderException cex ) {			
			
		}
		finally {
			exitTrace( "==>>test11_DeletionOfNonExistentProject()" ) ;
		}
	}

	public void _test12_ReadSpreadsheet() {
		enterTrace( "==>>test12_ReadSpreadsheet()" ) ;
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());		
		try {
			I2B2Project project = new I2B2Project() ;
			project.setSpreadsheetFile( spreadsheetFile ) ;
			project.readSpreadsheet() ;
			
			Row columnNames = project.getColumnNames() ;
		    Row toolTips = project.getToolTips() ;
			Row ontologyCodes = project.getOntologyCodes() ;
			
			assertNotNull( "columnNames should not be null", columnNames ) ;
			assertNotNull( "toolTips should not be null", toolTips ) ;
			assertNotNull( "ontologyCodes should not be null", ontologyCodes ) ;
			
			log.debug( "Column names: " ) ;
			Iterator<Cell> it = columnNames.cellIterator() ;
			while( it.hasNext() ) {
				Cell cell = it.next() ;
				log.debug( cell.getStringCellValue() ) ;
			}
			
			log.debug( "Tool tips: " ) ;
			it = toolTips.cellIterator() ;
			while( it.hasNext() ) {
				Cell cell = it.next() ;
				log.debug( cell.getStringCellValue() ) ;
			}
			
			log.debug( "Codes: " ) ;
			it = ontologyCodes.cellIterator() ;
			while( it.hasNext() ) {
				Cell cell = it.next() ;
				log.debug( cell.getStringCellValue() ) ;
			}
			
			log.debug( "========= Values: " ) ;
			DataFormatter df = new DataFormatter() ;
			Iterator<Row> rowIt = project.getSheetOne().rowIterator() ;
			rowIt.next() ; // tab past column names
			rowIt.next() ; // tab past tool tips
			rowIt.next() ; // tab past codes
			while( rowIt.hasNext() ) {
				Row row = rowIt.next() ;
				log.debug( ">>> values for row number: " + row.getRowNum() ) ;
				it = row.cellIterator() ;
				while( it.hasNext() ) {
					Cell cell = it.next() ;
					log.debug( df.formatCellValue( cell ).trim() ) ;		
				}
			}
					
		}
		catch( UploaderException cex ) {
			fail( "CreationException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>test12_ReadSpreadsheet()" ) ;
		}
						
	}
	
	
	public void test13_SpreadsheetWithLotsEmptyRows() { 
		enterTrace( "==>>test13_SpreadsheetWithLotsEmptyRows()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
		String projectId = "gpcut1" ;
		try {
			if( I2B2Project.Factory.projectExists( projectId ) ) {
				I2B2Project.Factory.delete( projectId ) ;
			}	
			I2B2Project project = I2B2Project.Factory.newInstance( projectId ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
		}
		catch( UploaderException cex ) {
			log.error( "Failed to process spreadsheet with lots of empty rows.", cex ) ;
			fail( "Failed to process spreadsheet with lots of empty rows." ) ;
						
		}
		finally {
			exitTrace( "==>>test13_SpreadsheetWithLotsEmptyRows()" ) ;
		}	
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
	
}