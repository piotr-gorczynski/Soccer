package piotr_gorczynski.soccer2;

import java.util.List;

final class TournamentVisibility {
    private TournamentVisibility() {
    }

    static boolean isVisible(Object visibleInFlavoursValue, String currentFlavour) {
        if (!(visibleInFlavoursValue instanceof List<?> visibleInFlavours)) {
            // Tournaments created before flavour visibility was introduced remain visible.
            return true;
        }

        return visibleInFlavours.contains("global")
                || visibleInFlavours.contains(currentFlavour);
    }
}
