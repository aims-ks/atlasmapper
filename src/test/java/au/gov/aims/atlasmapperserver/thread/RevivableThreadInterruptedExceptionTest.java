package au.gov.aims.atlasmapperserver.thread;

import au.gov.aims.atlasmapperserver.Utils;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RevivableThreadInterruptedExceptionTest extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(RevivableThreadInterruptedExceptionTest.class.getName());

    public void testCatch() {
        try {
            try {
                throw new RevivableThreadInterruptedException();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Unexpected exception: " + Utils.getExceptionMessage(ex), ex);
                Assert.fail("Unexpected exception");
            }
        } catch (RevivableThreadInterruptedException ex) {
            Assert.assertNotNull("The RevivableThreadInterruptedException is null", ex);
            Assert.assertTrue("The RevivableThreadInterruptedException stack trace is empty", ex.getStackTrace().length > 1);
        }
    }
}
