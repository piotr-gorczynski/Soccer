package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.view.View;

public class MatchAdapter
        extends RecyclerView.Adapter<MatchAdapter.VH> {

    public static class VH extends RecyclerView.ViewHolder {
        TextView opponent, status;
        VH(View v) {
            super(v);
            opponent = v.findViewById(R.id.opponent);
            status   = v.findViewById(R.id.status);
        }
    }

    private final List<DocumentSnapshot> matches = new ArrayList<>();
    private final String myUid;
    private final OnMatchClick clickCB;

      private final Map<String,String> nickCache = new HashMap<>();

    interface OnMatchClick { void onOpen(String matchId); }

    MatchAdapter(String myUid, OnMatchClick cb) {
        this.myUid   = myUid;
        this.clickCB = cb;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v)
    { return new VH(LayoutInflater.from(p.getContext())
            .inflate(R.layout.item_match, p, false)); }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DocumentSnapshot m = matches.get(pos);
        String a = m.getString("playerA");
        String b = m.getString("playerB");
        String oppUid = myUid.equals(a) ? b : a;

        /* ----------- nickname lookup ----------- */
        String nick = nickCache.get(oppUid);
        if (nick == null) {
            h.opponent.setText(Objects.requireNonNull(oppUid).substring(0, 6));   // temporary stub

            FirebaseFirestore.getInstance()
                    .collection("users").document(oppUid).get()
                    .addOnSuccessListener(d -> {
                        String n = d.getString("nickname");
                        if (n == null) return;

                        nickCache.put(oppUid, n);

                        int posNow = h.getAdapterPosition();        // ← works on all versions
                        if (posNow != RecyclerView.NO_POSITION) {
                            notifyItemChanged(posNow);              // refresh the correct row
                        }
                    });
        } else {
            h.opponent.setText(nick);   // nickname already cached
        }

        /* ----------- status label ----------- */
        String st = m.getString("status");          // scheduled | playing | done
        h.status.setText(st);
        h.status.setTextColor(
                switch (Objects.requireNonNull(st)) {
                    case "playing"   -> 0xFFFF9800;   // orange
                    case "done"      -> 0xFF9E9E9E;   // grey
                    default          -> 0xFFFFFFFF;   // white for scheduled
                });

        h.itemView.setOnClickListener(v -> clickCB.onOpen(m.getId()));
    }


    @Override public int getItemCount() { return matches.size(); }

    /* public helpers for the listener */
    void add(int idx, DocumentSnapshot d) { matches.add(idx, d); notifyItemInserted(idx); }
    void set(int idx, DocumentSnapshot d){ matches.set(idx, d);  notifyItemChanged(idx); }
    void move(int o,int n,DocumentSnapshot d){
        matches.remove(o); matches.add(n,d); notifyItemMoved(o,n); }
    void remove(int idx){ matches.remove(idx); notifyItemRemoved(idx); }
}

