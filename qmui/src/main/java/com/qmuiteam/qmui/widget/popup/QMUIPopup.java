/*
 * Tencent is pleased to support the open source community by making QMUI_Android available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qmuiteam.qmui.widget.popup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.qmuiteam.qmui.R;
import com.qmuiteam.qmui.layout.QMUIFrameLayout;
import com.qmuiteam.qmui.layout.QMUILayoutHelper;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIResHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.AnimRes;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;


public class QMUIPopup extends QMUIBasePopup<QMUIPopup> {
    public static final int ANIM_AUTO = 0;
    public static final int ANIM_GROW_FROM_LEFT = 1;
    public static final int ANIM_GROW_FROM_RIGHT = 2;
    public static final int ANIM_GROW_FROM_CENTER = 3;
    public static final int ANIM_SPEC = 4;

    @IntDef(value = {ANIM_AUTO, ANIM_GROW_FROM_LEFT, ANIM_GROW_FROM_RIGHT, ANIM_GROW_FROM_CENTER, ANIM_SPEC})
    @interface AnimStyle {
    }

    public static final int DIRECTION_TOP = 0;
    public static final int DIRECTION_BOTTOM = 1;
    public static final int DIRECTION_CENTER_IN_SCREEN = 2;

    @IntDef({DIRECTION_CENTER_IN_SCREEN, DIRECTION_TOP, DIRECTION_BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {
    }

    protected @AnimStyle int mAnimStyle;
    protected int mSpecAnimStyle;
    private int mEdgeProtectionTop;
    private int mEdgeProtectionLeft;
    private int mEdgeProtectionRight;
    private int mEdgeProtectionBottom;
    private boolean mShowArrow = true;
    private boolean mAddShadow = false;
    private int mRadius = NOT_SET;
    private int mBorderColor = NOT_SET;
    private int mBorderWidth = NOT_SET;
    private int mShadowElevation = NOT_SET;
    private float mShadowAlpha = 0f;
    private int mShadowInset = NOT_SET;
    private int mBgColor = NOT_SET;
    private int mOffsetX = 0;
    private int mOffsetYIfTop = 0;
    private int mOffsetYIfBottom = 0;
    private @Direction int mPreferredDirection = DIRECTION_BOTTOM;
    private int mInitWidth;
    private int mInitHeight;
    private int[] mAnchorLocation = new int[2];
    private int mArrowWidth = NOT_SET;
    private int mArrowHeight = NOT_SET;
    private boolean mRemoveBorderWhenShadow = false;
    private View mContentView;

    public QMUIPopup(Context context, int width, int height) {
        super(context);
        mInitWidth = width;
        mInitHeight = height;
    }

    public QMUIPopup arrow(boolean showArrow) {
        mShowArrow = showArrow;
        return this;
    }

    public QMUIPopup arrowSize(int width, int height) {
        mArrowWidth = width;
        mArrowHeight = height;
        return this;
    }

    public QMUIPopup shadow(boolean addShadow) {
        mAddShadow = addShadow;
        return this;
    }

    public QMUIPopup border(@ColorInt int borderColor, int borderWidth) {
        mBorderColor = borderColor;
        mBorderWidth = borderWidth;
        return this;
    }

    public QMUIPopup removeBorderWhenShadow(boolean removeBorderWhenShadow) {
        mRemoveBorderWhenShadow = removeBorderWhenShadow;
        return this;
    }

    public QMUIPopup animStyle(@AnimStyle int animStyle) {
        mAnimStyle = animStyle;
        return this;
    }

    public QMUIPopup customAnimStyle(@AnimRes int animStyle) {
        mAnimStyle = ANIM_SPEC;
        mSpecAnimStyle = animStyle;
        return this;
    }

    public QMUIPopup radius(int radius) {
        mRadius = radius;
        return this;
    }

    public QMUIPopup shadowElevation(int shadowElevation, float shadowAlpha) {
        mShadowAlpha = shadowAlpha;
        mShadowElevation = shadowElevation;
        return this;
    }

    public QMUIPopup shadowInset(int shadowInset) {
        mShadowInset = shadowInset;
        return this;
    }

    public QMUIPopup edgeProtection(int distance) {
        mEdgeProtectionLeft = distance;
        mEdgeProtectionRight = distance;
        mEdgeProtectionTop = distance;
        mEdgeProtectionBottom = distance;
        return this;
    }

    public QMUIPopup edgeProtection(int left, int top, int right, int bottom) {
        mEdgeProtectionLeft = left;
        mEdgeProtectionRight = top;
        mEdgeProtectionTop = right;
        mEdgeProtectionBottom = bottom;
        return this;
    }

    public QMUIPopup offsetX(int offsetX) {
        mOffsetX = offsetX;
        return this;
    }

    public QMUIPopup offsetYIfTop(int y) {
        mOffsetYIfTop = y;
        return this;
    }

    public QMUIPopup offsetYIfBottom(int y) {
        mOffsetYIfBottom = y;
        return this;
    }

    public QMUIPopup preferredDirection(@Direction int preferredDirection) {
        mPreferredDirection = preferredDirection;
        return this;
    }

    public QMUIPopup view(View contentView) {
        mContentView = contentView;
        return this;
    }

    public QMUIPopup view(@LayoutRes int contentViewResId) {
        return view(LayoutInflater.from(mContext).inflate(contentViewResId, null));
    }

    class ShowInfo {
        int screenWidth = QMUIDisplayHelper.getScreenWidth(mContext);
        int screenHeight = QMUIDisplayHelper.getScreenHeight(mContext);
        int width;
        int height;
        int x;
        int y;
        View anchor;
        int anchorCenter;
        int direction = mPreferredDirection;
        int contentWidthMeasureSpec;
        int contentHeightMeasureSpec;
        int decorationLeft = 0;
        int decorationRight = 0;
        int decorationTop = 0;
        int decorationBottom = 0;

        float anchorProportion() {
            return (anchorCenter - x) / (float) width;
        }

        int windowWidth() {
            return decorationLeft + width + decorationRight;
        }

        int windowHeight() {
            return decorationTop + height + decorationBottom;
        }
    }

    private boolean shouldShowShadow() {
        return mAddShadow && QMUILayoutHelper.useFeature();
    }

    public QMUIPopup show(@NonNull View anchor) {
        if (mContentView == null) {
            throw new RuntimeException("you should call view() to set your content view");
        }
        ShowInfo showInfo = new ShowInfo();
        calculateWindowSize(showInfo);
        getInfoFromAnchor(showInfo, anchor);
        calculateXY(showInfo);
        adjustShowInfo(showInfo);
        decorateContentView(showInfo);
        setAnimationStyle(showInfo.anchorProportion(), showInfo.direction);
        mWindow.setWidth(showInfo.windowWidth());
        mWindow.setHeight(showInfo.windowHeight());
        showAtLocation(anchor, showInfo.x, showInfo.y);
        return this;
    }

    private void getInfoFromAnchor(ShowInfo showInfo, @NonNull View anchor) {
        showInfo.anchor = anchor;
        showInfo.anchorCenter = mAnchorLocation[0] + anchor.getWidth() / 2;
        anchor.getLocationOnScreen(mAnchorLocation);
    }

    private void decorateContentView(ShowInfo showInfo) {
        ContentView contentView = ContentView.wrap(mContentView, mInitWidth, mInitHeight);
        if (mBorderColor == NOT_SET) {
            mBorderColor = QMUIResHelper.getAttrColor(mContext, R.attr.qmui_popup_border_color);
            mBorderWidth = QMUIResHelper.getAttrDimen(mContext, R.attr.qmui_popup_border_width);
        }

        if (mBgColor == NOT_SET) {
            mBgColor = QMUIResHelper.getAttrColor(mContext, R.attr.qmui_popup_bg_color);
        }
        contentView.setBackgroundColor(mBgColor);
        contentView.setBorderColor(mBorderColor);
        contentView.setBorderWidth(mBorderWidth);
        contentView.setShowBorderOnlyBeforeL(mRemoveBorderWhenShadow);
        if (mRadius == NOT_SET) {
            mRadius = QMUIResHelper.getAttrDimen(mContext, R.attr.qmui_popup_radius);
        }

        if (shouldShowShadow()) {
            contentView.setRadiusAndShadow(mRadius, mShadowElevation, mShadowAlpha);
        } else {
            contentView.setRadius(mRadius);
        }

        DecorRootView decorRootView = new DecorRootView(mContext, showInfo);
        decorRootView.setContentView(contentView);
        mWindow.setContentView(decorRootView);
    }

    private void adjustShowInfo(ShowInfo showInfo) {
        if (shouldShowShadow()) {
            if (mShadowElevation == NOT_SET) {
                mShadowElevation = QMUIResHelper.getAttrDimen(mContext, R.attr.qmui_popup_shadow_elevation);
                mShadowAlpha = QMUIResHelper.getAttrFloatValue(mContext, R.attr.qmui_popup_shadow_alpha);
            }
            if (mShadowInset == NOT_SET) {
                mShadowInset = QMUIResHelper.getAttrDimen(mContext, R.attr.qmui_popup_shadow_inset);
            }

            int originX = showInfo.x, originY = showInfo.y;
            if (originX - mShadowInset > 0) {
                showInfo.x -= mShadowInset;
                showInfo.decorationLeft = mShadowInset;
            } else {
                showInfo.decorationLeft = originX;
                showInfo.x = 0;
            }
            if (originX + showInfo.width + mShadowInset < showInfo.screenWidth) {
                showInfo.decorationRight = mShadowInset;
            } else {
                showInfo.decorationRight = showInfo.screenWidth - originX - showInfo.width;
            }
            if (originY - mShadowInset > 0) {
                showInfo.y -= mShadowInset;
                showInfo.decorationTop = mShadowInset;
            } else {
                showInfo.decorationTop = originY;
                showInfo.y = 0;
            }
            if (originY + showInfo.height + mShadowInset < showInfo.screenHeight) {
                showInfo.decorationBottom = mShadowInset;
            } else {
                showInfo.decorationBottom = showInfo.screenHeight - originY - showInfo.height;
            }
        }

        if (mShowArrow && showInfo.direction != DIRECTION_CENTER_IN_SCREEN) {
            if (mArrowWidth == NOT_SET) {
                mArrowWidth = QMUIResHelper.getAttrDimen(mContext, R.attr.qmui_popup_arrow_width);
            }
            if (mArrowHeight == NOT_SET) {
                mArrowHeight = QMUIResHelper.getAttrDimen(mContext, R.attr.qmui_popup_arrow_height);
            }
            if (showInfo.direction == DIRECTION_BOTTOM) {
                if (shouldShowShadow()) {
                    showInfo.y += mArrowHeight;
                }
                showInfo.decorationTop = Math.max(showInfo.decorationTop, mArrowHeight);
            } else if (showInfo.direction == DIRECTION_TOP) {
                showInfo.decorationBottom = Math.max(showInfo.decorationBottom, mArrowHeight);
                showInfo.y -= mArrowHeight;
            }
        }
    }

    private void calculateXY(ShowInfo showInfo) {
        if (showInfo.anchorCenter < showInfo.screenWidth / 2) { // anchor point on the left
            showInfo.x = Math.max(mEdgeProtectionLeft, showInfo.anchorCenter - showInfo.width / 2 + mOffsetX);
        } else { // anchor point on the left
            showInfo.x = Math.min(showInfo.screenWidth - mEdgeProtectionRight - showInfo.width,
                    showInfo.anchorCenter - showInfo.width / 2 + mOffsetX);
        }
        int nextDirection = DIRECTION_CENTER_IN_SCREEN;
        if (mPreferredDirection == DIRECTION_BOTTOM) {
            nextDirection = DIRECTION_TOP;
        } else if (mPreferredDirection == DIRECTION_TOP) {
            nextDirection = DIRECTION_BOTTOM;
        }
        handleDirection(showInfo, mPreferredDirection, nextDirection);
    }

    private void handleDirection(ShowInfo showInfo, int currentDirection, int nextDirection) {
        if (currentDirection == DIRECTION_CENTER_IN_SCREEN) {
            showInfo.x = (showInfo.screenWidth - showInfo.width) / 2;
            showInfo.y = (showInfo.screenHeight - showInfo.height) / 2;
            showInfo.direction = DIRECTION_CENTER_IN_SCREEN;
        } else if (currentDirection == DIRECTION_TOP) {
            showInfo.y = mAnchorLocation[1] - showInfo.height - mOffsetYIfTop;
            if (showInfo.y < mEdgeProtectionTop) {
                handleDirection(showInfo, nextDirection, DIRECTION_CENTER_IN_SCREEN);
            } else {
                showInfo.direction = DIRECTION_TOP;
            }
        } else if (currentDirection == DIRECTION_BOTTOM) {
            showInfo.y = mAnchorLocation[1] + showInfo.anchor.getHeight() + mOffsetYIfBottom;
            if (showInfo.y > showInfo.screenHeight - mEdgeProtectionBottom - showInfo.height) {
                handleDirection(showInfo, nextDirection, DIRECTION_CENTER_IN_SCREEN);
            } else {
                showInfo.direction = DIRECTION_BOTTOM;
            }
        }
    }

    private void calculateWindowSize(ShowInfo showInfo) {
        boolean needMeasureForWidth = false, needMeasureForHeight = false;
        if (mInitWidth > 0) {
            showInfo.width = mInitWidth;
            showInfo.contentWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    mInitWidth, View.MeasureSpec.EXACTLY);
        } else {
            int maxWidth = showInfo.screenWidth - mEdgeProtectionLeft - mEdgeProtectionRight;
            if (mInitWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                showInfo.width = maxWidth;
                showInfo.contentWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        maxWidth, View.MeasureSpec.EXACTLY);
            } else {
                needMeasureForWidth = true;
                showInfo.contentWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        maxWidth, View.MeasureSpec.AT_MOST);
            }
        }
        if (mInitHeight > 0) {
            showInfo.height = mInitHeight;
            showInfo.contentHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    mInitHeight, View.MeasureSpec.EXACTLY);
        } else {
            int maxHeight = showInfo.screenHeight - mEdgeProtectionTop - mEdgeProtectionBottom;
            if (mInitHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                showInfo.height = maxHeight;
                showInfo.contentHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        maxHeight, View.MeasureSpec.EXACTLY);
            } else {
                needMeasureForHeight = true;
                showInfo.contentHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        maxHeight, View.MeasureSpec.AT_MOST);
            }
        }

        if (needMeasureForWidth || needMeasureForHeight) {
            mContentView.measure(
                    showInfo.contentWidthMeasureSpec, showInfo.contentHeightMeasureSpec);
            if (needMeasureForWidth) {
                showInfo.width = mContentView.getMeasuredWidth();
            }
            if (needMeasureForHeight) {
                showInfo.height = mContentView.getMeasuredHeight();
            }
        }
    }

    private void setAnimationStyle(float anchorProportion, @Direction int direction) {
        boolean onTop = direction == DIRECTION_TOP;
        switch (mAnimStyle) {
            case ANIM_GROW_FROM_LEFT:
                mWindow.setAnimationStyle(onTop ? R.style.QMUI_Animation_PopUpMenu_Left : R.style.QMUI_Animation_PopDownMenu_Left);
                break;

            case ANIM_GROW_FROM_RIGHT:
                mWindow.setAnimationStyle(onTop ? R.style.QMUI_Animation_PopUpMenu_Right : R.style.QMUI_Animation_PopDownMenu_Right);
                break;

            case ANIM_GROW_FROM_CENTER:
                mWindow.setAnimationStyle(onTop ? R.style.QMUI_Animation_PopUpMenu_Center : R.style.QMUI_Animation_PopDownMenu_Center);
                break;
            case ANIM_AUTO:
                if (anchorProportion <= 0.25f) {
                    mWindow.setAnimationStyle(onTop ? R.style.QMUI_Animation_PopUpMenu_Left : R.style.QMUI_Animation_PopDownMenu_Left);
                } else if (anchorProportion > 0.25f && anchorProportion < 0.75f) {
                    mWindow.setAnimationStyle(onTop ? R.style.QMUI_Animation_PopUpMenu_Center : R.style.QMUI_Animation_PopDownMenu_Center);
                } else {
                    mWindow.setAnimationStyle(onTop ? R.style.QMUI_Animation_PopUpMenu_Right : R.style.QMUI_Animation_PopDownMenu_Right);
                }
                break;
            case ANIM_SPEC:
                mWindow.setAnimationStyle(mSpecAnimStyle);
                break;
        }
    }

    static class ContentView extends QMUIFrameLayout {
        private ContentView(Context context) {
            super(context);
        }

        static ContentView wrap(View businessView, int width, int height) {
            ContentView contentView = new ContentView(businessView.getContext());
            if (businessView.getParent() != null) {
                ((ViewGroup) businessView.getParent()).removeView(businessView);
            }
            contentView.addView(businessView, new FrameLayout.LayoutParams(width, height));
            return contentView;
        }
    }

    class DecorRootView extends FrameLayout {
        private ShowInfo mShowInfo;
        private View mContentView;
        private Paint mArrowPaint;
        private Path mArrowPath;

        private int mPendingWidth;
        private int mPendingHeight;
        private Runnable mUpdateWindowAction = new Runnable() {
            @Override
            public void run() {
                mShowInfo.width = mPendingWidth;
                mShowInfo.height = mPendingHeight;
                calculateXY(mShowInfo);
                adjustShowInfo(mShowInfo);
                mWindow.update(mShowInfo.x, mShowInfo.y, mShowInfo.windowWidth(), mShowInfo.windowHeight());
            }
        };

        private DecorRootView(Context context, ShowInfo showInfo) {
            super(context);
            mShowInfo = showInfo;
            mArrowPaint = new Paint();
            mArrowPaint.setAntiAlias(true);
            mArrowPath = new Path();
        }


        public void setContentView(View contentView) {
            if (mContentView != null) {
                removeView(mContentView);
            }
            if (contentView.getParent() != null) {
                ((ViewGroup) contentView.getParent()).removeView(contentView);
            }
            mContentView = contentView;
            addView(contentView);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            removeCallbacks(mUpdateWindowAction);
            if (mContentView != null) {
                mContentView.measure(mShowInfo.contentWidthMeasureSpec, mShowInfo.contentHeightMeasureSpec);
                int measuredWidth = mContentView.getMeasuredWidth();
                int measuredHeight = mContentView.getMeasuredHeight();
                if (mShowInfo.width != measuredWidth || mShowInfo.height != measuredHeight) {
                    mPendingWidth = measuredWidth;
                    mPendingHeight = measuredHeight;
                    post(mUpdateWindowAction);
                }
            }
            setMeasuredDimension(mShowInfo.windowWidth(), mShowInfo.windowHeight());
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if (mContentView != null) {
                mContentView.layout(mShowInfo.decorationLeft, mShowInfo.decorationTop,
                        mShowInfo.width + mShowInfo.decorationLeft,
                        mShowInfo.height + mShowInfo.decorationTop);
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            removeCallbacks(mUpdateWindowAction);
        }



        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (mShowArrow) {
                if (mShowInfo.direction == DIRECTION_TOP) {
                    canvas.save();
                    mArrowPaint.setStyle(Paint.Style.FILL);
                    mArrowPaint.setColor(mBgColor);
                    int l = mShowInfo.anchorCenter - mShowInfo.x - mArrowWidth / 2;
                    l = Math.min(Math.max(l, mShowInfo.decorationLeft),
                            getWidth() - mShowInfo.decorationRight - mArrowWidth);
                    int t = mShowInfo.decorationTop + mShowInfo.height - mBorderWidth - 1;
                    canvas.translate(l, t);
                    mArrowPath.reset();
                    mArrowPath.setLastPoint(0, 0);
                    mArrowPath.lineTo(mArrowWidth / 2, mArrowHeight);
                    mArrowPath.lineTo(mArrowWidth, 0);
                    mArrowPath.close();
                    canvas.drawPath(mArrowPath, mArrowPaint);
                    if (!mRemoveBorderWhenShadow || !shouldShowShadow()) {
                        mArrowPaint.setStrokeWidth(mBorderWidth);
                        mArrowPaint.setColor(mBorderColor);
                        mArrowPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawLine(0, 0, mArrowWidth / 2, mArrowHeight, mArrowPaint);
                        canvas.drawLine(mArrowWidth / 2, mArrowHeight, mArrowWidth, 0, mArrowPaint);
                    }
                    canvas.restore();
                } else if (mShowInfo.direction == DIRECTION_BOTTOM) {
                    canvas.save();
                    mArrowPaint.setStyle(Paint.Style.FILL);
                    mArrowPaint.setColor(mBgColor);
                    int l = mShowInfo.anchorCenter - mShowInfo.x - mArrowWidth / 2;
                    l = Math.min(Math.max(l, mShowInfo.decorationLeft),
                            getWidth() - mShowInfo.decorationRight - mArrowWidth);
                    int t = mShowInfo.decorationTop + mBorderWidth + 1;
                    canvas.translate(l, t);
                    mArrowPath.reset();
                    mArrowPath.setLastPoint(0, 0);
                    mArrowPath.lineTo(mArrowWidth / 2, -mArrowHeight);
                    mArrowPath.lineTo(mArrowWidth, 0);
                    mArrowPath.close();
                    canvas.drawPath(mArrowPath, mArrowPaint);
                    if (!mRemoveBorderWhenShadow || !shouldShowShadow()) {
                        mArrowPaint.setStrokeWidth(mBorderWidth);
                        mArrowPaint.setStyle(Paint.Style.STROKE);
                        mArrowPaint.setColor(mBorderColor);
                        canvas.drawLine(0, 0, mArrowWidth / 2, -mArrowHeight, mArrowPaint);
                        canvas.drawLine(mArrowWidth / 2, -mArrowHeight, mArrowWidth, 0, mArrowPaint);
                    }
                    canvas.restore();
                }
            }
        }
    }
}