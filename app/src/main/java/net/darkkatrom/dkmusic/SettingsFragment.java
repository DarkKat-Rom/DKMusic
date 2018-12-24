/*
 * Copyright (C) 2018 DarkKat
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

package net.darkkatrom.dkmusic;

import android.Manifest.permission;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.TwoStatePreference;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {

    private static final int TOAST_SPACE_TOP = 24;

    private static final int DLG_PERMISSION_INFO = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        checkPermission();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void checkPermission() {
        if (Config.getShowVisualizer(getActivity())) {
            if (getActivity().checkSelfPermission(permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED) {
                showPermissionInfoDialog(DLG_PERMISSION_INFO);
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (Config.PREF_KEY_SHOW_VISUALIZER.equals(key)) {
            checkPermission();
        }
    }

    private void showPermissionInfoDialog(int id) {
        DialogFragment newFragment = PermissionInfoDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class PermissionInfoDialogFragment extends DialogFragment {

        public static PermissionInfoDialogFragment newInstance(int id) {
            PermissionInfoDialogFragment frag = new PermissionInfoDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        SettingsFragment getOwner() {
            return (SettingsFragment) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_PERMISSION_INFO:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_message)
                    .setNegativeButton(R.string.dlg_cancel, 
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().disableVisualizer(false);
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().showRequestPermissionDialog();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private void showRequestPermissionDialog() {
        requestPermissions(new String[] { permission.RECORD_AUDIO }, 1);
    }

    private void disableVisualizer(boolean showToast) {
        ((TwoStatePreference) findPreference(Config.PREF_KEY_SHOW_VISUALIZER))
                .setChecked(false);
        if (showToast) {
            showToast(R.string.toast_message);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                boolean granted = false;
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                }
                if (!granted) {
                    disableVisualizer(true);
                }
                break;
            }
        }
    }

    private void showToast(int resId) {
		float density = getActivity().getResources().getDisplayMetrics().density;
        int actionBarHeight = ((AppCompatActivity) getActivity()).getSupportActionBar().getHeight();
        int spaceTopDP = TOAST_SPACE_TOP * Math.round(density);

        View v = getActivity().getLayoutInflater().inflate(R.layout.transient_notification, null);

        Toast toast = new Toast(getActivity());
        toast.setView(v);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }
}
