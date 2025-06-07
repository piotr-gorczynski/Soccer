package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**  Simple list of nicknames (uid → nickname). */
public class PlayerAdapter
        extends RecyclerView.Adapter<PlayerAdapter.VH> {

    /* ---------------- hold one row ---------------- */
    public static class VH extends RecyclerView.ViewHolder {
        TextView nick;
        public VH(@NonNull View v) {
            super(v);
            nick = v.findViewById(R.id.nick);
        }
    }

    /* ---------------- data ---------------- */
    private final List<String> uids = new ArrayList<>();
    private final Map<String, String> cache = new HashMap<>();   // uid → nickname

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View row = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_player, p, false);
        return new VH(row);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        String uid  = uids.get(i);
        String nick = cache.get(uid);

        h.nick.setText(nick != null ? nick
                : uid.substring(0, 6));  // fallback

        // Resolve nickname once
        if (nick == null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(uid).get()
                    .addOnSuccessListener(d -> {
                        cache.put(uid, d.getString("nickname"));
                        notifyItemChanged(i);
                    });
        }
    }

    @Override public int getItemCount() { return uids.size(); }

    /* ---------- DIFF-based updater ---------- */
    public void setAll(List<String> newList) {

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override public int getOldListSize() { return uids.size(); }
                    @Override public int getNewListSize() { return newList.size(); }

                    @Override
                    public boolean areItemsTheSame(int oldPos, int newPos) {
                        return uids.get(oldPos).equals(newList.get(newPos));
                    }

                    @Override
                    public boolean areContentsTheSame(int oldPos, int newPos) {
                        // Items are just UID strings—contents equal if IDs equal
                        return true;
                    }
                });

        uids.clear();
        uids.addAll(newList);
        diff.dispatchUpdatesTo(this);          // precise notifyItem* calls
    }
}
