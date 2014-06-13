/*
 * Copyright (C) 2014 Lucas Rocha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lucasr.twowayview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class TWGridView extends TWView {
    private static final String LOGTAG = "TwoWayGridView";

    private static final int NUM_COLS = 2;
    private static final int NUM_ROWS = 2;

    private TWLayoutState mLayoutState;

    private int mNumColumns;
    private int mNumRows;
    private int mLaneSize;

    private boolean mIsVertical;

    private final Rect mTempRect = new Rect();

    public TWGridView(Context context) {
        this(context, null);
    }

    public TWGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TWGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TWGridView, defStyle, 0);
        mNumColumns = Math.max(NUM_COLS, a.getInt(R.styleable.TWGridView_numColumns, -1));
        mNumRows = Math.max(NUM_ROWS, a.getInt(R.styleable.TWGridView_numRows, -1));
        a.recycle();

        Orientation orientation = getOrientation();
        mLayoutState = new TWLayoutState(orientation, getLaneCount());
        mIsVertical = (orientation == Orientation.VERTICAL);
    }

    private int getLaneCount() {
        return (mIsVertical ? mNumColumns : mNumRows);
    }

    private int getLaneForPosition(int position) {
        return (position % getLaneCount());
    }

    private int getChildFrame(View child, int lane, Flow flow, Rect frame) {
        mLayoutState.get(lane, mTempRect);

        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();

        final int delta;

        if (mIsVertical) {
            frame.left = mTempRect.left;
            frame.right = mTempRect.right;

            final boolean hasSpacing = (mTempRect.top != mTempRect.bottom);
            if (flow == Flow.FORWARD) {
                frame.top = mTempRect.bottom + (hasSpacing ? getVerticalSpacing() : 0);
                frame.bottom = frame.top + childHeight;
                delta = frame.bottom - mTempRect.bottom;
            } else {
                frame.top = mTempRect.top - childHeight - (hasSpacing ? getVerticalSpacing() : 0);
                frame.bottom = frame.top + childHeight;
                delta = mTempRect.top - frame.top;
            }
        } else {
            frame.top = mTempRect.top;
            frame.bottom = mTempRect.bottom;

            final boolean hasSpacing = (mTempRect.left != mTempRect.right);
            if (flow == Flow.FORWARD) {
                frame.left = mTempRect.right + (hasSpacing ? getHorizontalSpacing() : 0);
                frame.right = frame.left + childWidth;
                delta = frame.right - mTempRect.right;
            } else {
                frame.left = mTempRect.left - childWidth - (hasSpacing ? getHorizontalSpacing() : 0);
                frame.right = frame.left + childWidth;
                delta = mTempRect.left - frame.left;
            }
        }

        return delta;
    }

    private void clearLayout() {
        mLayoutState = new TWLayoutState(getOrientation(), getLaneCount());
    }

    @Override
    public void setOrientation(Orientation orientation) {
        final boolean changed = (getOrientation() != orientation);
        super.setOrientation(orientation);

        if (changed) {
            mIsVertical = (orientation == Orientation.VERTICAL);
            clearLayout();
        }
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public void setNumColumns(int numColumns) {
        if (mNumColumns == numColumns) {
            return;
        }

        mNumColumns = numColumns;
        if (mIsVertical) {
            clearLayout();
        }
    }

    public int getNumRows() {
        return mNumRows;
    }

    public void setNumRows(int numRows) {
        if (mNumRows == numRows) {
            return;
        }

        mNumRows = numRows;
        if (!mIsVertical) {
            clearLayout();
        }
    }

    @Override
    protected void offsetLayout(int offset) {
        mLayoutState.offset(offset);
    }

    @Override
    protected void resetLayout(int offset) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        final int laneCount = getLaneCount();
        final int verticalSpacing = getVerticalSpacing();
        final int horizontalSpacing = getHorizontalSpacing();

        if (mIsVertical) {
            final int width = getWidth() - paddingLeft - paddingRight;
            final int spacing = horizontalSpacing * (laneCount - 1);
            mLaneSize = (width - spacing) / laneCount;
        } else {
            final int height = getHeight() - paddingTop - paddingBottom;
            final int spacing = verticalSpacing * (laneCount - 1);
            mLaneSize = (height - spacing) / laneCount;
        }

        for (int i = 0; i < laneCount; i++) {
            final int spacing = i * (mIsVertical ? horizontalSpacing : verticalSpacing);
            final int start = (i * mLaneSize) + spacing;

            final int l = paddingLeft + (mIsVertical ? start : offset);
            final int t = paddingTop + (mIsVertical ? offset : start);
            final int r = (mIsVertical ? l + mLaneSize : l);
            final int b = (mIsVertical ? t : t + mLaneSize);

            mLayoutState.set(i, l, t, r, b);
        }
    }

    @Override
    protected int getOuterStartEdge() {
        return mLayoutState.getOuterStartEdge();
    }

    @Override
    protected int getInnerStartEdge() {
        return mLayoutState.getInnerStartEdge();
    }

    @Override
    protected int getInnerEndEdge() {
        return mLayoutState.getInnerEndEdge();
    }

    @Override
    protected int getOuterEndEdge() {
        return mLayoutState.getOuterEndEdge();
    }

    @Override
    protected int getChildWidthMeasureSpec(View child, int position, LayoutParams lp) {
        if (!mIsVertical && lp.width == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        }
    }

    @Override
    protected int getChildHeightMeasureSpec(View child, int position, LayoutParams lp) {
        if (mIsVertical && lp.height == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else if (!mIsVertical) {
            return MeasureSpec.makeMeasureSpec(mLaneSize, MeasureSpec.EXACTLY);
        } else {
            return MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }
    }

    @Override
    protected void detachChildFromLayout(View child, int position, Flow flow) {
        final int spacing = (mIsVertical ? getVerticalSpacing() : getHorizontalSpacing());
        final int dimension = (mIsVertical ? child.getHeight() : child.getWidth());
        final int lane = getLaneForPosition(position);
        mLayoutState.remove(lane, flow, dimension + spacing);
    }

    @Override
    protected void attachChildToLayout(View child, int position, Flow flow, Rect childRect) {
        final int lane = getLaneForPosition(position);
        mLayoutState.add(lane, flow, getChildFrame(child, lane, flow, childRect));
    }
}
