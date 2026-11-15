package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.ListenerRegistration;
import android.widget.Button;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TournamentsActivity extends BaseActivity {

    private AnalyticsManager analyticsManager;
    private boolean listViewReported;
    private RemoteConfigHelper remoteConfigHelper;
    private TournamentArchivePager archive;
    private ListenerRegistration activeListener;
    private boolean started;
    private int generation;
    private final List<DocumentSnapshot> registeringDocs = new ArrayList<>();
    private final List<DocumentSnapshot> runningDocs = new ArrayList<>();
    private TournamentAdapter registeringAdapter, runningAdapter, endedAdapter;
    private Button loadMoreEnded;

    protected AnalyticsManager tournamentAnalytics() { return ((SoccerApp)getApplicationContext()).getAnalyticsManager(); }
    protected RemoteConfigHelper tournamentRemoteConfig() { return ((SoccerApp)getApplicationContext()).getRemoteConfigHelper(); }
    protected Query activeTournamentsQuery() {
        return FirebaseFirestore.getInstance().collection("tournaments")
                .whereIn("status", java.util.Arrays.asList("registering", "running"));
    }
    protected Task<List<DocumentSnapshot>> fetchArchivePage(DocumentSnapshot cursor) {
        // A single-field deadline index preserves chronological paging without a
        // new composite index or changing legacy market visibility semantics.
        // Status/market filtering happens on each bounded page in the pager.
        Query query = FirebaseFirestore.getInstance().collection("tournaments")
                .orderBy("matchesDeadline", Query.Direction.DESCENDING).limit(TournamentArchivePager.PAGE_SIZE);
        if (cursor != null) query = query.startAfter(cursor);
        return query.get(Source.SERVER).continueWith(task -> task.getResult().getDocuments());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournaments);
        Toolbar toolbar = findViewById(R.id.tournaments_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.tournaments);

        // Get analytics manager from SoccerApp
        analyticsManager = tournamentAnalytics();
        
        // Get Remote Config helper
        remoteConfigHelper = tournamentRemoteConfig();

        // ── RecyclerView ─────────────────────────────────────────────
        RecyclerView registeringList = findViewById(R.id.registeringList);
        RecyclerView runningList = findViewById(R.id.runningList);
        RecyclerView endedList = findViewById(R.id.endedList);
        loadMoreEnded = findViewById(R.id.loadMoreEnded);

        registeringList.setLayoutManager(new LinearLayoutManager(this));
        runningList.setLayoutManager(new LinearLayoutManager(this));
        endedList.setLayoutManager(new LinearLayoutManager(this));

        archive = new TournamentArchivePager(this::fetchArchivePage, new TournamentArchivePager.Listener() {
            @Override public void changed() {
                if (endedAdapter == null) return;
                endedAdapter.notifyDataSetChanged(); updateSections();
            }
            @Override public void failed() {
                Toast.makeText(TournamentsActivity.this, R.string.help_load_failed, Toast.LENGTH_LONG).show();
            }
        }, AppFlavourDetector.getCurrentFlavour(this));
        loadMoreEnded.setOnClickListener(view -> archive.loadMore());

        registeringAdapter = new TournamentAdapter(
                registeringDocs,
                this::joinTournament,
                this::leaveTournament
        );
        runningAdapter = new TournamentAdapter(
                runningDocs,
                this::joinTournament,
                this::leaveTournament,
                doc -> {
                    Intent i = new Intent(this, TournamentResultsActivity.class)
                            .putExtra("tournamentId", doc.getId());
                    startActivity(i);
                }
        );
        endedAdapter = new TournamentAdapter(
                archive.documents,
                doc -> {
                    Intent i = new Intent(this, TournamentResultsActivity.class)
                            .putExtra("tournamentId", doc.getId());
                    startActivity(i);
                }
        );
        registeringList.setAdapter(registeringAdapter);
        runningList.setAdapter(runningAdapter);
        endedList.setAdapter(endedAdapter);

        updateSections();
    }

    private void updateSections() {
        boolean registering = !registeringDocs.isEmpty(), running = !runningDocs.isEmpty();
        boolean ended = archive != null && !archive.documents.isEmpty();
        boolean more = archive != null && archive.hasMore();
        findViewById(R.id.registeringList).setVisibility(registering ? View.VISIBLE : View.GONE);
        findViewById(R.id.registeringHeader).setVisibility(registering ? View.VISIBLE : View.GONE);
        findViewById(R.id.runningList).setVisibility(running ? View.VISIBLE : View.GONE);
        findViewById(R.id.runningHeader).setVisibility(running ? View.VISIBLE : View.GONE);
        findViewById(R.id.endedList).setVisibility(ended ? View.VISIBLE : View.GONE);
        findViewById(R.id.endedHeader).setVisibility(ended || more ? View.VISIBLE : View.GONE);
        loadMoreEnded.setVisibility(more ? View.VISIBLE : View.GONE);
        loadMoreEnded.setEnabled(started && !archive.isLoading());
        loadMoreEnded.setText(archive.isLoading() ? R.string.menu_loading_message : R.string.load_more);
        findViewById(R.id.emptyTournaments).setVisibility(!registering && !running && !ended && !more
                ? View.VISIBLE : View.GONE);
    }

    @Override protected void onStart() {
        super.onStart();
        if (started) return;
        started = true; archive.start();
        final int request = ++generation;
        activeListener = activeTournamentsQuery().addSnapshotListener((snap, error) -> {
            if (!started || request != generation || isFinishing() || isDestroyed()) return;
            if (error != null || snap == null) return;
            registeringDocs.clear(); runningDocs.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                if (!TournamentVisibility.isVisible(doc.get("visibleInFlavours"),
                        AppFlavourDetector.getCurrentFlavour(this))) continue;
                String status = TournamentArchivePager.status(doc);
                if ("registering".equals(status)) registeringDocs.add(doc);
                else if ("running".equals(status)) runningDocs.add(doc);
            }
            registeringAdapter.notifyDataSetChanged(); runningAdapter.notifyDataSetChanged(); updateSections();
            if (!AnalyticsManager.isBangladesh() || !listViewReported) {
                listViewReported = true;
                analyticsManager.trackTournamentListViewed(registeringDocs.size(), runningDocs.size(), archive.documents.size());
            }
        });
    }

    @Override protected void onStop() {
        started = false; generation++;
        if (activeListener != null) { activeListener.remove(); activeListener = null; }
        if (archive != null) archive.stop();
        super.onStop();
    }

    /** Calls your Cloud Function `joinTournament` (see blueprint) */
    private void joinTournament(DocumentSnapshot tournamentDoc) {
        if (tournamentDoc == null || !tournamentDoc.exists()) return;

        String tid = tournamentDoc.getId();
        String regId = tournamentDoc.getString("regulation");
        
        // Check if user is authenticated
        boolean isAuthenticated = FirebaseAuth.getInstance().getCurrentUser() != null;
        
        // Track tournament join start
        analyticsManager.trackTournamentJoinStart(tid, isAuthenticated);
        analyticsManager.addTournamentBreadcrumb("join_start", tid, "authenticated=" + isAuthenticated);
        
        if (!isAuthenticated) {
            // Check Remote Config to see if we should show decline dialog
            if (remoteConfigHelper.shouldShowDeclineDialog()) {
                // Show signup decline reason dialog for unauthenticated users trying to join tournaments
                SignupDeclineReasonDialog.show(this, "tournament_join_unauthenticated", analyticsManager, reason -> {
                    Log.d("TAG_Soccer", "User declined to register for tournament join, reason: " + reason);
                    // Show registration prompt with A/B tested copy
                    showRegistrationPrompt(tid);
                });
            } else {
                // Just show registration prompt directly
                showRegistrationPrompt(tid);
            }
            return;
        }

        Intent i = new Intent(this, RegulationActivity.class)
                .putExtra("tournamentId", tid)
                .putExtra("regulationId", regId);
        startActivity(i);
    }

    /** Prompt confirmation and call leaveTournament Cloud Function */
    private void leaveTournament(DocumentSnapshot tournamentDoc) {
        if (tournamentDoc == null || !tournamentDoc.exists()) return;

        String tid = tournamentDoc.getId();

        new AlertDialog.Builder(this)
                .setMessage(R.string.leave_tournament_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> doLeave(tid))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void doLeave(String tid) {
        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("leaveTournament")
                .call(java.util.Collections.singletonMap("tournamentId", tid))
                .addOnSuccessListener(r ->
                        Toast.makeText(this, R.string.left_tournament, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseFunctionsException ffe) {
                        Toast.makeText(this, ffe.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    /**
     * Show registration prompt with A/B tested copy
     */
    private void showRegistrationPrompt(String tournamentId) {
        String title = remoteConfigHelper.getRegistrationTitle(this);
        String message = remoteConfigHelper.getRegistrationMessage(this);
        
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.register_now, (dialog, which) -> {
                    analyticsManager.addTournamentBreadcrumb("registration_prompt_accepted", tournamentId, "A/B variant: " + remoteConfigHelper.getRegistrationCopyVariant());
                    
                    // Open Universal Login Activity
                    Intent intent = new Intent(this, UniversalLoginActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.maybe_later, (dialog, which) -> {
                    analyticsManager.addTournamentBreadcrumb("registration_prompt_declined", tournamentId, "A/B variant: " + remoteConfigHelper.getRegistrationCopyVariant());
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
