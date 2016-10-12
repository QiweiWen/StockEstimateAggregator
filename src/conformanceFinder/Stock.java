package conformanceFinder;
import java.util.*;
//a particular stock
public class Stock {
	
	public Stock (String name){
		this.cusip = name;
	}
	
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Stock))return false;
	    Stock otherMyClass = (Stock)other;
	    if (otherMyClass.cusip == this.cusip) return true;
	    return false;
	}
	//cusip of the stock
	private String cusip;
	//"rating" for all the recommendation levels
	private HashMap <RecLvl, Double> ratings;
}
