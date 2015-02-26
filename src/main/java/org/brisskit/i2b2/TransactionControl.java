package org.brisskit.i2b2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class TransactionControl {

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger( TransactionControl.class ) ;
	
	private Connection connection ;
	
	private Map<String,PreparedStatement> psMap = new ConcurrentHashMap<String,PreparedStatement>() ;
	
	private int commitSize = 1 ;
	
	private int updates = 0 ;
	
	@SuppressWarnings("unused")
	private TransactionControl() {}
	
	public TransactionControl( Connection connection ) {
		this.connection = connection ;
	}
	
	public synchronized void loadPreparedStatement( String key, String sql ) throws SQLException {
		PreparedStatement ps = connection.prepareStatement( sql ) ;
		psMap.put( key, ps ) ;
	}
	
	public synchronized PreparedStatement getPreparedStatement( String key ) throws SQLException {	
		PreparedStatement ps = psMap.get( key ) ;
//		if( ps != null ) {
//			ps.clearParameters() ;
//		}
		return ps ;
	}
	
	public synchronized void unloadPreparedStatement( String key ) throws SQLException {
		if( updates > 0 ) {
			PreparedStatement ps = psMap.get( key ) ;
			commit( new PreparedStatement[]{ ps }, true ) ;		
		}
		psMap.remove( key ).close() ;
	}
	
	public synchronized void setCommitSize( int size ) throws SQLException {
		this.connection.setAutoCommit( false ) ;
		this.commitSize = size ;
	}
	
	public synchronized int getCommitSize() {
		return this.commitSize ;
	}
	
	public synchronized void commit( PreparedStatement[] pss, boolean now ) throws SQLException {
		if( now ) {
			if( pss != null ) {
				for( int i=0; i<pss.length; i++ ) {
					if( pss[i] != null ) {
						pss[i].executeBatch() ;
					}					
				}
			}		
			connection.commit() ;
			connection.setAutoCommit( true ) ;
			updates = 0 ;
		}
		else {
			updates++ ;
			if( updates == commitSize ) {
				if( pss != null ) {
					for( int i=0; i<pss.length; i++ ) {
						if( pss[i] != null ) {
							pss[i].executeBatch() ;
						}					
					}
				}
				connection.commit() ;
				updates = 0 ;
			}
		}		
	}
	
	public synchronized void commit( PreparedStatement[] pss ) throws SQLException {
		commit( pss, false ) ;
	}
	
}
