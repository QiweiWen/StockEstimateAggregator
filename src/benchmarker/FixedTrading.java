package benchmarker;

import java.io.BufferedReader;

public class FixedTrading extends Benchmark {

	public FixedTrading(boolean derp, int starty, int startm, int startd,
			int mparam, int aparam, BufferedReader pFile,
			Benchmark_options opt, String opponent, int num_months)
			throws Exception {
		super(derp, starty, startm, startd, mparam, aparam, pFile, opt, opponent,
				num_months);
	}

	private final int[] reclvl_to_shares = {100, 50, 0, -50, -100};
	@Override
	protected int reclvl_to_share(int reclvl) {
		return reclvl_to_shares [reclvl - 1];
	}

}
