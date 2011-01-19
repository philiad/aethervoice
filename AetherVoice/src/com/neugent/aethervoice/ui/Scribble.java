package com.neugent.aethervoice.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.view.View;

/**
 * Class for the scribble/memo pad
 * 
 * @author Amando Jose Quinto II
 * 
 */
public class Scribble extends View {
	private static final int INVALID_POINTER_ID = -1;
	

	/** The paint used in drawing the path. **/
	private final Paint mPaint;

	/** The canvas for the path to be drawn. **/
	private static Canvas mCanvas;

	/** The path 1 to be drawn. **/
	private final Path mPath1;
	
	/** The path 2 to be drawn. **/
	private final Path mPath2;

	/** The paint used by the bitmap **/
	private final Paint mBitmapPaint;

	/** Bitmap for the screen **/
	private final Bitmap mBitmap = Bitmap.createBitmap(480, 390,
			Bitmap.Config.ARGB_8888);

	/** The flag for erasing the canvas. **/
	private boolean mErase = false;

	/** The starting point for x-coordinate. **/
	private float mX1;

	/** The starting point for y-coordinate. **/
	private float mY1;
	
	/** The starting point for x-coordinate. **/
	private float mX2;

	/** The starting point for y-coordinate. **/
	private float mY2;

	/** The tolerance of the finger movement. **/
	private static final float TOUCH_TOLERANCE = 4;
	
    private int mActivePointerId = INVALID_POINTER_ID;

	/**
	 * Instantiate the Scribble.
	 * 
	 * @param context
	 *            The application context
	 */
	public Scribble(final Context context) {
		super(context);

		Scribble.mCanvas = new Canvas(mBitmap);
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		
		mPath1 = new Path();
		mPath2 = new Path();

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.RED);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(6);

	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw,
			final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	/**
	 * Draw/Erases the path determined by the user
	 * 
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(final Canvas canvas) {
		if (mErase) {
			final Paint p = new Paint();
			p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			Scribble.mCanvas.drawRect(0, 0, getWidth(), getHeight(), p);
			mErase = false;
		} else {
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

			canvas.drawPath(mPath1, mPaint);
			canvas.drawPath(mPath2, mPaint);
		}
	}

	/**
	 * 
	 */
	public void eraseAll() {
		mErase = true;
		invalidate();
	}

	/**
	 * @param x
	 *            the x-coordinate
	 * @param y
	 *            the y-coordinate
	 * 
	 * @see android.view.View#onTouchEvent(MotionEvent)
	 * @see android.view.MotionEvent#ACTION_DOWN
	 */
	private void touch_start(final float x, final float y, final int index) {
		switch(index){
		case 0:
			mPath1.reset();
			mPath1.moveTo(x, y);
			mX1 = x;
			mY1 = y;
			break;
		case 1:
			mPath2.reset();
			mPath2.moveTo(x, y);
			mX2 = x;
			mY2 = y;
			break;
		}
	}

	/**
	 * Draws the path.
	 * 
	 * @param x
	 *            the x-coordinate
	 * @param y
	 *            the y-coordinate
	 * 
	 * @see android.view.View#onTouchEvent(MotionEvent)
	 * @see android.view.MotionEvent#ACTION_MOVE
	 */
	private void touch_move(final float x, final float y, int pointerIndex) {
		switch(pointerIndex){
		case 0:
			if (Math.abs(x - mX1) >= Scribble.TOUCH_TOLERANCE || Math.abs(y - mY1) >= Scribble.TOUCH_TOLERANCE) {
				mPath1.quadTo(mX1, mY1, (x + mX1) / 2, (y + mY1) / 2);
				mX1 = x;
				mY1 = y;
			}
			break;
		case 1:
			if (Math.abs(x - mX2) >= Scribble.TOUCH_TOLERANCE || Math.abs(y - mY2) >= Scribble.TOUCH_TOLERANCE) {
				mPath2.quadTo(mX2, mY2, (x + mX2) / 2, (y + mY2) / 2);
				mX2 = x;
				mY2 = y;
			}
			break;
		}
	}

	/**
	 * Finishes the path.
	 * 
	 * @see android.view.View#onTouchEvent(MotionEvent)
	 * @see android.view.MotionEvent#ACTION_UP
	 */
	private void touch_up(int index) {
		switch(index){
		case 0:
			mPath1.lineTo(mX1, mY1);
			// commit the path to our offscreen
			Scribble.mCanvas.drawPath(mPath1, mPaint);
			// kill this so we don't double draw
			mPath1.reset();
			break;
		case 1:
			mPath2.lineTo(mX2, mY2);
			// commit the path to our offscreen
			Scribble.mCanvas.drawPath(mPath2, mPaint);
			// kill this so we don't double draw
			mPath2.reset();
			break;
		}
		
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final int action = event.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:{
			final float x = event.getX();
			final float y = event.getY();
			
	        // Save the ID of this pointer
	        mActivePointerId = event.getPointerId(0);
	        
//	        System.out.println("AetherVoice ++++++++++ ACTION_DOWN mX1 "+mX1 +" : x "+x+" === mY1 "+mY1+" : y "+y);
			
			touch_start(x, y, 0);
			invalidate();
			break;
		} case MotionEvent.ACTION_POINTER_DOWN:{
			final int pointerIndex2 = event.findPointerIndex(event.getPointerCount() - 1);

	        final float x = event.getX(pointerIndex2);
	        final float y = event.getY(pointerIndex2);
			
//			System.out.println("AetherVoice ++++++++++ ACTION_POINTER_DOWN mX2 "+mX2 +" : x2 "+x+" === mY2 "+mY2+" : y2 "+y);
			
			touch_start(x, y, 1);
			invalidate();
			break;
		} case MotionEvent.ACTION_MOVE:{
			final int count = event.getPointerCount();
			// Find the index of the active pointer and fetch its position
	        final int pointerIndex = event.findPointerIndex(mActivePointerId);
	        final int pointerIndex2 = event.findPointerIndex(count - 1);

	        final float x = event.getX(pointerIndex);
	        final float y = event.getY(pointerIndex);
	        
	        touch_move(x, y, pointerIndex);
	        	        
	        if(count > 1 && pointerIndex2 != pointerIndex){
	        	final float x2 = event.getX(pointerIndex2);
		        final float y2= event.getY(pointerIndex2);
		        
		        touch_move(x2, y2, pointerIndex2);
	        }
	        
			invalidate();
			break;
		} case MotionEvent.ACTION_UP:
			mActivePointerId = INVALID_POINTER_ID;
//			System.out.println("AetherVoice ++++++++++++++++++++++++++ ACTION_UP");
			if(event.getPointerCount() < 2)
				touch_up(0);
			break;
			
		case MotionEvent.ACTION_CANCEL:
			mActivePointerId = INVALID_POINTER_ID;
			
//			System.out.println("AetherVoice +++++++++++++++++ ACTION_CANCEL");
			break;
		case MotionEvent.ACTION_POINTER_UP:
//			System.out.println("AetherVoice ++++++++++++++++++ ACTION_POINTER_UP");
			
			final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = event.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
		        // This was our active pointer going up. Choose a new
		        // active pointer and adjust accordingly.
		        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
		        mActivePointerId = event.getPointerId(newPointerIndex);
		        touch_up(0);
		        mPath1.moveTo(mX2, mY2);
		        mX1 = mX2;
		        mY1 = mY2;
			}
//			System.out.println("AetherVoice ++++++++++++++++++++++ mActivePointerId "+mActivePointerId);
			touch_up(1);
			break;
		}
		//invalidate();
		return true;
	}

}
