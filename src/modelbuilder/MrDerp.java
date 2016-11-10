package modelbuilder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


//the algorithm calculates consensus only and disregards helpfulness
public class MrDerp extends AnalystJudge {
	public MrDerp(int endy, int endm, int endd, List <String> portfolio, HelpfulnessFinder h) throws Exception {
		super(endy, endm, endd, portfolio, h);
	}

	private boolean do_it (float num){
		
		return true;
	}
	
	protected double evaluate_analysts_specific (Connection locl_c, ResultSet rs, String analyst) throws IOException, SQLException{
		Random unidist = new Random();
		//int sum = 0, tot = 0;
		while (rs.next()){
			String cusip = rs.getString("cusip");
			int reclvl = rs.getInt("reclvl");
			if (do_it(unidist.nextFloat())){
				//++sum;
				put_reclvl(analyst, cusip, reclvl, true);
			}
		//	++tot;
		}
		//System.out.println ("added "+sum+" ratings for analyst " +analyst +" out of "+tot);
		return 1;
	}
}
