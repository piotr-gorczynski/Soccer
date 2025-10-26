package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

final class SectionHeaderAdapter extends RecyclerView.Adapter<SectionHeaderAdapter.VH> {

    private CharSequence title;

    SectionHeaderAdapter(@NonNull CharSequence title) {
        this.title = title;
    }

    void setTitle(@NonNull CharSequence title) {
        this.title = title;
        notifyItemChanged(0);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_header, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.title.setText(title);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView title;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sectionTitle);
        }
    }
}
