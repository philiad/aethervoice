package org.miscwidgets.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.neugent.aethervoice.R;

public class Panel extends LinearLayout {

	private static final String TAG = "Panel";

	/**
	 * Callback invoked when the panel is opened/closed.
	 */
	public static interface OnPanelListener {
		/**
		 * Invoked when the panel becomes fully closed.
		 */
		public void onPanelClosed(Panel panel);

		/**
		 * Invoked when the panel becomes fully opened.
		 */
		public void onPanelOpened(Panel panel);
	}

	private boolean mIsShrinking;
	private final int mPosition;
	private final int mDuration;
	private final boolean mLinearFlying;
	private final int mHandleId;
	private final int mContentId;
	private View mHandle;
	private View mContent;
	private final Drawable mOpenedHandle;
	private final Drawable mClosedHandle;
	private float mTrackX;
	private float mTrackY;
	private float mVelocity;

	private OnPanelListener panelListener;

	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;

	private enum State {
		ABOUT_TO_ANIMATE, ANIMATING, READY, TRACKING, FLYING,
	};

	private State mState;
	private Interpolator mInterpolator;
	private final GestureDetector mGestureDetector;
	private int mContentHeight;
	private int mContentWidth;
	private final int mOrientation;
	private float mWeight;
	private final PanelOnGestureListener mGestureListener;
	private boolean mBringToFront;

	public Panel(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.Panel);
		mDuration = a.getInteger(R.styleable.Panel_animationDuration, 750); // duration
																			// defaults
																			// to
																			// 750
																			// ms
		mPosition = a.getInteger(R.styleable.Panel_position, Panel.BOTTOM); // position
																			// defaults
																			// to
																			// BOTTOM
		mLinearFlying = a.getBoolean(R.styleable.Panel_linearFlying, false); // linearFlying
																				// defaults
																				// to
																				// false
		mWeight = a.getFraction(R.styleable.Panel_weight, 0, 1, 0.0f); // weight
																		// defaults
																		// to
																		// 0.0
		if (mWeight < 0 || mWeight > 1)
			mWeight = 0.0f;
		// Log.w(TAG, a.getPositionDescription() +
		// ": weight must be > 0 and <= 1");
		mOpenedHandle = a.getDrawable(R.styleable.Panel_openedHandle);
		mClosedHandle = a.getDrawable(R.styleable.Panel_closedHandle);

		RuntimeException e = null;
		mHandleId = a.getResourceId(R.styleable.Panel_handle, 0);
		if (mHandleId == 0)
			e = new IllegalArgumentException(
					a.getPositionDescription()
							+ ": The handle attribute is required and must refer to a valid child.");
		mContentId = a.getResourceId(R.styleable.Panel_content, 0);
		if (mContentId == 0)
			e = new IllegalArgumentException(
					a.getPositionDescription()
							+ ": The content attribute is required and must refer to a valid child.");
		a.recycle();

		if (e != null)
			throw e;
		mOrientation = (mPosition == Panel.TOP || mPosition == Panel.BOTTOM) ? LinearLayout.VERTICAL
				: LinearLayout.HORIZONTAL;
		setOrientation(mOrientation);
		mState = State.READY;
		mGestureListener = new PanelOnGestureListener();
		mGestureDetector = new GestureDetector(mGestureListener);
		mGestureDetector.setIsLongpressEnabled(false);

		// i DON'T really know why i need this...
		setBaselineAligned(false);
	}

	/**
	 * Sets the listener that receives a notification when the panel becomes
	 * open/close.
	 * 
	 * @param onPanelListener
	 *            The listener to be notified when the panel is opened/closed.
	 */
	public void setOnPanelListener(final OnPanelListener onPanelListener) {
		panelListener = onPanelListener;
	}

	/**
	 * Gets Panel's mHandle
	 * 
	 * @return Panel's mHandle
	 */
	public View getHandle() {
		return mHandle;
	}

	/**
	 * Gets Panel's mContent
	 * 
	 * @return Panel's mContent
	 */
	public View getContent() {
		return mContent;
	}

	/**
	 * Sets the acceleration curve for panel's animation.
	 * 
	 * @param i
	 *            The interpolator which defines the acceleration curve
	 */
	public void setInterpolator(final Interpolator i) {
		mInterpolator = i;
	}

	/**
	 * Set the opened state of Panel.
	 * 
	 * @param open
	 *            True if Panel is to be opened, false if Panel is to be closed.
	 * @param animate
	 *            True if use animation, false otherwise.
	 * 
	 * @return True if operation was performed, false otherwise.
	 * 
	 */
	public boolean setOpen(final boolean open, final boolean animate) {
		if (mState == State.READY && isOpen() ^ open) {
			mIsShrinking = !open;
			if (animate) {
				mState = State.ABOUT_TO_ANIMATE;
				if (!mIsShrinking)
					// this could make flicker so we test mState in
					// dispatchDraw()
					// to see if is equal to ABOUT_TO_ANIMATE
					mContent.setVisibility(View.VISIBLE);
				post(startAnimation);
			} else {
				mContent.setVisibility(open ? View.VISIBLE : View.GONE);
				postProcess();
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the opened status for Panel.
	 * 
	 * @return True if Panel is opened, false otherwise.
	 */
	public boolean isOpen() {
		return mContent.getVisibility() == View.VISIBLE;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mHandle = findViewById(mHandleId);
		if (mHandle == null) {
			final String name = getResources().getResourceEntryName(mHandleId);
			throw new RuntimeException(
					"Your Panel must have a child View whose id attribute is 'R.id."
							+ name + "'");
		}
		mHandle.setOnTouchListener(touchListener);
		mHandle.setOnClickListener(clickListener);

		mContent = findViewById(mContentId);
		if (mContent == null) {
			final String name = getResources().getResourceEntryName(mHandleId);
			throw new RuntimeException(
					"Your Panel must have a child View whose id attribute is 'R.id."
							+ name + "'");
		}

		// reposition children
		removeView(mHandle);
		removeView(mContent);
		if (mPosition == Panel.TOP || mPosition == Panel.LEFT) {
			addView(mContent);
			addView(mHandle);
		} else {
			addView(mHandle);
			addView(mContent);
		}

		if (mClosedHandle != null)
			mHandle.setBackgroundDrawable(mClosedHandle);
		mContent.setClickable(true);
		mContent.setVisibility(View.GONE);
		if (mWeight > 0) {
			final ViewGroup.LayoutParams params = mContent.getLayoutParams();
			if (mOrientation == LinearLayout.VERTICAL)
				params.height = ViewGroup.LayoutParams.FILL_PARENT;
			else
				params.width = ViewGroup.LayoutParams.FILL_PARENT;
			mContent.setLayoutParams(params);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		final ViewParent parent = getParent();
		if (parent != null && parent instanceof FrameLayout)
			mBringToFront = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mWeight > 0 && mContent.getVisibility() == View.VISIBLE) {
			final View parent = (View) getParent();
			if (parent != null)
				if (mOrientation == LinearLayout.VERTICAL)
					heightMeasureSpec = MeasureSpec.makeMeasureSpec(
							(int) (parent.getHeight() * mWeight),
							MeasureSpec.EXACTLY);
				else
					widthMeasureSpec = MeasureSpec.makeMeasureSpec(
							(int) (parent.getWidth() * mWeight),
							MeasureSpec.EXACTLY);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(final boolean changed, final int l, final int t,
			final int r, final int b) {
		super.onLayout(changed, l, t, r, b);
		mContentWidth = mContent.getWidth();
		mContentHeight = mContent.getHeight();
	}

	@Override
	protected void dispatchDraw(final Canvas canvas) {
		// String name = getResources().getResourceEntryName(getId());
		// Log.d(TAG, name + " ispatchDraw " + mState);
		// this is why 'mState' was added:
		// avoid flicker before animation start
		if (mState == State.ABOUT_TO_ANIMATE && !mIsShrinking) {
			int delta = mOrientation == LinearLayout.VERTICAL ? mContentHeight
					: mContentWidth;
			if (mPosition == Panel.LEFT || mPosition == Panel.TOP)
				delta = -delta;
			if (mOrientation == LinearLayout.VERTICAL)
				canvas.translate(0, delta);
			else
				canvas.translate(delta, 0);
		}
		if (mState == State.TRACKING || mState == State.FLYING)
			canvas.translate(mTrackX, mTrackY);
		super.dispatchDraw(canvas);
	}

	private float ensureRange(float v, final int min, final int max) {
		v = Math.max(v, min);
		v = Math.min(v, max);
		return v;
	}

	OnTouchListener touchListener = new OnTouchListener() {
		int initX;
		int initY;
		boolean setInitialPosition;

		public boolean onTouch(final View v, final MotionEvent event) {
			if (mState == State.ANIMATING)
				// we are animating
				return false;
			// Log.d(TAG, "state: " + mState + " x: " + event.getX() + " y: " +
			// event.getY());
			final int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				if (mBringToFront)
					bringToFront();
				initX = 0;
				initY = 0;
				if (mContent.getVisibility() == View.GONE)
					// since we may not know content dimensions we use factors
					// here
					if (mOrientation == LinearLayout.VERTICAL)
						initY = mPosition == Panel.TOP ? -1 : 1;
					else
						initX = mPosition == Panel.LEFT ? -1 : 1;
				setInitialPosition = true;
			} else {
				if (setInitialPosition) {
					// now we know content dimensions, so we multiply factors...
					initX *= mContentWidth;
					initY *= mContentHeight;
					// ... and set initial panel's position
					mGestureListener.setScroll(initX, initY);
					setInitialPosition = false;
					// for offsetLocation we have to invert values
					initX = -initX;
					initY = -initY;
				}
				// offset every ACTION_MOVE & ACTION_UP event
				event.offsetLocation(initX, initY);
			}
			if (!mGestureDetector.onTouchEvent(event))
				if (action == MotionEvent.ACTION_UP)
					// tup up after scrolling
					post(startAnimation);
			return false;
		}
	};

	OnClickListener clickListener = new OnClickListener() {
		public void onClick(final View v) {
			if (mBringToFront)
				bringToFront();
			if (initChange())
				post(startAnimation);
		}
	};

	public boolean initChange() {
		if (mState != State.READY)
			// we are animating or just about to animate
			return false;
		mState = State.ABOUT_TO_ANIMATE;
		mIsShrinking = mContent.getVisibility() == View.VISIBLE;
		if (!mIsShrinking)
			// this could make flicker so we test mState in dispatchDraw()
			// to see if is equal to ABOUT_TO_ANIMATE
			mContent.setVisibility(View.VISIBLE);
		return true;
	}

	Runnable startAnimation = new Runnable() {
		public void run() {
			// this is why we post this Runnable couple of lines above:
			// now its save to use mContent.getHeight() && mContent.getWidth()
			TranslateAnimation animation;
			int fromXDelta = 0, toXDelta = 0, fromYDelta = 0, toYDelta = 0;
			if (mState == State.FLYING)
				mIsShrinking = (mPosition == Panel.TOP || mPosition == Panel.LEFT)
						^ (mVelocity > 0);
			int calculatedDuration;
			if (mOrientation == LinearLayout.VERTICAL) {
				final int height = mContentHeight;
				if (!mIsShrinking)
					fromYDelta = mPosition == Panel.TOP ? -height : height;
				else
					toYDelta = mPosition == Panel.TOP ? -height : height;
				if (mState == State.TRACKING) {
					if (Math.abs(mTrackY - fromYDelta) < Math.abs(mTrackY
							- toYDelta)) {
						mIsShrinking = !mIsShrinking;
						toYDelta = fromYDelta;
					}
					fromYDelta = (int) mTrackY;
				} else if (mState == State.FLYING)
					fromYDelta = (int) mTrackY;
				// for FLYING events we calculate animation duration based on
				// flying velocity
				// also for very high velocity make sure duration >= 20 ms
				if (mState == State.FLYING && mLinearFlying) {
					calculatedDuration = (int) (1000 * Math
							.abs((toYDelta - fromYDelta) / mVelocity));
					calculatedDuration = Math.max(calculatedDuration, 20);
				} else
					calculatedDuration = mDuration
							* Math.abs(toYDelta - fromYDelta) / mContentHeight;
			} else {
				final int width = mContentWidth;
				if (!mIsShrinking)
					fromXDelta = mPosition == Panel.LEFT ? -width : width;
				else
					toXDelta = mPosition == Panel.LEFT ? -width : width;
				if (mState == State.TRACKING) {
					if (Math.abs(mTrackX - fromXDelta) < Math.abs(mTrackX
							- toXDelta)) {
						mIsShrinking = !mIsShrinking;
						toXDelta = fromXDelta;
					}
					fromXDelta = (int) mTrackX;
				} else if (mState == State.FLYING)
					fromXDelta = (int) mTrackX;
				// for FLYING events we calculate animation duration based on
				// flying velocity
				// also for very high velocity make sure duration >= 20 ms
				if (mState == State.FLYING && mLinearFlying) {
					calculatedDuration = (int) (1000 * Math
							.abs((toXDelta - fromXDelta) / mVelocity));
					calculatedDuration = Math.max(calculatedDuration, 20);
				} else
					calculatedDuration = mDuration
							* Math.abs(toXDelta - fromXDelta) / mContentWidth;
			}

			mTrackX = mTrackY = 0;
			if (calculatedDuration == 0) {
				mState = State.READY;
				if (mIsShrinking)
					mContent.setVisibility(View.GONE);
				postProcess();
				return;
			}

			animation = new TranslateAnimation(fromXDelta, toXDelta,
					fromYDelta, toYDelta);
			animation.setDuration(calculatedDuration);
			animation.setAnimationListener(animationListener);
			if (mState == State.FLYING && mLinearFlying)
				animation.setInterpolator(new LinearInterpolator());
			else if (mInterpolator != null)
				animation.setInterpolator(mInterpolator);
			startAnimation(animation);
		}
	};

	private final AnimationListener animationListener = new AnimationListener() {
		public void onAnimationEnd(final Animation animation) {
			mState = State.READY;
			if (mIsShrinking)
				mContent.setVisibility(View.GONE);
			postProcess();
		}

		public void onAnimationRepeat(final Animation animation) {
		}

		public void onAnimationStart(final Animation animation) {
			mState = State.ANIMATING;
		}
	};

	private void postProcess() {
		if (mIsShrinking && mClosedHandle != null)
			mHandle.setBackgroundDrawable(mClosedHandle);
		else if (!mIsShrinking && mOpenedHandle != null)
			mHandle.setBackgroundDrawable(mOpenedHandle);
		// invoke listener if any
		if (panelListener != null)
			if (mIsShrinking)
				panelListener.onPanelClosed(Panel.this);
			else
				panelListener.onPanelOpened(Panel.this);
	}

	class PanelOnGestureListener implements OnGestureListener {
		float scrollY;
		float scrollX;

		public void setScroll(final int initScrollX, final int initScrollY) {
			scrollX = initScrollX;
			scrollY = initScrollY;
		}

		public boolean onDown(final MotionEvent e) {
			scrollX = scrollY = 0;
			initChange();
			return true;
		}

		public boolean onFling(final MotionEvent e1, final MotionEvent e2,
				final float velocityX, final float velocityY) {
			mState = State.FLYING;
			mVelocity = mOrientation == LinearLayout.VERTICAL ? velocityY
					: velocityX;
			post(startAnimation);
			return true;
		}

		public void onLongPress(final MotionEvent e) {
			// not used
		}

		public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
				final float distanceX, final float distanceY) {
			mState = State.TRACKING;
			float tmpY = 0, tmpX = 0;
			if (mOrientation == LinearLayout.VERTICAL) {
				scrollY -= distanceY;
				if (mPosition == Panel.TOP)
					tmpY = ensureRange(scrollY, -mContentHeight, 0);
				else
					tmpY = ensureRange(scrollY, 0, mContentHeight);
			} else {
				scrollX -= distanceX;
				if (mPosition == Panel.LEFT)
					tmpX = ensureRange(scrollX, -mContentWidth, 0);
				else
					tmpX = ensureRange(scrollX, 0, mContentWidth);
			}
			if (tmpX != mTrackX || tmpY != mTrackY) {
				mTrackX = tmpX;
				mTrackY = tmpY;
				invalidate();
			}
			return true;
		}

		public void onShowPress(final MotionEvent e) {
			// not used
		}

		public boolean onSingleTapUp(final MotionEvent e) {
			// not used
			return false;
		}
	}
}
