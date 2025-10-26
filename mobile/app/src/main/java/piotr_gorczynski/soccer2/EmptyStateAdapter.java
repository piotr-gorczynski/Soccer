package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

final class EmptyStateAdapter extends RecyclerView.Adapter<EmptyStateAdapter.VH> {

    private CharSequence message;
    private boolean visible;

    EmptyStateAdapter(@NonNull CharSequence message) {
        this.message = message;
        this.visible = true;
    }

    void setMessage(@NonNull CharSequence message) {
        this.message = message;
        if (visible) {
            notifyItemChanged(0);
        }
    }

    void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        if (visible) {
            notifyItemInserted(0);
        } else {
            notifyItemRemoved(0);
        }
    }

    boolean isVisible() {
        return visible;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty_state, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.message.setText(message);
    }

    @Override
    public int getItemCount() {
        return visible ? 1 : 0;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView message;

        VH(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.emptyMessage);
        }
    }
}
