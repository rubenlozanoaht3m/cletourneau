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
package com.qmuiteam.qmui.skin.handler;

import android.content.res.ColorStateList;
import android.view.View;

import com.qmuiteam.qmui.layout.IQMUILayout;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

public class QMUISkinRuleBorderHandler extends QMUISkinRuleColorStateListHandler {

    @Override
    void handle(View view, String name, ColorStateList colorStateList) {

        if (view instanceof IQMUILayout) {
            ((IQMUILayout) view).setBorderColor(colorStateList.getDefaultColor());
        } else if (view instanceof QMUIRadiusImageView) {
            ((QMUIRadiusImageView) view).setBorderColor(colorStateList.getDefaultColor());
        } else if (view instanceof QMUIRoundButton) {
            ((QMUIRoundButton) view).setStrokeColors(colorStateList);
        }
    }
}
