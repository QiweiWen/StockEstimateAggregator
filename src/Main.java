import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;

import modelbuilder.*;


public class Main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		AnalystJudge j = new NormalReturn (2008,10,26,"59491810",new UpDownMan());
		j.buildAnalystList();
		j.evaluate_analysts();
	}

}
