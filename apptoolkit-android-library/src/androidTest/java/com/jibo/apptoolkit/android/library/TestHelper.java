package com.jibo.apptoolkit.android.library;

import com.jibo.apptoolkit.android.util.FlavourHelper;

/**
 * Created by dacuesta on 20/02/2018.
 */

final class TestHelper {
    private final static String IP_ADDRESS = "10.0.2.2";

    static String getJiboURL() {
        return "ws://" + IP_ADDRESS + ":" + FlavourHelper.SOCKET_PORT;
    }
}
