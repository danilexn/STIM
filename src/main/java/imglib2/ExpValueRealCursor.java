package imglib2;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealCursor;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;

public class ExpValueRealCursor< T > implements RealCursor< T >
{
	final LocationRealCursor locationCursor;
	final Cursor< T > valueCursor;
	final IterableInterval< T > values;

	public ExpValueRealCursor(
			final RandomAccessibleInterval< DoubleType > locations,
			final IterableInterval< T > values )
	{
		this.locationCursor = new LocationRealCursor( locations );

		// we need a special hyperslice or cursor here that returns a different part of the big 2d RAI 
		this.valueCursor = values.localizingCursor();
		this.values = values;
	}

	protected ExpValueRealCursor( final ExpValueRealCursor< T > realCursor )
	{
		this.locationCursor = realCursor.locationCursor.copyCursor();
		this.valueCursor = realCursor.valueCursor.copyCursor();
		this.values = realCursor.values;
	}

	@Override
	public T get()
	{
		try
		{
			final T type = valueCursor.get();
			return type;
		}
		catch( Exception e )
		{
			System.out.println( Util.printInterval( values ) );
			System.out.println( valueCursor.getIntPosition( 0 ) );
			e.printStackTrace();
			System.exit( 0 );


			return null;
			// throw new RuntimeException( "we caught it: " + e );
		}
	}

	@Override
	public void jumpFwd( final long steps )
	{
		locationCursor.jumpFwd( steps );
		valueCursor.jumpFwd( steps );
	}

	@Override
	public void fwd()
	{
		locationCursor.fwd();
		valueCursor.fwd();
	}

	@Override
	public void reset()
	{
		locationCursor.reset();
		valueCursor.reset();
	}

	@Override
	public boolean hasNext()
	{
		return locationCursor.hasNext();
	}

	@Override
	public T next()
	{
		fwd();
		return get();
	}

	@Override
	public void localize( final float[] position )
	{
		locationCursor.localize( position );
	}

	@Override
	public void localize( final double[] position )
	{
		locationCursor.localize( position );
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return locationCursor.getFloatPosition( d );
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return locationCursor.getDoublePosition( d );
	}

	@Override
	public int numDimensions()
	{
		return locationCursor.numDimensions();
	}

	@Override
	public Sampler< T > copy()
	{
		return copyCursor();
	}

	@Override
	public RealCursor< T > copyCursor()
	{
		return new ExpValueRealCursor< T >( this );
	}
}