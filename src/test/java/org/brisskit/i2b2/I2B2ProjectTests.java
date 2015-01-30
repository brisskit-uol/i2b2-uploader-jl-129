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

	public void testI2B2Project() {
		enterTrace( "==>>testI2B2Project()" ) ;
		try {
			I2B2Project project = I2B2Project.Factory._newInstance( "project1", "kjshf" ) ;
			assert( project.getProjectId().equals( "project1" ) ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "testI2B2Project() failed: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>testI2B2Project()" ) ;
		}
	}

	
	public void testCreateNewProject() { 
		enterTrace( "==>>testCreateNewProject()" ) ;
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
		try {
			I2B2Project project = I2B2Project.Factory._newInstance( "laheart", "qwerty" ) ;
			if( I2B2Project.Factory.projectExists( project ) ) {
				I2B2Project.Factory.delete( project ) ;
			}	
			project = I2B2Project.Factory.newInstance( "laheart", "qwerty" ) ;
			project.processSpreadsheet( spreadsheetFile ) ;
		}
		catch( UploaderException cex ) {			
			cex.printStackTrace( System.out ) ;
			fail( "CreationException thrown: " + cex.getLocalizedMessage() ) ;
		}
		finally {
			exitTrace( "==>>testCreateNewProject()" ) ;
		}
		
	}
	
	
	public void testDeletionOfProject() { 
		enterTrace( "==>>testDeletionOfProject()" ) ;
		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/test-02.xls").getFile());
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/EG1-laheart.xlsx").getFile());
//		File spreadsheetFile = new File(getClass().getClassLoader().getResource("spreadsheets/GP_CUT1.xlsx").getFile());		
		try {
			//
			// First - if need be - create the project and deploy to JBoss...
			I2B2Project project = I2B2Project.Factory.newInstance( "test02dele", "qwerty" ) ;
			//
			// Now delete it...
			I2B2Project.Factory.delete( project ) ;
			
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
			I2B2Project project = I2B2Project.Factory._newInstance( "projectX", "qwerty" ) ;
			I2B2Project.Factory.delete( project ) ;
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
			I2B2Project project = I2B2Project.Factory._newInstance( "infarction", "kjshf" ) ;
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
