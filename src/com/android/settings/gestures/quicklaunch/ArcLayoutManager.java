/*
 * Copyright (C) 2026 Project ASCP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures.quicklaunch;

import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Trace;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class ArcLayoutManager extends RecyclerView.LayoutManager {

    private static final int MAX_ITEMS = 5;
    public static final float START_ANGLE = 180.0f;
    public static final float SWEEP_ANGLE = 180.0f;

    private KeyFrames mKeyFrames;

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    public ArcLayoutManager(Resources resources) {
        if (resources != null) {
            this.mKeyFrames = new KeyFrames(obtainArcPath(resources));
        }
    }

    public ArcLayoutManager(Path path) {
        if (path != null) {
            this.mKeyFrames = new KeyFrames(path);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        Trace.traceBegin(Trace.TRACE_TAG_APP, "ArcLayoutManager#onLayoutChildren");
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            Trace.traceEnd(Trace.TRACE_TAG_APP);
            return;
        }
        detachAndScrapAttachedViews(recycler);
        List<PosTan> needLayoutItems = getNeedLayoutItems();
        if (needLayoutItems.isEmpty() || state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
        } else {
            onLayout(recycler, needLayoutItems);
        }
        Trace.traceEnd(Trace.TRACE_TAG_APP);
    }

    private void onLayout(RecyclerView.Recycler recycler, List<PosTan> list) {
        for (PosTan posTan : list) {
            if (posTan.index >= getItemCount()) continue;
            View viewForPosition = recycler.getViewForPosition(posTan.index);
            addView(viewForPosition);
            measureChild(viewForPosition, 0, 0);
            int decoratedMeasuredWidth = ((int) posTan.x) - (getDecoratedMeasuredWidth(viewForPosition) / 2);
            int decoratedMeasuredHeight = ((int) posTan.y) - (getDecoratedMeasuredHeight(viewForPosition) / 2);
            layoutDecorated(viewForPosition, decoratedMeasuredWidth, decoratedMeasuredHeight,
                    decoratedMeasuredWidth + getDecoratedMeasuredWidth(viewForPosition),
                    decoratedMeasuredHeight + getDecoratedMeasuredHeight(viewForPosition));
        }
    }

    private List<PosTan> getNeedLayoutItems() {
        ArrayList<PosTan> arrayList = new ArrayList<>();
        if (this.mKeyFrames != null) {
            int itemCount = getItemCount();
            if (itemCount > MAX_ITEMS) {
                itemCount = MAX_ITEMS;
            }
            this.mKeyFrames.initPath(itemCount);
            for (int i = 0; i < itemCount; i++) {
                PosTan value = this.mKeyFrames.getValue(i);
                if (value != null) {
                    arrayList.add(new PosTan(value, i));
                }
            }
        }
        return arrayList;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(-2, -2);
    }

    public static class KeyFrames {
        private Path mPath;
        private float mPathLen;
        private PosTan mTemp = new PosTan();
        private float[] mX = new float[0];
        private float[] mY = new float[0];

        public KeyFrames(Path path) {
            this.mPath = path;
        }

        public void initPath(int count) {
            if (count <= 0) return;
            float f2;
            float f3;
            float f4;
            Path path = this.mPath;
            if (path == null || path.isEmpty()) {
                return;
            }
            PathMeasure pathMeasure = new PathMeasure(path, false);
            this.mX = new float[0];
            this.mY = new float[0];
            do {
                this.mPathLen = pathMeasure.getLength();
                float[] fArr = new float[count];
                float[] fArr2 = new float[count];
                float[] fArr3 = new float[2];
                float[] fArr4 = new float[2];
                for (int i = 0; i < count; i++) {
                    if (count == 1) {
                        f2 = this.mPathLen;
                        f3 = 2.0f;
                        f4 = f2 / f3;
                    } else if (count == 2) {
                        f4 = (this.mPathLen / 4.0f) * ((i * 2) + 1);
                    } else {
                        f2 = i * this.mPathLen;
                        f3 = count - 1;
                        f4 = f2 / f3;
                    }
                    pathMeasure.getPosTan(f4, fArr3, fArr4);
                    fArr[i] = fArr3[0];
                    fArr2[i] = fArr3[1];
                }
                float[] fArr5 = this.mX;
                float[] fArr6 = new float[fArr5.length + count];
                System.arraycopy(fArr5, 0, fArr6, 0, fArr5.length);
                System.arraycopy(fArr, 0, fArr6, this.mX.length, count);
                this.mX = fArr6;
                float[] fArr7 = this.mY;
                float[] fArr8 = new float[fArr7.length + count];
                System.arraycopy(fArr7, 0, fArr8, 0, fArr7.length);
                System.arraycopy(fArr2, 0, fArr8, this.mY.length, count);
                this.mY = fArr8;
            } while (pathMeasure.nextContour());
        }

        public PosTan getValue(int index) {
            if (index < 0 || index >= mX.length || index >= mY.length) return null;
            this.mTemp.set(this.mX[index], this.mY[index]);
            return this.mTemp;
        }
    }

    public static class PosTan extends PointF {
        public int index;

        public PosTan() {
        }

        public PosTan(int index, float x, float y) {
            super(x, y);
            this.index = index;
        }

        public PosTan(PosTan posTan, int index) {
            this(index, posTan.x, posTan.y);
        }
    }

    private Path obtainArcPath(Resources resources) {
        int width = resources.getDimensionPixelOffset(R.dimen.kgd_fp_ql_recycler_view_width);
        int marginCenter = resources.getDimensionPixelOffset(R.dimen.kgd_ql_app_item_margin_center);
        int marginTop = resources.getDimensionPixelOffset(R.dimen.kgd_ql_app_item_margin_top);
        Path path = new Path();
        int centerX = width / 2;
        path.addArc(new RectF(centerX - marginCenter, marginTop,
                centerX + marginCenter, marginTop + (marginCenter * 2)),
                START_ANGLE, SWEEP_ANGLE);
        return path;
    }
}
