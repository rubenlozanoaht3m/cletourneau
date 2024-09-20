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

package com.qmuiteam.qmui.skin;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.popup.QMUIPopups;
import com.qmuiteam.qmui.widget.tab.QMUITabSegment;
import com.tencent.mmkv.MMKV;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

/**
 * encode: prefix:methodName@fieldName   classFullName;codeValueInfo
 * prefix: r-ref,m-method,i-id,a-adapter,c-createViewHolder
 * methodName: generated methodName to wrap generate code
 */
public class QMUISkinMaker {
    private static final String MMKV_ID = "qmui_skin_maker";
    private static QMUISkinMaker sSkinMaker;
    private int sNextId = 1;
    private SparseArray<HashMap<View, ViewInfo>> mBindInfo = new SparseArray<>();
    private String[] mPackageNames;
    private List<String> mAttrsInR;
    private static Set<Class<? extends View>> mFilterViews = new HashSet<>();

    public static boolean isInited() {
        return sSkinMaker != null;
    }

    public static QMUISkinMaker init(Context context, String[] packageNames, Class<?> attrsClassInR) {
        if (sSkinMaker == null) {
            sSkinMaker = new QMUISkinMaker();
        }
        MMKV.initialize(context);
        sSkinMaker.mPackageNames = packageNames;


        Field[] fields = attrsClassInR.getFields();

        sSkinMaker.mAttrsInR = new ArrayList<>();
        for (Field field : fields) {
            String name = field.getName();
            if (name.startsWith("qmui") || name.startsWith("color") || name.startsWith("app") || name.contains("skin")) {
                String lowerCase = name.toLowerCase();
                if (lowerCase.endsWith("width")
                        || lowerCase.endsWith("height")
                        || lowerCase.endsWith("size")
                        || lowerCase.endsWith("style")
                        || lowerCase.endsWith("theme")) {
                    continue;
                }
                sSkinMaker.mAttrsInR.add(name);
            }

        }
        return sSkinMaker;
    }

    public static QMUISkinMaker getInstance() {
        if (sSkinMaker == null) {
            throw new RuntimeException("must invoke init() to init sSkinMaker");
        }
        return sSkinMaker;
    }

    private QMUISkinMaker() {
    }

    public static void addFilterView(Class<? extends View> view) {
        mFilterViews.add(view);
    }

    /**
     * must be called after {@link Activity#setContentView(View)}
     *
     * @param activity
     * @return
     */
    public int bind(Activity activity) {
        int ret = bindView(QMUIViewHelper.getActivityRoot(activity));
        HashMap<View, ViewInfo> viewInfoMap = mBindInfo.get(ret);
        bindField(activity, activity.getClass().getSimpleName(), viewInfoMap);
        restoreSkinFromMMKV(viewInfoMap);
        handleForAdapter(viewInfoMap);
        return ret;
    }

    /**
     * must be called in or after {@link Fragment#onViewCreated(View, Bundle)}}
     *
     * @param fragment
     * @return
     */
    public int bind(Fragment fragment) {
        int ret = bindView(fragment.getView());
        HashMap<View, ViewInfo> viewInfoMap = mBindInfo.get(ret);
        bindField(fragment, fragment.getClass().getSimpleName(), viewInfoMap);
        restoreSkinFromMMKV(viewInfoMap);
        handleForAdapter(viewInfoMap);
        return ret;
    }

    public int bindViewHolder(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, String prefix) {
        int ret = bindView(viewHolder.itemView);
        HashMap<View, ViewInfo> viewInfoMap = mBindInfo.get(ret);
        String fullPrefix = prefix + "_" + viewHolder.itemView.getClass().getSimpleName();
        bindField(viewHolder, fullPrefix, viewInfoMap);
        restoreSkinFromMMKV(viewInfoMap);
        handleForAdapter(viewInfoMap);
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if(adapter != null){
            MMKV.mmkvWithID(MMKV_ID).encode("c:" + viewHolder.getClass().getName() + "@" + viewHolder.itemView.getClass().getSimpleName(),
                    adapter.getClass().getName() + ";" + fullPrefix);
        }
        return ret;
    }

    private void bindField(Object object, String prefix, HashMap<View, ViewInfo> viewInfoMap) {
        Set<View> unIdentifyViews = new HashSet<>(viewInfoMap.keySet());
        // avoid circular reference
        Set<Object> scannedObjects = new HashSet<>(100);
        FieldNode tree = new FieldNode();
        tree.fieldClassName = object.getClass().getName();
        tree.fieldName = prefix;
        tree.node = object;
        recursiveReflect(tree, viewInfoMap, unIdentifyViews, scannedObjects);
        if (!unIdentifyViews.isEmpty()) {
            for (View view : unIdentifyViews) {
                int id = view.getId();
                if (id != View.NO_ID) {
                    try {
                        String idName = view.getResources().getResourceName(view.getId());
                        if (idName != null && idName.length() > 0) {
                            idName = idName.replaceAll(":id/", ".R.id.");
                            ViewInfo viewInfo = viewInfoMap.get(view);
                            if (viewInfo != null) {
                                viewInfo.idName = idName;
                                viewInfo.fieldNode = tree;
                            }
                        }
                    } catch (Resources.NotFoundException ignore) {
                    }
                }
            }
        }
    }

    private void handleForAdapter(HashMap<View, ViewInfo> viewInfoMap) {
        for (View view : viewInfoMap.keySet()) {
            if (view instanceof RecyclerView) {
                ViewInfo viewInfo = viewInfoMap.get(view);
                if (viewInfo == null || viewInfo.fieldNode == null) {
                    continue;
                }
                String fieldKey = viewInfo.fieldNode.getKey();
                String prefix = fieldKey + "_" + viewInfo.getSimpleFieldName();
                view.setTag(R.id.qmui_skin_adapter, prefix);
                MMKV.mmkvWithID(MMKV_ID).encode("a:" + fieldKey + "@" + viewInfo.getFullFieldNameWithPrefix(),
                        viewInfo.fieldNode.fieldClassName + ";" + prefix + viewInfo.getSuffix());
                RecyclerView recyclerView = (RecyclerView) view;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    bindViewHolder(recyclerView, recyclerView.getChildViewHolder(recyclerView.getChildAt(i)), prefix);
                }
                recyclerView.addOnChildAttachStateChangeListener(mRecyclerViewChildAttachStateChangeListener);
            }
        }
    }


    private int bindView(View view) {
        int id = sNextId++;
        HashMap<View, ViewInfo> viewInfoMap = new HashMap<>();
        mBindInfo.put(id, viewInfoMap);
        innerBind(view, viewInfoMap);
        return id;
    }

    private RecyclerView.OnChildAttachStateChangeListener mRecyclerViewChildAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {

        private HashMap<View, Integer> mChildBindInfo = new HashMap<>();

        @Override
        public void onChildViewAttachedToWindow(@NonNull View view) {
            RecyclerView parent = (RecyclerView) view.getParent();
            String prefix = (String) parent.getTag(R.id.qmui_skin_adapter);
            int bindId = bindViewHolder(parent, parent.getChildViewHolder(view), prefix);
            mChildBindInfo.put(view, bindId);
        }

        @Override
        public void onChildViewDetachedFromWindow(@NonNull View view) {
            Integer bindId = mChildBindInfo.remove(view);
            if (bindId != null) {
                unbind(bindId);
            }

        }
    };

    private void innerBind(View view, HashMap<View, ViewInfo> viewInfoMap) {
        if (view instanceof QMUITopBar ||
                view instanceof QMUITopBarLayout ||
                view instanceof QMUITabSegment ||
                view instanceof WebView) {
            // for navigation, stop bind
            return;
        }

        for (Class<? extends View> cls : mFilterViews) {
            if (cls.isAssignableFrom(view.getClass())) {
                return;
            }
        }
        viewInfoMap.put(view, generateViewInfo(view));
        if (/*view instanceof AbsListView || view instanceof ViewPager || */view instanceof RecyclerView) {
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                innerBind(viewGroup.getChildAt(i), viewInfoMap);
            }
        }
    }

    public void unbind(int id) {
        HashMap<View, ViewInfo> viewInfoMap = mBindInfo.get(id);
        if (viewInfoMap != null) {
            for (View view : viewInfoMap.keySet()) {
                ViewInfo viewInfo = viewInfoMap.get(view);
                if (viewInfo != null) {
                    view.setOnClickListener(viewInfo.originClickListener);
                }
                if (view instanceof RecyclerView) {
                    ((RecyclerView) view).removeOnChildAttachStateChangeListener(mRecyclerViewChildAttachStateChangeListener);
                }
            }
        }
    }

    private ViewInfo generateViewInfo(final View view) {
        final ViewInfo viewInfo = new ViewInfo();
        viewInfo.view = view;
        try {
            Field listenerInfoFiled = View.class.getDeclaredField("mListenerInfo");
            listenerInfoFiled.setAccessible(true);
            Object listenerInfo = listenerInfoFiled.get(view);
            if(listenerInfo != null){
                Field onClickListenerField = listenerInfo.getClass().getDeclaredField("mOnClickListener");
                onClickListenerField.setAccessible(true);
                viewInfo.originClickListener = (View.OnClickListener) onClickListenerField.get(listenerInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        View.OnClickListener skinOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                if (viewInfo.fieldNode == null) {
                    Toast.makeText(context,
                            "No Id And No Reference, Can not set skin by skinMaker.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                QMUIGroupListView groupListView = new QMUIGroupListView(context);
                groupListView.setId(QMUIViewHelper.generateViewId());
                QMUIGroupListView.newSection(context)
                        .addItemView(groupListView.createItemView("Background"), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                chooseAttr(view, new ValueWriter() {
                                    @Override
                                    public void write(String attrName) {
                                        viewInfo.valueBuilder.background(attrName);
                                        QMUISkinHelper.setSkinValue(view, viewInfo.valueBuilder);
                                        viewInfo.saveToMMKV();
                                        QMUISkinManager.getInstance(context).refreshTheme(view);
                                    }
                                });
                            }
                        })
                        .setUseTitleViewForSectionSpace(false)
                        .setShowSeparator(false)
                        .addTo(groupListView);
                QMUIPopups.popup(view.getContext(), QMUIDisplayHelper.dp2px(context, 200))
                        .arrow(true)
                        .shadow(true)
                        .view(groupListView)
                        .dismissIfOutsideTouch(false)
                        .show(v);
            }
        };
        viewInfo.skinClickListener = skinOnClickListener;
        view.setOnClickListener(skinOnClickListener);
        return viewInfo;
    }

    private void chooseAttr(View anchorView, ValueWriter valueWriter) {
        SkinAttrChooseMakerPopup popup = new SkinAttrChooseMakerPopup(anchorView.getContext(), mAttrsInR, valueWriter);
        popup.show(anchorView);
    }

    private void recursiveReflect(FieldNode tree, HashMap<View, ViewInfo> viewInfoMap,
                                  Set<View> unIdentifiedViews,
                                  Set<Object> scannedObjects) {

        // BFS
        Queue<FieldNode> queue = new LinkedList<>();
        queue.add(tree);

        while (true) {
            if (unIdentifiedViews.isEmpty()) {
                break;
            }
            FieldNode fieldNode = queue.poll();
            if (fieldNode == null) {
                break;
            }
            Object object = fieldNode.node;
            if (scannedObjects.contains(object)) {
                continue;
            }
            scannedObjects.add(object);
            if (!isBusinessClass(fieldNode.fieldClassName)) {
                continue;
            }

            Field[] fields = object.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (value != null) {
                        if (scannedObjects.contains(value)) {
                            continue;
                        }
                        if (value instanceof View) {
                            if (viewInfoMap.containsKey(value)) {
                                ViewInfo viewInfo = viewInfoMap.get(value);
                                if (viewInfo != null) {
                                    unIdentifiedViews.remove(value);
                                    fieldNode.viewInfos.add(viewInfo);
                                    viewInfo.fieldNode = fieldNode;
                                    viewInfo.fieldName = field.getName();
                                }
                            }
                        }
                        FieldNode childNode = new FieldNode();
                        childNode.fieldClassName = value.getClass().getName();
                        childNode.fieldName = field.getName();
                        childNode.parent = fieldNode;
                        childNode.node = value;
                        fieldNode.children.add(childNode);
                        queue.add(childNode);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        alignTree(tree);
    }

    private void alignTree(FieldNode node) {
        if (!node.children.isEmpty()) {
            for (int i = node.children.size() - 1; i >= 0; i--) {
                FieldNode child = node.children.get(i);
                alignTree(child);
                if (child.children.isEmpty() && child.viewInfos.isEmpty()) {
                    node.children.remove(child);
                }
            }
        }
    }


    private void restoreSkinFromMMKV(HashMap<View, ViewInfo> viewInfoMap) {
        for (View view : viewInfoMap.keySet()) {
            ViewInfo viewInfo = viewInfoMap.get(view);
            FieldNode fieldNode = viewInfo.fieldNode;
            if (fieldNode == null) {
                continue;
            }
            String mmkvKey = viewInfo.getKey();
            String result = MMKV.mmkvWithID(MMKV_ID).decodeString(mmkvKey);
            if (result == null || result.isEmpty()) {
                continue;
            }
            String[] splits = result.split(";");
            if (fieldNode.fieldClassName.equals(splits[0])) {
                String value = splits[1].replaceAll("@.*", "");
                QMUISkinHelper.setSkinValue(view, value);
                viewInfo.valueBuilder.convertFrom(value);
            }
            QMUISkinManager.getInstance(view.getContext()).refreshTheme(view);
        }
    }

    private boolean isBusinessClass(String className) {
        for (String packageName : mPackageNames) {
            if (className.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    static class FieldNode {
        String fieldClassName;
        String fieldName;
        Object node;
        List<ViewInfo> viewInfos = new ArrayList<>();
        List<FieldNode> children = new ArrayList<>();
        FieldNode parent = null;

        @Nullable
        public String getKey() {
            StringBuilder key = new StringBuilder();
            key.append(fieldName);
            FieldNode p = parent;
            while (p != null) {
                key.insert(0, "_");
                key.insert(0, parent.fieldName);
                p = p.parent;
            }
            return key.toString();
        }
    }


    class ViewInfo {
        public View.OnClickListener originClickListener;
        public View.OnClickListener skinClickListener;
        public View view;
        public QMUISkinValueBuilder valueBuilder = new QMUISkinValueBuilder();
        public FieldNode fieldNode;
        public String fieldName;
        public String idName;

        @Nullable
        public String getKey() {
            if (fieldNode == null) {
                return null;
            }
            if (fieldName != null) {
                return "r:" + fieldNode.getKey() + "@" + fieldName;
            } else {
                return "i:" + fieldNode.getKey() + "@" + idName;
            }
        }

        @Nullable
        public String getSimpleFieldName(){
            if(fieldName != null){
                return fieldName;
            }

            if(idName != null){
                int index = idName.lastIndexOf(".");
                if(index >= 0 && index < idName.length()){
                    return idName.substring(index + 1);
                }
            }
            return null;
        }

        @Nullable
        public String getFullFieldNameWithPrefix(){
            if(fieldName != null){
                return fieldName;
            }

            if(idName != null){
                return "i/" + idName;
            }
            return null;
        }

        private String getSuffix(){
            if(idName != null){
                if (fieldNode.node instanceof Activity || fieldNode.node instanceof ViewGroup) {
                    return "@this";
                } else if (fieldNode.node instanceof Fragment) {
                    return "@getView()";
                } else if(fieldNode.node instanceof RecyclerView.ViewHolder){
                    return"@itemView";
                }
            }
            return "";
        }

        public void saveToMMKV() {
            if (fieldNode == null) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(fieldNode.fieldClassName);
            builder.append(";");
            builder.append(valueBuilder.build());
            builder.append(getSuffix());

            MMKV.mmkvWithID(MMKV_ID).encode(getKey(), builder.toString());
            FieldNode parent = fieldNode.parent;
            FieldNode current = fieldNode;
            while (parent != null) {
                String baseKey = parent.getKey();
                builder.setLength(0);
                builder.append(parent.fieldClassName);
                builder.append(";");
                builder.append(baseKey + "_" + current.fieldName);
                MMKV.mmkvWithID(MMKV_ID).encode("m:" + baseKey + "@" + current.fieldName, builder.toString());
                current = parent;
                parent = parent.parent;
            }
        }
    }

    public void export(Activity activity) {
        new ExportTask(activity).execute();
    }

    interface ValueWriter {
        void write(String attrNaME);
    }

    class ExportTask extends AsyncTask<Void, Integer, Boolean> {

        private WeakReference<Activity> mActivityWeakReference;
        private QMUITipDialog mQMUITipDialog;

        ExportTask(Activity activity) {
            mActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = mActivityWeakReference.get();
            if (activity != null) {
                mQMUITipDialog = new QMUITipDialog.Builder(activity)
                        .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                        .setTipWord("exporting")
                        .create();
                mQMUITipDialog.show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Activity activity = mActivityWeakReference.get();
            if (activity == null) {
                return false;
            }
            MMKV kv = MMKV.mmkvWithID(MMKV_ID);
            String[] keys = kv.allKeys();
            HashMap<String, ArrayList<String>> result = new HashMap<>();
            for (String key : keys) {
                String value = kv.decodeString(key);
                String[] vv = value.split(";");
                String className = vv[0];
                ArrayList<String> code = result.get(className);
                if (code == null) {
                    code = new ArrayList<>();
                    result.put(className, code);
                }

                int fieldIndex = key.lastIndexOf("@");
                if (key.startsWith("m:")) {
                    code.add(key.substring(2, fieldIndex) +
                            ",method," +
                            key.substring(fieldIndex + 1) +
                            "," +
                            vv[1]);
                } else if (key.startsWith("r:")) {
                    code.add(key.substring(2, fieldIndex) +
                            ",ref," +
                            key.substring(fieldIndex + 1) +
                            "," +
                            vv[1]);
                } else if (key.startsWith("i:")) {
                    code.add(key.substring(2, fieldIndex) +
                            ",id," +
                            key.substring(fieldIndex + 1) +
                            "," +
                            vv[1]);
                }else if(key.startsWith("a:")){
                    code.add(key.substring(2, fieldIndex) +
                            ",adapter," +
                            key.substring(fieldIndex + 1) +
                            "," +
                            vv[1]);
                }else if(key.startsWith("c:")){
                    code.add(key.substring(2, fieldIndex) +
                            ",c," +
                            key.substring(fieldIndex + 1) +
                            "," +
                            vv[1]);
                }

            }

            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "qmui-skin-maker");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_kk.mm.ss", Locale.getDefault());
            File file = new File(dir, "skin_" + dateFormat.format(new Date(System.currentTimeMillis())) + ".txt");
            try {
                FileWriter fileWriter = new FileWriter(file);
                for (String key : result.keySet()) {
                    List<String> values = result.get(key);
                    if (values != null && !values.isEmpty()) {
                        fileWriter.write(key);
                        fileWriter.write('\n');
                        for (String v : values) {
                            fileWriter.write(v);
                            fileWriter.write('\n');
                        }
                        fileWriter.write(";\n");
                    }
                }
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mQMUITipDialog != null) {
                mQMUITipDialog.dismiss();
            }
            Activity activity = mActivityWeakReference.get();
            if (activity != null) {
                if (success) {
                    Toast.makeText(activity, "export success", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "export failed", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }
}
