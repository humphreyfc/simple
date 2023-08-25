/*
 * This file is part of FairEmail.
 *
 * FairEmail is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * FairEmail is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with FairEmail. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2018, Marcel Bokhorst (M66B)
 * Copyright 2018-2023, Distopico (dystopia project) <distopico@riseup.net> and contributors
 */
package org.dystopia.email;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.HttpsURLConnection;

public class ActivityView extends ActivityBase
    implements FragmentManager.OnBackStackChangedListener {
    private View view;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private long message = -1;
    private long attachment = -1;

    private OpenPgpServiceConnection pgpService;

    private static final int ATTACHMENT_BUFFER_SIZE = 8192; // bytes

    static final int REQUEST_UNIFIED = 1;
    static final int REQUEST_THREAD = 2;
    static final int REQUEST_ERROR = 3;

    static final int REQUEST_ATTACHMENT = 1;
    static final int REQUEST_DECRYPT = 3;

    static final String ACTION_VIEW_MESSAGES = BuildConfig.APPLICATION_ID + ".VIEW_MESSAGES";
    static final String ACTION_VIEW_THREAD = BuildConfig.APPLICATION_ID + ".VIEW_THREAD";
    static final String ACTION_VIEW_FULL = BuildConfig.APPLICATION_ID + ".VIEW_FULL";
    static final String ACTION_EDIT_FOLDER = BuildConfig.APPLICATION_ID + ".EDIT_FOLDER";
    static final String ACTION_EDIT_ANSWER = BuildConfig.APPLICATION_ID + ".EDIT_ANSWER";
    static final String ACTION_STORE_ATTACHMENT = BuildConfig.APPLICATION_ID + ".STORE_ATTACHMENT";
    static final String ACTION_DECRYPT = BuildConfig.APPLICATION_ID + ".DECRYPT";

    static final String UPDATE_LATEST_API =
        "https://framagit.org/api/v4/projects/dystopia-project%2Fsimple-email/repository/tags";
    static final long UPDATE_INTERVAL = 12 * 3600 * 1000L; // milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = LayoutInflater.from(this).inflate(R.layout.activity_view, null);
        setContentView(view);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(Helper.resolveColor(this, R.attr.colorDrawerScrim));

        drawerToggle =
            new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name) {
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    getSupportActionBar().setTitle(getString(R.string.app_name));
                }

                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    getSupportActionBar().setTitle(getString(R.string.app_name));
                }
            };
        drawerLayout.addDrawerListener(drawerToggle);

        drawerList = findViewById(R.id.drawer_list);
        drawerList.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    DrawerItem item = (DrawerItem) parent.getAdapter().getItem(position);
                    switch (item.getId()) {
                        case -1:
                            onMenuFolders((long) item.getData());
                            break;
                        case R.string.menu_setup:
                            onMenuSetup();
                            break;
                        case R.string.menu_answers:
                            onMenuAnswers();
                            break;
                        case R.string.menu_operations:
                            onMenuOperations();
                            break;
                        case R.string.menu_legend:
                            onMenuLegend();
                            break;
                        case R.string.menu_faq:
                            onMenuFAQ();
                            break;
                        case R.string.menu_privacy:
                            onMenuPrivacy();
                            break;
                        case R.string.menu_about:
                            onMenuAbout();
                            break;
                        case R.string.menu_invite:
                            onMenuInvite();
                            break;
                    }

                    drawerLayout.closeDrawer(drawerList);
                }
            });

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        DB.getInstance(this)
            .account()
            .liveAccounts(true)
            .observe(
                this,
                new Observer<List<EntityAccount>>() {
                    @Override
                    public void onChanged(@Nullable List<EntityAccount> accounts) {
                        if (accounts == null) {
                            accounts = new ArrayList<>();
                        }

                        ArrayAdapterDrawer drawerArray = new ArrayAdapterDrawer(ActivityView.this);

                        final Collator collator = Collator.getInstance(Locale.getDefault());
                        collator.setStrength(Collator.SECONDARY); // Case insensitive, process accents
                        // etc

                        Collections.sort(
                            accounts,
                            new Comparator<EntityAccount>() {
                                @Override
                                public int compare(EntityAccount a1, EntityAccount a2) {
                                    return collator.compare(a1.name, a2.name);
                                }
                            });

                        for (EntityAccount account : accounts) {
                            drawerArray.add(
                                new DrawerItem(
                                    R.layout.item_drawer,
                                    -1,
                                    R.drawable.baseline_folder_24,
                                    account.name,
                                    account.id));
                        }

                        drawerArray.add(new DrawerItem(R.layout.item_drawer_separator));

                        drawerArray.add(
                            new DrawerItem(
                                ActivityView.this,
                                R.layout.item_drawer,
                                R.drawable.baseline_settings_applications_24,
                                R.string.menu_setup));
                        drawerArray.add(
                            new DrawerItem(
                                ActivityView.this,
                                R.layout.item_drawer,
                                R.drawable.baseline_reply_24,
                                R.string.menu_answers));

                        drawerArray.add(new DrawerItem(R.layout.item_drawer_separator));

                        if (PreferenceManager.getDefaultSharedPreferences(ActivityView.this)
                            .getBoolean("debug", false)) {
                            drawerArray.add(
                                new DrawerItem(
                                    ActivityView.this,
                                    R.layout.item_drawer,
                                    R.drawable.baseline_list_24,
                                    R.string.menu_operations));
                        }

                        drawerArray.add(
                            new DrawerItem(
                                ActivityView.this,
                                R.layout.item_drawer,
                                R.drawable.baseline_help_24,
                                R.string.menu_legend));

                        if (getIntentFAQ().resolveActivity(getPackageManager()) != null) {
                            drawerArray.add(
                                new DrawerItem(
                                    ActivityView.this,
                                    R.layout.item_drawer,
                                    R.drawable.baseline_question_answer_24,
                                    R.string.menu_faq));
                        }

                        drawerArray.add(
                            new DrawerItem(
                                ActivityView.this,
                                R.layout.item_drawer,
                                R.drawable.baseline_account_box_24,
                                R.string.menu_privacy));

                        drawerArray.add(
                            new DrawerItem(
                                ActivityView.this,
                                R.layout.item_drawer,
                                R.drawable.baseline_info_24,
                                R.string.menu_about));

                        drawerArray.add(new DrawerItem(R.layout.item_drawer_separator));

                        drawerArray.add(
                            new DrawerItem(
                                ActivityView.this,
                                R.layout.item_drawer,
                                R.drawable.baseline_share_24,
                                R.string.menu_invite));

                        drawerList.setAdapter(drawerArray);
                    }
                });

        if (getSupportFragmentManager().getFragments().size() == 0) {
            Bundle args = new Bundle();
            args.putLong("folder", -1);

            FragmentMessages fragment = new FragmentMessages();
            fragment.setArguments(args);

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("unified");
            fragmentTransaction.commit();
        }

        if (savedInstanceState != null) {
            drawerToggle.setDrawerIndicatorEnabled(savedInstanceState.getBoolean("toggle"));
        }

        checkFirst();
        checkCrash();
        // TODO: check update from menu
        // checkUpdate();

        pgpService = new OpenPgpServiceConnection(this, "org.sufficientlysecure.keychain");
        pgpService.bindToService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("toggle", drawerToggle.isDrawerIndicatorEnabled());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        IntentFilter iff = new IntentFilter();
        iff.addAction(ACTION_VIEW_MESSAGES);
        iff.addAction(ACTION_VIEW_THREAD);
        iff.addAction(ACTION_VIEW_FULL);
        iff.addAction(ACTION_EDIT_FOLDER);
        iff.addAction(ACTION_EDIT_ANSWER);
        iff.addAction(ACTION_STORE_ATTACHMENT);
        iff.addAction(ACTION_DECRYPT);
        lbm.registerReceiver(receiver, iff);

        if (!pgpService.isBound()) {
            pgpService.bindToService();
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Log.i(Helper.TAG, "View intent=" + intent + " action=" + action);
        if (action != null && action.startsWith("thread")) {
            intent.setAction(null);
            setIntent(intent);

            ViewModelMessages model = ViewModelProviders.of(this).get(ViewModelMessages.class);
            model.setMessages(null);

            intent.putExtra("thread", action.split(":", 2)[1]);
            onViewThread(intent);
        }

        if (getIntent().hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
            String search = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString();

            intent.removeExtra(Intent.EXTRA_PROCESS_TEXT);
            setIntent(intent);

            Bundle args = new Bundle();
            args.putString("search", search);

            new SimpleTask<Long>() {
                @Override
                protected Long onLoad(Context context, Bundle args) {
                    DB db = DB.getInstance(context);

                    EntityFolder archive = db.folder().getPrimaryArchive();
                    if (archive == null) {
                        throw new IllegalArgumentException(getString(R.string.title_no_primary_archive));
                    }

                    db.message().deleteFoundMessages();

                    return archive.id;
                }

                @Override
                protected void onLoaded(Bundle args, Long archive) {
                    Bundle sargs = new Bundle();
                    sargs.putLong("folder", archive);
                    sargs.putString("search", args.getString("search"));

                    FragmentMessages fragment = new FragmentMessages();
                    fragment.setArguments(sargs);

                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("search");
                    fragmentTransaction.commit();
                }

                @Override
                protected void onException(Bundle args, Throwable ex) {
                    Helper.unexpectedError(ActivityView.this, ex);
                }
            }.load(this, args);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        if (pgpService != null) {
            pgpService.unbindFromService();
        }

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawerList)) {
            drawerLayout.closeDrawer(drawerList);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            finish();
        } else {
            if (drawerLayout.isDrawerOpen(drawerList)) {
                drawerLayout.closeDrawer(drawerList);
            }
            drawerToggle.setDrawerIndicatorEnabled(count == 1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    getSupportFragmentManager().popBackStack();
                }
                return true;
            default:
                return false;
        }
    }

    private void checkFirst() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("first", true)) {
            new DialogBuilderLifecycle(this, this)
                .setMessage(getString(R.string.title_hint_sync))
                .setPositiveButton(
                    android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().putBoolean("first", false).apply();
                        }
                    })
                .show();
        }
    }

    private void checkCrash() {
        new SimpleTask<Long>() {
            @Override
            protected Long onLoad(Context context, Bundle args) throws Throwable {
                File file = new File(context.getCacheDir(), "crash.log");
                if (file.exists()) {
                    // Get version info
                    StringBuilder sb = new StringBuilder();
                    Locale locale = Locale.US;

                    sb.append(context.getString(R.string.title_crash_info_remark)).append("\n\n\n\n");

                    sb.append(
                        String.format(
                            locale,
                            "%s: %s %s/%s\r\n",
                            context.getString(R.string.app_name),
                            BuildConfig.APPLICATION_ID,
                            BuildConfig.VERSION_NAME,
                            Helper.hasValidFingerprint(context) ? "1" : "3"));
                    sb.append(
                        String.format(
                            locale,
                            "Android: %s (SDK %d)\r\n",
                            Build.VERSION.RELEASE,
                            Build.VERSION.SDK_INT));
                    sb.append("\r\n");

                    // Get device info
                    sb.append(String.format(locale, "Brand: %s\r\n", Build.BRAND));
                    sb.append(String.format(locale, "Manufacturer: %s\r\n", Build.MANUFACTURER));
                    sb.append(String.format(locale, "Model: %s\r\n", Build.MODEL));
                    sb.append(String.format(locale, "Product: %s\r\n", Build.PRODUCT));
                    sb.append(String.format(locale, "Device: %s\r\n", Build.DEVICE));
                    sb.append(String.format(locale, "Host: %s\r\n", Build.HOST));
                    sb.append(String.format(locale, "Display: %s\r\n", Build.DISPLAY));
                    sb.append(String.format(locale, "Id: %s\r\n", Build.ID));
                    sb.append("\r\n");

                    BufferedReader in = null;
                    try {
                        String line;
                        in = new BufferedReader(new FileReader(file));
                        while ((line = in.readLine()) != null) {
                            sb.append(line).append("\r\n");
                        }
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                    }

                    file.delete();

                    String body = "<pre>" + sb.toString().replaceAll("\\r?\\n", "<br />") + "</pre>";

                    EntityMessage draft = null;

                    DB db = DB.getInstance(context);
                    try {
                        db.beginTransaction();

                        EntityFolder drafts = db.folder().getPrimaryDrafts();
                        if (drafts != null) {
                            draft = new EntityMessage();
                            draft.account = drafts.account;
                            draft.folder = drafts.id;
                            draft.msgid = EntityMessage.generateMessageId();
                            draft.to = new Address[] {Helper.myAddress()};
                            draft.subject =
                                context.getString(R.string.app_name)
                                    + " "
                                    + BuildConfig.VERSION_NAME
                                    + " crash log";
                            draft.content = true;
                            draft.received = new Date().getTime();
                            draft.seen = false;
                            draft.ui_seen = false;
                            draft.flagged = false;
                            draft.ui_flagged = false;
                            draft.ui_hide = false;
                            draft.ui_found = false;
                            draft.ui_ignored = false;
                            draft.id = db.message().insertMessage(draft);
                            draft.write(context, body);
                        }

                        EntityOperation.queue(db, draft, EntityOperation.ADD);

                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }

                    EntityOperation.process(context);

                    return (draft == null ? null : draft.id);
                }

                return null;
            }

            @Override
            protected void onLoaded(Bundle args, Long id) {
                if (id != null) {
                    startActivity(
                        new Intent(ActivityView.this, ActivityCompose.class)
                            .putExtra("action", "edit")
                            .putExtra("id", id));
                }
            }
        }.load(this, new Bundle());
    }

    private class UpdateInfo {
        String tag_name; // version
        String html_url;
    }

    private void checkUpdate() {
        final long now = new Date().getTime();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getLong("last_update_check", 0) + UPDATE_INTERVAL > now) {
            return;
        }

        new SimpleTask<UpdateInfo>() {
            @Override
            protected UpdateInfo onLoad(Context context, Bundle args) throws Throwable {
                StringBuilder json = new StringBuilder();
                HttpsURLConnection urlConnection = null;
                try {
                    URL latest = new URL(UPDATE_LATEST_API);
                    urlConnection = (HttpsURLConnection) latest.openConnection();
                    BufferedReader br =
                        new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                    String line;
                    while ((line = br.readLine()) != null) {
                        json.append(line);
                    }

                    JSONObject jroot = new JSONObject(json.toString());
                    if (jroot.has("tag_name") && jroot.has("html_url") && jroot.has("assets")) {
                        prefs.edit().putLong("last_update_check", now).apply();

                        UpdateInfo info = new UpdateInfo();
                        info.tag_name = jroot.getString("tag_name");
                        info.html_url = jroot.getString("html_url");
                        if (TextUtils.isEmpty(info.html_url)) {
                            return null;
                        }

                        JSONArray jassets = jroot.getJSONArray("assets");
                        for (int i = 0; i < jassets.length(); i++) {
                            JSONObject jasset = jassets.getJSONObject(i);
                            if (jasset.has("name")) {
                                String name = jasset.getString("name");
                                if (name != null && name.endsWith(".apk")) {
                                    if (TextUtils.isEmpty(info.tag_name)) {
                                        info.tag_name = name;
                                    }

                                    Log.i(Helper.TAG, "Latest version=" + info.tag_name);
                                    if (BuildConfig.VERSION_NAME.equals(info.tag_name)) {
                                        break;
                                    } else {
                                        return info;
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }

                return null;
            }

            @Override
            protected void onLoaded(Bundle args, UpdateInfo info) {
                if (info == null) {
                    return;
                }

                final Intent update = new Intent(Intent.ACTION_VIEW, Uri.parse(info.html_url));
                if (update.resolveActivity(getPackageManager()) != null) {
                    new DialogBuilderLifecycle(ActivityView.this, ActivityView.this)
                        .setMessage(getString(R.string.title_updated, info.tag_name))
                        .setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Helper.view(ActivityView.this, update);
                                }
                            })
                        .show();
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (BuildConfig.DEBUG) {
                    Helper.unexpectedError(ActivityView.this, ex);
                }
            }
        }.load(this, new Bundle());
    }

    private Intent getIntentFAQ() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(
            Uri.parse(
                "https://framagit.org/dystopia-project/simple-email/blob/HEAD/docs/FAQ.md"));
        return intent;
    }

    /**
     * Get Intent for invite to use SimpleEmail
     *
     * @return Intent with share/send action
     */
    private Intent getIntentInvite() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.title_try_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.title_try));
        shareIntent.setType("text/plain");

        return Intent.createChooser(shareIntent, getString(R.string.title_try_text));
    }

    private void onMenuFolders(long account) {
        getSupportFragmentManager().popBackStack("unified", 0);

        Bundle args = new Bundle();
        args.putLong("account", account);

        FragmentFolders fragment = new FragmentFolders();
        fragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("folders");
        fragmentTransaction.commit();
    }

    private void onMenuSetup() {
        startActivity(new Intent(ActivityView.this, ActivitySetup.class));
    }

    private void onMenuAnswers() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction
            .replace(R.id.content_frame, new FragmentAnswers())
            .addToBackStack("answers");
        fragmentTransaction.commit();
    }

    private void onMenuOperations() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction
            .replace(R.id.content_frame, new FragmentOperations())
            .addToBackStack("operations");
        fragmentTransaction.commit();
    }

    private void onMenuLegend() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new FragmentLegend()).addToBackStack("legend");
        fragmentTransaction.commit();
    }

    private void onMenuFAQ() {
        Helper.view(this, getIntentFAQ());
    }

    private void onMenuPrivacy() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction
            .replace(R.id.content_frame, new FragmentPrivacy())
            .addToBackStack("privacy");
        fragmentTransaction.commit();
    }

    private void onMenuAbout() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new FragmentAbout()).addToBackStack("about");
        fragmentTransaction.commit();
    }

    private void onMenuInvite() {
        startActivity(getIntentInvite());
    }

    private class DrawerItem {
        private int layout;
        private int id;
        private int icon;
        private String title;
        private Object data;

        DrawerItem(int layout) {
            this.layout = layout;
        }

        DrawerItem(Context context, int layout, int icon, int title) {
            this.layout = layout;
            this.id = title;
            this.icon = icon;
            this.title = context.getString(title);
        }

        DrawerItem(int layout, int id, int icon, String title, Object data) {
            this.layout = layout;
            this.id = id;
            this.icon = icon;
            this.title = title;
            this.data = data;
        }

        public int getId() {
            return this.id;
        }

        public Object getData() {
            return this.data;
        }
    }

    private static class ArrayAdapterDrawer extends ArrayAdapter<DrawerItem> {
        ArrayAdapterDrawer(@NonNull Context context) {
            super(context, -1);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            DrawerItem item = getItem(position);
            View row = LayoutInflater.from(getContext()).inflate(item.layout, null);

            ImageView iv = row.findViewById(R.id.ivItem);
            TextView tv = row.findViewById(R.id.tvItem);

            if (iv != null) {
                iv.setImageResource(item.icon);
            }
            if (tv != null) {
                tv.setText(item.title);
            }

            return row;
        }
    }

    BroadcastReceiver receiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_VIEW_MESSAGES.equals(intent.getAction())) {
                    onViewMessages(intent);
                } else if (ACTION_VIEW_THREAD.equals(intent.getAction())) {
                    onViewThread(intent);
                } else if (ACTION_VIEW_FULL.equals(intent.getAction())) {
                    onViewFull(intent);
                } else if (ACTION_EDIT_FOLDER.equals(intent.getAction())) {
                    onEditFolder(intent);
                } else if (ACTION_EDIT_ANSWER.equals(intent.getAction())) {
                    onEditAnswer(intent);
                } else if (ACTION_STORE_ATTACHMENT.equals(intent.getAction())) {
                    onStoreAttachment(intent);
                } else if (ACTION_DECRYPT.equals(intent.getAction())) {
                    onDecrypt(intent);
                }
            }
        };

    private void onViewMessages(Intent intent) {
        Bundle args = new Bundle();
        args.putLong("account", intent.getLongExtra("account", -1));
        args.putLong("folder", intent.getLongExtra("folder", -1));
        args.putString("folderType", intent.getStringExtra("folderType"));

        FragmentMessages fragment = new FragmentMessages();
        fragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("messages");
        fragmentTransaction.commit();
    }

    private void onViewThread(Intent intent) {
        getSupportFragmentManager().popBackStack("thread", FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Bundle args = new Bundle();
        args.putLong("account", intent.getLongExtra("account", -1));
        args.putLong("folder", intent.getLongExtra("folder", -1));
        args.putString("thread", intent.getStringExtra("thread"));

        FragmentMessages fragment = new FragmentMessages();
        fragment.setArguments(args);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("thread");
        fragmentTransaction.commit();
    }

    private void onViewFull(Intent intent) {
        FragmentWebView fragment = new FragmentWebView();
        fragment.setArguments(intent.getExtras());

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("webview");
        fragmentTransaction.commit();
    }

    private void onEditFolder(Intent intent) {
        FragmentFolder fragment = new FragmentFolder();
        fragment.setArguments(intent.getExtras());
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("folder");
        fragmentTransaction.commit();
    }

    private void onEditAnswer(Intent intent) {
        FragmentAnswer fragment = new FragmentAnswer();
        fragment.setArguments(intent.getExtras());
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment).addToBackStack("answer");
        fragmentTransaction.commit();
    }

    private void onStoreAttachment(Intent intent) {
        attachment = intent.getLongExtra("id", -1);
        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        create.addCategory(Intent.CATEGORY_OPENABLE);
        create.setType(intent.getStringExtra("type"));
        create.putExtra(Intent.EXTRA_TITLE, intent.getStringExtra("name"));

        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(create, 0);

        if (activities.isEmpty()) {
            Snackbar.make(view, R.string.title_no_storage_framework, Snackbar.LENGTH_LONG).show();
        }

        try {
            startActivityForResult(create, REQUEST_ATTACHMENT);
        } catch (ActivityNotFoundException _err) {
            Snackbar.make(view, R.string.title_no_storage_framework, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onDecrypt(Intent intent) {
        if (pgpService.isBound()) {
            Intent data = new Intent();
            data.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);
            data.putExtra(OpenPgpApi.EXTRA_USER_IDS, new String[] {intent.getStringExtra("to")});
            data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

            decrypt(data, intent.getLongExtra("id", -1));
        } else {
            Snackbar snackbar = Snackbar.make(view, R.string.title_no_openpgp, Snackbar.LENGTH_LONG);
            if (Helper.getIntentOpenKeychain().resolveActivity(getPackageManager()) != null) {
                snackbar.setAction(
                    R.string.title_fix,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(Helper.getIntentOpenKeychain());
                        }
                    });
            }
            snackbar.show();
        }
    }

    private void decrypt(Intent data, long id) {
        Bundle args = new Bundle();
        args.putLong("id", id);
        args.putParcelable("data", data);

        new SimpleTask<PendingIntent>() {
            @Override
            protected PendingIntent onLoad(Context context, Bundle args) throws Throwable {
                // Get arguments
                long id = args.getLong("id");
                Intent data = args.getParcelable("data");

                DB db = DB.getInstance(context);

                // Find encrypted data
                List<EntityAttachment> attachments = db.attachment().getAttachments(id);
                for (EntityAttachment attachment : attachments) {
                    if (attachment.available && "encrypted.asc".equals(attachment.name)) {
                        // Serialize encrypted data
                        FileInputStream encrypted =
                            new FileInputStream(EntityAttachment.getFile(context, attachment.id));
                        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();

                        // Decrypt message
                        OpenPgpApi api = new OpenPgpApi(context, pgpService.getService());
                        Intent result = api.executeApi(data, encrypted, decrypted);
                        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                            case OpenPgpApi.RESULT_CODE_SUCCESS:
                                // Decode message
                                Properties props =
                                    MessageHelper.getSessionProperties(Helper.AUTH_TYPE_PASSWORD, false);
                                Session isession = Session.getInstance(props, null);
                                ByteArrayInputStream is = new ByteArrayInputStream(decrypted.toByteArray());
                                MimeMessage imessage = new MimeMessage(isession, is);
                                MessageHelper helper = new MessageHelper(imessage);

                                try {
                                    db.beginTransaction();

                                    // Write decrypted body
                                    EntityMessage m = db.message().getMessage(id);
                                    m.write(context, helper.getHtml());

                                    // Remove previously decrypted attachments
                                    for (EntityAttachment a : attachments) {
                                        if (!"encrypted.asc".equals(a.name)) {
                                            db.attachment().deleteAttachment(a.id);
                                        }
                                    }

                                    // Add decrypted attachments
                                    int sequence = db.attachment().getAttachmentSequence(id);
                                    for (EntityAttachment a : helper.getAttachments()) {
                                        a.message = id;
                                        a.sequence = ++sequence;
                                        a.id = db.attachment().insertAttachment(a);
                                    }

                                    db.message().setMessageStored(id, new Date().getTime());

                                    db.setTransactionSuccessful();
                                } finally {
                                    db.endTransaction();
                                }

                                break;

                            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                                message = id;
                                return result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                            case OpenPgpApi.RESULT_CODE_ERROR:
                                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                                throw new IllegalArgumentException(error.getMessage());
                        }

                        break;
                    }
                }

                return null;
            }

            @Override
            protected void onLoaded(Bundle args, PendingIntent pi) {
                if (pi != null) {
                    try {
                        startIntentSenderForResult(
                            pi.getIntentSender(), ActivityView.REQUEST_DECRYPT, null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException ex) {
                        Helper.unexpectedError(ActivityView.this, ex);
                    }
                }
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                if (ex instanceof IllegalArgumentException) {
                    Snackbar.make(view, ex.getMessage(), Snackbar.LENGTH_LONG).show();
                } else {
                    Helper.unexpectedError(ActivityView.this, ex);
                }
            }
        }.load(ActivityView.this, args);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(
            Helper.TAG,
            "View onActivityResult request=" + requestCode + " result=" + resultCode + " data=" + data);
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_ATTACHMENT) {
                if (data != null) {
                    Bundle args = new Bundle();
                    args.putLong("id", attachment);
                    args.putParcelable("uri", data.getData());

                    new SimpleTask<Void>() {
                        @Override
                        protected Void onLoad(Context context, Bundle args) throws Throwable {
                            long id = args.getLong("id");
                            Uri uri = args.getParcelable("uri");

                            File file = EntityAttachment.getFile(context, id);

                            ParcelFileDescriptor pfd = null;
                            FileOutputStream fos = null;
                            FileInputStream fis = null;
                            try {
                                pfd = context.getContentResolver().openFileDescriptor(uri, "w");
                                fos = new FileOutputStream(pfd.getFileDescriptor());
                                fis = new FileInputStream(file);

                                byte[] buffer = new byte[ATTACHMENT_BUFFER_SIZE];
                                int read;
                                while ((read = fis.read(buffer)) != -1) {
                                    fos.write(buffer, 0, read);
                                }
                            } catch (FileNotFoundException ex) {
                                Log.w(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                            } finally {
                                try {
                                    if (pfd != null) {
                                        pfd.close();
                                    }
                                } catch (Throwable ex) {
                                    Log.w(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                                }
                                try {
                                    if (fos != null) {
                                        fos.close();
                                    }
                                } catch (Throwable ex) {
                                    Log.w(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                                }
                                try {
                                    if (fis != null) {
                                        fis.close();
                                    }
                                } catch (Throwable ex) {
                                    Log.w(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                                }
                            }

                            return null;
                        }

                        @Override
                        protected void onLoaded(Bundle args, Void data) {
                            Toast.makeText(ActivityView.this, R.string.title_attachment_saved, Toast.LENGTH_LONG)
                                .show();
                        }

                        @Override
                        protected void onException(Bundle args, Throwable ex) {
                            Log.e(Helper.TAG, ex + "\n" + Log.getStackTraceString(ex));
                            Helper.unexpectedError(ActivityView.this, ex);
                        }
                    }.load(this, args);
                }
            } else if (requestCode == REQUEST_DECRYPT) {
                if (data != null) {
                    decrypt(data, message);
                }
            }
        }
    }
}
