package modelbuilder;

//a simplistic approach:
//combine BUY with STRONGBUY and SELL with STRONGSELL
//HOLD is correct if the value is within  1 +/- 10% of original
//BUY or STRONGBUY is correct if the value is > 105% of original
//SELL is correct if the value the value is < 95% of original
public class UpDownMan extends HelpfulnessFinder{
	
	public UpDownMan() {
		super();
	}

	public Boolean is_correct(int reclvl, double value){
		Boolean res = false;
		switch (reclvl){
			case 1:
			case 2:{
				if (value >= 0.05) 
					res = true;
				break;
			}
			case 3:{
				if (value <= 0.10 && value >= -0.10)
					res = true;
				break;
			}
			case 4:
			case 5:{
				if (value <= -0.05)
					res = true;
				break;
			}
			default: break;
		}
		return res;
	}
}
