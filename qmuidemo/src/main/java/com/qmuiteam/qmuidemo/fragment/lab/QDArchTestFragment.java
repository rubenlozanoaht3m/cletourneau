package com.qmuiteam.qmuidemo.fragment.lab;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.qmuiteam.qmui.arch.QMUIFragment;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.qmuiteam.qmuidemo.QDMainActivity;
import com.qmuiteam.qmuidemo.R;
import com.qmuiteam.qmuidemo.activity.ArchTestActivity;
import com.qmuiteam.qmuidemo.base.BaseFragment;
import com.qmuiteam.qmuidemo.lib.annotation.Widget;

import butterknife.BindView;
import butterknife.ButterKnife;


@Widget(name = "QMUIFragment", iconRes = R.mipmap.icon_grid_layout)
public class QDArchTestFragment extends BaseFragment {
    private static final String TAG = "QDArchTestFragment";
    private static final String ARG_INDEX = "arg_index";
    private static final int REQUEST_CODE = 1;
    private static final String DATA_TEST = "data_test";

    @BindView(R.id.topbar) QMUITopBarLayout mTopBar;
    @BindView(R.id.title) TextView mTitleTv;
    @BindView(R.id.btn) QMUIRoundButton mBtn;
    @BindView(R.id.btn_1) QMUIRoundButton mBtn1;


    @Override
    protected View onCreateView() {
        Bundle args = getArguments();
        final int index = args == null ? 1 : args.getInt(ARG_INDEX);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_arch_test, null);
        ButterKnife.bind(this, view);
        mTopBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        injectEntrance(mTopBar);
        mTopBar.setTitle(String.valueOf(index));
        mTitleTv.setText(String.valueOf(index));
        final int next = index + 1;
        final boolean destroyCurrent = next % 3 == 0;
        String btnText = destroyCurrent ? "startFragmentAndDestroyCurrent" : "startFragment";
        mBtn.setText(btnText);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QMUIFragment fragment = newInstance(next);
                if (destroyCurrent) {
                    startFragmentAndDestroyCurrent(fragment);
                } else {
                    startFragmentForResult(fragment, REQUEST_CODE);
                }

            }
        });
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
                Intent intent = QDMainActivity.createArchTestIntent(getContext());
                startActivity(intent);
            }
        });
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent();
        intent.putExtra(DATA_TEST, "test");
        setFragmentResult(RESULT_OK, intent);
    }

    public static QDArchTestFragment newInstance(int index) {
        Bundle args = new Bundle();
        args.putInt(ARG_INDEX, index);
        QDArchTestFragment fragment = new QDArchTestFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onFragmentResult(int requestCode, int resultCode, Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        if (data != null) {
            Log.i(TAG, data.getStringExtra(DATA_TEST));
        }
    }

    public static void injectEntrance(final QMUITopBarLayout topbar){
        topbar.addRightTextButton("new Activity", QMUIViewHelper.generateViewId())
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBottomSheetList(topbar.getContext());
                    }
                });
    }

    public static void showBottomSheetList(final Context context) {
        new QMUIBottomSheet.BottomListSheetBuilder(context)
                .addItem("Normal Arch Test")
                .addItem("WebView Test")
                .addItem("SurfaceView Test")
                .addItem("Directly Activity")
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        dialog.dismiss();
                        if(position == 0){
                            Intent intent = QDMainActivity.createArchTestIntent(context);
                            context.startActivity(intent);
                        }else if(position == 1){
                            Intent intent = QDMainActivity.createWebExplorerIntent(context,
                                    "https://github.com/QMUI/QMUI_Android",
                                    context.getResources().getString(R.string.about_item_github));
                            context.startActivity(intent);
                        } else if(position == 2){
                            Intent intent = QDMainActivity.createSurfaceTestIntent(context);
                            context.startActivity(intent);
                        } else  if(position == 3){
                            Intent intent = new Intent(context, ArchTestActivity.class);
                            context.startActivity(intent);
                        }
                    }
                })
                .build()
                .show();
    }
}
