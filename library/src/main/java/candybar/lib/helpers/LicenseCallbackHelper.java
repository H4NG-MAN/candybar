package candybar.lib.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.danimahardhika.android.helpers.license.LicenseCallback;
import com.danimahardhika.android.helpers.license.LicenseHelper;

import candybar.lib.R;
import candybar.lib.fragments.dialog.ChangelogFragment;
import candybar.lib.preferences.Preferences;

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class LicenseCallbackHelper implements LicenseCallback {

    private final Context mContext;
    private final MaterialDialog mDialog;

    public LicenseCallbackHelper(@NonNull Context context) {
        mContext = context;

        MaterialDialog.Builder builder = new MaterialDialog.Builder(mContext);
        builder.typeface(
                TypefaceHelper.getMedium(mContext),
                TypefaceHelper.getRegular(mContext));
        builder.content(R.string.license_checking)
                .progress(true, 0);

        mDialog = builder.build();
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onLicenseCheckStart() {
        mDialog.show();
    }

    @Override
    public void onLicenseCheckFinished(LicenseHelper.Status status) {
        mDialog.dismiss();
        if (status == LicenseHelper.Status.RETRY) {
            showRetryDialog();
            return;
        }

        showLicenseDialog(status);
    }

    private void showLicenseDialog(LicenseHelper.Status status) {
        int message = status == LicenseHelper.Status.SUCCESS ?
                R.string.license_check_success : R.string.license_check_failed;
        new MaterialDialog.Builder(mContext)
                .typeface(
                        TypefaceHelper.getMedium(mContext),
                        TypefaceHelper.getRegular(mContext))
                .title(R.string.license_check)
                .content(message)
                .positiveText(R.string.close)
                .onPositive((dialog, which) -> {
                    onLicenseChecked(status);
                    dialog.dismiss();
                })
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();
    }

    private void showRetryDialog() {
        new MaterialDialog.Builder(mContext)
                .typeface(
                        TypefaceHelper.getMedium(mContext),
                        TypefaceHelper.getRegular(mContext))
                .title(R.string.license_check)
                .content(R.string.license_check_retry)
                .positiveText(R.string.close)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .onPositive((dialog, which) -> ((AppCompatActivity) mContext).finish())
                .show();
    }

    private void onLicenseChecked(LicenseHelper.Status status) {
        Preferences.get(mContext).setFirstRun(false);
        if (status == LicenseHelper.Status.SUCCESS) {
            Preferences.get(mContext).setLicensed(true);
            if (Preferences.get(mContext).isNewVersion())
                ChangelogFragment.showChangelog(((AppCompatActivity) mContext).getSupportFragmentManager());
        } else if (status == LicenseHelper.Status.FAILED) {
            Preferences.get(mContext).setLicensed(false);
            PackageManager pm = mContext.getPackageManager();
            ComponentName compName = new ComponentName(mContext.getPackageName(), mContext.getPackageName() + ".alias.Intent");
            pm.setComponentEnabledSetting(
                    compName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            ((AppCompatActivity) mContext).finish();
        }
    }
}
