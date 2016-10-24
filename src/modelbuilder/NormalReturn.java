package modelbuilder;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;

public class NormalReturn extends AnalystJudge{

	public NormalReturn(int endy, int endm, int endd, List <String> portfolio,
			HelpfulnessFinder h) throws Exception {
		super(endy, endm, endd, portfolio, h);
		// TODO Auto-generated constructor stub
	}

	private double get_normal_return (Calendar beginning, String cusip) throws SQLException{
		double result = 0;
		//"copy construction"
		Calendar c = Calendar.getInstance();
		c.setTime(beginning.getTime());
		//find the market values of the security
		//at the beginning and end of the six month period
		//and also the compound sp500 return, as an "expected return"
		int startyear = c.get(Calendar.YEAR);
		int startmonth = c.get(Calendar.MONTH) + 1;
		c.add(Calendar.MONTH, 6);
		int endyear = c.get(Calendar.YEAR);
		int endmonth = c.get(Calendar.MONTH) + 1;
		String queryformat = "select * from monthlystock " +
				"where year = %d and month = %d and cusip = '%s';";
		String startingquery = String.format(queryformat, startyear,startmonth,cusip);
		String endingquery   = String.format(queryformat, endyear, endmonth, cusip);
		//System.out.println(startingquery);
		//System.out.println(endingquery);
		Statement s = this.c.createStatement();
		ResultSet rs = s.executeQuery(startingquery);
		if (!rs.next()){
			return (double) -1;
		}
		double startmktval = rs.getDouble("mktval");
		rs = s.executeQuery(endingquery);
		if (!rs.next()){
			return (double) -1;
		}
		double endmktval = rs.getDouble("mktval");
		result = endmktval/startmktval - 1;
		//System.out.println("beg: "+startmktval+"end: "+endmktval);
		return result;
	}
	
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
			double nml_return = get_normal_return (c, cusip);
			if (nml_return == (double) -1){
				continue;
			}
			boolean is_he_right = this.h.is_correct(reclvl, nml_return);
			if (is_he_right){
				++num_correct;
			}
			++num_predictions;
		}
		return (double)num_correct/num_predictions;
	}

}
