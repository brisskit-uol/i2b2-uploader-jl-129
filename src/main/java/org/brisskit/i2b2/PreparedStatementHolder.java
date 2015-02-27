package org.brisskit.i2b2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class PreparedStatementHolder {

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger( PreparedStatementHolder.class ) ;
	
	private Connection connection ;
	
	private Map<String,PreparedStatement> psMap = new ConcurrentHashMap<String,PreparedStatement>() ;
	
	@SuppressWarnings("unused")
	private PreparedStatementHolder() {}
	
	public PreparedStatementHolder( Connection connection ) {
		this.connection = connection ;
	}
	
	public void setPreparedStatement( String key, String sql ) throws UploaderException {	
		try {
			if( psMap.containsKey( key ) ) {
				throw new UploaderException( "PreparedStatement key: [" + key + "] already exists." ) ;
	 		}
			PreparedStatement ps = connection.prepareStatement( sql ) ;
			psMap.put( key, ps ) ;
		}
		catch( SQLException sqlex ) {
			logger.error( "Failed to prepare statement:\n\"" + sql + "\"", sqlex ) ;
			throw new UploaderException( sqlex ) ;
		}
	}
	
	public PreparedStatement getPreparedStatement( String key ) throws UploaderException {	
		PreparedStatement ps = psMap.get( key ) ;
		if( ps == null ) {
			throw new UploaderException( "PreparedStatement key: [" + key + "] does not exist." ) ;
		}
		try {
			if( ps.isClosed() ) {
				throw new UploaderException( "PreparedStatement key: [" + key + "] is closed." ) ;
			}
		}
		catch( SQLException sqlex ) {
			throw new UploaderException( "PreparedStatement key: [" + key + "] could be closed.", sqlex ) ;
		}		
		return ps ;
	}
	
}
