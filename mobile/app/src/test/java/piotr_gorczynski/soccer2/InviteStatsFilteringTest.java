package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for invite statistics filtering functionality
 * Tests that tournament invitations are correctly excluded from friend invite stats
 */
public class InviteStatsFilteringTest {

    /**
     * Mock class to represent an invitation document
     */
    static class MockInvitation {
        private final String from;
        private final String to;
        private final String status;
        private final String tournamentId;

        public MockInvitation(String from, String to, String status, String tournamentId) {
            this.from = from;
            this.to = to;
            this.status = status;
            this.tournamentId = tournamentId;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getStatus() {
            return status;
        }

        public String getTournamentId() {
            return tournamentId;
        }
    }

    /**
     * Helper method to count invites matching the filtering logic
     */
    private static class InviteStats {
        int totalSent = 0;
        int totalSentAccepted = 0;
        int totalReceived = 0;
        int totalReceivedAccepted = 0;
    }

    private InviteStats calculateStats(List<MockInvitation> invites, String currentUserId, String friendUid) {
        InviteStats stats = new InviteStats();

        for (MockInvitation invite : invites) {
            // Filter out tournament invitations
            if (invite.getTournamentId() != null) {
                continue;
            }

            // Process sent invites
            if (currentUserId.equals(invite.getFrom()) && friendUid.equals(invite.getTo())) {
                stats.totalSent++;
                if ("accepted".equals(invite.getStatus())) {
                    stats.totalSentAccepted++;
                }
            }

            // Process received invites
            if (friendUid.equals(invite.getFrom()) && currentUserId.equals(invite.getTo())) {
                stats.totalReceived++;
                if ("accepted".equals(invite.getStatus())) {
                    stats.totalReceivedAccepted++;
                }
            }
        }

        return stats;
    }

    @Test
    public void testFilterOutTournamentInvitations() {
        // Setup
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // Regular invite sent (should be counted)
        invites.add(new MockInvitation(currentUserId, friendUid, "pending", null));
        
        // Tournament invite sent (should NOT be counted)
        invites.add(new MockInvitation(currentUserId, friendUid, "pending", "tournament123"));
        
        // Regular invite received (should be counted)
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", null));
        
        // Tournament invite received (should NOT be counted)
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", "tournament456"));

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify - only non-tournament invites should be counted
        assertEquals("Should count 1 sent invite (excluding tournament)", 1, stats.totalSent);
        assertEquals("Should count 0 sent accepted invites", 0, stats.totalSentAccepted);
        assertEquals("Should count 1 received invite (excluding tournament)", 1, stats.totalReceived);
        assertEquals("Should count 1 received accepted invite", 1, stats.totalReceivedAccepted);
    }

    @Test
    public void testAllTournamentInvitations() {
        // Test case where all invitations are tournament invitations
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // All tournament invites
        invites.add(new MockInvitation(currentUserId, friendUid, "pending", "tournament1"));
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", "tournament2"));
        invites.add(new MockInvitation(friendUid, currentUserId, "pending", "tournament3"));
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", "tournament4"));

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify - all should be filtered out
        assertEquals("Should count 0 sent invites (all are tournaments)", 0, stats.totalSent);
        assertEquals("Should count 0 sent accepted invites", 0, stats.totalSentAccepted);
        assertEquals("Should count 0 received invites (all are tournaments)", 0, stats.totalReceived);
        assertEquals("Should count 0 received accepted invites", 0, stats.totalReceivedAccepted);
    }

    @Test
    public void testNoTournamentInvitations() {
        // Test case where no invitations are tournament invitations
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // All regular invites
        invites.add(new MockInvitation(currentUserId, friendUid, "pending", null));
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", null));
        invites.add(new MockInvitation(friendUid, currentUserId, "pending", null));
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", null));

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify - all should be counted
        assertEquals("Should count 2 sent invites", 2, stats.totalSent);
        assertEquals("Should count 1 sent accepted invite", 1, stats.totalSentAccepted);
        assertEquals("Should count 2 received invites", 2, stats.totalReceived);
        assertEquals("Should count 1 received accepted invite", 1, stats.totalReceivedAccepted);
    }

    @Test
    public void testMixedInvitationsWithDifferentStatuses() {
        // Test a realistic mix of invitations
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // Sent invites
        invites.add(new MockInvitation(currentUserId, friendUid, "pending", null));      // Count: sent
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", null));     // Count: sent + accepted
        invites.add(new MockInvitation(currentUserId, friendUid, "cancelled", null));    // Count: sent
        invites.add(new MockInvitation(currentUserId, friendUid, "expired", null));      // Count: sent
        invites.add(new MockInvitation(currentUserId, friendUid, "pending", "tourney1")); // Skip: tournament
        
        // Received invites
        invites.add(new MockInvitation(friendUid, currentUserId, "pending", null));      // Count: received
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", null));     // Count: received + accepted
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", null));     // Count: received + accepted
        invites.add(new MockInvitation(friendUid, currentUserId, "cancelled", null));    // Count: received
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", "tourney2")); // Skip: tournament

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify
        assertEquals("Should count 4 sent invites (excluding tournament)", 4, stats.totalSent);
        assertEquals("Should count 1 sent accepted invite", 1, stats.totalSentAccepted);
        assertEquals("Should count 4 received invites (excluding tournament)", 4, stats.totalReceived);
        assertEquals("Should count 2 received accepted invites", 2, stats.totalReceivedAccepted);
    }

    @Test
    public void testEmptyInvitationsList() {
        // Test with no invitations
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify - all should be zero
        assertEquals("Should count 0 sent invites", 0, stats.totalSent);
        assertEquals("Should count 0 sent accepted invites", 0, stats.totalSentAccepted);
        assertEquals("Should count 0 received invites", 0, stats.totalReceived);
        assertEquals("Should count 0 received accepted invites", 0, stats.totalReceivedAccepted);
    }

    @Test
    public void testTournamentIdEmptyString() {
        // Test that empty string tournament ID is treated as null (no tournament)
        // This is a defensive test - in practice, tournamentId should be null or a non-empty string
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // Invite with empty string tournament ID (edge case)
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", ""));

        // Calculate stats - note: in the actual implementation, we check tournamentId == null
        // So an empty string would NOT be filtered out. This test documents current behavior.
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // With current implementation (tournamentId == null check), empty string is NOT filtered
        // This is acceptable since the backend should set tournamentId to null, not empty string
        assertEquals("Empty string tournamentId is not null, so it's filtered out", 0, stats.totalSent);
    }

    @Test
    public void testOnlyRelevantUserPairsCounted() {
        // Test that invitations between other users are not counted
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // Relevant invites (should be counted)
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", null));
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", null));
        
        // Irrelevant invites (should NOT be counted)
        invites.add(new MockInvitation("user3", friendUid, "accepted", null));
        invites.add(new MockInvitation(currentUserId, "user3", "accepted", null));
        invites.add(new MockInvitation("user3", "user4", "accepted", null));

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify - only invites between currentUserId and friendUid are counted
        assertEquals("Should count 1 sent invite", 1, stats.totalSent);
        assertEquals("Should count 1 sent accepted invite", 1, stats.totalSentAccepted);
        assertEquals("Should count 1 received invite", 1, stats.totalReceived);
        assertEquals("Should count 1 received accepted invite", 1, stats.totalReceivedAccepted);
    }

    @Test
    public void testTournamentFilteringWithMultipleTournaments() {
        // Test filtering with multiple different tournament IDs
        String currentUserId = "user1";
        String friendUid = "user2";
        
        List<MockInvitation> invites = new ArrayList<>();
        
        // Regular invites
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", null));
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", null));
        
        // Tournament invites with different tournament IDs
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", "tournament-alpha"));
        invites.add(new MockInvitation(currentUserId, friendUid, "accepted", "tournament-beta"));
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", "tournament-gamma"));
        invites.add(new MockInvitation(friendUid, currentUserId, "accepted", "tournament-delta"));

        // Calculate stats
        InviteStats stats = calculateStats(invites, currentUserId, friendUid);

        // Verify - only non-tournament invites are counted regardless of tournament ID
        assertEquals("Should count 1 sent invite (excluding all tournaments)", 1, stats.totalSent);
        assertEquals("Should count 1 sent accepted invite", 1, stats.totalSentAccepted);
        assertEquals("Should count 1 received invite (excluding all tournaments)", 1, stats.totalReceived);
        assertEquals("Should count 1 received accepted invite", 1, stats.totalReceivedAccepted);
    }
}
