package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
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

    interface OnMatchClick { void onOpen(String matchId); }

    MatchAdapter(String myUid, OnMatchClick cb) {
        this.myUid   = myUid;
        this.clickCB = cb;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v)
    { return new VH(LayoutInflater.from(p.getContext())
            .inflate(R.layout.item_match, p, false)); }

    @Override public void onBindViewHolder(@NonNull VH h,int pos) {
        DocumentSnapshot m = matches.get(pos);
        String a = m.getString("playerA");
        String b = m.getString("playerB");
        String oppUid = myUid.equals(a) ? b : a;

        // ⚠️  you probably store nicknames elsewhere; fetch them if needed
        h.opponent.setText(oppUid);

        String st = m.getString("status");           // scheduled | playing | done
        h.status.setText(st);                        // easy i18n via strings.xml
        int colour = switch (Objects.requireNonNull(st)) {
            case "playing"   -> 0xFFEF6C00;          // orange
            case "done"      -> 0xFF9E9E9E;          // grey
            default /*scheduled*/ -> 0xFF388E3C;     // green
        };
        h.status.setTextColor(colour);

        h.itemView.setOnClickListener(v ->
                clickCB.onOpen(m.getId()));
    }

    @Override public int getItemCount() { return matches.size(); }

    /* public helpers for the listener */
    void add(int idx, DocumentSnapshot d) { matches.add(idx, d); notifyItemInserted(idx); }
    void set(int idx, DocumentSnapshot d){ matches.set(idx, d);  notifyItemChanged(idx); }
    void move(int o,int n,DocumentSnapshot d){
        matches.remove(o); matches.add(n,d); notifyItemMoved(o,n); }
    void remove(int idx){ matches.remove(idx); notifyItemRemoved(idx); }
}

