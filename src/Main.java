import java.util.TreeMap;

import modelbuilder.*;
import conformanceFinder.*;

public class Main {

	private static void put_reclvl(TreeMap <String, TreeMap<String, Integer>> atocr,
								   TreeMap <String, TreeMap<String, Integer>> ctoar,
								   String analyst, String cusip, int reclvl)
	{
		if (!atocr.containsKey(analyst)){
			atocr.put(analyst, new TreeMap <String, Integer>());
		}
		if (!ctoar.containsKey(cusip)){
			ctoar.put(cusip, new TreeMap <String, Integer>());
		}
		atocr.get(analyst).put(cusip, reclvl);
		ctoar.get(cusip).put(analyst, reclvl);
	}
	
	private final static String[] portfolio
		= {"03783310","59491810","45920010","45814010"};
	
	private final static String[] cunts
		= {"A","B","C","D","E"};
	
	private final static Integer[][] fucked
		= {{5,2,1,4},{3,4,3,4},{1,2,1,3},{2,2,2,2},{5,1,5,2}};
	
	private final static Double[] k
		= {(double) 1,(double) 1,(double)1,(double) 1,(double) 1};
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		/*
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
		*/
		TreeMap <String, TreeMap<String, Integer>> atocr
			= new TreeMap <String, TreeMap<String, Integer>>();
		TreeMap <String, TreeMap<String, Integer>> ctoar
			= new TreeMap <String, TreeMap<String, Integer>>();
		//populate the data structures with test rating data
		for (int i = 0; i < 5; ++i){
			for (int j = 0; j < 4; ++j){
				String analyst = cunts[i];
				String cusip = portfolio[j];
				Integer cuntlvl = fucked[i][j];
				put_reclvl (atocr, ctoar, analyst, cusip, cuntlvl);
			}
		}
		TreeMap <String, Double> krl
			= new TreeMap <String, Double>();
		for (int i = 0; i < 5; ++i){
			String analyst = cunts[i];
			krl.put(analyst, k[i]);
		}
		
		ConsensusCalc c = new ConsensusCalc (atocr, ctoar, krl);
		c.converge();
		System.out.println (c.get_winner("45814010"));
	}

}
