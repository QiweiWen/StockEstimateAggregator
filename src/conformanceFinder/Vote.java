package conformanceFinder;

public class Vote {
	
	//the stock for which the vote was cast
	public Stock stock;
	//how helpful the vote is
	//Krl in paper
	public double helpfulness;
	//which recommendation level
	//strong buy, sell, etc
	public RecLvl r;
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Vote))return false;
	    Vote otherMyClass = (Vote)other;
	    if (otherMyClass.stock == this.stock && otherMyClass.r == this.r) return true;
	    return false;
	}
}
