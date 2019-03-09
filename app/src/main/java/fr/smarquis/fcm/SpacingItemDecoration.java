package fr.smarquis.fcm;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

class SpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int horizontal;
    private final int vertical;

    SpacingItemDecoration(@Px int horizontal, @Px int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        RecyclerView.Adapter adapter = parent.getAdapter();
        boolean isLastItem = position != (adapter != null ? adapter.getItemCount() : 0) - 1;
        outRect.top = vertical;
        outRect.bottom = isLastItem ? 0 : vertical;
        int viewWidth = view.getLayoutParams().width;
        switch (viewWidth) {
            case MATCH_PARENT:
            case WRAP_CONTENT:
                outRect.left = outRect.right = horizontal;
                break;
            default:
                outRect.left = outRect.right = Math.max(horizontal, (parent.getWidth() - viewWidth) / 2);
                break;
        }
    }
}