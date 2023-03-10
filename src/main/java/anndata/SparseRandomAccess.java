package anndata;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.*;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class SparseRandomAccess<
        D extends NativeType<D> & NumericType<D>,
        I extends NativeType<I> & IntegerType<I>>
        extends AbstractLocalizable
        implements RandomAccess<D> {

    protected final AbstractCompressedStorageRai<D, I> rai;
    protected final RandomAccess<D> dataAccess;
    protected final RandomAccess<I> indicesAccess;
    protected final RandomAccess<I> indptrAccess;
    protected final D fillValue;

    public SparseRandomAccess(AbstractCompressedStorageRai<D, I> rai) {
        super(rai.numDimensions());
        System.out.println( "instantiate");
        this.rai = rai;
        this.dataAccess = rai.data.randomAccess();
        this.indicesAccess = rai.indices.randomAccess();
        this.indptrAccess = rai.indptr.randomAccess();

        this.fillValue = Views.extendBorder( rai.data ).randomAccess().get().createVariable();
        this.fillValue.setOne();
    }

    public SparseRandomAccess(SparseRandomAccess<D, I> ra) {
        super(ra.rai.numDimensions());

        this.rai = ra.rai;
        this.setPosition( ra );

        this.indicesAccess = ra.indicesAccess.copyRandomAccess();
        this.indptrAccess = ra.indptrAccess.copyRandomAccess();
        this.dataAccess = ra.dataAccess.copyRandomAccess();
        this.fillValue = ra.fillValue.createVariable();
        this.fillValue.setOne();
    }

    @Override
    public RandomAccess<D> copyRandomAccess() {
    	
        return new SparseRandomAccess<>(this);
    }

    @Override
    public void fwd(int d) {
        ++position[d];
    }

    @Override
    public void bck(int d) {
        --position[d];
    }

    @Override
    public void move(int distance, int d) {
        position[d] += distance;
    }

    @Override
    public void move(long distance, int d) {
        position[d] += distance;
    }

    @Override
    public void move(Localizable localizable) {
        for (int d = 0; d < n; ++d) {
            position[d] += localizable.getLongPosition(d);
        }
    }

    @Override
    public void move(int[] distance) {
        for (int d = 0; d < n; ++d) {
            position[d] += distance[d];
        }
    }

    @Override
    public void move(long[] distance) {
        for (int d = 0; d < n; ++d) {
            position[d] += distance[d];
        }
    }

    @Override
    public void setPosition(Localizable localizable) {
        for (int d = 0; d < n; ++d) {
            position[d]  = localizable.getLongPosition(d);
        }
    }

    @Override
    public void setPosition(int[] position) {
        for (int d = 0; d < n; ++d) {
            this.position[d] = position[d];
        }
    }

    @Override
    public void setPosition(long[] position) {
        for (int d = 0; d < n; ++d) {
            this.position[d] = position[d];
        }
    }

    @Override
    public void setPosition(int position, int d) {
        this.position[d] = position;
    }

    @Override
    public void setPosition(long position, int d) {
        this.position[d] = position;
    }

    final AtomicBoolean concurrent = new AtomicBoolean( false );
    final AtomicInteger concurrentCount = new AtomicInteger(0);
    @Override
    public D get() {
    	final int a = concurrentCount.getAndIncrement();
    	//if ( a > 0 )
    	//	System.out.println( "concurrent accesses: " + a );
    	//if ( concurrent.getAndSet(true) )
    	//	System.out.println( "concurrent access.");

    	long ptr=-12122,start=-12122;
    	try
    	{
        // determine range of indices to search
        ptr = rai.targetPointer(position);
        indptrAccess.setPosition(ptr, 0);
        //indptrAccess.setPosition(new long[] { ptr } ); // this fixes it too, but I think it only shadows a bug
        start = indptrAccess.get().getIntegerLong();
        indptrAccess.setPosition(ptr + 1L, 0);
    	}
    	catch (Exception e )
    	{
    		System.out.println( "concurrent accesses: " + a );
			System.out.println( Integer.toHexString(hashCode()) + ": " + "ptr: "  + ptr + " indptr: " + Util.printInterval(rai.indptr) );
			e.printStackTrace();
			System.exit( 0 );
			throw new ArrayIndexOutOfBoundsException();
    	}
//        indptrAccess.fwd(0);
//        final long end = indptrAccess.get().getIntegerLong();

        // todo: make this more efficient, e.g., by bisection
//        indicesAccess.setPosition(start, 0);
//        while (indicesAccess.getLongPosition(0) < end) {
//            if (indicesAccess.get().getIntegerLong() < rai.targetCursor(position)) {
//                indicesAccess.fwd(0);
//            }
//            else if (indicesAccess.get().getIntegerLong() == rai.targetCursor(position)) {
//                dataAccess.setPosition(indicesAccess);
//                return dataAccess.get();
//            }
//            else {
//                break;
//            }
//        }
    	//concurrent.set(false);
    	concurrentCount.decrementAndGet();
        return fillValue;
    }

    @Override
    public Sampler<D> copy() {
        return copyRandomAccess();
    }
}
