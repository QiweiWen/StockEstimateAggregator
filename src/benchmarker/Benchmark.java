package benchmarker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;

import conformanceFinder.ConsensusCalc;

import benchmarker.*;
import modelbuilder.*;

public abstract class Benchmark {
	private final int MONTHS_PER_TRADE = 2;
	public Benchmark (boolean derp,
				      int starty, int startm, int startd,
				      int mparam, int aparam,
				      BufferedReader pFile, BufferedWriter bw,
				      Benchmark_options opt,
				      String opponent, int num_months) throws Exception
	{
		this.opt = opt;
		this.opponent = opponent;
		this.start_year = starty;
		this.start_month = startm;
		this.start_day = startd;
		this.months_to_run = num_months;
		this.derp = derp;
		this.output_file = bw;
		this.aparam = aparam;
		this.mparam = mparam;
		//build the portfolio list
		portfolio = new LinkedList <String> ();
		String line;
		while ((line = pFile.readLine()) != null){
			//the file should contain cusips
			//which are 8 char long without the checksum
			if (line.length() != 8){
				throw new IOException ("Wrong CUSIP format");
			}
			portfolio.add(line);
		}
	
		if (derp){
			judge = new MrDerp (starty, startm, startd, portfolio, null);
		} else{
			judge = new NormalReturn (starty, startm, startd, portfolio, new UpDownMan());
		}
		judge.set_parameters(aparam, mparam);
	}
	
	private int get_opponent_reclvl(Calendar end,String cusip) throws SQLException{
		int res = 0;
		String datefmt = "%04d%02d%02d";
		String begindateexpr,
			   enddateexpr;
		Calendar c = Calendar.getInstance();
		c.setTime(end.getTime());
		String sqlfmt,sqlquery;
		Connection sqlconnection
			= judge.get_connection();
		Statement s = sqlconnection.createStatement();
		ResultSet rs;
		enddateexpr = String.format(datefmt, c.get(Calendar.YEAR),
		  		 							 c.get(Calendar.MONTH) + 1, 
		  		 							 c.get(Calendar.DAY_OF_MONTH));
		c.add(Calendar.MONTH, -6);
		begindateexpr = String.format(datefmt, c.get(Calendar.YEAR),
											   c.get(Calendar.MONTH) + 1,
											   c.get(Calendar.DAY_OF_MONTH));
		if (opt == Benchmark_options.analyst){
			sqlfmt = "select * from recommendations where analyst = '%s' and cusip = '%s' and ancdate >= '%s' and ancdate < '%s' order by ancdate desc limit(1)";
			sqlquery = String.format(sqlfmt, this.opponent, cusip, begindateexpr,enddateexpr);
			System.out.println (sqlquery);
			rs = s.executeQuery(sqlquery);
			
			if (rs.next()){
				res = rs.getInt("reclvl");
			}else{
				//if no updates in the past half-year, hold
				res = 3;
			}
			
		}else{
			
			String whereclause = String.format("(cusip = '%s' and ancdate >= '%s' and ancdate < '%s')", 
												cusip, begindateexpr, enddateexpr);
			sqlfmt = "select ana as analyst, recl as reclvl from "+
					 "((select ancdate as ad, reclvl as recl, analyst as ana from recommendations where %s) as allrec "+ 
					 "join "+
					 "(select analyst, max (ancdate) as maxy from recommendations where %s group by analyst) as newest "+
					 "on allrec.ad = newest.maxy and allrec.ana = newest.analyst) as foo;";
			sqlquery = String.format(sqlfmt, whereclause, whereclause);
			rs = s.executeQuery(sqlquery);
			LinkedList <Integer> recs = new LinkedList <Integer> ();
			while (rs.next()){
				recs.add(rs.getInt("reclvl"));
			}
			Collections.sort(recs);
			if (opt == Benchmark_options.mean){
				//System.out.println ("MEAN:");
				//System.out.println (recs);
				int sum = 0;
				for (Integer rec: recs){
					sum += rec;
				}
				res = (int) ((double)sum/(double)recs.size());
			}else if (opt == Benchmark_options.median){
				//System.out.println ("MEDIAN:");
				//System.out.println (recs);
				if (recs.size() % 2 == 0){
					int one = recs.get(recs.size()/2 - 1);
					int other = recs.get(recs.size()/2);
					res = (int)(((double)one + other)/2);
				}else{
					res =  recs.get(recs.size()/2);
				}
			}
		}
		
		sqlconnection.close();
		return res;
	}
	//given recommendation, what do we do?
	protected abstract int reclvl_to_share (int reclvl);
	
	//negative shares = sell
	//positive shares = buy
	//returns change to balance;
	private double trade (Calendar c, int shares, String cusip) throws SQLException{
		double res = 0;
		Connection connie = judge.get_connection();
		Statement s = connie.createStatement();
		String condstr = String.format("(cusip = '%s' and year = '%d' and month = '%d')",
									   cusip,c.get(Calendar.YEAR),c.get(Calendar.MONTH) + 1);
		String sqlquery = String.format("select * from monthlystock where %s;",condstr);
		ResultSet rs = s.executeQuery(sqlquery);
		rs.next();
		double price = 0;
		if (shares > 0){
			//get ask price
			price = rs.getDouble("closask");
			//System.out.println (String.format("buying %d shares at %f", shares, price));
		}else{
			//get bid price
			price = rs.getDouble("closbid");
			//System.out.println (String.format("selling %d shares at %f", -1*shares, price));
		}
		res = -1 * price * shares;
		connie.close();
		return res;
	}
	
	private double sell_everything (Calendar c, TreeMap <String, Integer> shares) throws SQLException{
		double res = 0;
		for (String stock: portfolio){
			int sharesleft = shares.get(stock);
			if (sharesleft > 0){
				res += trade (c, -1 * sharesleft, stock);
			}
		}
		return res;
	}
	
	private void make_report (double my, double his) throws IOException{
		if (derp){
			output_file.write("Conformity Only");
		}else{
			output_file.write("With Helpfulness Input");
		}
		output_file.write("\n");
		output_file.write(String.format("Using model parameters a = %d, m = %d\n", this.aparam, this.mparam));
		
		output_file.write(String.format("Started trading on %04d-%02d-%02d for %d months\n",
										this.start_year, this.start_month, this.start_day, this.months_to_run));
		output_file.write("======\nStock Portfolio:\n");
		for (String stock: this.portfolio){
			output_file.write(stock + "\n");
		}
		output_file.write("======\nBenchmarked Against:");
		if (this.opt == Benchmark_options.mean){
			output_file.write("Simple mean\n");
		}else if (this.opt == Benchmark_options.median){
			output_file.write("Simple median\n");
		}else {
			output_file.write(this.opponent + "\n");
		}
		output_file.write(String.format("Resulting Balance: $%f for us, $%f for the opponent\n",my,his));	
	}
	
	public void run_bench() throws Exception{
		int months_elapsed = 0;
		Calendar c = Calendar.getInstance();
		c.set(start_year, start_month - 1, start_day);
		//assume unlimited funding
		//so the balance can go negative
		double opponent_balance = 0,
			   my_balance = 0;
		TreeMap <String, Integer> my_shares =
				new TreeMap <String,Integer> ();
		TreeMap <String, Integer> opponent_shares =
				new TreeMap <String,Integer> ();
		for (String stock: portfolio){
			my_shares.put(stock, 100);
			opponent_shares.put(stock, 100);
		}
		//trade every MONTHS_PER_TRADE many months,
		//sell everything at the end
		//calculate profit or loss
		int opponent_reclvl,
			consensus_reclvl;
		ConsensusCalc cons;
		while (months_elapsed < months_to_run){
			judge.rewind();
			judge.set_date(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, 
						   c.get(Calendar.DAY_OF_MONTH));
			judge.buildAnalystList();
			judge.evaluate_analysts();
			cons = judge.get_consensus_instance();
			cons.converge();
			for (String stock: portfolio){
				opponent_reclvl = get_opponent_reclvl(c,stock);
				consensus_reclvl = cons.get_winner(stock);
				int mytrade = reclvl_to_share(consensus_reclvl);
				int opponenttrade = reclvl_to_share (opponent_reclvl);
				System.out.println (opponent_reclvl+ " " + consensus_reclvl);
				//can't sell more than we own
				int my_existing = my_shares.get(stock);
				if (my_existing + mytrade < 0){
					mytrade = -1 * my_existing;
				}
				int opponent_existing = opponent_shares.get(stock);
				if (opponent_existing + opponenttrade < 0){
					opponenttrade = -1* opponent_existing;
				}
				//adjust shares held
				my_shares.put(stock, my_existing + mytrade);
				opponent_shares.put(stock, opponent_existing + opponenttrade);
				//adjust balance
				my_balance += trade (c, mytrade, stock);
				opponent_balance += trade (c, opponenttrade, stock);
			}
			c.add(Calendar.MONTH, MONTHS_PER_TRADE);
			months_elapsed += MONTHS_PER_TRADE;
			System.out.println("my balance "+my_balance+",\nopponent balance "+opponent_balance);
		}
		//sell everything
		my_balance += sell_everything (c, my_shares);
		opponent_balance += sell_everything (c, opponent_shares);
		//System.out.println("my final balance "+my_balance+",\nopponent final balance "+opponent_balance);
		make_report(my_balance, opponent_balance);
	}
	
	private AnalystJudge judge;
	private int start_year,
				start_month,
				start_day;
	private Benchmark_options opt;
	private String opponent;
	private int months_to_run;
	private LinkedList <String> portfolio;
	private BufferedWriter output_file;
	private boolean derp;
	private int aparam, mparam;
}
