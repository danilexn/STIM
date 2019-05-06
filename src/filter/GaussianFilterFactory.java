package filter;

import net.imglib2.KDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.RealType;

public class GaussianFilterFactory< S extends RealType< S >, T extends RealType< T > > extends RadiusSearchFilterFactory< S, T >
{
	final T outofbounds;
	final double radius, sigma;
	final boolean normalize;

	public GaussianFilterFactory(
			final T outofbounds,
			final double radius,
			final double sigma,
			final boolean normalize )
	{
		this.outofbounds = outofbounds;
		this.radius = radius;
		this.sigma = sigma;
		this.normalize = normalize;
	}

	@Override
	public Filter< T > createFilter( final KDTree< S > tree )
	{
		return new GaussianFilter< S, T >(
				new RadiusNeighborSearchOnKDTree<>( tree ),
				outofbounds,
				radius,
				sigma,
				normalize );
	}

	@Override
	public T create()
	{
		return outofbounds.createVariable();
	}
}
