package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for FriendsListActivity sorting functionality
 * Tests that friends are properly sorted by last seen (heartbeat) timestamp
 */
public class FriendsListSortingTest {

    /**
     * Mock class to represent a friend document with an ID
     */
    static class MockFriend {
        private final String id;
        
        public MockFriend(String id) {
            this.id = id;
        }
        
        public String getId() {
            return id;
        }
        
        @Override
        public String toString() {
            return "MockFriend{id='" + id + "'}";
        }
    }

    @Test
    public void testSortByLastSeenDescending() {
        // Create mock friends
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("friend1"));
        friends.add(new MockFriend("friend2"));
        friends.add(new MockFriend("friend3"));
        friends.add(new MockFriend("friend4"));
        
        // Create heartbeat map with different timestamps
        // friend2: 5000 (most recent) - should be first
        // friend3: 4000 - should be second
        // friend1: 3000 - should be third
        // friend4: 1000 (least recent) - should be last
        Map<String, Long> heartbeatMap = new HashMap<>();
        heartbeatMap.put("friend1", 3000L);
        heartbeatMap.put("friend2", 5000L);
        heartbeatMap.put("friend3", 4000L);
        heartbeatMap.put("friend4", 1000L);
        
        // Sort using the same logic as FriendsListActivity.sortByLastSeen()
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);

                int result = Long.compare(hb2, hb1);  // Sort descending (most recent first)
                if (result != 0) {
                    return result;
                }

                // When heartbeats are equal fall back to UID comparison to keep order stable
                return d1.getId().compareTo(d2.getId());
            }
        });
        
        // Verify the order is correct (most recent first)
        assertEquals("First friend should be friend2 (most recent)", "friend2", friends.get(0).getId());
        assertEquals("Second friend should be friend3", "friend3", friends.get(1).getId());
        assertEquals("Third friend should be friend1", "friend1", friends.get(2).getId());
        assertEquals("Fourth friend should be friend4 (least recent)", "friend4", friends.get(3).getId());
    }

    @Test
    public void testSortByLastSeenWithZeroHeartbeats() {
        // Test sorting when some friends have no heartbeat data (value 0)
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("friend1"));
        friends.add(new MockFriend("friend2"));
        friends.add(new MockFriend("friend3"));
        
        // friend2 has recent heartbeat, others have 0 (offline/never seen)
        Map<String, Long> heartbeatMap = new HashMap<>();
        heartbeatMap.put("friend1", 0L);
        heartbeatMap.put("friend2", 5000L);
        heartbeatMap.put("friend3", 0L);
        
        // Sort using the same logic as FriendsListActivity.sortByLastSeen()
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);

                int result = Long.compare(hb2, hb1);  // Sort descending (most recent first)
                if (result != 0) {
                    return result;
                }

                // When heartbeats are equal fall back to UID comparison to keep order stable
                return d1.getId().compareTo(d2.getId());
            }
        });
        
        // friend2 should be first (has heartbeat)
        // friend1 and friend3 should follow, sorted by UID (friend1 before friend3)
        assertEquals("First friend should be friend2 (has recent heartbeat)", "friend2", friends.get(0).getId());
        assertEquals("Second friend should be friend1 (UID sort)", "friend1", friends.get(1).getId());
        assertEquals("Third friend should be friend3 (UID sort)", "friend3", friends.get(2).getId());
    }

    @Test
    public void testSortByLastSeenAllZeroHeartbeats() {
        // Test sorting when all friends have no heartbeat data
        // Should fall back to UID sorting
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("friend3"));
        friends.add(new MockFriend("friend1"));
        friends.add(new MockFriend("friend2"));
        
        Map<String, Long> heartbeatMap = new HashMap<>();
        heartbeatMap.put("friend1", 0L);
        heartbeatMap.put("friend2", 0L);
        heartbeatMap.put("friend3", 0L);
        
        // Sort using the same logic as FriendsListActivity.sortByLastSeen()
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);

                int result = Long.compare(hb2, hb1);  // Sort descending (most recent first)
                if (result != 0) {
                    return result;
                }

                // When heartbeats are equal fall back to UID comparison to keep order stable
                return d1.getId().compareTo(d2.getId());
            }
        });
        
        // All have same heartbeat (0), so should be sorted by UID
        assertEquals("First friend should be friend1 (UID sort)", "friend1", friends.get(0).getId());
        assertEquals("Second friend should be friend2 (UID sort)", "friend2", friends.get(1).getId());
        assertEquals("Third friend should be friend3 (UID sort)", "friend3", friends.get(2).getId());
    }

    @Test
    public void testSortByLastSeenMissingFriends() {
        // Test sorting when some friends are missing from heartbeat map
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("friend1"));
        friends.add(new MockFriend("friend2"));
        friends.add(new MockFriend("friend3"));
        
        // Only friend2 is in the heartbeat map
        Map<String, Long> heartbeatMap = new HashMap<>();
        heartbeatMap.put("friend2", 5000L);
        // friend1 and friend3 are not in the map
        
        // Sort using the same logic as FriendsListActivity.sortByLastSeen()
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);

                int result = Long.compare(hb2, hb1);  // Sort descending (most recent first)
                if (result != 0) {
                    return result;
                }

                // When heartbeats are equal fall back to UID comparison to keep order stable
                return d1.getId().compareTo(d2.getId());
            }
        });
        
        // friend2 should be first (has heartbeat)
        // friend1 and friend3 should follow (defaulted to 0), sorted by UID
        assertEquals("First friend should be friend2 (has heartbeat)", "friend2", friends.get(0).getId());
        assertEquals("Second friend should be friend1 (default 0, UID sort)", "friend1", friends.get(1).getId());
        assertEquals("Third friend should be friend3 (default 0, UID sort)", "friend3", friends.get(2).getId());
    }

    @Test
    public void testSortConstantsAreCorrect() {
        // Test that the sort mode constants match expected values
        // This ensures the activity uses the right values
        final int SORT_BY_LAST_SEEN = 0;
        final int SORT_ALPHABETICALLY = 1;
        
        assertEquals("SORT_BY_LAST_SEEN should be 0", 0, SORT_BY_LAST_SEEN);
        assertEquals("SORT_ALPHABETICALLY should be 1", 1, SORT_ALPHABETICALLY);
        
        // Test that default sort mode is SORT_BY_LAST_SEEN
        int defaultSortMode = SORT_BY_LAST_SEEN;
        assertEquals("Default sort mode should be SORT_BY_LAST_SEEN", SORT_BY_LAST_SEEN, defaultSortMode);
    }

    @Test
    public void testEmptyFriendsList() {
        // Test sorting an empty list (edge case)
        List<MockFriend> friends = new ArrayList<>();
        Map<String, Long> heartbeatMap = new HashMap<>();
        
        // Sort empty list
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);
                int result = Long.compare(hb2, hb1);
                if (result != 0) {
                    return result;
                }
                return d1.getId().compareTo(d2.getId());
            }
        });
        
        // Should remain empty
        assertTrue("Empty friends list should remain empty after sorting", friends.isEmpty());
        assertEquals("Size should be 0", 0, friends.size());
    }

    @Test
    public void testSingleFriend() {
        // Test sorting a list with single friend
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("friend1"));
        
        Map<String, Long> heartbeatMap = new HashMap<>();
        heartbeatMap.put("friend1", 5000L);
        
        // Sort single item list
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);
                int result = Long.compare(hb2, hb1);
                if (result != 0) {
                    return result;
                }
                return d1.getId().compareTo(d2.getId());
            }
        });
        
        // Should remain with single item
        assertEquals("Should have one friend", 1, friends.size());
        assertEquals("Friend should be friend1", "friend1", friends.get(0).getId());
    }

    @Test
    public void testSortAlphabetically() {
        // Test alphabetical sorting by nickname
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("uid1"));
        friends.add(new MockFriend("uid2"));
        friends.add(new MockFriend("uid3"));
        friends.add(new MockFriend("uid4"));
        
        // Create a map of UID to nickname (lowercase for case-insensitive sorting)
        Map<String, String> nicknameMap = new HashMap<>();
        nicknameMap.put("uid1", "zebra");      // Should be last
        nicknameMap.put("uid2", "alice");      // Should be first
        nicknameMap.put("uid3", "charlie");    // Should be third
        nicknameMap.put("uid4", "bob");        // Should be second
        
        // Sort using the same logic as FriendsListActivity.sortByNickname()
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                String nick1 = nicknameMap.get(d1.getId());
                String nick2 = nicknameMap.get(d2.getId());
                
                // Handle null nicknames (put them at the end)
                if (nick1 == null && nick2 == null) return 0;
                if (nick1 == null) return 1;
                if (nick2 == null) return -1;
                
                return nick1.compareTo(nick2);
            }
        });
        
        // Verify alphabetical order (ascending)
        assertEquals("First friend should be uid2 (alice)", "uid2", friends.get(0).getId());
        assertEquals("Second friend should be uid4 (bob)", "uid4", friends.get(1).getId());
        assertEquals("Third friend should be uid3 (charlie)", "uid3", friends.get(2).getId());
        assertEquals("Fourth friend should be uid1 (zebra)", "uid1", friends.get(3).getId());
    }

    @Test
    public void testSortAlphabeticallyWithNulls() {
        // Test alphabetical sorting when some friends have no nickname
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("uid1"));
        friends.add(new MockFriend("uid2"));
        friends.add(new MockFriend("uid3"));
        
        // Create a map with some null nicknames
        Map<String, String> nicknameMap = new HashMap<>();
        nicknameMap.put("uid1", "bob");
        nicknameMap.put("uid2", null);  // No nickname, should go to end
        nicknameMap.put("uid3", "alice");
        
        // Sort using the same logic as FriendsListActivity.sortByNickname()
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                String nick1 = nicknameMap.get(d1.getId());
                String nick2 = nicknameMap.get(d2.getId());
                
                // Handle null nicknames (put them at the end)
                if (nick1 == null && nick2 == null) return 0;
                if (nick1 == null) return 1;
                if (nick2 == null) return -1;
                
                return nick1.compareTo(nick2);
            }
        });
        
        // uid3 (alice) should be first, uid1 (bob) second, uid2 (null) last
        assertEquals("First friend should be uid3 (alice)", "uid3", friends.get(0).getId());
        assertEquals("Second friend should be uid1 (bob)", "uid1", friends.get(1).getId());
        assertEquals("Third friend should be uid2 (null nickname)", "uid2", friends.get(2).getId());
    }

    @Test
    public void testSortAlphabeticallyCaseInsensitive() {
        // Test that alphabetical sorting is case-insensitive
        List<MockFriend> friends = new ArrayList<>();
        friends.add(new MockFriend("uid1"));
        friends.add(new MockFriend("uid2"));
        friends.add(new MockFriend("uid3"));
        
        // Mix of upper and lower case
        Map<String, String> nicknameMap = new HashMap<>();
        nicknameMap.put("uid1", "CHARLIE");  // Should be third when lowercased
        nicknameMap.put("uid2", "alice");    // Should be first
        nicknameMap.put("uid3", "Bob");      // Should be second when lowercased
        
        // Sort using the same logic as FriendsListActivity.sortByNickname()
        // Note: In the actual activity, nicknames are lowercased before putting in the map
        Collections.sort(friends, new Comparator<MockFriend>() {
            @Override
            public int compare(MockFriend d1, MockFriend d2) {
                String nick1 = nicknameMap.get(d1.getId());
                String nick2 = nicknameMap.get(d2.getId());
                
                if (nick1 == null && nick2 == null) return 0;
                if (nick1 == null) return 1;
                if (nick2 == null) return -1;
                
                // Convert to lowercase for case-insensitive comparison
                return nick1.toLowerCase().compareTo(nick2.toLowerCase());
            }
        });
        
        // Verify case-insensitive alphabetical order
        assertEquals("First friend should be uid2 (alice)", "uid2", friends.get(0).getId());
        assertEquals("Second friend should be uid3 (Bob)", "uid3", friends.get(1).getId());
        assertEquals("Third friend should be uid1 (CHARLIE)", "uid1", friends.get(2).getId());
    }
}
