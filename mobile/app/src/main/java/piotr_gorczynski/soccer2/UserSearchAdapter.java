package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {
    interface OnAddClick { void onAdd(String uid); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nickname;
        final Button addBtn;
        String uid;
        VH(@NonNull View v) {
            super(v);
            nickname = v.findViewById(R.id.nickname);
            addBtn = v.findViewById(R.id.addBtn);
        }
    }

    private final List<DocumentSnapshot> data = new ArrayList<>();
    private final OnAddClick listener;
    private Set<String> friendUids = new HashSet<>();

    UserSearchAdapter(OnAddClick listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        VH h = new VH(v);
        h.addBtn.setOnClickListener(btn -> {
            if (h.uid != null) listener.onAdd(h.uid);
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DocumentSnapshot d = data.get(position);
        h.uid = d.getId();
        String nick = d.getString("nickname");
        if (nick == null) nick = h.uid.substring(0, 6);
        h.nickname.setText(nick);
        
        // Disable "Add" button if this user is already a friend
        boolean isAlreadyFriend = friendUids.contains(h.uid);
        h.addBtn.setEnabled(!isAlreadyFriend);
        h.addBtn.setAlpha(isAlreadyFriend ? 0.3f : 1.0f);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId().hashCode() & 0xffffffffL;
    }

    @Override
    public int getItemCount() { return data.size(); }

    void addResults(List<DocumentSnapshot> docs) {
        int start = data.size();
        data.addAll(docs);
        notifyItemRangeInserted(start, docs.size());
    }

    void clear() {
        data.clear();
        notifyDataSetChanged();
    }
    
    void setFriendUids(Set<String> friendUids) {
        this.friendUids = new HashSet<>(friendUids);
        notifyDataSetChanged(); // Refresh all items to update button states
    }
}
