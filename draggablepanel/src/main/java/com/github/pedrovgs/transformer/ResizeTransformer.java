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
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Transformer extension created to resize the view instead of scale it as the other
 * implementation does. This implementation is based on change the LayoutParams.
 *
 * @author Pedro Vicente Gómez Sánchez
 */
class ResizeTransformer extends Transformer {

    private final FrameLayout.LayoutParams layoutParams;

    ResizeTransformer(View dragView, View resizeView, View parent) {
        super(dragView, resizeView, parent);
        layoutParams = (FrameLayout.LayoutParams) resizeView.getLayoutParams();
    }

    /**
     * Changes view scale using view's LayoutParam.
     *
     * @param verticalDragOffset used to calculate the new size.
     */
    @Override
    public void updateScaleAndPosition(float verticalDragOffset) {
        if (verticalDragOffset == 0) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            layoutParams.width = (int) (getOriginalWidth() * getScaleX(verticalDragOffset));
            layoutParams.height = (int) (getOriginalHeight() * getScaleY(verticalDragOffset));
        }

        layoutParams.bottomMargin = (int) (getMarginBottom() * verticalDragOffset);
        layoutParams.rightMargin = (int) (getMarginRight() * verticalDragOffset);

        getResizeView().setLayoutParams(layoutParams);
        getDragView().layout(getDragView().getLeft(), getDragView().getTop(),
                getDragView().getRight(), getDragView().getBottom());
    }

    /**
     * Calculate the current view right position for a given verticalDragOffset.
     *
     * @param verticalDragOffset used to calculate the new right position.
     */
    private int getViewRightPosition(float verticalDragOffset) {
        return (int) ((getOriginalWidth()) - getMarginRight() * verticalDragOffset);
    }

}
