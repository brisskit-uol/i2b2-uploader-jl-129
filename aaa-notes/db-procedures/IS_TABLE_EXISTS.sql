CREATE OR REPLACE FUNCTION istableexists (tableName IN text) 
RETURNS varchar AS $body$
DECLARE

flag varchar(10);
countTableCur REFCURSOR;
countTable varchar(1); 

BEGIN 
    open countTableCur for EXECUTE 'SELECT count(1) FROM pg_catalog.pg_class WHERE relname = '''||tableName||''' ' ;
    LOOP
        FETCH countTableCur INTO countTable;
        IF countTable = '0'
            THEN 
            flag := 'FALSE';
            EXIT;
        ELSE
            flag := 'TRUE';
            EXIT;
    END IF;
    
    END LOOP;
    close countTableCur ;
    return flag;

    EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'An error was encountered - % -ERROR- %',SQLSTATE,SQLERRM;                
    END;
    $body$
    LANGUAGE PLPGSQL;
