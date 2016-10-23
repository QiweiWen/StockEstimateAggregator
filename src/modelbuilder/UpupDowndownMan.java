package modelbuilder;

//a more sophisticated version of updownman
//no longer treats buy and strongbuy the same way
//probably very misguided, don't use
public class UpupDowndownMan extends HelpfulnessFinder {
	
	@Override
	public Boolean is_correct(int reclvl, double value) {
		Boolean res = false;
		
		switch (reclvl){
			case 1:{
				if (value >= 0.20){
					res = true;
				}
				break;
			}
			case 2:{
				if (value < 0.20 && value >= 0.10) 
					res = true;
				break;
			}
			case 3:{
				if (value <= 0.10 && value > -0.10)
					res = true;
				break;
			}
			case 4:{
				if (value <= -0.10 && value > -0.20)
					res = true;
				break;
			}
			case 5:{
				if (value <= -0.20)
					res = true;
				break;
			}
			default: break;
		}
		return res;
	}

}
