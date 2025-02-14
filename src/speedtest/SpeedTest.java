package speedtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import process.ProcessManager;
import process.StandardProcess;
import process.io.ProcessStreamSiphon;

public class SpeedTest {

	public String convertToCSV( String[] data ) {
		return Stream.of( data ).map( this::escapeSpecialCharacters ).collect( Collectors.joining( "," ) );
	}

	public void writeOut( List<String[]> data ) throws IOException {
		File csvOutputFile = new File( "./speedtestlog.csv" );
		try (PrintWriter pw = new PrintWriter( new FileOutputStream( csvOutputFile, true ) ) ) {
			data.stream().map( this::convertToCSV ).forEach( pw::println );
		}
	}

	public String escapeSpecialCharacters( String data ) {
		if ( data == null ) {
			throw new IllegalArgumentException( "Input data cannot be null" );
		}
		String escapedData = data.replaceAll( "\\R", " " );
		if ( data.contains( "," ) || data.contains( "\"" ) || data.contains( "'" ) ) {
			data = data.replace( "\"", "\"\"" );
			escapedData = "\"" + data + "\"";
		}
		return escapedData;
	}
	
	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Date now = new Date();
		String strDate = sdf.format( now );
		return strDate;
	}
	
	public static void main( String[] args ) throws IOException {
		ProcessManager manager = ProcessManager.getInstance();
		String name = "SpeedTest";
		StandardProcess p = new StandardProcess( name, "speedtest.exe" );

		
		manager.registerSiphon( name, new ProcessStreamSiphon() {
			String dl = ""; 
			String ul = "";
			
			@Override
			public void skimMessage( String name, String s ) {
				
				s = s.trim().replaceAll(" +", " ");
				System.out.println( s );
				String[] split = s.split( " " );
				if ( s.startsWith( "Download" ) ) {
					dl = split[ 1 ] + " " + split[ 2 ];
				}
				if ( s.startsWith( "Upload" ) ) {
					ul = split[ 1 ] + " " + split[ 2 ];
				}
//				for ( int i = 0; i < split.length; i++ ) {
//					System.out.println( i + ") " + split[ i ] );
//				}
			}
			
			@Override
			public void notifyProcessStarted( String name ) { }
			
			@Override
			public void notifyProcessEnded( String name ) { 
				try {
					dl = "";
					ul = "";
					List<String[]> data = new ArrayList<>();
					data.add( new String[] {  getCurrentTimeStamp(), dl, ul } );
					new SpeedTest().writeOut( data );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
				try {
					Thread.sleep( 300000 );
				} catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				p.restartProcess();
			}
		});
		
		manager.registerProcess( name, p );
	}
}