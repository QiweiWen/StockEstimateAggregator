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
			//System.out.println (ator);
			ArrayList <LinkedList <String>> newentry = 
					new ArrayList <LinkedList <String>> ();
			for (int i = 0; i < 5; ++i){
				newentry.add(new LinkedList<String>());
			}
			//System.out.println ("---------------");
			for (Map.Entry<String, Integer> ar_entry: ator.entrySet()){
				String analyst = ar_entry.getKey();
				if (!helpfulness.containsKey(analyst)){
					continue;
				}
				Integer reclvl = ar_entry.getValue();
				newentry.get(reclvl - 1).add(analyst);
				//System.out.println (newentry);
			}
			ctora.put(cusip, newentry);
		}
	}

	private List <Double> to_vector_rou (TreeMap <String, ArrayList <Double>> rou){
		LinkedList <Double> vecrou = new LinkedList <Double>();
		//System.out.println (rou.size());
		//important to iterate in order or won't converge
		for (Map.Entry<String, ArrayList<Double>> me: rou.entrySet()){
			ArrayList <Double> candidates = me.getValue();
			//System.out.println (candidates.size());
			vecrou.addAll(candidates);
		}
		//System.out.println(vecrou.size());
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
	
	public int get_winner (String cusip){
		int res = 0;
		ArrayList <Double> scores = rou.get(cusip);
		System.out.println (scores);
		double max = 0;
		int maxindex = 0;
		for (int i = 0; i < 5; ++i){
			double cand = scores.get(i);
			if (cand >= max){
				maxindex = i;
				max = cand;
			}
		}
		return maxindex + 1;
	}
	
	public void set_parameters (int newa, int newm){
		if ((newa > 0 && newm > 0)){
			this.a = newa;
			this.m = newm;
		}
	}
	
	//iterate until convergence
	//ctoar and atocr contain a superset of analysts found in helpfulness,
	//hence the many checks
	public void converge (){
		TreeMap <String, Double> Tr = new TreeMap <String,Double> ();
		rou = new TreeMap <String, ArrayList<Double>> ();
		//initialise Tr;
		for (Map.Entry<String, Double> me : helpfulness.entrySet()){
			Tr.put(me.getKey(), (double)1);
		}
		//initialise rouLI
		for (Map.Entry<String, TreeMap <String,Integer>> me: ctoar.entrySet()){
			String cusip = me.getKey();
			ArrayList <LinkedList <String>> reclvl_to_analyst = ctora.get(cusip);
			//System.out.println (reclvl_to_analyst);
			ArrayList <Double> benefits = new ArrayList <Double>(Collections.nCopies(5, (double)0));
			double sum = 0;
			for (int i = 0; i < 5; ++i){
				sum = 0;
				//analysts who voted on this list, for this level
				LinkedList <String> analysts = reclvl_to_analyst.get(i);
				
				for (String analyst: analysts){
					if (!helpfulness.containsKey(analyst)) continue;
					sum += helpfulness.get(analyst);
				}
				benefits.set(i, sum);
			}
			//compute common denominator
			double common_denominator = 0;
			for (int i = 0; i < 5; ++i){
				common_denominator += benefits.get(i)*benefits.get(i);
			}
			//System.out.println(common_denominator);
			common_denominator = Math.sqrt(common_denominator);
			
			//compute individual rouLI
			for (int i = 0; i < 5; ++i){
				double numerator = benefits.get(i);
				if (rou.get(cusip) == null){
					rou.put(cusip, new ArrayList <Double> (Collections.nCopies(5, (double)0)));
				}
				rou.get(cusip).set(i, numerator/common_denominator);
			}
		}
		double discrepency = 0;
		TreeMap <String, ArrayList <Double>> benefits = new TreeMap <String, ArrayList<Double>> ();
		int iteration_count = 0;
		//start spinning
		do{
			List <Double> initrou = to_vector_rou (rou);
			//System.out.println (rou);
			//<do stuff>
			
			/*
			 *  To avoid having to copy stuff around or, perish the thought, to calculate things twice
			 *  compute in this order: t->beta->rou
			 *  
			 */
			
	
			//step 1. compute Tr
			for (Map.Entry<String, Double> me: helpfulness.entrySet()){
				
				TreeMap <String,Integer> c_to_r = atocr.get(me.getKey());
				double running_sum = 0;
				double my_k = me.getValue();
				for (Map.Entry<String, Integer> crtuple: c_to_r.entrySet()){
					//compute sum k*rouLI
					String cusip = crtuple.getKey();
					Integer reclvl = crtuple.getValue();
					double oldrou = rou.get(cusip).get(reclvl - 1);
					running_sum += oldrou*my_k;
					
					//compute the second part of the Tr sum
					double common_factor = (1 - my_k)/(this.m + 1);
					for (int i = 0; i < 5;++i){
						double rouLJ = rou.get(cusip).get(i);
						rouLJ = Math.pow(rouLJ, (this.m + 1));
						running_sum += rouLJ * common_factor;
					}
				}
				Tr.put(me.getKey(), running_sum);
			}
			//step 2. compute benefits
			for (Map.Entry<String, ArrayList <LinkedList <String>>> me: ctora.entrySet()){
				String cusip = me.getKey();
				ArrayList <LinkedList <String>> votes = me.getValue();
				//Compute benefits from krlTr
				for (int i = 0; i < 5; ++i){
					double krltr = 0;
					LinkedList <String> analysts = votes.get(i);
					for (String analyst: analysts){
						if (!helpfulness.containsKey(analyst)) continue;
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
					candidate_benefits.set(i, candidate_benefits.get(i) + krltr); 
				}
				//compute benefits from the second part of the equation
				TreeMap<String,Integer> cusip_rating_set = ctoar.get(cusip);
				for (int i = 0; i < 5; ++i){
					double running_sum = 0;
					for (Map.Entry<String, Integer> analyst_reclvl_tuple : cusip_rating_set.entrySet()){
						if (!helpfulness.containsKey(analyst_reclvl_tuple.getKey())) continue;
						double k = helpfulness.get(analyst_reclvl_tuple.getKey());
						double unhelpfulness = (1-k);
						double oldrou = rou.get(cusip).get(i);
						oldrou = Math.pow(oldrou, this.m);
						double tr = Tr.get(analyst_reclvl_tuple.getKey());
						tr = Math.pow(tr, this.a);
						running_sum += (unhelpfulness * oldrou * tr);
					}
					ArrayList <Double> candidate_benefits = benefits.get(cusip);
					candidate_benefits.set(i, candidate_benefits.get(i) + running_sum);
				}
			}
			
			//step 3. compute rou(p + 1)
			for (Map.Entry<String, ArrayList <LinkedList <String>>> me: ctora.entrySet()){
				String cusip = me.getKey();
				//compute the common denominator
				double common_denominator = 0;
				for (int i = 0; i < 5; ++i){
					double blj = benefits.get(cusip).get(i);
					common_denominator += blj*blj;
				}
				common_denominator = Math.sqrt(common_denominator);
				//compute the individual rouLI
				for (int i = 0; i < 5; ++i){
					double bli = benefits.get(cusip).get(i);
					rou.get(cusip).set(i, bli/common_denominator);
				}
			}
			//<\do stuff>
			List <Double> finalrou = to_vector_rou (rou);
			discrepency = get_epsilon(initrou, finalrou);
			//System.out.println (discrepency);
			//System.out.println(Tr);
			//assert (discrepency != (double)-1);
			++iteration_count;
		}while (discrepency >= epsilon);
		System.out.println ("Iterations: "+iteration_count);
	}
	
	//constants controlling the weight
	private int a = 2, m = 3;
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
	
	TreeMap <String, ArrayList <Double>> rou;
}

