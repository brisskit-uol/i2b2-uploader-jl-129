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
	
	
	public void testCreateNewProject_WithEmptyRow() { 
		enterTrace( "==>>testCreateNewProject_WithEmptyRow()" ) ;
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
			exitTrace( "==>>testCreateNewProject_WithEmptyRow()" ) ;
		}
		
	}
		
	public void testSpreadsheetBeyondMaxRows() { 
		enterTrace( "==>>testSpreadsheetBeyondMaxRows()" ) ;
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
			cex.printStackTrace( System.out ) ;			
		}
		finally {
			exitTrace( "==>>testSpreadsheetBeyondMaxRows()" ) ;
		}	
	}

	
	public void testCreateNewLaHeartProject() { 
		enterTrace( "==>>testCreateNewLaHeartProject()" ) ;
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
			exitTrace( "==>>testCreateNewLaHeartProject()" ) ;
		}
		
	}
	
	
	public void testCreateNewProjectWithStartDateColumn() { 
		enterTrace( "==>>testCreateNewProjectWithStartDateColumn()" ) ;
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
			exitTrace( "==>>testCreateNewProjectWithStartDateColumn()" ) ;
		}
		
	}

	
	public void testSupplementingExistingProject() { 
		enterTrace( "==>>testSupplementingExistingProject()" ) ;
		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
		File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());
//		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
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
			exitTrace( "==>>testSupplementingExistingProject()" ) ;
		}
		
	}
	
	public void testSupplementingExistingProject_AddionalData() { 
		enterTrace( "==>>testSupplementingExistingProject_AddionalData()" ) ;
		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
		File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01-samepeople.xls").getFile());
//		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
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
			exitTrace( "==>>testSupplementingExistingProject_AddionalData()" ) ;
		}
	}
	
	
	
	public void testSupplementingExistingProject_AddionalMetadata() { 
		enterTrace( "==>>testSupplementingExistingProject_AddionalMetadata()" ) ;
		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
		File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-02-additionalmetadata.xls").getFile());
//		File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
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
			exitTrace( "==>>testSupplementingExistingProject_AddionalMetadata()" ) ;
		}
		
	}
		
		
		public void testSupplementingExistingProject_SameInstance() { 
			enterTrace( "==>>testSupplementingExistingProject_SameInstance()" ) ;
			File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/test-01.xls").getFile());
			File spreadsheetFile2 = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());
//			File spreadsheetFile1 = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());
//			File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
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
				// And attempt to process subsequent spreadsheet...
				project.processSpreadsheet( spreadsheetFile2 ) ;
			}
			catch( UploaderException cex ) {			
				cex.printStackTrace( System.out ) ;
				fail( "UploaderException thrown: " + cex.getLocalizedMessage() ) ;
			}
			finally {
				exitTrace( "==>>testSupplementingExistingProject_SameInstance()" ) ;
			}
		
	}
	
	
	public void testDeletionOfProject() { 
		enterTrace( "==>>testDeletionOfProject()" ) ;		
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
			exitTrace( "==>>testDeletionOfProject()" ) ;
		}
	}

	
	public void testDeletionOfNonExistentProject() {
		enterTrace( "==>>testDeletionOfNonExistentProject()" ) ;
		try {
			I2B2Project.Factory.delete( "projectX" ) ;
			fail( "Should not be able to delete a non-existent project: projectX" ) ;
		}
		catch( UploaderException cex ) {			
			
		}
		finally {
			exitTrace( "==>>testDeletionOfNonExistentProject()" ) ;
		}
	}

	public void testReadSpreadsheet() {
		enterTrace( "==>>testReadSpreadsheet()" ) ;
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
			exitTrace( "==>>testReadSpreadsheet()" ) ;
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
