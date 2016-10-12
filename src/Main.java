import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;

import modelbuilder.*;


public class Main {

	public static void main(String[] args) throws IOException, ParseException {
		// TODO Auto-generated method stub
		
		AnalystJudge i = new EndOfPeriod(2012, 9, 2, "59491810", new UpDownMan());
		try {
			i.buildAnalystList();
			i.evaluate_analysts();
			i.endConnection();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
