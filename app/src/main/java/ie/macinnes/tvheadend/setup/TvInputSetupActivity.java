/* Copyright 2016 Kiall Mac Innes <kiall@macinnes.ie>

Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License. You may obtain
a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
*/
package ie.macinnes.tvheadend.setup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ie.macinnes.tvheadend.Constants;
import ie.macinnes.tvheadend.MiscUtils;
import ie.macinnes.tvheadend.R;
import ie.macinnes.tvheadend.TvContractUtils;
import ie.macinnes.tvheadend.account.AccountUtils;
import ie.macinnes.tvheadend.settings.SettingsActivity;
import ie.macinnes.tvheadend.sync.EpgSyncService;

public class TvInputSetupActivity extends Activity {
    private static final String TAG = TvInputSetupActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GuidedStepFragment fragment = new IntroFragment();
        fragment.setArguments(getIntent().getExtras());
        GuidedStepFragment.addAsRoot(this, fragment, android.R.id.content);
    }

    public static abstract class BaseGuidedStepFragment extends GuidedStepFragment {
        protected AccountManager mAccountManager;

        protected static Account sAccount;

        @Override
        public int onProvideTheme() {
            return R.style.Theme_Wizard_Setup;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String inputId = getActivity().getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);

            if (!inputId.equals(TvContractUtils.getInputId())) {
                // Ensure the provided ID matches what we expect, as we only have a single input.
                throw new RuntimeException(
                        "Setup Activity called for unknown inputId: " + inputId + " (expected: " + TvContractUtils.getInputId() + ")");
            }

            mAccountManager = AccountManager.get(getActivity());
        }

        protected Account getAccountByName(String name) {
            Log.d(TAG, "getAccountByName(" + name + ")");

            Account[] accounts = AccountUtils.getAllAccounts(getActivity());

            Log.d(TAG, "Checking " + Integer.toString(accounts.length) + " accounts");

            for (Account account : accounts) {
                Log.d(TAG, "Checking Account: " + account.name);

                if (account.name.equals(name)) {
                    Log.d(TAG, "Found account");
                    return account;
                }
            }

            Log.d(TAG, "Failed to find account, no accounts with matching name");
            return null;
        }
    }

    public static class IntroFragment extends BaseGuidedStepFragment {
        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(
                    "Introduction",
                    "Welcome to the Tvhheadend Live Channel, we'll guide you through the setup " +
                    "process now, once done you will be ready to watch TV",
                    "TVHeadend",
                    null);

            return guidance;
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .title("Begin")
                    .description("Start Tvheadend Live Channel Setup")
                    .editable(false)
                    .build();

            actions.add(action);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            // Move onto the next step
            GuidedStepFragment fragment = new AccountSelectorFragment();
            fragment.setArguments(getArguments());
            add(getFragmentManager(), fragment);
        }
    }

    public static class AccountSelectorFragment extends BaseGuidedStepFragment {
        private static final int ACTION_ID_CONFIRM = 1;
        private static final int ACTION_ID_SELECT_ACCOUNT = 2;
        private static final int ACTION_ID_NEW_ACCOUNT = 3;

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(
                    "Select An Account",
                    "Please choose an existing, or create a new TVHeadend account to use",
                    "TVHeadend",
                    null);

            return guidance;
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            List<GuidedAction> subActions = new ArrayList();

            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_SELECT_ACCOUNT)
                    .title("Account Selection")
                    .editTitle("")
                    .description("Select Account")
                    .subActions(subActions)
                    .build();

            actions.add(action);

            action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_CONFIRM)
                    .title("Confirm")
                    .description("Confirm This Selection")
                    .editable(false)
                    .build();
            action.setEnabled(false);

            actions.add(action);
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(TAG, "onResume()");
            GuidedAction accountAction = findActionById(ACTION_ID_SELECT_ACCOUNT);

            List<GuidedAction> accountSubActions = accountAction.getSubActions();
            accountSubActions.clear();

            Account[] accounts = AccountUtils.getAllAccounts(getActivity());

            for (Account account : accounts) {
                GuidedAction action = new GuidedAction.Builder(getActivity())
                        .title(account.name)
                        .description(mAccountManager.getUserData(account, Constants.KEY_HOSTNAME))
                        .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                        .build();

                accountSubActions.add(action);
            }

            accountSubActions.add(new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_NEW_ACCOUNT)
                    .title("Add New Account")
                    .description("")
                    .editable(false)
                    .build()
            );

            if (sAccount != null) {
                accountAction.setDescription(sAccount.name);
                findActionById(ACTION_ID_CONFIRM).setEnabled(true);
            } else {
                findActionById(ACTION_ID_CONFIRM).setEnabled(false);
            }

            notifyActionChanged(findActionPositionById(ACTION_ID_CONFIRM));
        }

        @Override
        public boolean onSubGuidedActionClicked(GuidedAction action) {
            if (action.isChecked()) {
                sAccount = getAccountByName(action.getTitle().toString());

                findActionById(ACTION_ID_SELECT_ACCOUNT).setDescription(sAccount.name);
                notifyActionChanged(findActionPositionById(ACTION_ID_SELECT_ACCOUNT));

                findActionById(ACTION_ID_CONFIRM).setEnabled(true);
                notifyActionChanged(findActionPositionById(ACTION_ID_CONFIRM));

                return true;
            } else {

                mAccountManager.addAccount(Constants.ACCOUNT_TYPE, null, null, new Bundle(), getActivity(), new AddAccountCallback(), null);
                return true;
            }
        }

        private class AddAccountCallback implements AccountManagerCallback<Bundle> {
            @Override
            public void run(AccountManagerFuture<Bundle> result) {
                onResume();
            }
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (ACTION_ID_CONFIRM == action.getId()) {
                // Move onto the next step
                GuidedStepFragment fragment = new SessionSelectorFragment();
                fragment.setArguments(getArguments());
                add(getFragmentManager(), fragment);
            }
        }
    }

    public static class SessionSelectorFragment extends BaseGuidedStepFragment {
        private static final int ACTION_ID_MEDIA_PLAYER = 1;
        private static final int ACTION_ID_EXO_PLAYER = 2;
        private static final int ACTION_ID_VLC = 3;

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(
                    "Session Selector",
                    "There are several Session implementatioms, please choose one",
                    "TVHeadend",
                    null);

            return guidance;
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_VLC)
                    .title("LibVLC")
                    .description("VideoLAN LibVLC (Recommended)")
                    .editable(false)
                    .build();

            actions.add(action);

            action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_MEDIA_PLAYER)
                    .title("Media Player")
                    .description("Android Media Player")
                    .editable(false)
                    .build();

            actions.add(action);

            action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_EXO_PLAYER)
                    .title("ExoPlayer")
                    .description("Google ExoPlayer (Experimental)")
                    .editable(false)
                    .build();

            actions.add(action);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            String session;

            if (action.getId() == ACTION_ID_MEDIA_PLAYER) {
                session = Constants.SESSION_MEDIA_PLAYER;
            } else if (action.getId() == ACTION_ID_EXO_PLAYER) {
                session = Constants.SESSION_EXO_PLAYER;
            } else if (action.getId() == ACTION_ID_VLC) {
                session = Constants.SESSION_VLC;
            } else {
                return;
            }

            // Store the chosen session type
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                    Constants.PREFERENCE_TVHEADEND, Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.KEY_SESSION, session);
            editor.apply();

            // Move onto the next step
            GuidedStepFragment fragment = new SyncingFragment();
            fragment.setArguments(getArguments());
            add(getFragmentManager(), fragment);
        }
    }

    public static class SyncingFragment extends BaseGuidedStepFragment {

        protected Runnable mInitialSyncCompleteCallback = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Initial Sync Completed");

                // Move to the CompletedFragment
                GuidedStepFragment fragment = new CompletedFragment();
                fragment.setArguments(getArguments());
                add(getFragmentManager(), fragment);

                // Re-Start EPG sync service (removing quick-sync)
                Context context = getActivity().getBaseContext();

                Intent intent = new Intent(context, EpgSyncService.class);
                context.stopService(intent);
                context.startService(intent);
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            EpgSyncService.addInitialSyncCompleteCallback(mInitialSyncCompleteCallback);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            EpgSyncService.removeInitialSyncCompleteCallback(mInitialSyncCompleteCallback);
        }

        @Override
        public void onStart() {
            super.onStart();

            // Re-Start EPG sync service
            Context context = getActivity().getBaseContext();

            Intent intent = new Intent(context, EpgSyncService.class);
            intent.putExtra(EpgSyncService.SYNC_QUICK, true);

            context.stopService(intent);
            context.startService(intent);
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            GuidedActionsStylist stylist = new GuidedActionsStylist() {
                @Override
                public int onProvideItemLayoutId() {
                    return R.layout.setup_progress;
                }

            };
            return stylist;
        }

        @Override
        public int onProvideTheme() {
            return R.style.Theme_Wizard_Setup_NoSelector;
        }

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(
                    "Syncing Channels and Program data",
                    "Just a few seconds please :)",
                    "TVHeadend",
                    null);

            return guidance;
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .title("Processing")
                    .infoOnly(true)
                    .build();
            actions.add(action);
        }
    }

    public static class CompletedFragment extends BaseGuidedStepFragment {
        private static final int ACTION_ID_SETTINGS = 1;
        private static final int ACTION_ID_COMPLETE = 2;

        @NonNull
        @Override
        public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
            GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(
                    "Setup Complete",
                    "More EPG data, channel logs, etc are downloading in the background",
                    "TVHeadend",
                    null);

            return guidance;
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_SETTINGS)
                    .title("Settings")
                    .description("Advanced Settings")
                    .editable(false)
                    .build();

            actions.add(action);

            action = new GuidedAction.Builder(getActivity())
                    .id(ACTION_ID_COMPLETE)
                    .title("Complete")
                    .description("You're all set!")
                    .editable(false)
                    .build();

            actions.add(action);
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == ACTION_ID_SETTINGS) {
                startActivity(SettingsActivity.getPreferencesIntent(getActivity()));
            } else if (action.getId() == ACTION_ID_COMPLETE) {
                MiscUtils.setSetupComplete(getActivity(), true);
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            }
        }
    }
}
