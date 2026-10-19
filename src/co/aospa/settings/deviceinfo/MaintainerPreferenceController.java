/*
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;

/** Shows the build maintainer, which device trees set through ro.penguin.maintainer. */
public class MaintainerPreferenceController extends BasePreferenceController {

    private static final String PROPERTY_MAINTAINER = "ro.penguin.maintainer";

    public MaintainerPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return TextUtils.isEmpty(getMaintainer()) ? UNSUPPORTED_ON_DEVICE : AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public CharSequence getSummary() {
        return getMaintainer();
    }

    private static String getMaintainer() {
        return SystemProperties.get(PROPERTY_MAINTAINER);
    }
}
