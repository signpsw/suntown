package com.suntown.suntownshop.widget;

import com.suntown.suntownshop.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
/**
 * Item��������ʾɾ����ť��ListView
 * @author Administrator
 *
 */
public class SwipeListView extends ListView {
	private Boolean mIsHorizontal;

	private View mPreItemView;

	private View mCurrentItemView;

	private float mFirstX;

	private float mFirstY;

	private int mRightViewWidth;

	// private boolean mIsInAnimation = false;
	private final int mDuration = 100;

	private final int mDurationStep = 10;

	private boolean mIsShown;
	
	private boolean isSwipeable = true;

	public SwipeListView(Context context) {
		this(context, null);
	}

	public SwipeListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
				R.styleable.swipelistviewstyle);

		// ��ȡ�Զ������Ժ�Ĭ��ֵ
		mRightViewWidth = (int) mTypedArray.getDimension(
				R.styleable.swipelistviewstyle_right_width, 200);

		mTypedArray.recycle();
	}

	/**
	 * return true, deliver to listView. return false, deliver to child. if
	 * move, return true
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if(!isSwipeable){
			return super.onInterceptTouchEvent(ev);
		}
		float lastX = ev.getX();
		float lastY = ev.getY();
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mIsHorizontal = null;
			System.out.println("onInterceptTouchEvent----->ACTION_DOWN");
			mFirstX = lastX;
			mFirstY = lastY;
			int motionPosition = pointToPosition((int) mFirstX, (int) mFirstY);

			if (motionPosition >= 0) {
				View currentItemView = getChildAt(motionPosition
						- getFirstVisiblePosition());
				if (mCurrentItemView != null) {
					mPreItemView = mCurrentItemView;
				}
				mCurrentItemView = currentItemView;
			} else {
				if (mCurrentItemView != null) {
					mPreItemView = mCurrentItemView;
				}
				mCurrentItemView = null; // ����listview�հ״�
			}
			break;

		case MotionEvent.ACTION_MOVE:
			float dx = lastX - mFirstX;
			float dy = lastY - mFirstY;

			if (Math.abs(dx) >= 5 && Math.abs(dy) >= 5) {
				return true;
			}
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			System.out.println("onInterceptTouchEvent----->ACTION_UP");
			if (mIsShown
					&& (mPreItemView != mCurrentItemView || isHitCurItemLeft(lastX))) {
				System.out.println("1---> hiddenRight");
				/**
				 * ���һ��
				 * <p>
				 * һ��Item���ұ߲����Ѿ���ʾ��
				 * <p>
				 * ��ʱ��������һ��item, ��ô�Ǹ��ұ߲�����ʾ��item�������ұ߲���
				 */
				hiddenRight(mPreItemView);
			}
			break;
		}

		return super.onInterceptTouchEvent(ev);
	}

	private boolean isHitCurItemLeft(float x) {
		return x < getWidth() - mRightViewWidth;
	}

	/**
	 * @param dx
	 * @param dy
	 * @return judge if can judge scroll direction
	 */
	private boolean judgeScrollDirection(float dx, float dy) {
		boolean canJudge = true;

		if (Math.abs(dx) > 30 && Math.abs(dx) > 2 * Math.abs(dy)) {
			mIsHorizontal = true;
			System.out.println("mIsHorizontal---->" + mIsHorizontal);
		} else if (Math.abs(dy) > 30 && Math.abs(dy) > 2 * Math.abs(dx)) {
			mIsHorizontal = false;
			System.out.println("mIsHorizontal---->" + mIsHorizontal);
		} else {
			canJudge = false;
		}

		return canJudge;
	}

	/**
	 * return false, can't move any direction. return true, cant't move
	 * vertical, can move horizontal. return super.onTouchEvent(ev), can move
	 * both.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if(!isSwipeable){
			return super.onTouchEvent(ev);
		}
		float lastX = ev.getX();
		float lastY = ev.getY();

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			System.out.println("---->ACTION_DOWN");
			break;

		case MotionEvent.ACTION_MOVE:
			float dx = lastX - mFirstX;
			float dy = lastY - mFirstY;

			// confirm is scroll direction
			if (mIsHorizontal == null) {
				
				if (!judgeScrollDirection(dx, dy)) {
					break;
				}
				
			}

			if (mIsHorizontal) {
				if (mCurrentItemView == null) {
					/**
					 * ���6�������listview�հ״�������ԭ��item�ұ߲���
					 */
					if(mIsShown){
						System.out.println("6---> hiddenRight");
						hiddenRight(mPreItemView);
					}
					return true;
				}
				if (mIsShown && mPreItemView != mCurrentItemView) {
					System.out.println("2---> hiddenRight");
					/**
					 * �������
					 * <p>
					 * һ��Item���ұ߲����Ѿ���ʾ��
					 * <p>
					 * ��ʱ�����һ�������һ��item,�Ǹ��ұ߲�����ʾ��item�������ұ߲���
					 * <p>
					 * ���󻬶�ֻ��������������һ������ᴥ�������
					 */
					hiddenRight(mPreItemView);
				}

				if (mIsShown && mPreItemView == mCurrentItemView) {
					dx = dx - mRightViewWidth;
					System.out.println("======dx " + dx);
				}

				// can't move beyond boundary
				if (dx < 0 && dx > -mRightViewWidth) {
					mCurrentItemView.scrollTo((int) (-dx), 0);
				}

				return true;
			} else {
				if (mIsShown) {
					System.out.println("3---> hiddenRight");
					/**
					 * �������
					 * <p>
					 * һ��Item���ұ߲����Ѿ���ʾ��
					 * <p>
					 * ��ʱ�����¹���ListView,��ô�Ǹ��ұ߲�����ʾ��item�������ұ߲���
					 */
					hiddenRight(mPreItemView);
				}
			}

			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			System.out.println("============ACTION_UP");
			clearPressedState();
			System.out.println("============ACTION_CANCEL");
			if (mIsShown) {
				System.out.println("4---> hiddenRight");
				/**
				 * ����ģ�
				 * <p>
				 * һ��Item���ұ߲����Ѿ���ʾ��
				 * <p>
				 * ��ʱ�����һ�����ǰһ��item,�Ǹ��ұ߲�����ʾ��item�������ұ߲���
				 */
				hiddenRight(mPreItemView);
			}

			if (mIsHorizontal != null && mIsHorizontal) {
				if (mCurrentItemView == null) {
					return true;
				}
				if (mFirstX - lastX > mRightViewWidth / 2) {
					showRight(mCurrentItemView);
				} else {
					System.out.println("5---> hiddenRight");
					/**
					 * ����壺
					 * <p>
					 * ���һ���һ��item,�һ����ľ��볬�����ұ�View�Ŀ��ȵ�һ�룬����֮��
					 */
					hiddenRight(mCurrentItemView);
				}

				return true;
			}

			break;
		}

		return super.onTouchEvent(ev);
	}

	private void clearPressedState() {
		// TODO current item is still has background, issue
		// �˴����ж�mCurrentItemView�Ƿ�Ϊnull,���򵱵�һ�ε��Ϊlistview�հ״������� by ken
		// 2014-12-24
		if (mCurrentItemView != null) {
			mCurrentItemView.setPressed(false);
		}
		setPressed(false);
		refreshDrawableState();
		// invalidate();
	}

	private void showRight(View view) {
		System.out.println("=========showRight");

		Message msg = new MoveHandler().obtainMessage();
		msg.obj = view;
		msg.arg1 = view.getScrollX();
		msg.arg2 = mRightViewWidth;
		msg.sendToTarget();

		mIsShown = true;
	}

	private void hiddenRight(View view) {
		System.out.println("=========hiddenRight");
		/*if (mCurrentItemView == null) {
			return;
		}*/
		Message msg = new MoveHandler().obtainMessage();//
		msg.obj = view;
		msg.arg1 = view.getScrollX();
		msg.arg2 = 0;
		System.out.println("RX------>"+msg.arg1);
		msg.sendToTarget();

		mIsShown = false;
	}

	/**
	 * show or hide right layout animation
	 */
	@SuppressLint("HandlerLeak")
	class MoveHandler extends Handler {
		int stepX = 0;

		int fromX;

		int toX;

		View view;

		private boolean mIsInAnimation = false;

		private void animatioOver() {
			mIsInAnimation = false;
			stepX = 0;
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (stepX == 0) {
				if (mIsInAnimation) {
					return;
				}
				mIsInAnimation = true;
				view = (View) msg.obj;
				fromX = msg.arg1;
				toX = msg.arg2;
				stepX = (int) ((toX - fromX) * mDurationStep * 1.0 / mDuration);
				if (stepX < 0 && stepX > -1) {
					stepX = -1;
				} else if (stepX > 0 && stepX < 1) {
					stepX = 1;
				}
				if (Math.abs(toX - fromX) < 10) {
					view.scrollTo(toX, 0);
					animatioOver();
					return;
				}
			}

			fromX += stepX;
			boolean isLastStep = (stepX > 0 && fromX > toX)
					|| (stepX < 0 && fromX < toX);
			if (isLastStep) {
				fromX = toX;
			}

			view.scrollTo(fromX, 0);
			invalidate();

			if (!isLastStep) {
				this.sendEmptyMessageDelayed(0, mDurationStep);
			} else {
				animatioOver();
			}
		}
	}

	public int getRightViewWidth() {
		return mRightViewWidth;
	}

	public void setRightViewWidth(int mRightViewWidth) {
		this.mRightViewWidth = mRightViewWidth;
	}

	public boolean isSwipeable() {
		return isSwipeable;
	}

	public void setSwipeable(boolean isSwipeable) {
		this.isSwipeable = isSwipeable;
	}
	
	
}