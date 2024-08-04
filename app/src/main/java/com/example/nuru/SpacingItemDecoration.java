package com.example.nuru;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpaceHeight;
    private final int horizontalSpaceWidth;

    public SpacingItemDecoration(int verticalSpaceHeight, int horizontalSpaceWidth) {
        this.verticalSpaceHeight = verticalSpaceHeight;
        this.horizontalSpaceWidth = horizontalSpaceWidth;
    }

    @Override
    public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.left = horizontalSpaceWidth;
        outRect.right = horizontalSpaceWidth;
        outRect.top = verticalSpaceHeight;
        outRect.bottom = verticalSpaceHeight;
    }
}
