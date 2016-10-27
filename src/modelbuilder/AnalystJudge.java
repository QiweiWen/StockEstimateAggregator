package modelbuilder;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.postgresql.ds.PGPoolingDataSource;

import conformanceFinder.ConsensusCalc;


public abstract class AnalystJudge {
	public final int MAX_PORTFOLIO_SIZE = 20;
	public final int MIN_PORTFOLIO_SIZE = 1;
	public final int NUM_CPU = 4;
	
	
	public AnalystJudge (int endy, int endm, int endd, List <String> portfolio, HelpfulnessFinder h) throws Exception{
		enddate = Calendar.getInstance();
		enddate.set(endy, endm - 1, endd);
		if (portfolio.size() < MIN_PORTFOLIO_SIZE || portfolio.size() > MAX_PORTFOLIO_SIZE)
			throw new Exception ("too many or too few stocks");
		this.portfolio = portfolio;
		this.h = h;
		
		//start database connection
		 c = null;
	      try {
	         Class.forName("org.postgresql.Driver");
	         connpool = new PGPoolingDataSource();
	         connpool.setDataSourceName("mypool");
	         connpool.setServerName("localhost:5432");
	         connpool.setDatabaseName("4121");
	         connpool.setMaxConnections(NUM_CPU + 1);

	         this.c = connpool.getConnection();
	      } catch (Exception e) {
	         e.printStackTrace();
	         System.err.println(e.getClass().getName()+": "+e.getMessage());
	         System.exit(0);
	      }
	      System.out.println("Opened database successfully");
	}
	
	
	public void endConnection () throws SQLException{
		c.close();
		connpool.close();
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
	
	protected void sem_wait (Semaphore s){
		for (;;)
		try {
			s.acquire();
			break;
		} catch (InterruptedException e) {continue;}
	}
	
	protected void sem_post (Semaphore s){
		s.release();
	}
	
	private void put_reclvl (String analyst, String cusip, int reclvl, boolean threadsafe){
		if (threadsafe){
			sem_wait (sem_atocr);
			sem_wait (sem_ctoar);
		}
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
		if (threadsafe){
			sem_post (sem_ctoar);
			sem_post (sem_atocr);
		}
	}
	
	private boolean exist_reclvl (String analyst, String cusip, boolean threadsafe){
		if (threadsafe){
			sem_wait (sem_atocr);
		}
		if (!analyst_to_cusip_and_reclvl.containsKey(analyst)){
			if (threadsafe){
				sem_post (sem_atocr);
			}
			return false;
		}
		TreeMap <String,Integer> tm = analyst_to_cusip_and_reclvl.get(analyst);
		if (tm == null) {
			if (threadsafe){
				sem_post (sem_atocr);
			}
			return false;
		}
		if (threadsafe){
			sem_post (sem_atocr);
		}
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
			if (exist_reclvl(analyst,cusip,false)) 
				continue;
			int reclvl = rs.getInt("reclvl");
			//System.out.println (rs.getString("cusip") + " "+analyst+" "+Integer.toString(reclvl));
			//analyst_to_reclvl.put(analyst, reclvl);
			put_reclvl (analyst, cusip, reclvl,false);
		}
	}
	

	public void evaluate_analysts () throws Exception{
		
		class kthread implements Runnable{

			private LinkedList <String> arg;
			private Semaphore s;
			public kthread (LinkedList <String> arg, Semaphore s){
				this.arg = arg;
				this.s = s;
			}
			
			public void run() {
				try {
					AnalystJudge.this.thread_evaluate_analysts(arg, s);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		LinkedList <String> all_analysts = new LinkedList <String> ();
		for (Map.Entry <String,TreeMap<String,Integer>> anpair : analyst_to_cusip_and_reclvl.entrySet()){
			all_analysts.add(anpair.getKey());
		}
		Semaphore listsem = new Semaphore (1, true);
		
		LinkedList <Thread> threads = new LinkedList <Thread> ();
		for (int i = 0; i < NUM_CPU; ++i){
			Thread t = new Thread (new kthread(all_analysts, listsem));
			threads.add(i, t);
			t.setPriority(Thread.MAX_PRIORITY);
		}
		for (int i = 0; i < NUM_CPU; ++i){
			threads.get(i).start();
		}
		for (int i = 0; i < NUM_CPU; ++i){
			threads.get(i).join();
		}
		System.out.println (cache.hitrate());
	}
	
	public ConsensusCalc get_consensus_instance (){
		return new ConsensusCalc (analyst_to_cusip_and_reclvl,
								  cusip_to_analyst_and_reclvl,
								  analyst_to_helpfulness);
	}
	
	private void thread_evaluate_analysts(LinkedList<String> my_analysts, Semaphore sem) throws Exception{
		int starting_year = enddate.get(Calendar.YEAR) - 1;
		int month = enddate.get(Calendar.MONTH) + 1;
		int day = enddate.get(Calendar.DAY_OF_MONTH);
		String fmt = "%04d-%02d-%02d";
		
		Connection locl_c = connpool.getConnection();
		Statement s = locl_c.createStatement();
		ResultSet rs;
		boolean is_empty = false;
		int twat = 0;
		while (true){
			sem_wait (sem);
			String analyst = "";
			try{
				analyst = my_analysts.pop();
			}catch (Exception e){
				is_empty = true;
			}finally{
				sem_post (sem);
				if (is_empty){
					break;
				}
			}

			//step 1
			//find ratings from the past year
			String begindateexpr = String.format(fmt,starting_year, month, day);
			Calendar halfyearbefore = Calendar.getInstance();
			halfyearbefore.setTime(enddate.getTime());
			halfyearbefore.add(Calendar.MONTH, -6);
			
			String enddateexpr = String.format (fmt, halfyearbefore.get(Calendar.YEAR), 
													 halfyearbefore.get(Calendar.MONTH) + 1, 
													 halfyearbefore.get(Calendar.DAY_OF_MONTH));
			String sql = "select count(*) from (select cusip from recommendations where ancdate >= '" + begindateexpr +
						 "' and ancdate < '" + enddateexpr +
						 "' and analyst = '" + analyst +"' group by cusip) as foo";
			rs = s.executeQuery(sql);
			rs.next();
			System.out.println (sql);
			int num_ratings = rs.getInt ("count");
			//System.out.println(num_ratings);
			
			if (num_ratings < num_ratings_threshold){
				continue;
			}
			String queryfmt = "select company as cusip, recl as reclvl, ad as ancdate from "+ 
					"((select ancdate as ad, reclvl as recl, cusip as company from recommendations where ancdate >= '%s' and ancdate < '%s' and analyst = '%s') as allrec "+
					"join "+
					"(select cusip, max(ancdate) from recommendations where ancdate >= '%s' and ancdate < '%s' and analyst = '%s' group by cusip) as newest "+
					"on (allrec.ad = newest.max and allrec.company = newest.cusip)) as foo;";

			sql = String.format(queryfmt, begindateexpr, enddateexpr, analyst, begindateexpr, enddateexpr, analyst);
			
			//System.out.println (sql);
			
			rs = s.executeQuery(sql);
			double helpfulness = evaluate_analysts_specific (locl_c, rs, analyst);
			System.out.println (analyst+":"+helpfulness);
			sem_wait (sem_atoh);
			analyst_to_helpfulness.put(analyst, helpfulness);
			sem_post (sem_atoh);
			
		}
		locl_c.close();
	}
	
	
	protected abstract double evaluate_analysts_specific (Connection c, ResultSet rs, String analyst) 
			throws Exception;
	
	//acquire in this order
	private final Semaphore sem_atocr = new Semaphore(1, true);
	protected TreeMap <String, TreeMap<String, Integer>> analyst_to_cusip_and_reclvl = new TreeMap <String, TreeMap<String,Integer>> ();
	
	private final Semaphore sem_ctoar = new Semaphore(1, true);
	protected TreeMap <String, TreeMap<String, Integer>> cusip_to_analyst_and_reclvl = new TreeMap <String, TreeMap<String,Integer>> ();
	
	private final Semaphore sem_atoh = new Semaphore(1, true);
	private TreeMap <String, Double> analyst_to_helpfulness = new TreeMap <String,Double> ();
	
	protected HelpfulnessFinder h;
	//postgres stuff
	protected Connection c;
	protected PGPoolingDataSource connpool;
	//cusips of the stocks of interest
	protected List<String> portfolio;
	//the last day with whose data to build the model
	//this is to allow using historical
	//data to check the model
	protected Calendar enddate;
	//how many times can someone be right about things
	//before I stop attributing it to chance? 5 will convince me
	private static final int num_ratings_threshold = 5;
	private ConsensusCalc conform;
	
	protected Semaphore sem_cache = new Semaphore (1, true);
	protected MktvalCache cache = new MktvalCache();
}
