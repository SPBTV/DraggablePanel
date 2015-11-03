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
 * Transformer extension created to scale the view instead of resize it as the other
 * implementation does. This implementation is based on Nineoldanroids library to scale
 * the view.
 *
 * @author Pedro Vicente Gómez Sánchez
 */
class ScaleTransformer extends Transformer {

    ScaleTransformer(View dragView, View resizeView, View parent) {
        super(dragView, resizeView, parent);
    }

    @Override
    public void updateScaleAndPosition(float verticalDragOffset) {
        getResizeView().setScaleX(getScaleX(verticalDragOffset));
        getResizeView().setScaleY(getScaleY(verticalDragOffset));
        getResizeView().setPivotX(getDragView().getWidth() - (getMarginRight() / getScaleX(1)));
        getResizeView().setPivotY(0);
    }

}
