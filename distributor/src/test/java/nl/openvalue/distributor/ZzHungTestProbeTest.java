package nl.openvalue.distributor;

import java.time.Duration;

import org.junit.jupiter.api.Test;

// THROWAWAY PROBE for fix-ci-report-on-timeout. Never merged: simulates a hung test so the CI job times out.
class ZzHungTestProbeTest {

    @Test
    void hangsLongerThanTheCiTimeout() throws InterruptedException {
        Thread.sleep(Duration.ofMinutes(30));
    }
}
