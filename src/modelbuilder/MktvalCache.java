package modelbuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Semaphore;

//Date indexed, cusip tagged variable-way-associative cache
//really stretching the metaphor here
public class MktvalCache {
	private final int CACHE_CAPACITY = 50000;
	private final int PREFETCH_SIZE = 12;

	//not threadsafe
	private void kick (){
		
		//kick a random "cacheline" on capacity miss
		Random r = new Random();
		ArrayList<String> keys = new ArrayList<String> (_storage.keySet());
		try{
			String rndkey = keys.get(r.nextInt(keys.size()) );
			currsize -= _storage.get(rndkey).size();
			_storage.remove(rndkey);
		}catch (IllegalArgumentException iae){
			System.err.println ("what happened?");
			System.err.println (keys.size());
			System.err.println (currsize);
			System.exit(1);
		}
		
	}
	
	public double hitrate(){
		if (tot == 0) return 0;
		return (double)hits/tot;
	}
	
	public class MktvalResult{
		public MktvalResult(double mktval, double spret){
			this.mktval = mktval;
			this.spret = spret;
		}
		public double mktval;
		public double spret;
	}
	
	private String to_monthexpr (int year, int month){
		String fmt = "%04d%02d";
		return String.format(fmt, year,month);
	}
	
	//not threadsafe
	private Boolean hit(String monthexpr, String cusip){
		if (!_storage.containsKey(monthexpr)){
			return false;
		}
		HashMap <String, MktvalResult> cacheline =
				_storage.get(monthexpr);
		if (!cacheline.containsKey(cusip)){
			return false;
		}
		return true;
	}
	
	//number of tuples the cache holds before kicking
	public MktvalCache (){}
	
	//empty the cache
	//threadsafe
	public void clear(){
		_storage.clear();
		currsize = 0;
		hits = 0;
		tot = 0;
	}
	
	public MktvalResult fetch (Connection c, int year, int month, String cusip) throws SQLException{
		String monthexpr = to_monthexpr (year,month);
		++tot;
		if (hit(monthexpr, cusip)){
			++hits;
			return _storage.get(monthexpr).get(cusip);
		}else{
			if (currsize + PREFETCH_SIZE >= CACHE_CAPACITY){
				kick();
			}
			MktvalResult m = null;
			Calendar cal = Calendar.getInstance();
			cal.set(year, month - 1, 1);
			cal.add(Calendar.MONTH, PREFETCH_SIZE);
			int endyear = cal.get(Calendar.YEAR);
			int endmonth = cal.get(Calendar.MONTH) + 1;
			String sqlfmt, sqlquery;
			if (endyear != year){
				sqlfmt = "select * from monthlystock where ((year = %d and month >= %d) "+
						 "or (year = %d and month < %d)) and cusip = '%s' order by year, month";
				sqlquery = String.format(sqlfmt, year,month,endyear, endmonth,cusip);
			}else{
				sqlfmt = "select * from monthlystock where (year = %d) and (month >= %d)" +
						"and (month < %d) and (cusip = '%s') order by year, month;";
				sqlquery = String.format (sqlfmt, year, month, endmonth, cusip);
			}
			//System.out.println (sqlquery);
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery(sqlquery);
			int rowsize = 0;
			while (rs.next()){
				int newyear = rs.getInt("year");
				int newmonth = rs.getInt("month");
				double newmktval = rs.getDouble("mktval");
				double newspret = rs.getDouble("spret");
				String newmonthexpr = to_monthexpr (newyear, newmonth);
				if (!_storage.containsKey(newmonthexpr)){
					_storage.put(newmonthexpr, new HashMap <String, MktvalResult> ());
				}
				MktvalResult tuple = new MktvalResult (newmktval, newspret);
				_storage.get(newmonthexpr).put(cusip, new MktvalResult(newmktval, newspret));
				if (rowsize == 0){
					m = tuple;
				}
				++rowsize;
			}
			if (rowsize == 0){
				m = new MktvalResult (-1,-1);
			}
			currsize += rowsize;
			return m;
		}
	}
	
	private HashMap <String, HashMap <String,MktvalResult>> _storage
		= new HashMap <String, HashMap <String, MktvalResult>> ();
	private int currsize = 0;
	private int hits = 0;
	private int tot = 0;
}
