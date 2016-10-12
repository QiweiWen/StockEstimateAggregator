package modelbuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

//how high is the company's market value at the end of
//the quarter compared to the beginning?
//(averaged over the first few days and last few days)
public class EndOfPeriod extends AnalystJudge {
	
	//numbers of days over which we avarage
	//the company's market value
	//at the beginning and end of the quarter
	private final int BEGIN_DAYS = 5;
	private final int END_DAYS = 5;
	
	public EndOfPeriod(int endy, int endm, int endd, String cusip,
			HelpfulnessFinder h) {
		super(endy, endm, endd, cusip, h);
		// TODO Auto-generated constructor stub
	}
/*
	private void add_to_needed (Date day, String cusip){
		if (companies_needed_on_day.get(day) == null){
			TreeSet <String> nl = new TreeSet <String> ();
			nl.add(cusip);
			companies_needed_on_day.put(day, nl);
		}else{
			companies_needed_on_day.get(day).add(cusip);
		}
	}
*/
	//private TreeMap <String, Integer> company_to_reclvl = new TreeMap <String, Integer> ();
	private StockPriceCache spc = new StockPriceCache();
	private TreeSet <String> no_such_file = new TreeSet <String> ();
	
	@Override
	protected double evaluate_analysts_specific(ResultSet rs, String analyst) 
			throws Exception {
		double res = 0;
		int predictions = 0;
		int correct_predictions = 0;
		//int cntr_debug = 0;
		//step 1: build the list of stock price entries needed
		while (rs.next()){
			//System.out.println (cntr_debug++);
			Statement s = c.createStatement();
			String company = rs.getString("cusip");
			if (no_such_file.contains(company)){
				continue;
			}
			int reclvl = rs.getInt("reclvl");
			String ancdate = rs.getString("ancdate");
			Date quarter_beginning = quarter_begin(ancdate, s);
			Date quarter_ending    = quarter_end  (ancdate, s);
			Calendar gc = new GregorianCalendar ();
			gc.setTime(quarter_beginning);
			double oldval = 0;
			boolean no_price_info = false;
			//System.out.println (company);
			for (int i = 0; i < BEGIN_DAYS; ++i){
				try{
					double mktval = spc.fetch(gc.getTime(),company);
					if (mktval == (double)-1){
						//throw new Exception ("fucked");
						continue;
					}else{
						//some price values from db is negative
						mktval = mktval > 0? mktval: (-1)*mktval;
					}
					oldval += mktval;
					gc.add(Calendar.DATE, 1);
					//System.out.println ("found file");
				}catch (FileNotFoundException e){
					//System.out.println ("does not exist");
					no_such_file.add(company);
					no_price_info = true;
					break;
				}
			}
			if (no_price_info){
				continue;
			}
			oldval /= (double)BEGIN_DAYS;
			
			double newval = 0;
			gc.setTime(quarter_ending);
			for (int i = 0; i < END_DAYS; ++i){
				double mktval = spc.fetch(gc.getTime(),company);
				if (mktval == (double)-1){
					//throw new Exception ("fucked");
					continue;
				}else{
					//some price values from db is negative
					mktval = mktval > 0? mktval: (-1)*mktval;
				}
				newval += mktval;
				gc.add(Calendar.DATE, -1);
			}
			newval /= (double)END_DAYS;
			boolean he_was_right = h.is_correct(reclvl, oldval, newval);
			
			//company_to_reclvl.put(company, reclvl);
			++predictions;
			if (he_was_right) ++correct_predictions;
		}
		res = (double)correct_predictions / (double) predictions;
		return res;
	}
}

