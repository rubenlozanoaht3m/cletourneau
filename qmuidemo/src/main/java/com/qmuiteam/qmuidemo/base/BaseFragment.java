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

package com.qmuiteam.qmuidemo.base;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Bundle;
import android.view.View;

import com.qmuiteam.qmui.arch.QMUIFragment;
import com.qmuiteam.qmui.skin.QMUISkinMaker;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmuidemo.QDApplication;
import com.qmuiteam.qmuidemo.QDMainActivity;
import com.qmuiteam.qmuidemo.fragment.home.HomeFragment;
import com.qmuiteam.qmuidemo.manager.QDDataManager;
import com.qmuiteam.qmuidemo.manager.QDUpgradeManager;
import com.qmuiteam.qmuidemo.model.QDItemDescription;

/**
 * Created by cgspine on 2018/1/7.
 */

public abstract class BaseFragment extends QMUIFragment {

    private int mBindId = -1;

    public BaseFragment() {
    }

    @Override
    protected int backViewInitOffset() {
        return QMUIDisplayHelper.dp2px(getContext(), 100);
    }

    @Override
    public void onResume() {
        super.onResume();
        QDUpgradeManager.getInstance(getContext()).runUpgradeTipTaskIfExist(getActivity());
    }

    @Override
    public Object onLastFragmentFinish() {
        return new HomeFragment();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(QDApplication.openSkinMake){
            openSkinMaker();
        }
    }

    public void openSkinMaker(){
        if(mBindId < 0){
            mBindId = QMUISkinMaker.getInstance().bind(this);
        }
    }

    public void closeSkinMaker(){
        QMUISkinMaker.getInstance().unBind(mBindId);
        mBindId = -1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        closeSkinMaker();
    }

    protected void goToWebExplorer(@NonNull String url, @Nullable String title) {
        Intent intent = QDMainActivity.createWebExplorerIntent(getContext(), url, title);
        startActivity(intent);
    }

    protected void injectDocToTopBar(QMUITopBar topBar) {
        final QDItemDescription description = QDDataManager.getInstance().getDescription(this.getClass());
        if (description != null) {
            topBar.addRightTextButton("DOC", QMUIViewHelper.generateViewId())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            goToWebExplorer(description.getDocUrl(), description.getName());
                        }
                    });
        }
    }

    protected void injectDocToTopBar(QMUITopBarLayout topBar){
        final QDItemDescription description = QDDataManager.getInstance().getDescription(this.getClass());
        if (description != null) {
            topBar.addRightTextButton("DOC", QMUIViewHelper.generateViewId())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            goToWebExplorer(description.getDocUrl(), description.getName());
                        }
                    });
        }
    }
}
