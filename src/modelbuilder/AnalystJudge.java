package modelbuilder;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;

public abstract class AnalystJudge {
	public AnalystJudge (int endy, int endm, int endd, String cusip, HelpfulnessFinder h){
		enddate = Calendar.getInstance();
		enddate.set(endy, endm - 1, endd);
		this.cusip = cusip;
		this.h = h;
		
		//start database connection
		 c = null;
	      try {
	         Class.forName("org.postgresql.Driver");
	         c = DriverManager
	            .getConnection("jdbc:postgresql://localhost:5432/4121");
	      } catch (Exception e) {
	         e.printStackTrace();
	         System.err.println(e.getClass().getName()+": "+e.getMessage());
	         System.exit(0);
	      }
	      System.out.println("Opened database successfully");
	}
	
	
	public void endConnection () throws SQLException{
		c.close();
	}

	
	//build the list of relevant analysts
	public void buildAnalystList () throws SQLException{
		Statement s = this.c.createStatement();
		int year = enddate.get(Calendar.YEAR);
		int month = enddate.get(Calendar.MONTH) + 1;
		int day = enddate.get(Calendar.DAY_OF_MONTH);
		String dateexpr = String.format("%04d-%02d-%02d",year,month,day);
		enddate.add(Calendar.MONTH, -6);
		int startyear = enddate.get(Calendar.YEAR);
		int startmonth = enddate.get(Calendar.MONTH) + 1;
		int startday = enddate.get(Calendar.DAY_OF_MONTH);
		String startdateexpr = String.format("%04d-%02d-%02d",startyear,startmonth,startday);
		//find all analysts who rated the company in the last six months
		String sql = String.format("select * from recommendations where cusip = '%s' and ancdate < '%s'"
									+ " and ancdate >= '%s' order by analyst, ancdate desc;", 
									this.cusip, dateexpr, startdateexpr);
		enddate.add(Calendar.MONTH, 6);
		System.out.println(sql);
		ResultSet rs = s.executeQuery(sql);
		while (rs.next()){
			String analyst = rs.getString("analyst");
			//take only their latest recommendations
			if (analyst_to_reclvl.containsKey(analyst)) 
				continue;
			int reclvl = rs.getInt("reclvl");
			System.out.println (analyst+" "+Integer.toString(reclvl));
			analyst_to_reclvl.put(analyst, reclvl);
		}
	}
	
	public void evaluate_analysts () throws Exception{
		
		int starting_year = enddate.get(Calendar.YEAR) - 1;
		int month = enddate.get(Calendar.MONTH);
		int day = enddate.get(Calendar.DAY_OF_MONTH);
		String fmt = "%04d-%02d-%02d";
		Statement s = c.createStatement();
		ResultSet rs;
		for (Map.Entry <String,Integer> anpair : analyst_to_reclvl.entrySet()){
			
			//step 1
			//find ratings from the past year
			String begindateexpr = String.format(fmt,starting_year, month, day);
			String enddateexpr = String.format (fmt, enddate.get(Calendar.YEAR), month, day);
			String sql = "select count(*) as count from recommendations where ancdate >= '" + begindateexpr +
						 "' and ancdate < '" + enddateexpr +
						 "' and analyst = '" + anpair.getKey() +"'";
			rs = s.executeQuery(sql);
			rs.next();
			int num_ratings = rs.getInt ("count");
			
			if (num_ratings < num_ratings_threshold){
				ignored_analysts.add(anpair.getKey());
				continue;
			}
			sql = "select * from recommendations where ancdate >= '" + begindateexpr  +
					 "' and ancdate < '" + enddateexpr +
					 "' and analyst = '" + anpair.getKey() + "'";
			System.out.println (sql);
			rs = s.executeQuery(sql);
			double helpfulness = evaluate_analysts_specific (rs, anpair.getKey());
			System.out.println (anpair.getKey()+":"+helpfulness);
			analyst_to_helpfulness.put(anpair.getKey(), helpfulness);
		}
	}
	
	TreeMap <Date, TreeSet<String>> companies_needed_on_day = new TreeMap <Date, TreeSet<String>> ();
	protected abstract double evaluate_analysts_specific (ResultSet rs, String analyst) 
			throws Exception;

	protected TreeMap <String, Integer> analyst_to_reclvl = new TreeMap <String, Integer> ();
	protected TreeMap <String, Double> analyst_to_helpfulness = new TreeMap <String,Double> ();
	protected TreeSet <String> ignored_analysts = new TreeSet<String> ();
	protected HelpfulnessFinder h;
	//postgres stuff
	protected Connection c;
	//cusip of the stock of interest
	protected String cusip;
	//the last day with whose data to build the model
	//this is to allow using historical
	//data to check the model
	protected Calendar enddate;
	//how many times can someone be right about things
	//before I stop attributing it to chance? 5 will convince me
	private static final int num_ratings_threshold = 5;
}
