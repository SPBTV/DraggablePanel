/*
 * Copyright (C) 2014 Pedro Vicente Gómez Sánchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs.transformer;

import android.view.View;

/**
 * Abstract class created to be implemented by different classes are going to change the size of a
 * dragView. The most basic one is going to scale the dragView and the most complex used with
 * VideoView is
 * going to change the size of the dragView.
 * <p/>
 * The dragView used in this class has to be contained by a RelativeLayout.
 * <p/>
 * This class also provide information about the size of the dragView and the position because
 * different Transformer implementations could change the size of the dragView but not the
 * position,
 * like ScaleTransformer does.
 *
 * @author Pedro Vicente Gómez Sánchez
 */
public abstract class Transformer {

    private final View dragView;
    private final View resizeView;
    private final View parent;

    private int marginRight;
    private int marginBottom;

    private float xScaleFactor;
    private float yScaleFactor;
    private int minHeight;
    private int minWidth;

    public Transformer(View dragView, View resizeView, View parent) {
        this.dragView = dragView;
        this.resizeView = resizeView;
        this.parent = parent;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    protected int getMinHeight() {
        return minHeight;
    }

    protected int getMinWidth() {
        return minWidth;
    }

    public float getXScaleFactor() {
        if (minHeight > 0 && minWidth > 0) {
            return getOriginalWidth() / (float) minWidth;
        }

        return xScaleFactor;
    }

    public void setXScaleFactor(float xScaleFactor) {
        this.xScaleFactor = xScaleFactor;
    }

    public float getYScaleFactor() {
        if (minHeight > 0 && minWidth > 0) {
            return getOriginalHeight() / (float) minHeight;
        }

        return yScaleFactor;
    }

    public void setYScaleFactor(float yScaleFactor) {
        this.yScaleFactor = yScaleFactor;
    }

    protected float getScaleY(float verticalDragOffset) {
        return calculateScale(verticalDragOffset, getYScaleFactor());
    }

    protected float getScaleX(float verticalDragOffset) {
        return calculateScale(verticalDragOffset, getXScaleFactor());
    }

    protected float calculateScale(float verticalDragOffset, float scaleFactor) {
        return ((scaleFactor - 1) * (1 - verticalDragOffset) + 1) / scaleFactor;
    }

    public int getMarginRight() {
        return marginRight;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = Math.round(marginRight);
    }

    public int getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = Math.round(marginBottom);
    }

    protected View getDragView() {
        return dragView;
    }

    protected View getResizeView() {
        return resizeView;
    }

    protected View getParentView() {
        return parent;
    }

    public abstract void updateScaleAndPosition(float verticalDragOffset);

    /**
     * @return height of the dragView before it has change the size.
     */
    public int getOriginalHeight() {
        return dragView.getMeasuredHeight();
    }

    /**
     * @return width of the dragView before it has change the size.
     */
    public int getOriginalWidth() {
        return dragView.getMeasuredWidth();
    }

    public boolean isAboveTheMiddle() {
        int parentHeight = parent.getHeight();
        float viewYPosition = dragView.getY() + (dragView.getHeight() * 0.5f);
        return viewYPosition < (parentHeight * 0.5);
    }

    /**
     * @return true if the right corner of the view matches with the parent view width.
     */
    public boolean isViewAtRight() {
        return getDragView().getRight() == getParentView().getWidth();
    }

    /**
     * @return true if the left position of the view is to the left of sixty percent of the parent
     * width.
     */
    public boolean isNextToLeftBound() {
        return (getDragView().getRight() - getMarginRight()) < getParentView().getWidth() * 0.6;
    }

    /**
     * @return true if the right position of the view is to the right of the one hundred twenty five
     * five percent of the parent view width.
     */
    public boolean isNextToRightBound() {
        return (getDragView().getRight() - getMarginRight()) > getParentView().getWidth() * 1.25;
    }

    /**
     * @return min view height taking into account the configured margin.
     */
    public int getMinHeightPlusMargin() {
        if (getMinWidth() != 0 && getMinHeight() != 0) {
            return getMinHeight() + getMarginBottom();
        }

        return (int) (getOriginalHeight() * getScaleY(1)) + getMarginBottom();
    }
}
