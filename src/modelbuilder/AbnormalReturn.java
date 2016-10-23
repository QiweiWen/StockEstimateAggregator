package modelbuilder;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

public class AbnormalReturn extends AnalystJudge {
	private final int NUM_MONTHS = 6;
	public AbnormalReturn(int endy, int endm, int endd, String cusip,
			HelpfulnessFinder h) {
		super(endy, endm, endd, cusip, h);
	}

	private double get_abnormal_return (Calendar beginning, String cusip) throws SQLException{
		double result = 0;
		//"copy construction"
		Calendar c = Calendar.getInstance();
		c.setTime(beginning.getTime());
		//find the market values of the security
		//at the beginning and end of the six month period
		//and also the compound sp500 return, as an "expected return"
		int startyear = c.get(Calendar.YEAR);
		int startmonth = c.get(Calendar.MONTH) + 1;
		c.add(Calendar.MONTH, NUM_MONTHS);
		int endyear = c.get(Calendar.YEAR);
		int endmonth = c.get(Calendar.MONTH) + 1;
		assert (endyear - startyear<= 1);
		String priceformat;
		String query;
		if (endyear != startyear){
			priceformat = 
				"select * from monthlystock where ((year = %d and month >= %d)"
				+" or (year = %d and month <= %d)) and cusip = '%s' order by year, month;";
			query = String.format(priceformat, startyear, startmonth, endyear,endmonth,cusip);
		}else{
			priceformat = 
				"select * from monthlystock where (year = %d) and (month >= %d and month <= %d) and cusip = '%s' order by month;";
			query = String.format(priceformat, startyear, startmonth, endmonth,cusip);
		}
		
		//System.out.println(query);
		Statement s = this.c.createStatement();
		ResultSet rs = s.executeQuery(query);
		//variables of interest
		double spret = 1;
		int monthcount = 0;
		double oldmktval = 0, newmktval = 0, finalmktval = 0;
		
		while (rs.next()){
			
			newmktval = rs.getDouble("mktval");
			if (monthcount == 0){
				oldmktval = newmktval;
				++monthcount;
				continue;
			}
			double newspret = 1 + rs.getDouble("spret");
			spret *= newspret;
			++monthcount;
		}
		if (monthcount != NUM_MONTHS + 1){
			return (double)-1;
		}
		finalmktval = newmktval;
		
		//compute our definition of "abnormal return"
		double act_return = (finalmktval/oldmktval);
		result = act_return - spret;
		//String fucked = String.format("finalval: %f, initval: %f, spret: %f, result: %f",
			//							finalmktval, oldmktval, spret, result);
		//System.out.println(fucked);
		return result;
	}
	
	//rs: tuples from table "recommendations" that contains
	//all the recommendations made by the analyst in the last year
	//return value: helpfulness (0-1) of the analyst
	@Override
	protected double evaluate_analysts_specific(ResultSet rs, String analyst)
			throws Exception {
		int num_predictions = 0,
			num_correct = 0;
		// TODO Auto-generated method stub
		while (rs.next()){
			Date ancdate = rs.getDate("ancdate");
			Calendar c = Calendar.getInstance();
			c.setTime(ancdate);
			int reclvl = rs.getInt("reclvl");
			String cusip = rs.getString("cusip");
			double abn_return = get_abnormal_return (c, cusip);
			if (abn_return == (double) -1){
				continue;
			}
			boolean is_he_right = this.h.is_correct(reclvl, abn_return);
			if (is_he_right){
				++num_correct;
			}
			++num_predictions;
		}
		return (double)num_correct/num_predictions;
	}

}
