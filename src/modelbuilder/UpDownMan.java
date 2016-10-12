package modelbuilder;

//a simplistic approach:
//combine BUY with STRONGBUY and SELL with STRONGSELL
//HOLD is correct if the value is within  1 +/- 10% of original
//BUY or STRONGBUY is correct if the value is > 110% of original
//SELL is correct if the value the value is < 90% of original
public class UpDownMan extends HelpfulnessFinder{
	
	public UpDownMan() {
		super();
	}

	public Boolean is_correct(int reclvl, double oldval, double newval){
		Boolean res = false;
		double ratio = newval/oldval;
		switch (reclvl){
			case 1:
			case 2:{
				if (ratio >= 1.10) 
					res = true;
				break;
			}
			case 3:{
				if (ratio <= 1.10 && ratio >= 0.90)
					res = true;
				break;
			}
			case 4:
			case 5:{
				if (ratio <= 0.90)
					res = true;
				break;
			}
			default: break;
		}
		return res;
	}
}
