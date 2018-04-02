package com.jibo.apptoolkit.android;

import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.jibo.apptoolkit.android.library.TestRomCommanderWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by dacuesta on 20/02/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RomSdkTest {

    // attributes

    private TestRomCommanderWrapper testCommanderWrapper = new TestRomCommanderWrapper();

    // before & after functions

    @Before
    public void setup() throws Exception {
        testCommanderWrapper.connect();
        Thread.sleep(10000);
    }

    @After
    public void clean() {
        testCommanderWrapper.disconnect();
    }

    // test functions

    @Test
    public void connected() throws Exception {
        assertTrue(testCommanderWrapper.isConnected());
    }

    @Test
    public void takePhoto() throws Exception {
        testCommanderWrapper.takePhoto();
        Thread.sleep(50000);

        testCommanderWrapper.cancel();
        Thread.sleep(5000);

        assertEquals(0, testCommanderWrapper.getErrorCounter());
    }

    @Test
    // FIXME the test is failing
    public void video() throws Exception {
        testCommanderWrapper.video();
        Thread.sleep(5000);

        testCommanderWrapper.cancel();
        Thread.sleep(5000);

        assertEquals(0, testCommanderWrapper.getErrorCounter());
    }

    @Test
    public void say() throws Exception {
        testCommanderWrapper.say();
        Thread.sleep(5000);

        assertEquals(0, testCommanderWrapper.getErrorCounter());
    }

    @Test
    // FIXME there is some lookAt event which is failing ( 2 -> lookAtEntity)
    public void lookAt() throws Exception {
        for (int i = 0; i < 4; i ++) {
            testCommanderWrapper.lookAt(i);
            Thread.sleep(5000);
        }

        assertEquals(0, testCommanderWrapper.getErrorCounter());
    }

}
