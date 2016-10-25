import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;

import modelbuilder.*;


public class Main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ArrayList <String> my_portfolio = new ArrayList <String> ();
		my_portfolio.add("03783310");
		my_portfolio.add("59491810");
		my_portfolio.add("45920010");
		my_portfolio.add("45814010");
		//for (;;){
			AnalystJudge j = new NormalReturn(2008, 10, 26, my_portfolio, new UpDownMan());
			j.buildAnalystList();
			j.evaluate_analysts();
			j.endConnection();
	//	}
	}

}
