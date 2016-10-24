package modelbuilder;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//A very confused gentleman who does not have much of an idea
//to whom he should listen, so everyone is equally helpful to him
//
//that is, the algorithm calculates consensus only and disregards helpfulness
public class MrDerp extends AnalystJudge {
	public MrDerp(int endy, int endm, int endd, List <String> portfolio, HelpfulnessFinder h) throws Exception {
		super(endy, endm, endd, portfolio, h);
	}

	protected double evaluate_analysts_specific (ResultSet rs, String analyst) throws IOException{
		return 1;
	}
}
