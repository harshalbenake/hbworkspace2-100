package com.aviary.android.feather.effects;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.AviaryCds.PackType;
import com.aviary.android.feather.cds.TrayColumns;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.headless.filters.INativeFilter;
import com.aviary.android.feather.headless.filters.NativeFilterProxy;
import com.aviary.android.feather.headless.filters.impl.EffectFilter;
import com.aviary.android.feather.headless.moa.MoaAction;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaResult;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.IAviaryController;
import com.squareup.picasso.Generator;

public class EffectsPanel extends BordersPanel {

	private int mThumbPadding;
	private int mThumbRoundedCorners;
	private int mThumbStrokeColor;
	private int mThumbStrokeWidth;
	private double mFactor;

	public EffectsPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry, PackType.EFFECT );
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		mLogger.info( "FastPreview enabled: " + mEnableFastPreview );

		mThumbPadding = mConfigService.getDimensionPixelSize( R.dimen.aviary_effect_thumb_padding );
		mThumbRoundedCorners = mConfigService.getDimensionPixelSize( R.dimen.aviary_effect_thumb_radius );
		mThumbStrokeWidth = mConfigService.getDimensionPixelSize( R.dimen.aviary_effect_thumb_stroke );
		mThumbStrokeColor = mConfigService.getColor( R.color.aviary_effect_thumb_stroke_color );
		
		mFactor = 1.4;
		
		int cpuSpeed = SystemUtils.getCpuMhz();
		if( cpuSpeed > 0 ) {
			if( cpuSpeed < Constants.MHZ_CPU_FAST ){
				mFactor = 2.0;
			}
		}
		
		mLogger.log( "thumbnails scale factor: " + mFactor + " with cpu: " + cpuSpeed );
		
	}
	
	@Override
	protected Bitmap generateThumbnail( Bitmap input, int width, int height ) {
		return ThumbnailUtils.extractThumbnail( input, (int)((double)width/mFactor), (int)((double)height/mFactor) );
	}

	@Override
	protected void onDispose() {
		super.onDispose();
	}

	@Override
	protected void onProgressEnd() {
		if ( !mEnableFastPreview ) {
			super.onProgressModalEnd();
		} else {
			super.onProgressEnd();
		}
	}

	@Override
	protected void onProgressStart() {
		if ( !mEnableFastPreview ) {
			super.onProgressModalStart();
		} else {
			super.onProgressStart();
		}
	}

	@Override
	protected ListAdapter createListAdapter( Context context, Cursor cursor ) {
		return new EffectsListAdapter( context, R.layout.aviary_frame_item, R.layout.aviary_effect_item_more, R.layout.aviary_frame_item_external,
				R.layout.aviary_frame_item_divider, cursor );
	}

	@Override
	protected RenderTask createRenderTask( int position ) {
		return new EffectsRenderTask( position );
	}

	@Override
	protected INativeFilter loadNativeFilter( final TrayColumns.TrayCursorWrapper item, int position, boolean hires ) {
		EffectFilter filter = (EffectFilter) FilterLoaderFactory.get( Filters.EFFECTS );
		if ( null != item ) {
			filter.setMoaLiteEffect( item.getPath() + "/" + item.getIdentifier() + ".json" );
		}
		return filter;
	}

	@Override
	protected CharSequence[] getOptionalEffectsLabels() {
		return super.getOptionalEffectsLabels();
	}

	@Override
	protected CharSequence[] getOptionalEffectsValues() {
		return new CharSequence[] { "-1" };
	}

	protected class EffectsRenderTask extends RenderTask {

		// private Object mOpenGlCompleted = new Object();
		// FutureListener<Boolean> mOpenGlBackgroundListener = new
		// FutureListener<Boolean>() {
		//
		// @Override
		// public void onFutureDone( Future<Boolean> arg0 ) {
		// mLogger.info( "mOpenGlBackgroundListener::onFutureDone" );
		// synchronized ( mOpenGlCompleted ) {
		// mOpenGlCompleted.notify();
		// }
		// }
		// };

		public EffectsRenderTask ( int position ) {
			super( position );
		}

	}

	class EffectsListAdapter extends ListAdapter {

		public EffectsListAdapter ( Context context, int mainResId, int moreResId, int externalResId, int dividerResId, Cursor cursor ) {
			super( context, mainResId, moreResId, externalResId, dividerResId, cursor );
		}

		@Override
		protected Generator createContentCallable( long id, int position, String identifier, String path ) {
			if ( null != identifier ) {
				return new FilterThumbnailCallable( mThumbBitmap );
			}
			return null;
		}

		@Override
		protected BitmapDrawable getExternalBackgroundDrawable( Context context ) {
			return (BitmapDrawable) context.getResources().getDrawable( R.drawable.aviary_effects_pack_background );
		}
	}

	class FilterThumbnailCallable implements Generator {

		INativeFilter mFilter;
		Bitmap srcBitmap;

		public FilterThumbnailCallable ( Bitmap bitmap ) {
			srcBitmap = bitmap;
		}
		
		private INativeFilter loadFilter( CharSequence effectFileName ) {
			EffectFilter filter = (EffectFilter) FilterLoaderFactory.get( Filters.EFFECTS );
			filter.setMoaLiteEffect( (String) effectFileName );
			mLogger.log( "loadFilter: " + effectFileName );
			return filter;
		}
		
		@Override
		public Bitmap decode( Uri uri ) throws IOException {
			try {
				Log.d( "EffectsPanel", "loading thumbnail: " + uri );
				return call( uri.getPath() );
			} catch( Throwable t ) {
				throw new IOException( t );
			}
		}

		public Bitmap call( String filename ) throws Exception {

			boolean is_valid = true;
			if ( null == mFilter ) {
				try {
					mFilter = loadFilter( filename );
				} catch ( Throwable t ) {
					t.printStackTrace();
					is_valid = false;
				}
			}
			
			MoaActionList actionList = actionsForRoundedThumbnail( is_valid, mFilter );
//			Bitmap dst = Bitmap.createBitmap( srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888 );
			
			MoaResult moaresult = NativeFilterProxy.prepareActions( actionList, srcBitmap, null, 1, 1 );
			moaresult.execute();
			Bitmap result = moaresult.outputBitmap;
			return result;
		}

		MoaActionList actionsForRoundedThumbnail( final boolean isValid, INativeFilter filter ) {

			MoaActionList actions = MoaActionFactory.actionList();
			MoaAction action;
			
			if ( null != filter ) {
				actions.addAll( filter.getActions() );
			}
			
			if( mFactor != 1 ) {
				action = MoaActionFactory.action( "resize" );
				action.setValue( "size", mThumbSize );
				action.setValue( "force", true );
				actions.add( action );
			}
			
			action = MoaActionFactory.action( "ext-roundedborders" );
			action.setValue( "padding", mThumbPadding );
			action.setValue( "roundPx", mThumbRoundedCorners );
			action.setValue( "strokeColor", mThumbStrokeColor );
			action.setValue( "strokeWeight", mThumbStrokeWidth );

			if ( !isValid ) {
				action.setValue( "overlaycolor", 0x99000000 );
			}
			actions.add( action );
			
			
			return actions;
		}
	}
}
