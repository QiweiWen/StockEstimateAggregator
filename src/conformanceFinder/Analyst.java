package conformanceFinder;

public class Analyst {
	public Analyst (int aid, String name){
		this.aid = aid;
		this.name = name;
	}
	
	public void setTR (double TR){
		this.TR = TR;
	}
	
	public double getTR (){
		return TR;
	}
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Analyst))return false;
	    Analyst otherMyClass = (Analyst)other;
	    if (otherMyClass.aid == this.aid) return true;
	    return false;
	}
	//analyst ID
	public int aid;
	//trustworthiness (Tr in paper)
	private double TR;
	//for debugging
	private String name;
}
