package gui.bdv;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import filter.FilterFactory;
import filter.Filters;
import filter.GaussianFilterFactory;
import filter.MeanFilterFactory;
import filter.MedianFilterFactory;
import filter.SingleSpotRemovingFilterFactory;
import net.imglib2.RealCursor;
import net.imglib2.RealPointSampleList;
import net.imglib2.type.numeric.real.DoubleType;
import net.miginfocom.swing.MigLayout;

public class STIMCardFilter
{
	private final JPanel panel;
	private final FilterTableModel tableModel;
	private final STIMCard stimcard;

	public STIMCardFilter( final STIMCard stimcard )
	{
		this.stimcard = stimcard;

		this.panel = new JPanel(new MigLayout("gap 0, ins 5 5 5 0, fill", "[right][grow]", "center"));

		final JTable table = new JTable();
		table.setModel( this.tableModel = new FilterTableModel( table ) );
		table.setPreferredScrollableViewportSize(new Dimension(260, 65));
		table.setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 10));
		table.getColumnModel().getColumn(0).setPreferredWidth(40);
		table.getColumnModel().getColumn(1).setPreferredWidth(260-40-50);
		table.getColumnModel().getColumn(2).setPreferredWidth(50);
		table.setRowSelectionAllowed(false);

		final DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		table.getColumnModel().getColumn(2).setCellRenderer( centerRenderer );

		// adding it to JScrollPane
		final JScrollPane sp = new JScrollPane(table);
		final JPanel extraPanel2 = new JPanel();
		extraPanel2.setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 15));
		extraPanel2.add( sp );

		panel.add( extraPanel2, "span,growx,pushy");
	}

	public List< FilterFactory< DoubleType, DoubleType > > filterFactories()
	{
		final List< FilterFactory< DoubleType, DoubleType > > f = new ArrayList<>();

		if ( tableModel.currentActiveValues[ 0 ] ) // single spot filter
			f.add( new SingleSpotRemovingFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * tableModel.currentRadiusValues[ 0 ] ) );

		if ( tableModel.currentActiveValues[ 1 ] ) // median filter
			f.add( new MedianFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * tableModel.currentRadiusValues[ 1 ] ) );

		if ( tableModel.currentActiveValues[ 2 ] ) // Gaussian filter
			f.add( new GaussianFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * tableModel.currentRadiusValues[ 2 ] ) );

		if ( tableModel.currentActiveValues[ 3 ] ) // Mean filter
			f.add( new MeanFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * tableModel.currentRadiusValues[ 3 ] ) );

		return f;
	}

	public FilterTableModel getTableModel() { return tableModel; }
	public JPanel getPanel() { return panel; }

	protected class FilterTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = -8220316170021615088L;

		boolean[] currentActiveValues = { false, false, false, false };
		double[] currentRadiusValues = { 1.5, 5.0, 5.0, 5.0 };

		boolean isEditable = true;

		final JTable table;

		final Object[][] filters = {
				{ false, "Single Spot Removing Filter", 1.5 },
				{ false, "Median Filter", 2.0 },
				{ false, "Gaussian Filter", 2.0 },
				{ false, "Mean/Avg Filter", 3.5 } };

		final String[] columnNames = { "Active", "Filter type", "Radius" };

		public FilterTableModel( final JTable table )
		{
			this.table = table;
		}

		@Override
		public boolean isCellEditable( final int row, final int column )
		{
			return isEditable && (column == 0 || column == 2);
		}

		@Override
		public void setValueAt( final Object value, final int row, final int column )
		{
			filters[row][column] = value;

			boolean changed = false;
			for ( int r = 0; r < getRowCount(); ++r )
			{
				if ( currentActiveValues[ r ] != (boolean)filters[ r ][ 0 ] )
				{
					changed = true;
					currentActiveValues[ r ] = (boolean)filters[ r ][ 0 ];
				}

				if ( currentRadiusValues[ r ] != (double)filters[ r ][ 2 ] )
				{
					changed = true;
					currentRadiusValues[ r ] = (double)filters[ r ][ 2 ];
				}
			}

			if ( changed )
			{
				isEditable = false;

				new Thread( () ->
				{
					table.setForeground( Color.lightGray );

					// replace original values first
					stimcard.sourceData().forEach( (gene,data) ->
					{
						final Iterator<Double> iAFilt = data.getA().originalValues().iterator();
						data.getA().tree().forEach( t -> t.set( iAFilt.next() ) );

						final Iterator<Double> iBFilt = data.getB().originalValues().iterator();
						data.getB().tree().forEach( t -> t.set( iBFilt.next() ) );
					} );

					for ( final FilterFactory<DoubleType, DoubleType> filterFactory : filterFactories() )
					{
						stimcard.sourceData().forEach( (gene,data) ->
						{
							final RealPointSampleList<DoubleType> filteredA =
									Filters.filter( data.getA().tree(), data.getA().tree().iterator(), filterFactory );
							final RealPointSampleList<DoubleType> filteredB =
									Filters.filter( data.getB().tree(), data.getB().tree().iterator(), filterFactory );
	
							final RealCursor<DoubleType> iAFilt = filteredA.cursor();
							data.getA().tree().forEach( t -> t.set( iAFilt.next() ) );
	
							final RealCursor<DoubleType> iBFilt = filteredB.cursor();
							data.getB().tree().forEach( t -> t.set( iBFilt.next() ) );
						});
					}

					/*
					if ( currentActiveValues[ 0 ] ) // single spot filter
					{
						stimcard.sourceData().forEach( (gene,data) ->
						{
							final RealPointSampleList<DoubleType> filteredA =
									Filters.filter( data.getA().tree(), data.getA().tree().iterator(), new SingleSpotRemovingFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 0 ] ) );
							final RealPointSampleList<DoubleType> filteredB =
									Filters.filter( data.getB().tree(), data.getB().tree().iterator(), new SingleSpotRemovingFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 0 ] ) );

							final RealCursor<DoubleType> iAFilt = filteredA.cursor();
							data.getA().tree().forEach( t -> t.set( iAFilt.next() ) );

							final RealCursor<DoubleType> iBFilt = filteredB.cursor();
							data.getB().tree().forEach( t -> t.set( iBFilt.next() ) );
						} );
					}

					if ( currentActiveValues[ 1 ] ) // median filter
					{
						stimcard.sourceData().forEach( (gene,data) ->
						{
							final RealPointSampleList<DoubleType> filteredA =
									Filters.filter( data.getA().tree(), data.getA().tree().iterator(), new MedianFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 1 ] ) );
							final RealPointSampleList<DoubleType> filteredB =
									Filters.filter( data.getB().tree(), data.getB().tree().iterator(), new MedianFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 1 ] ) );

							final RealCursor<DoubleType> iAFilt = filteredA.cursor();
							data.getA().tree().forEach( t -> t.set( iAFilt.next() ) );

							final RealCursor<DoubleType> iBFilt = filteredB.cursor();
							data.getB().tree().forEach( t -> t.set( iBFilt.next() ) );
						} );
					}

					if ( currentActiveValues[ 2 ] ) // Gaussian filter
					{
						stimcard.sourceData().forEach( (gene,data) ->
						{
							final RealPointSampleList<DoubleType> filteredA =
									Filters.filter( data.getA().tree(), data.getA().tree().iterator(), new GaussianFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 2] ) );
							final RealPointSampleList<DoubleType> filteredB =
									Filters.filter( data.getB().tree(), data.getB().tree().iterator(), new GaussianFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 2 ] ) );

							final RealCursor<DoubleType> iAFilt = filteredA.cursor();
							data.getA().tree().forEach( t -> t.set( iAFilt.next() ) );

							final RealCursor<DoubleType> iBFilt = filteredB.cursor();
							data.getB().tree().forEach( t -> t.set( iBFilt.next() ) );
						} );
					}

					if ( currentActiveValues[ 3 ] ) // Mean filter
					{
						stimcard.sourceData().forEach( (gene,data) ->
						{
							final RealPointSampleList<DoubleType> filteredA =
									Filters.filter( data.getA().tree(), data.getA().tree().iterator(), new MeanFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 3 ] ) );
							final RealPointSampleList<DoubleType> filteredB =
									Filters.filter( data.getB().tree(), data.getB().tree().iterator(), new MeanFilterFactory<>( new DoubleType( 0 ), stimcard.medianDistance() * currentRadiusValues[ 3 ] ) );

							final RealCursor<DoubleType> iAFilt = filteredA.cursor();
							data.getA().tree().forEach( t -> t.set( iAFilt.next() ) );

							final RealCursor<DoubleType> iBFilt = filteredB.cursor();
							data.getB().tree().forEach( t -> t.set( iBFilt.next() ) );
						} );
					}
					*/
					stimcard.bdvhandle().getViewerPanel().requestRepaint();

					table.setForeground( Color.black );
					isEditable = true;
				}).start();
			}
		}

		@Override
		public Object getValueAt( final int row, final int column )
		{
			return filters[ row ][ column ];
		}

		@Override
		public Class<?> getColumnClass( final int column)
		{
			if ( column == 0 )
				return Boolean.class;
			else if ( column == 2 )
				return Double.class;
			else
				return String.class;
		}

		@Override
		public String getColumnName( final int column ) { return columnNames[ column ]; }

		@Override
		public int getRowCount() { return filters.length; }

		@Override
		public int getColumnCount() { return filters[ 0 ].length; }
	}
}