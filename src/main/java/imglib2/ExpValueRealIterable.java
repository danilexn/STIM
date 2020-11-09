package imglib2;

import java.util.Iterator;

import net.imglib2.IterableRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealInterval;
import net.imglib2.RealPositionable;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

public class ExpValueRealIterable< T > implements IterableRealInterval< T >
{
	final RandomAccessibleInterval< DoubleType > locations;
	final RandomAccessibleInterval< T > values;
	final RealInterval realInterval;
	final long lastIndex, size;
	long valueIndex;

	public ExpValueRealIterable(
			final RandomAccessibleInterval< DoubleType > locations,
			final RandomAccessibleInterval< T > values,
			final long valueIndex, //getIndexForGene( geneName )
			final RealInterval realInterval )
	{
		this.locations = locations;
		this.values = values;
		this.realInterval = realInterval;
		this.size = locations.dimension( 0 );
		this.lastIndex = locations.dimension( 0 ) - 1;
		this.valueIndex = valueIndex;
	}

	/**
	 * @return the current gene that is being rendered - STData.getIndexForGene( geneName )
	 */
	public long getValueIndex() { return valueIndex; }

	/**
	 * Set which gene is currently being rendered
	 * @param valueIndex - the index of the gene - STData.getIndexForGene( geneName )
	 */
	public void setValueIndex( final long valueIndex ) { this.valueIndex = valueIndex; }

	@Override
	public RealCursor< T > localizingCursor()
	{
		return new ExpValueRealCursor< T >(
				locations,
				new SwitchableGeneVector<>( this ) );
				//Views.flatIterable( Views.hyperSlice( values, 0, valueIndex ) ) );
	}

	@Override
	public double realMin( final int d )
	{
		return realInterval.realMin( d );
	}

	@Override
	public void realMin( final double[] min )
	{
		realInterval.realMin( min );
	}

	@Override
	public void realMin( final RealPositionable min )
	{
		realInterval.realMin( min );
	}

	@Override
	public double realMax( final int d )
	{
		return realInterval.realMax( d );
	}

	@Override
	public void realMax( final double[] max )
	{
		realInterval.realMax( max );
	}

	@Override
	public void realMax( final RealPositionable max )
	{
		realInterval.realMax( max );
	}

	@Override
	public int numDimensions()
	{
		return realInterval.numDimensions();
	}

	@Override
	public Iterator< T > iterator()
	{
		return localizingCursor();
	}

	@Override
	public RealCursor< T > cursor()
	{
		return localizingCursor();
	}

	@Override
	public long size()
	{
		return size;
	}

	@Override
	public T firstElement()
	{
		return cursor().next();
	}

	@Override
	public Object iterationOrder()
	{
		return this;
	}
}
