package com.aviary.android.feather.effects;

import java.io.IOException;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.BaseContextService;
import com.aviary.android.feather.library.services.IAviaryController;

/**
 * This class is the delegate class for creating the appropriate tool panel
 * for the given tool name
 */
public class AbstractPanelLoaderService extends BaseContextService {

	public static final String NAME = "AbstractPanelLoaderService";

	public AbstractPanelLoaderService ( IAviaryController context ) {
		super( context );
	}

	/**
	 * Passing a {@link ToolEntry} return an instance of {@link AbstractPanel} used to
	 * create the requested tool.
	 * 
	 * @param entry
	 * @return
	 */
	public AbstractPanel createNew( ToolEntry entry ) {

		AbstractPanel panel = null;
		final IAviaryController context = getContext();

		switch ( entry.name ) {
			case ADJUST:
				panel = new AdjustEffectPanel( context, entry, Filters.ADJUST );
				break;

			case BRIGHTNESS:
				panel = new NativeEffectRangePanel( context, entry, Filters.BRIGHTNESS, "brightness" );
				break;

			case SATURATION:
				panel = new NativeEffectRangePanel( context, entry, Filters.SATURATION, "saturation" );
				break;

			case CONTRAST:
				panel = new NativeEffectRangePanel( context, entry, Filters.CONTRAST, "contrast" );
				break;

			case SHARPNESS:
				panel = new NativeEffectRangePanel( context, entry, Filters.SHARPNESS, "sharpen" );
				break;

			case COLORTEMP:
				panel = new NativeEffectRangePanel( context, entry, Filters.COLORTEMP, "temperature" );
				break;

			case ENHANCE:
				panel = new EnhanceEffectPanel( context, entry, Filters.ENHANCE );
				break;

			case EFFECTS:
				panel = new EffectsPanel( context, entry );
				break;

			case BORDERS:
				panel = new BordersPanel( context, entry );
				break;

			case CROP:
				panel = new CropPanel( context, entry );
				break;

			case RED_EYE:
				panel = new DelayedSpotDrawPanel( context, entry, Filters.RED_EYE, false );
				break;

			case WHITEN:
				panel = new DelayedSpotDrawPanel( context, entry, Filters.WHITEN, false );
				break;

			case BLEMISH:
				panel = new DelayedSpotDrawPanel( context, entry, Filters.BLEMISH, false );
				break;

			case DRAWING:
				panel = new DrawingPanel( context, entry );
				break;

			case STICKERS:
				panel = new StickersPanel( context, entry );
				break;

			case TEXT:
				panel = new TextPanel( context, entry );
				break;

			case MEME:
				panel = new MemePanel( context, entry );
				break;

			case COLOR_SPLASH:
				panel = new ColorSplashPanel( context, entry );
				break;

			case TILT_SHIFT:
				panel = new TiltShiftPanel( context, entry );
				break;

			default:
				Logger logger = LoggerFactory.getLogger( "EffectLoaderService", LoggerType.ConsoleLoggerType );
				logger.error( "Effect with " + entry.name + " could not be found" );
				break;
		}
		return panel;
	}

	/** The Constant mAllEntries. */
	static final ToolEntry[] mAllEntries;

	static {
		mAllEntries = new ToolEntry[] { new ToolEntry( FilterLoaderFactory.Filters.ENHANCE, R.drawable.aviary_tool_ic_enhance, R.string.feather_enhance ),

		new ToolEntry( FilterLoaderFactory.Filters.TILT_SHIFT, R.drawable.aviary_tool_ic_focus, R.string.feather_tool_tiltshift ),

		new ToolEntry( FilterLoaderFactory.Filters.EFFECTS, R.drawable.aviary_tool_ic_effects, R.string.feather_effects ),

		new ToolEntry( FilterLoaderFactory.Filters.BORDERS, R.drawable.aviary_tool_ic_frames, R.string.feather_borders ),

		new ToolEntry( FilterLoaderFactory.Filters.STICKERS, R.drawable.aviary_tool_ic_stickers, R.string.feather_stickers ),

		new ToolEntry( FilterLoaderFactory.Filters.CROP, R.drawable.aviary_tool_ic_crop, R.string.feather_crop ),

		new ToolEntry( FilterLoaderFactory.Filters.ADJUST, R.drawable.aviary_tool_ic_orientation, R.string.feather_adjust ),

		new ToolEntry( FilterLoaderFactory.Filters.BRIGHTNESS, R.drawable.aviary_tool_ic_brightness, R.string.feather_brightness ),

		new ToolEntry( FilterLoaderFactory.Filters.CONTRAST, R.drawable.aviary_tool_ic_contrast, R.string.feather_contrast ),

		new ToolEntry( FilterLoaderFactory.Filters.SATURATION, R.drawable.aviary_tool_ic_saturation, R.string.feather_saturation ),

		new ToolEntry( FilterLoaderFactory.Filters.COLORTEMP, R.drawable.aviary_tool_ic_warmth, R.string.feather_tool_temperature ),

		new ToolEntry( FilterLoaderFactory.Filters.SHARPNESS, R.drawable.aviary_tool_ic_sharpen, R.string.feather_sharpen ),

		new ToolEntry( FilterLoaderFactory.Filters.COLOR_SPLASH, R.drawable.aviary_tool_ic_colorsplash, R.string.feather_tool_colorsplash ),

		new ToolEntry( FilterLoaderFactory.Filters.DRAWING, R.drawable.aviary_tool_ic_draw, R.string.feather_draw ),

		new ToolEntry( FilterLoaderFactory.Filters.TEXT, R.drawable.aviary_tool_ic_text, R.string.feather_text ),

		new ToolEntry( FilterLoaderFactory.Filters.RED_EYE, R.drawable.aviary_tool_ic_redeye, R.string.feather_red_eye ),

		new ToolEntry( FilterLoaderFactory.Filters.WHITEN, R.drawable.aviary_tool_ic_whiten, R.string.feather_whiten ),

		new ToolEntry( FilterLoaderFactory.Filters.BLEMISH, R.drawable.aviary_tool_ic_blemish, R.string.feather_blemish ),

		new ToolEntry( FilterLoaderFactory.Filters.MEME, R.drawable.aviary_tool_ic_meme, R.string.feather_meme ), };
	}

	/**
	 * Return a list of available effects.
	 * 
	 * @return the effects
	 */
	public ToolEntry[] getToolsEntries() {
		return mAllEntries;
	}

	public ToolEntry findEntry( Filters name ) {
		for ( ToolEntry entry : mAllEntries ) {
			if ( entry.name.equals( name ) ) {
				return entry;
			}
		}
		return null;
	}

	public ToolEntry findEntry( String name ) {
		for ( ToolEntry entry : mAllEntries ) {
			if ( entry.name.name().equals( name ) ) {
				return entry;
			}
		}
		return null;
	}

	public static final ToolEntry[] getAllEntries() {
		return mAllEntries;
	}

	/**
	 * Check if the current application context has a valid folder "stickers" inside its
	 * assets folder.
	 * 
	 * @return true, if successful
	 */
	public boolean hasStickers() {
		try {
			String[] list = null;
			list = getContext().getBaseContext().getAssets().list( "stickers" );
			return list.length > 0;
		} catch ( IOException e ) {
		}

		return false;
	}

	@Override
	public void dispose() {}
}
