package com.example.xyzreader.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;

/**
 * Created by Neha on 20-05-2017.
 */

public class DynamicRecyclerView extends RecyclerView {
    private int mColumnWidth = -1;
    private StaggeredGridLayoutManager mManager;

    public DynamicRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            mColumnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        mManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        setLayoutManager(mManager);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (mColumnWidth > 0) {
            int spanCount = Math.max(1, getMeasuredWidth() / mColumnWidth);
            mManager.setSpanCount(spanCount);
        }
    }
}

