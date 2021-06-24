package cmd;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.janelia.saalfeldlab.n5.N5FSReader;

import align.GlobalOptSIFT;
import io.N5IO;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import util.Threads;

public class GlobalOpt implements Callable<Void> {

	@Option(names = {"-i", "--input"}, required = true, description = "input N5 container, e.g. -i /home/ssq.n5")
	private String input = null;

	@Option(names = {"-d", "--datasets"}, required = false, description = "ordered, comma separated list of one or more datasets, e.g. -d 'Puck_180528_20,Puck_180528_22' (default: all, in order as saved in N5 metadata)")
	private String datasets = null;

	// general display options
	@Option(names = {"--skipDisplayResults"}, required = false, description = "do not show a preview of the aligned stack (default: false)")
	private boolean skipDisplayResults = false;

	// alignment options
	@Option(names = {"--ignoreQuality"}, required = false, description = "ignore the amount of RANSAC inlier ratio, otherwise used it to determine - if necessary - which pairwise connections to remove during global optimization (default: false)")
	private boolean ignoreQuality = false;

	@Option(names = {"--lambda"}, required = false, description = "lambda of the affine model regularized with the rigid model, 0.0 means fully affine, 1.0 means just rigid (default: 0.1)")
	private double lambda = 0.1;

	@Option(names = {"--maxAllowedError"}, required = false, description = "maximally allowed error during global optimization (default: 300.0 for slideseq)")
	private double maxAllowedError = 300.0;

	@Option(names = {"--maxIterations"}, required = false, description = "maximum number of iterations (default: 3000)")
	private int maxIterations = 3000;

	@Option(names = {"--minIterations"}, required = false, description = "minimum number of iterations (default: 500)")
	private int minIterations = 500;

	@Option(names = {"--relativeThreshold"}, required = false, description = "relative threshold for dropping pairwise connections, i.e. if the pairwise error is n-times higher than the average error (default: 3.0)")
	private double relativeThreshold = 300.0;

	@Option(names = {"--absoluteThreshold"}, required = false, description = "absolute error threshold for dropping pairwise connections - consult the results of pairwise matching to identify a reasonable number (default: 160.0 for slideseq)")
	private double absoluteThreshold = 160.0;

	// ICP parameters
	@Option(names = {"--skipICP"}, required = false, description = "skip the ICP refinement step (default: false)")
	private boolean skipICP = false;

	@Option(names = {"--icpIterations"}, required = false, description = "maximum number of ICP iterations for each pair of slides (default: 100)")
	private int icpIterations = 100;

	@Option(names = {"--icpErrorFraction"}, required = false, description = "distance at which locations will be assigned as corresponding during ICP, relative to median distance between all locations (default: 0.5)")
	private double icpErrorFraction = 1.0;

	@Option(names = {"--maxAllowedErrorICP"}, required = false, description = "maximum error allowed during ICP runs after model fit - consult the results of pairwise matching to identify a reasonable number (default: 140.0 for slideseq)")
	private double maxAllowedErrorICP = 140.0;

	@Option(names = {"--maxIterationsICP"}, required = false, description = "maximum number of iterations during ICP (default: 3000)")
	private int maxIterationsICP = 500;

	@Option(names = {"--minIterationsICP"}, required = false, description = "minimum number of iterations during ICP (default: 3000)")
	private int minIterationsICP = 500;

	@Override
	public Void call() throws Exception {

		final File n5File = new File( input );

		if ( !n5File.exists() )
		{
			System.out.println( "N5 '" + n5File.getAbsolutePath() + "'not found. stopping.");
			return null;
		}

		final N5FSReader n5 = N5IO.openN5( n5File );
		final List< String > inputDatasets;

		if ( datasets == null || datasets.trim().length() == 0 )
			inputDatasets = N5IO.listAllDatasets( n5 );
		else
			inputDatasets = Arrays.asList( datasets.split( "," ) );

		if ( inputDatasets.size() == 0 )
		{
			System.out.println( "no input datasets available. stopping.");
			return null;
		}

		
		//final List< STData > stdata = new ArrayList<>();

		for ( final String dataset : inputDatasets )
		{
			if ( !n5.exists( n5.groupPath( dataset ) ) )
			{
				System.out.println( "dataset '" + dataset + "' not found. stopping.");
				return null;
			}
			/*else
			{
				stdata.add( N5IO.readN5( n5, dataset ) );
			}*/
		}

		// -d 'Puck_180602_20,Puck_180602_17'

		final boolean skipDisplayResults = this.skipDisplayResults;
		final boolean useQuality = !ignoreQuality;
		final double lambda = this.lambda;
		final double maxAllowedError = this.maxAllowedError;
		final int maxIterations = this.maxIterations;
		final int maxPlateauwidth = this.minIterations;
		final double relativeThreshold = this.relativeThreshold;
		final double absoluteThreshold = this.absoluteThreshold;

		final boolean doICP = !skipICP;
		final int icpIterations = this.icpIterations;
		final double icpErrorFraction = this.icpErrorFraction;
		final double maxAllowedErrorICP = this.maxAllowedErrorICP;
		final int maxIterationsICP = this.maxIterationsICP;
		final int maxPlateauwhidthICP = this.minIterationsICP;

		GlobalOptSIFT.globalOpt(
				n5File,
				inputDatasets,
				useQuality,
				lambda,
				maxAllowedError,
				maxIterations,
				maxPlateauwidth,
				relativeThreshold,
				absoluteThreshold,
				doICP,
				icpIterations,
				icpErrorFraction,
				maxAllowedErrorICP,
				maxIterationsICP,
				maxPlateauwhidthICP,
				Threads.numThreads(),
				skipDisplayResults );

		return null;
	}

	public static void main(final String... args) {
		CommandLine.call(new GlobalOpt(), args);
	}

}
