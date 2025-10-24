package piotr_gorczynski.soccer2;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link LinearLayoutManager} that measures all of its children so that the
 * RecyclerView can expand to wrap its content when it is hosted inside a
 * {@link android.widget.ScrollView}.
 *
 * <p>Android does not automatically remeasure a RecyclerView with
 * {@code layout_height="wrap_content"} that lives inside a ScrollView. As a
 * consequence the view keeps the height from its first measurement (often only
 * enough to display a handful of rows) even after the adapter loads more data.
 *
 * <p>This layout manager solves the issue by calculating the required height in
 * {@link #onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)} and by
 * reporting that height back to the parent. The view is therefore resized on
 * every layout pass, which allows additional pages of invitations to become
 * visible as soon as they are appended to the adapter.</p>
 */
class WrapContentLinearLayoutManager extends LinearLayoutManager {

    WrapContentLinearLayoutManager(Context context) {
        super(context);
    }

    WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public boolean canScrollVertically() {
        // The RecyclerView is nested inside a ScrollView, so it should delegate
        // vertical scrolling to the parent container.
        return false;
    }

    @Override
    public void onMeasure(@NonNull RecyclerView.Recycler recycler,
                          @NonNull RecyclerView.State state,
                          int widthSpec,
                          int heightSpec) {
        if (state.isPreLayout()) {
            // Fall back to default behaviour during pre-layout (animations).
            super.onMeasure(recycler, state, widthSpec, heightSpec);
            return;
        }

        final int widthMode = View.MeasureSpec.getMode(widthSpec);
        final int widthSize = View.MeasureSpec.getSize(widthSpec);
        final int heightMode = View.MeasureSpec.getMode(heightSpec);
        final int heightSize = View.MeasureSpec.getSize(heightSpec);

        int width = 0;
        int height = getPaddingTop() + getPaddingBottom();

        final int itemCount = getItemCount();
        for (int position = 0; position < itemCount; position++) {
            View view;
            try {
                view = recycler.getViewForPosition(position);
            } catch (IndexOutOfBoundsException ex) {
                break;
            }

            addView(view);
            measureChildWithMargins(view, 0, 0);

            final int measuredWidth = getDecoratedMeasuredWidth(view);
            final int measuredHeight = getDecoratedMeasuredHeight(view);

            width = Math.max(width, measuredWidth);
            height += measuredHeight;

            detachAndScrapView(view, recycler);

            // When hosted in a ScrollView the RecyclerView receives an AT_MOST
            // height spec equal to the viewport size. Returning that limited
            // height would prevent the ScrollView from growing to fit the
            // entire list, so we intentionally ignore the bound and allow the
            // view to report the full height that it requires.
        }

        width += getPaddingLeft() + getPaddingRight();

        if (widthMode == View.MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == View.MeasureSpec.AT_MOST) {
            width = Math.min(width, widthSize);
        }

        if (heightMode == View.MeasureSpec.EXACTLY) {
            height = heightSize;
        }

        setMeasuredDimension(width, height);
    }
}

