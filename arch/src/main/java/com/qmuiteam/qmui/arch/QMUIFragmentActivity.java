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

package com.qmuiteam.qmui.arch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelStoreOwner;

import com.qmuiteam.qmui.QMUILog;
import com.qmuiteam.qmui.arch.annotation.DefaultFirstFragment;
import com.qmuiteam.qmui.arch.first.FirstFragmentFinder;
import com.qmuiteam.qmui.arch.first.FirstFragmentFinders;
import com.qmuiteam.qmui.util.DoNotInterceptKeyboardInset;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.util.QMUIWindowInsetHelper;
import com.qmuiteam.qmui.widget.QMUIWindowInsetLayout;

/**
 * the container activity for {@link QMUIFragment}.
 * Created by cgspine on 15/9/14.
 */
public abstract class QMUIFragmentActivity extends InnerBaseActivity implements QMUIFragmentContainerProvider {
    public static final String QMUI_INTENT_DST_FRAGMENT = "qmui_intent_dst_fragment";
    public static final String QMUI_INTENT_DST_FRAGMENT_NAME = "qmui_intent_dst_fragment_name";
    public static final String QMUI_INTENT_FRAGMENT_ARG = "qmui_intent_fragment_arg";
    private static final String TAG = "QMUIFragmentActivity";
    private RootView mRootView;
    private boolean mIsFirstFragmentAdded = false;

    static {
        QMUIWindowInsetHelper.addHandleContainer(FragmentContainerView.class);
    }

    @Override
    public int getContextViewId() {
        return R.id.qmui_activity_fragment_container_id;
    }

    @Override
    public FragmentManager getContainerFragmentManager() {
        return getSupportFragmentManager();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        performTranslucent();
        mRootView = onCreateRootView(getContextViewId());
        setContentView(mRootView);
        if (savedInstanceState == null) {
            long start = System.currentTimeMillis();
            Intent intent = getIntent();
            Class<? extends QMUIFragment> firstFragmentClass = null;

            // 1. try get first fragment from annotation @FirstFragments.
            int dstFragment = intent.getIntExtra(QMUI_INTENT_DST_FRAGMENT, -1);
            if (dstFragment != -1) {
                FirstFragmentFinder finder = FirstFragmentFinders.getInstance().get(getClass());
                if (finder != null) {
                    firstFragmentClass = finder.getFragmentClassById(dstFragment);
                }

            }

            // 2. try get first fragment from fragment class name
            if (firstFragmentClass == null) {
                String fragmentClassName = intent.getStringExtra(QMUI_INTENT_DST_FRAGMENT_NAME);
                if (fragmentClassName != null) {
                    try {
                        firstFragmentClass = (Class<? extends QMUIFragment>) Class.forName(fragmentClassName);
                    } catch (ClassNotFoundException e) {
                        QMUILog.d(TAG, "Can not find " + fragmentClassName);
                    }
                }
            }

            // 3. try get fragment from annotation @DefaultFirstFragment
            if (firstFragmentClass == null) {
                firstFragmentClass = getDefaultFirstFragment();
            }

            if (firstFragmentClass != null) {
                QMUIFragment firstFragment = instantiationFirstFragment(firstFragmentClass, intent);
                if (firstFragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(getContextViewId(), firstFragment, firstFragment.getClass().getSimpleName())
                            .addToBackStack(firstFragment.getClass().getSimpleName())
                            .commit();
                    mIsFirstFragmentAdded = true;
                }
            }
            Log.i(TAG, "the time it takes to inject first fragment from annotation is " + (System.currentTimeMillis() - start));
        }
    }

    protected void performTranslucent() {
        QMUIStatusBarHelper.translucent(this);
    }

    /**
     * used for subclasses to see if the parent class initializes the first fragment。
     * it must be called after super.onCreate in subclasses.
     *
     * @return true if first fragment is initialized.
     */
    protected boolean isFirstFragmentAdded() {
        return mIsFirstFragmentAdded;
    }

    protected void setFirstFragmentAdded(boolean firstFragmentAdded) {
        mIsFirstFragmentAdded = firstFragmentAdded;
    }

    protected Class<? extends QMUIFragment> getDefaultFirstFragment() {
        Class<?> cls = getClass();
        while (cls != null && cls != QMUIFragmentActivity.class && QMUIFragmentActivity.class.isAssignableFrom(cls)) {
            if (cls.isAnnotationPresent(DefaultFirstFragment.class)) {
                DefaultFirstFragment defaultFirstFragment = cls.getAnnotation(DefaultFirstFragment.class);
                if (defaultFirstFragment != null) {
                    return defaultFirstFragment.value();
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    protected QMUIFragment instantiationFirstFragment(Class<? extends QMUIFragment> cls, Intent intent) {
        try {
            QMUIFragment fragment = cls.newInstance();
            Bundle args = intent.getBundleExtra(QMUI_INTENT_FRAGMENT_ARG);
            if (args != null) {
                fragment.setArguments(args);
            }
            return fragment;
        } catch (IllegalAccessException e) {
            QMUILog.d(TAG, "Can not access " + cls.getName() + " for first fragment");
        } catch (InstantiationException e) {
            QMUILog.d(TAG, "Can not instance " + cls.getName() + " for first fragment");
        }
        return null;
    }

    @Override
    public FragmentContainerView getFragmentContainerView() {
        return mRootView.getFragmentContainerView();
    }

    @Override
    public ViewModelStoreOwner getContainerViewModelStoreOwner() {
        return this;
    }

    protected RootView onCreateRootView(int fragmentContainerId) {
        return new DefaultRootView(this, fragmentContainerId);
    }

    /**
     * get the current Fragment.
     */
    @Nullable
    public Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(getContextViewId());
    }

    @Nullable
    private QMUIFragment getCurrentQMUIFragment() {
        Fragment current = getCurrentFragment();
        if (current instanceof QMUIFragment) {
            return (QMUIFragment) current;
        }
        return null;
    }


    public static Intent intentOf(@NonNull Context context,
                                  @NonNull Class<? extends QMUIFragmentActivity> targetActivity,
                                  @NonNull Class<? extends QMUIFragment> firstFragment) {
        return intentOf(context, targetActivity, firstFragment, null);
    }

    /**
     * create a intent for a new QMUIFragmentActivity
     *
     * @param context        Generally it is activity
     * @param targetActivity target activity class
     * @param firstFragment  first fragment in target activity
     * @param fragmentArgs   args for first fragment
     * @return
     */
    public static Intent intentOf(@NonNull Context context,
                                  @NonNull Class<? extends QMUIFragmentActivity> targetActivity,
                                  @NonNull Class<? extends QMUIFragment> firstFragment,
                                  @Nullable Bundle fragmentArgs) {
        Intent intent = new Intent(context, targetActivity);
        FirstFragmentFinder finder = FirstFragmentFinders.getInstance().get(targetActivity);
        int dstId = FirstFragmentFinder.NO_ID;
        if (finder != null) {
            dstId = finder.getIdByFragmentClass(firstFragment);
        }
        intent.putExtra(QMUI_INTENT_DST_FRAGMENT, dstId);
        intent.putExtra(QMUI_INTENT_DST_FRAGMENT_NAME, firstFragment.getName());
        if (fragmentArgs != null) {
            intent.putExtra(QMUI_INTENT_FRAGMENT_ARG, fragmentArgs);
        }
        return intent;
    }

    public static abstract class RootView extends QMUIWindowInsetLayout {


        public RootView(Context context, int fragmentContainerId) {
            super(context);
        }

        @Override
        public boolean applySystemWindowInsets21(Object insets) {
            super.applySystemWindowInsets21(insets);
            return true;
        }

        @Override
        public boolean applySystemWindowInsets19(Rect insets) {
            super.applySystemWindowInsets19(insets);
            return true;
        }

        public abstract FragmentContainerView getFragmentContainerView();
    }

    @SuppressLint("ViewConstructor")
    @DoNotInterceptKeyboardInset
    public static class DefaultRootView extends RootView {
        private FragmentContainerView mFragmentContainerView;

        public DefaultRootView(Context context, int fragmentContainerId) {
            super(context, fragmentContainerId);
            mFragmentContainerView = new FragmentContainerView(context);
            mFragmentContainerView.setId(fragmentContainerId);
            addView(mFragmentContainerView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mFragmentContainerView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    for (int i = 0; i < getChildCount(); i++) {
                        SwipeBackLayout.updateLayoutInSwipeBack(getChildAt(i));
                    }
                }
            });
        }

        @Override
        public FragmentContainerView getFragmentContainerView() {
            return mFragmentContainerView;
        }
    }
}