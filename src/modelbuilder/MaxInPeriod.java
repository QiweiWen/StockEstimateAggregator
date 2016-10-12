package modelbuilder;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MaxInPeriod extends AnalystJudge{
	public MaxInPeriod(int endy, int endm, int endd, String cusip,HelpfulnessFinder h) {
		super(endy, endm, endd, cusip,h);
	}
	//to evaluate an analyst look, for the past year,
	//at the maximum market values of companies they recommended
	//within the fiscal period (quarter) of the recommendation

	//WARNING: probably very slow
	protected double evaluate_analysts_specific (ResultSet rs, String analyst) throws SQLException, IOException{
		System.err.println ("Implement me!");
		return 0;
	}
}
