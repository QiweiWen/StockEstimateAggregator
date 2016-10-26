package conformanceFinder;
import java.util.*;

//implements formulae as shown in the paper
public class ConsensusCalc {

	public ConsensusCalc (TreeMap<String, TreeMap<String, Integer>> analyst_to_cusip_and_reclvl, 
						  TreeMap<String, TreeMap<String, Integer>> cusip_to_analyst_and_reclvl, 
						  TreeMap<String, Double> analyst_to_helpfulness)
	{
		this.atocr =  analyst_to_cusip_and_reclvl;
		this.ctoar =  cusip_to_analyst_and_reclvl;
		this.helpfulness =  analyst_to_helpfulness;
		this.ctora = new TreeMap <String, ArrayList <LinkedList <String>>> ();
		for (Map.Entry<String, TreeMap<String,Integer>> me : ctoar.entrySet()){
			String cusip = me.getKey();
			TreeMap <String,Integer> ator = me.getValue();
			ArrayList <LinkedList <String>> newentry = 
					new ArrayList <LinkedList <String>> (Collections.nCopies(5, new LinkedList <String> ()));
			
			for (Map.Entry<String, Integer> ar_entry: ator.entrySet()){
				String analyst = ar_entry.getKey();
				Integer reclvl = ar_entry.getValue();
				newentry.get(reclvl - 1).add(analyst);
			}
			ctora.put(cusip, newentry);
		}
	}

	private List <Double> to_vector_rou (TreeMap <String, ArrayList <Double>> rou){
		LinkedList <Double> vecrou = new LinkedList <Double>();
		//important to iterate in order or won't converge
		for (Map.Entry<String, ArrayList<Double>> me: rou.entrySet()){
			ArrayList <Double> candidates = me.getValue();
			vecrou.addAll(candidates);
		}
		return vecrou;
	}
	
	
	private double get_epsilon (List <Double> rou1, List <Double> rou2){
		if (rou1.size() != rou2.size()){
			return (double) -1;
		}
		double res = 0;
		Iterator<Double> itr1 = rou1.iterator();
		Iterator<Double> itr2 = rou2.iterator();
		for (int i = 0; i < rou1.size();++i){
			double d1 = itr1.next(),
				   d2 = itr2.next();
			res += (d1 - d2) * (d1 - d2);
		}
		res = Math.sqrt(res);
		return res;
	}
	
	//iterate until convergence
	public void converge (){
		TreeMap <String, Double> Tr = new TreeMap <String,Double> ();
		TreeMap <String, ArrayList <Double>> rou = new TreeMap <String, ArrayList<Double>> ();
		//initialise Tr;
		for (Map.Entry<String, Double> me : helpfulness.entrySet()){
			Tr.put(me.getKey(), (double)1);
		}
		//initialise rouLI
		for (Map.Entry<String, TreeMap <String,Integer>> me: ctoar.entrySet()){
			String cusip = me.getKey();
			ArrayList <LinkedList <String>> reclvl_to_analyst = ctora.get(cusip);
			ArrayList <Double> benefits = new ArrayList <Double>(Collections.nCopies(5, (double)0));
			double sum = 0;
			for (int i = 0; i < 5; ++i){
				sum = 0;
				//analysts who voted on this list, for this level
				LinkedList <String> analysts = reclvl_to_analyst.get(i);
				for (String analyst: analysts){
					sum += helpfulness.get(analyst);
				}
				benefits.add(i, sum);
			}
			//compute common denominator
			double common_denominator = 0;
			for (int i = 0; i < 5; ++i){
				common_denominator += benefits.get(i)*benefits.get(i);
			}
			common_denominator = Math.sqrt(common_denominator);
			//compute individual rouLI
			for (int i = 0; i < 5; ++i){
				double numerator = benefits.get(i);
				if (rou.get(cusip) == null){
					rou.put(cusip, new ArrayList <Double> (Collections.nCopies(5, (double)0)));
				}
				rou.get(cusip).add(i, numerator/common_denominator);
			}
		}
		double discrepency = 0;
		TreeMap <String, ArrayList <Double>> benefits = new TreeMap <String, ArrayList<Double>> ();
		//start spinning
		do{
			List <Double> initrou = to_vector_rou (rou);
			//<do stuff>
			
			/*
			 *  To avoid having to copy stuff around or, perish the thought, to calculate things twice
			 *  compute in this order: beta->t->rou
			 *  
			 */
			
			//step 1. compute benefits
			for (Map.Entry<String, ArrayList <LinkedList <String>>> me: ctora.entrySet()){
				String cusip = me.getKey();
				ArrayList <LinkedList <String>> votes = me.getValue();
				//Compute benefits from krlTr
				for (int i = 0; i < 5; ++i){
					double krltr = 0;
					LinkedList <String> analysts = votes.get(i);
					for (String analyst: analysts){
						double krl = helpfulness.get(analyst);
						double tr  = Math.pow(Tr.get(analyst),this.a);
						krltr += krl*tr;
					}
					ArrayList <Double> candidate_benefits = null;
					if (!benefits.containsKey(cusip)){
						candidate_benefits = new ArrayList <Double> (Collections.nCopies(5, (double)0));
						benefits.put(cusip, candidate_benefits);
					}else{
						candidate_benefits = benefits.get(cusip);
					}
					candidate_benefits.add(i, candidate_benefits.get(i) + krltr); 
				}
				//compute benefits from the second part of the equation
				TreeMap<String,Integer> cusip_rating_set = ctoar.get(cusip);
				for (int i = 0; i < 5; ++i){
					double running_sum = 0;
					for (Map.Entry<String, Integer> analyst_reclvl_tuple : cusip_rating_set.entrySet()){
						double k = helpfulness.get(analyst_reclvl_tuple.getKey());
						double unhelpfulness = (1-k);
						double oldrou = rou.get(cusip).get(i);
						oldrou = Math.pow(oldrou, this.m);
						double tr = Tr.get(analyst_reclvl_tuple.getKey());
						tr = Math.pow(tr, this.a);
						running_sum += (unhelpfulness * oldrou * tr);
					}
					ArrayList <Double> candidate_benefits = benefits.get(cusip);
					candidate_benefits.add(i, candidate_benefits.get(i) + running_sum);
				}
			}
			
			//step 2. compute conformance levels
			
			
			//<\do stuff>
			List <Double> finalrou = to_vector_rou (rou);
			discrepency = get_epsilon(initrou, finalrou);
			assert (discrepency != (double)-1);
		}while (discrepency >= epsilon);
	}
	
	//constants controlling the weight
	private final int a = 2, m = 3;
	//we set the convergence threshold as 10e-5
	private final double epsilon = 0.00001;
	
	private TreeMap <String, TreeMap <String, Integer>> atocr;
	private TreeMap <String, TreeMap <String, Integer>> ctoar;
	//derived from the input
	//given cusip and reclvl, return list of analysts
	//needed for all the r->li summings
	private TreeMap <String, ArrayList <LinkedList <String>>> ctora;
	
	//not every key in atocr appear here
	//use this to get list of analysts
	private TreeMap <String, Double>      helpfulness;
	
}

