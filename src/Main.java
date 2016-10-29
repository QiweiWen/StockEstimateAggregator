import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.cli.*;

import benchmarker.*;
import modelbuilder.*;
import conformanceFinder.*;

public class Main {


	
	public static void main(String[] args) throws Exception {
		/*
		 * Define default values for optional command line flags
		 */
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			System.out.println (e1.getMessage());
			System.out.println ("error locating postgresql driver class");
			return;
		}
		boolean use_mr_derp = false;
		String beginning_date = "20071026";
		Benchmark_options bench = Benchmark_options.analyst;
		String benchmark_analyst = "GOLDMAN";
		int default_length = 24;
		int defm = 3,
			defa = 2;
		/*
		 * Define all accepted command line arguments
		 * 
		 */
		Options alloptions = new Options();
		Option portfoliopt = new Option ("p","portfolio",true,"portfolio file");
		portfoliopt.setArgs(1);
		portfoliopt.setRequired(true);
		alloptions.addOption(portfoliopt);
		
		Option derpopt = new Option("ud","use-derp",false,"disregard helpfulness input. default false");
		derpopt.setRequired(false);
		alloptions.addOption(derpopt);
		
		Option begindateopt = new Option ("bd","beginning-date", true, "day on which to begin trading. default jan 1 2007");
		begindateopt.setArgs(1);
		begindateopt.setRequired(false);
		alloptions.addOption(begindateopt);
		
		Option benchmark = new Option ("bn", "benchmarked-analyst",true,"analyst with whom to benchmark. default goldman sachs");
		benchmark.setArgs(1);
		benchmark.setRequired(false);
		alloptions.addOption(benchmark);
		
		Option setmopt = new Option ("m","m-parameter",true,"m parameter in iterative voting. default 3");
		setmopt.setArgs(1);
		setmopt.setRequired(false);
		alloptions.addOption(setmopt);
		
		Option setaopt = new Option ("a","a-parameter",true,"alpha parameter in iterative voting. default 2");
		setaopt.setArgs(1);
		setaopt.setRequired(false);
		alloptions.addOption(setaopt);
		
		Option dump = new Option ("o", "output-file", true, "output file to save the result");
		dump.setArgs(1);
		dump.setRequired(true);
		alloptions.addOption(dump);
		
		Option nummonths = new Option ("nm", "num-months", true, "number of months for which to run the experiment");
		nummonths.setArgs(1);
		nummonths.setRequired(false);
		alloptions.addOption(nummonths);
		
		/*
		 * Sanity check on command line options
		 */
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		try{
			cmd = parser.parse(alloptions, args);
		}catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("stockman", alloptions);

            System.exit(1);
            return;
		}
		//1. attempt to open input and output files
		String dumpfile = cmd.getOptionValue("o");
		String portfoliofile = cmd.getOptionValue("p");
		BufferedReader br;
		BufferedWriter bw;
		try {
			br = new BufferedReader (new FileReader (portfoliofile));
			bw = new BufferedWriter (new FileWriter (dumpfile));
		} catch (FileNotFoundException e) {
			System.out.println (e.getMessage());
			return;
		}
		//2. verify date
		int year = 0,
			month = 0,
			day = 0;
		if (cmd.hasOption("bd")){
			beginning_date = cmd.getOptionValue("bd");
			if (beginning_date.length() != "YYYYMMDD".length()){
				System.out.println ("invalid beginning date");
				br.close();
				bw.close();
				return;
			}
		}
		try{
			year = Integer.parseInt(beginning_date.substring(0, 4));
			month = Integer.parseInt(beginning_date.substring(4,6));
			day = Integer.parseInt(beginning_date.substring(6,8));
		}catch (Exception e){
			System.out.println ("invalid beginning date");
			br.close();
			bw.close();
			return;
		}

		//3. analyst
		if (cmd.hasOption("bn")){
			String analyst = cmd.getOptionValue("bn");
			if (analyst == "MEAN"){
				bench = Benchmark_options.mean;
			}else if (analyst == "MEDIAN"){
				bench = Benchmark_options.median;
			}else{
				benchmark_analyst = analyst;
			}
		}
		
		//4. alpha and m
		if (cmd.hasOption("m")){
			String mexpr = cmd.getOptionValue("m");
			try {
				defm = Integer.parseInt(mexpr);
			}catch (Exception e){
				System.out.println ("invalid m, using "+defm+" instead");
			}
		}
		
		if (cmd.hasOption("a")){
			String aexpr = cmd.getOptionValue("a");
			try {
				defa = Integer.parseInt(aexpr);
			}catch (Exception e){
				System.out.println ("invalid a, using "+defa+" instead");
			}
		}
		//5.benchmark length
		if (cmd.hasOption("nm")){
			String monthexpr = cmd.getOptionValue("nm");
			try {
				default_length = Integer.parseInt(monthexpr);
			}catch (Exception e){
				System.out.println ("invalid number of months, using "+default_length+" instead");
			}
		}
		
		if (cmd.hasOption("ud")){
			use_mr_derp = true;
		}
		Benchmark b = new FixedTrading(use_mr_derp,
						  			   year, month, day,
						  			   defm, defa,
						  			   br,
						  			   bench,
						  			   benchmark_analyst,default_length);
		b.run_bench();
		//the end
		br.close();
		bw.close();
	}
}
