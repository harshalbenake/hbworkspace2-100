package com.aviary.android.feather.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;

import com.aviary.android.feather.R;
import com.aviary.android.feather.graphics.GlowBitmapDrawable;
import com.aviary.android.feather.utils.UIUtils;

@SuppressLint ( "NewApi" )
public class AviaryHighlightImageButton extends ImageView {

	private int mHighlightColorPressed = 0;
	private int mHighlightColorChecked = 0;
	private int mHighlightColorSelected = 0;
	private Mode mBlendMode;
	private ColorFilter mColorFilterTintPressed;
	private ColorFilter mColorFilterTintChecked;
	private ColorFilter mColorFilterTintSelected;

	private int mGlowStatus;
	private int mHighlightMode;
	private int mGlowSize;
	private boolean mToggleEnabled;
	private boolean mUnToggleUserEnabled;
	private boolean mChecked;
	private boolean mBroadcasting;
	private OnCheckedChangeListener mOnCheckedChangeListener;

	static boolean glowPressed( int status ) {
		return UIUtils.checkBits( status, UIUtils.GLOW_MODE_PRESSED );
	}

	static boolean glowChecked( int status ) {
		return UIUtils.checkBits( status, UIUtils.GLOW_MODE_CHECKED );
	}

	static boolean glowSelected( int status ) {
		return UIUtils.checkBits( status, UIUtils.GLOW_MODE_SELECTED );
	}

	public static interface OnCheckedChangeListener {

		void onCheckedChanged( AviaryHighlightImageButton buttonView, boolean isChecked, boolean fromUser );
	}

	static class SavedState extends BaseSavedState {

		boolean checked;

		SavedState ( Parcelable superState ) {
			super( superState );
		}

		private SavedState ( Parcel in ) {
			super( in );
			checked = (Boolean) in.readValue( null );
		}

		@Override
		public void writeToParcel( Parcel out, int flags ) {
			super.writeToParcel( out, flags );
			out.writeValue( checked );
		}

		@Override
		public String toString() {
			return "CompoundButton.SavedState{" + Integer.toHexString( System.identityHashCode( this ) ) + " checked=" + checked + "}";
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

			@Override
			public SavedState createFromParcel( Parcel in ) {
				return new SavedState( in );
			}

			@Override
			public SavedState[] newArray( int size ) {
				return new SavedState[size];
			}
		};
	}

	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
	@SuppressWarnings ( "unused" )
	private static final String LOG_TAG = "AviaryHighlightImageButton";

	public AviaryHighlightImageButton ( Context context ) {
		this( context, null );
	}

	public AviaryHighlightImageButton ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryHighlightImageButtonStyle );
	}

	public AviaryHighlightImageButton ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );

		final Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( attrs, R.styleable.AviaryHighlightImageButton, defStyle, 0 );
		mHighlightColorPressed = a.getColor( R.styleable.AviaryHighlightImageButton_aviary_highlightColorPressed, Color.WHITE );
		mHighlightColorChecked = a.getColor( R.styleable.AviaryHighlightImageButton_aviary_highlightColorChecked, Color.WHITE );
		mHighlightColorSelected = a.getColor( R.styleable.AviaryHighlightImageButton_aviary_highlightColorSelected, Color.WHITE );
		mToggleEnabled = a.getBoolean( R.styleable.AviaryHighlightImageButton_aviary_toggleable, false );
		mUnToggleUserEnabled = a.getBoolean( R.styleable.AviaryHighlightImageButton_aviary_untoggleable, true );
		String mode = a.getString( R.styleable.AviaryHighlightImageButton_aviary_blendMode );

		mGlowStatus = a.getInt( R.styleable.AviaryHighlightImageButton_aviary_glowMode, 0 );
		mGlowSize = a.getInt( R.styleable.AviaryHighlightImageButton_aviary_glowSize, 7 );
		mHighlightMode = a.getInteger( R.styleable.AviaryHighlightImageButton_aviary_highlightMode, 2 );

		boolean checked = a.getBoolean( R.styleable.AviaryHighlightImageButton_aviary_checked, false );
		if ( !mToggleEnabled ) checked = false;

		if ( UIUtils.checkBits( mGlowStatus, UIUtils.GLOW_MODE_PRESSED ) ) {
			mColorFilterTintPressed = new LightingColorFilter( mHighlightColorPressed, mHighlightColorPressed );
		}

		if ( UIUtils.checkBits( mGlowStatus, UIUtils.GLOW_MODE_CHECKED ) ) {
			mColorFilterTintChecked = new LightingColorFilter( mHighlightColorChecked, mHighlightColorChecked );
		}

		if ( UIUtils.checkBits( mGlowStatus, UIUtils.GLOW_MODE_SELECTED ) ) {
			mColorFilterTintSelected = new LightingColorFilter( mHighlightColorSelected, mHighlightColorSelected );
		}

		if ( null != mode ) {
			mBlendMode = Mode.valueOf( mode );
		} else {
			mBlendMode = Mode.MULTIPLY;
		}

		setChecked( checked, false );

		// check if the View is focusable first
		boolean focusable = a.getBoolean( R.styleable.AviaryHighlightImageButton_android_focusable, true );

		a.recycle();
		setFocusable( focusable );

		final Drawable drawable = getDrawable();

		if ( drawable instanceof GlowBitmapDrawable ) {
			GlowBitmapDrawable glow = (GlowBitmapDrawable) getDrawable();
			glow.updateConfig( mHighlightColorPressed, mHighlightColorChecked, mHighlightColorSelected, mGlowSize, mHighlightMode, mGlowStatus );
		}

		// TODO: verify this
		// } else if( drawable instanceof GlowDrawable ) {
		// ((GlowDrawable)drawable).update( mHighlightColorPressed,
		// mHighlightColorChecked, mHighlightColorSelected, mGlowSize, mHighlightMode,
		// mGlowStatus );
		// }
	}

	@Override
	public void setImageDrawable( Drawable drawable ) {

		if ( drawable instanceof BitmapDrawable ) {

			Drawable current = getDrawable();
			if ( current instanceof GlowBitmapDrawable ) {
				( (GlowBitmapDrawable) current ).setBitmap( ( (BitmapDrawable) drawable ).getBitmap() );
				drawable = current;
			} else {
				drawable = new GlowBitmapDrawable( getResources(), ( (BitmapDrawable) drawable ).getBitmap(), mHighlightColorPressed, mHighlightColorChecked,
						mHighlightColorSelected, mGlowSize, mHighlightMode, mGlowStatus );
			}
		} else if ( drawable instanceof StateListDrawable ) {
			// TODO: verify this
			// Drawable current = getDrawable();
			// if( current instanceof GlowDrawable ) {
			// ((GlowDrawable)current).setDrawable( drawable );
			// drawable = current;
			// } else {
			// drawable = new GlowDrawable( getResources(), drawable,
			// mHighlightColorPressed, mHighlightColorChecked, mHighlightColorSelected,
			// mGlowSize, mHighlightMode, mGlowStatus );
			// }
		}
		super.setImageDrawable( drawable );
	}

	@Override
	public void setImageBitmap( Bitmap bm ) {

		Drawable current = getDrawable();
		if ( current instanceof GlowBitmapDrawable ) {
			( (GlowBitmapDrawable) current ).setBitmap( bm );
			setImageDrawable( (GlowBitmapDrawable) current );
			return;
		}

		setImageDrawable( new GlowBitmapDrawable( getResources(), bm, mHighlightColorPressed, mHighlightColorChecked, mHighlightColorSelected, mGlowSize,
				mHighlightMode, mGlowStatus ) );
	}

	@Override
	public void setImageResource( int resId ) {

		Resources res = getResources();
		try {
			Drawable d = res.getDrawable( resId );
			setImageDrawable( d );
			invalidate();
			return;
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		super.setImageResource( resId );
	}

	@Override
	protected boolean onSetAlpha( int alpha ) {
		return false;
	}

	@Override
	public void onInitializeAccessibilityEvent( AccessibilityEvent event ) {
		super.onInitializeAccessibilityEvent( event );
		event.setClassName( AviaryHighlightImageButton.class.getName() );
		event.setChecked( mChecked );
	}

	@Override
	public void onInitializeAccessibilityNodeInfo( AccessibilityNodeInfo info ) {
		super.onInitializeAccessibilityNodeInfo( info );

		if ( android.os.Build.VERSION.SDK_INT >= 14 ) {
			info.setClassName( AviaryHighlightImageButton.class.getName() );
		}

		info.setCheckable( mToggleEnabled );
		info.setChecked( mChecked );
	}

	@Override
	public void setPressed( boolean pressed ) {
		super.setPressed( pressed );

		final Drawable d = getDrawable();

		if ( null != d && null != mBlendMode ) {
			updateDrawable( d, pressed, isChecked(), isSelected() );
		}
	}

	@Override
	public void setSelected( boolean selected ) {
		super.setSelected( selected );

		final Drawable d = getDrawable();

		if ( null != d && null != mBlendMode ) {
			updateDrawable( d, isPressed(), isChecked(), selected );
		}
	}

	public boolean isChecked() {
		return mChecked;
	}

	public void setChecked( boolean checked ) {
		setChecked( checked, false );
	}

	protected void setChecked( boolean checked, boolean fromUser ) {
		if ( !mToggleEnabled ) return;

		if ( mChecked != checked ) {
			mChecked = checked;
			refreshDrawableState();

			if ( mBroadcasting ) {
				return;
			}

			mBroadcasting = true;
			if ( mOnCheckedChangeListener != null ) {
				mOnCheckedChangeListener.onCheckedChanged( this, mChecked, fromUser );
			}
			mBroadcasting = false;
		}
	}

	public void setOnCheckedChangeListener( OnCheckedChangeListener listener ) {
		mOnCheckedChangeListener = listener;
	}

	@Override
	public int[] onCreateDrawableState( int extraSpace ) {
		final Drawable drawable = getDrawable();
		final int[] drawableState = super.onCreateDrawableState( extraSpace + 1 );

		if ( null != drawable && null != mBlendMode ) {
			final boolean pressed = isPressed();
			final boolean checked = isChecked();
			final boolean selected = isSelected();

			updateDrawable( drawable, pressed, checked, selected );

			if ( checked ) {
				mergeDrawableStates( drawableState, CHECKED_STATE_SET );
			}
		}
		return drawableState;
	}

	protected void updateDrawable( Drawable drawable, boolean pressed, boolean checked, boolean selected ) {

		if ( drawable.isStateful() ) {
			invalidate();
			return;
		}

		if ( pressed || checked || selected ) {

			if ( pressed && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_PRESSED ) ) {
				if ( mColorFilterTintPressed != null ) {
					drawable.setColorFilter( mColorFilterTintPressed );
				} else {
					drawable.setColorFilter( mHighlightColorPressed, mBlendMode );
				}
			} else if ( checked && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_CHECKED ) ) {
				if ( mColorFilterTintChecked != null ) {
					drawable.setColorFilter( mColorFilterTintChecked );
				} else {
					drawable.setColorFilter( mHighlightColorChecked, mBlendMode );
				}
			} else if ( selected && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_SELECTED ) ) {
				if ( mColorFilterTintSelected != null ) {
					drawable.setColorFilter( mColorFilterTintSelected );
				} else {
					drawable.setColorFilter( mHighlightColorChecked, mBlendMode );
				}
			}
		} else {
			drawable.clearColorFilter();
		}
	}

	public void toggle() {
		if ( mChecked && !mUnToggleUserEnabled ) {
			return;
		}
		setChecked( !mChecked, true );
	}

	@Override
	public boolean performClick() {
		toggle();
		return super.performClick();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState( superState );
		ss.checked = isChecked();
		return ss;
	}

	@Override
	public void onRestoreInstanceState( Parcelable state ) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState( ss.getSuperState() );
		setChecked( ss.checked, false );
		requestLayout();
	}

}
