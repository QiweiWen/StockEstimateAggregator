package modelbuilder;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;

public abstract class AnalystJudge {
	public final int MAX_PORTFOLIO_SIZE = 20;
	public final int MIN_PORTFOLIO_SIZE = 1;
	
	public AnalystJudge (int endy, int endm, int endd, List <String> portfolio, HelpfulnessFinder h) throws Exception{
		enddate = Calendar.getInstance();
		enddate.set(endy, endm, endd);
		if (portfolio.size() < MIN_PORTFOLIO_SIZE || portfolio.size() > MAX_PORTFOLIO_SIZE)
			throw new Exception ("too many or too few stocks");
		this.portfolio = portfolio;
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

	
	protected String portfolio_to_sql(){
		String res = "(";
		int count = 0;
		for (String cusip: this.portfolio){
			if (count++ == 0){
				res+= String.format("cusip = '%s' ", cusip);
			}else{
				res += String.format ("or cusip = '%s' ", cusip);
			}
		}
		res += ")";
		return res;
	}
	
	private void put_reclvl (String analyst, String cusip, int reclvl){
		TreeMap <String, Integer> cusip_to_reclvl = 
				analyst_to_cusip_and_reclvl.get(analyst);
		if (cusip_to_reclvl == null){
			TreeMap <String,Integer> ntm = new  TreeMap<String,Integer>();
			ntm.put(cusip, reclvl);
			analyst_to_cusip_and_reclvl.put(analyst, ntm);
		}else{
			cusip_to_reclvl.put(cusip, reclvl);
		}
		
		TreeMap <String,Integer> analyst_to_reclvl = 
				cusip_to_analyst_and_reclvl.get(cusip);
		if (analyst_to_reclvl == null){
			TreeMap <String,Integer> ntm = new  TreeMap<String,Integer>();
			ntm.put(analyst, reclvl);
			cusip_to_analyst_and_reclvl.put(cusip, ntm);
		}else{
			analyst_to_reclvl.put(analyst,reclvl);
		}
	}
	
	private boolean exist_reclvl (String analyst, String cusip){
		if (!analyst_to_cusip_and_reclvl.containsKey(analyst)){
			return false;
		}
		TreeMap <String,Integer> tm = analyst_to_cusip_and_reclvl.get(analyst);
		if (tm == null) return false;
		return tm.containsKey(cusip);
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
		String sql = String.format("select * from recommendations where %s and ancdate < '%s'"
									+ " and ancdate >= '%s' order by analyst, cusip, ancdate desc;", 
									portfolio_to_sql(), dateexpr, startdateexpr);
		enddate.add(Calendar.MONTH, 6);
		//System.out.println(sql);
		ResultSet rs = s.executeQuery(sql);
		while (rs.next()){
			String analyst = rs.getString("analyst");
			String cusip = rs.getString("cusip");
			//take only their latest recommendations
			if (exist_reclvl(analyst,cusip)) 
				continue;
			int reclvl = rs.getInt("reclvl");
			//System.out.println (rs.getString("cusip") + " "+analyst+" "+Integer.toString(reclvl));
			//analyst_to_reclvl.put(analyst, reclvl);
			put_reclvl (analyst, cusip, reclvl);
		}
	}
	
	public void evaluate_analysts () throws Exception{
		
		int starting_year = enddate.get(Calendar.YEAR) - 1;
		int month = enddate.get(Calendar.MONTH);
		int day = enddate.get(Calendar.DAY_OF_MONTH);
		String fmt = "%04d-%02d-%02d";
		Statement s = c.createStatement();
		ResultSet rs;
		for (Map.Entry <String,TreeMap<String,Integer>> anpair : analyst_to_cusip_and_reclvl.entrySet()){
			
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
			//System.out.println(num_ratings);
			
			if (num_ratings < num_ratings_threshold){
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
	
	
	protected abstract double evaluate_analysts_specific (ResultSet rs, String analyst) 
			throws Exception;
	
	protected TreeMap <String, TreeMap<String, Integer>> analyst_to_cusip_and_reclvl = new TreeMap <String, TreeMap<String,Integer>> ();
	protected TreeMap <String, TreeMap<String, Integer>> cusip_to_analyst_and_reclvl = new TreeMap <String, TreeMap<String,Integer>> ();
	private TreeMap <String, Double> analyst_to_helpfulness = new TreeMap <String,Double> ();
	
	protected HelpfulnessFinder h;
	//postgres stuff
	protected Connection c;
	//cusips of the stocks of interest
	protected List<String> portfolio;
	//the last day with whose data to build the model
	//this is to allow using historical
	//data to check the model
	protected Calendar enddate;
	//how many times can someone be right about things
	//before I stop attributing it to chance? 5 will convince me
	private static final int num_ratings_threshold = 5;
}
