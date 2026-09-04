package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HandTutorialAnimationTimingTest {

    @Test
    public void handRemainsHiddenDuringCharacterAnimations() {
        assertFalse(Field.shouldRenderHandTutorial(true, true, true,
                false, true, false));
        assertFalse(Field.shouldRenderHandTutorial(true, true, true,
                false, false, true));
        assertFalse(Field.shouldRenderHandTutorial(true, true, true,
                false, true, true));
    }

    @Test
    public void handAppearsAfterCharacterAnimationsFinish() {
        assertTrue(Field.shouldRenderHandTutorial(true, true, true,
                false, false, false));
    }

    @Test
    public void existingVisibilityRequirementsStillApply() {
        assertFalse(Field.shouldRenderHandTutorial(false, true, true,
                false, false, false));
        assertFalse(Field.shouldRenderHandTutorial(true, false, true,
                false, false, false));
        assertFalse(Field.shouldRenderHandTutorial(true, true, false,
                false, false, false));
        assertFalse(Field.shouldRenderHandTutorial(true, true, true,
                true, false, false));
    }
}
