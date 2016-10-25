package conformanceFinder;
import java.util.*;

//implements formulae as shown in the paper
public class consensusCalc {
	//(analyst)->(cusip, reclvl) or (cusip)->(analyst, reclvl)
	public class RatingMap extends TreeMap <String, TreeMap <String, Integer>>{
		private static final long serialVersionUID = 1L;}
	//(analyst->helpfulness)
	public class KMap extends TreeMap <String, Double> {
		private static final long serialVersionUID = 1L;}
	
	public consensusCalc (TreeMap<String, TreeMap<String, Integer>> analyst_to_cusip_and_reclvl, 
						  TreeMap<String, TreeMap<String, Integer>> cusip_to_analyst_and_reclvl, 
						  TreeMap<String, Double> analyst_to_helpfulness)
	{
		this.atocr = (RatingMap) analyst_to_cusip_and_reclvl;
		this.ctoar = (RatingMap) cusip_to_analyst_and_reclvl;
		this.helpfulness = (KMap) analyst_to_helpfulness;
	}

	
	//iterate until convergence
	public void converge (){
		
	}
	
	//constants controlling the weight
	private final int a = 2, m = 3;
	//we set the convergence threshold as 10e-5
	private final double epsilon = 0.00001;
	
	private RatingMap atocr;
	private RatingMap ctoar;
	private KMap      helpfulness;
	
}

