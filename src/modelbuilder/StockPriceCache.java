package modelbuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class StockPriceCache {

	//Date indexed, cusip tagged variable-way-associative cache
	//really stretching the metaphor here
	public StockPriceCache(){}
	
	private void kick (){
		//kick a random "cacheline" on capacity miss
		Random r = new Random();
		ArrayList<Date> keys = new ArrayList<Date> (_storage.keySet());
		Date rndkey = keys.get(r.nextInt(keys.size()) );
		currsize -= _storage.get(rndkey).size();
		_storage.remove(rndkey);
	}
	
	private boolean hit (Date d, String cusip){
		if (_storage.containsKey(d)){
			if (_storage.get(d).containsKey(cusip)){
				return true;
			} else return false;
		}else{
			return false;
		}
	}
	
	public void clear (){
		_storage.clear();
		hits = 0;
		tots = 0;
	}
	
	@SuppressWarnings("deprecation")
	public double fetch(Date d, String cusip) throws IOException, ParseException{
		if (hit (d, cusip)){
			++hits;
			return _storage.get(d).get(cusip).doubleValue();
		}
		double res = -1;
		while (currsize + prefetch_size > max_entrynum){
			kick();
		}
		int numput = 0;
		//1. open file
		String fname = String.format("stockprices/%s.stock", cusip);
		BufferedReader br = new BufferedReader (new FileReader (fname));
		String linebuf;

		boolean found = false;
		while ((linebuf = br.readLine()) != null){
			String []args = linebuf.split(",");
			String dateexpr = args[0];
			int yr = Integer.parseInt(dateexpr.substring(0, 4));
			int mth = Integer.parseInt(dateexpr.substring(4,6));
			int dte = Integer.parseInt(dateexpr.substring(6,8));
			Date now = new Date (yr - 1900, mth - 1, dte);
			Double mkval = Double.parseDouble(args[1]);
			//we can do this because dates are sorted
			
			if (!found && d.compareTo(now) <= 0){
				
				found = true;
				res = mkval.doubleValue();
			}
			if (found){
				TreeMap <String,Double> otm = _storage.get(now);
				if (otm == null){
					TreeMap <String,Double> ntm = new TreeMap <String,Double> ();
					ntm.put(cusip, mkval);
					_storage.put(now, ntm);
				}else{
					otm.put(cusip, mkval);
				}
				if (numput++ == prefetch_size){
					break;
				}
			}
		}
		br.close();
		++tots;

		return res;
	}
	
	public double hit_rate (){
		if (tots != 0){
			return (double)hits/(double)tots;
		}else return 0;
	}
	
	private int hits = 0;
	private int tots = 0;
	private final int max_entrynum = 5000;
	private final int prefetch_size = 50;
	
	private int currsize = 0;
	private TreeMap <Date, TreeMap <String, Double>> _storage
		= new TreeMap <Date, TreeMap <String, Double>> ();
}
