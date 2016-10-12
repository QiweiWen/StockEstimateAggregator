package conformanceFinder;
import java.util.*;

//implements formulae as shown in the paper
public class consensusCalc {

	public consensusCalc (int a, int m){
		
	}
	
	public void rate (int analyst, String cupid, RecLvl r, double helpfulness){
		assert (helpfulness > 0 && helpfulness <= 1);
	}
	
	//perform one iteration
	public void iterate_once (){
		
	}
	
	//iterate until convergence
	public void converge (){
		
	}
	
	//constants controlling the weight
	private int a = 2, m = 3;
	
	//given an analyst's ID, return the votes cast by the analyst
	private HashMap <Analyst, List<Vote>> analyst_to_vote;
	//given a stock and a recommendation level, return all the analysts that
	//cast the same vote
	private HashMap <Vote, List <Analyst>> vote_to_analyst;
	
}

