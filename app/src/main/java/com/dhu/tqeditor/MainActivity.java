package com.dhu.tqeditor;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dhu.tqeditor.model.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final String TAG = "TQEditor";
    private ArzFile arzFile;
    private ArcFile arcFile;
    private Toolbar toolbar;
    private RecyclerView listRecords;
    private RecyclerView listVariables;
    private RecyclerView listStrings;
    private RecyclerView listTexts;
    private RecordAdapter recordAdapter;
    private StringAdapter stringAdapter;
    private VariableAdapter variableAdapter;
    private TextAdapter textAdapter;
    private RecordInfo currentRecord;
    private AutoCompleteTextView editSearch;
    private static final int MODE_RECORDS = 0;
    private static final int MODE_VARIABLES = 1;
    private static final int MODE_STRINGS = 2;
    private static final int MODE_TEXTS = 3;
    private int mode = MODE_RECORDS;
    private BottomNavigationView navigationView;
    private LinkedList<RecordInfo> recordsHistory = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(this);
        // Check whether this app has write external storage permission or not.
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // If do not grant write external storage permission.
        if (writeExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            // Request user to grant write external storage permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        } else {
            readArzFile();
            readArcFile();
        }
        editSearch = findViewById(R.id.edit_search);
        editSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_ENTER) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[] {"(equip|loot)", "character", "defensive", "offensive", "retaliation"});
        editSearch.setAdapter(adapter);
        editSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !editSearch.isPopupShowing()) {
                    editSearch.showDropDown();
                }
            }
        });
        editSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doSearch();
            }
        });
        findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });
        TextInputLayout searchInput = findViewById(R.id.search_input);
        searchInput.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editSearch.setText(null);
                doSearch();
            }
        });
        listRecords = findViewById(R.id.list_records);
        listRecords.setHasFixedSize(true);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this);
        listRecords.setLayoutManager(layoutManager1);
        DividerItemDecoration decoration1 = new DividerItemDecoration(listRecords.getContext(), layoutManager1.getOrientation());
        listRecords.addItemDecoration(decoration1);
        recordAdapter = new RecordAdapter(this);
        listRecords.setAdapter(recordAdapter);

        listVariables = findViewById(R.id.list_variables);
        listVariables.setHasFixedSize(true);
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this);
        listVariables.setLayoutManager(layoutManager2);
        DividerItemDecoration decoration2 = new DividerItemDecoration(listVariables.getContext(), layoutManager2.getOrientation());
        listVariables.addItemDecoration(decoration2);
        variableAdapter = new VariableAdapter(this);
        variableAdapter.setHideEmptyVariable(true);
        listVariables.setAdapter(variableAdapter);
        enableSwipe();

        listStrings = findViewById(R.id.list_strings);
        listStrings.setHasFixedSize(true);
        LinearLayoutManager layoutManager3 = new LinearLayoutManager(this);
        listStrings.setLayoutManager(layoutManager3);
        DividerItemDecoration decoration3 = new DividerItemDecoration(listStrings.getContext(), layoutManager3.getOrientation());
        listStrings.addItemDecoration(decoration3);
        stringAdapter = new StringAdapter(this);
        listStrings.setAdapter(stringAdapter);

        listTexts = findViewById(R.id.list_texts);
        listTexts.setHasFixedSize(true);
        LinearLayoutManager layoutManager4 = new LinearLayoutManager(this);
        listTexts.setLayoutManager(layoutManager4);
        DividerItemDecoration decoration4 = new DividerItemDecoration(listTexts.getContext(), layoutManager3.getOrientation());
        listTexts.addItemDecoration(decoration4);
        textAdapter = new TextAdapter(this);
        listTexts.setAdapter(textAdapter);
        navigationView.setSelectedItemId(R.id.records);
    }

    private void doSearch() {
        editSearch.clearFocus();
        if (mode == MODE_RECORDS) {
            recordAdapter.setSearchText(editSearch.getText().toString());
            listRecords.requestFocus();
        } else if (mode == MODE_VARIABLES) {
            variableAdapter.setSearchText(editSearch.getText().toString());
            listVariables.requestFocus();
        } else if (mode == MODE_STRINGS) {
            stringAdapter.setSearchText(editSearch.getText().toString());
            listStrings.requestFocus();
        } else if (mode == MODE_TEXTS) {
            textAdapter.setSearchText(editSearch.getText().toString());
            listTexts.requestFocus();
        }
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
        }
    }

    private void enableSwipe(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                if (mode != MODE_VARIABLES) {
                    return;
                }
                final int position = viewHolder.getAdapterPosition();

                final Variable deletedVariable = variableAdapter.getData(position);
                variableAdapter.removeItem(position);
                // showing snack bar with Undo option
                Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), deletedVariable.getName() + " removed.", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // undo is selected, restore the deleted item
                        variableAdapter.restoreItem(deletedVariable, position);
                    }
                });
                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }

        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listVariables);
    }

    private static class LoadArzFileTask extends AsyncTask<Void, Void, ArzFile> {

        private WeakReference<MainActivity> activity;
        private WeakReference<ProgressDialog> progressDialogRef;
        private LoadArzFileTask(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
            ProgressDialog progressDialog = new ProgressDialog(activity);
            this.progressDialogRef = new WeakReference<ProgressDialog>(progressDialog);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressDialog progressDialog = progressDialogRef.get();
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("Loading records...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected ArzFile doInBackground(Void... voids) {
            try {
                File storage = Environment.getExternalStorageDirectory();
                File file = new File(storage, "Android/data/com.dotemu.titanquest/files/Database/database.arz");
                return ArzFile.load(file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArzFile arzFile) {
            if (progressDialogRef.get() != null && progressDialogRef.get().isShowing()) {
                progressDialogRef.get().dismiss();
            }
            MainActivity mainActivity = activity.get();
            if (mainActivity == null || mainActivity.isFinishing() || mainActivity.isDestroyed()) {
                return;
            }
            if (arzFile == null) {
                Snackbar.make(mainActivity.listRecords, "Load database.arz failed.", Snackbar.LENGTH_LONG).show();
            } else {
                mainActivity.arzFile = arzFile;
                mainActivity.recordAdapter.setArzFile(arzFile);
                mainActivity.stringAdapter.setArzFile(arzFile);
                if (mainActivity.arcFile != null) {
                    new LoadRecordsTask(mainActivity).execute();
                }
            }
        }
    }

    private void readArzFile() {
        new LoadArzFileTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class LoadArcFileTask extends AsyncTask<Void, Void, ArcFile> {

        private WeakReference<MainActivity> activity;
        private LoadArcFileTask(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected ArcFile doInBackground(Void... voids) {
            try {
                File storage = Environment.getExternalStorageDirectory();
                File file = new File(storage, "Android/data/com.dotemu.titanquest/files/Text/Text_CH.arc");
                return ArcFile.load(file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArcFile arcFile) {
            MainActivity mainActivity = activity.get();
            if (mainActivity == null || mainActivity.isFinishing() || mainActivity.isDestroyed()) {
                return;
            }
            if (arcFile == null) {
                Snackbar.make(mainActivity.listRecords, "Load Text_CH.arc failed.", Snackbar.LENGTH_LONG).show();
            } else {
                mainActivity.arcFile = arcFile;
                mainActivity.textAdapter.setArcFile(arcFile);
                if (mainActivity.arzFile != null) {
                    new LoadRecordsTask(mainActivity).execute();
                }
            }
        }

    }

    private void readArcFile() {
        new LoadArcFileTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class LoadRecordsTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<MainActivity> activity;
        private ArzFile arzFile;
        private ArcFile arcFile;
        private LoadRecordsTask(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
            this.arzFile = activity.arzFile;
            this.arcFile = activity.arcFile;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (RecordInfo recordInfo : arzFile.getRecordInfoList()) {
                try {
                    recordInfo.loadRecord(arzFile, arcFile);
                } catch (IOException e) {
                    Log.w("TQEditor", e.toString());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MainActivity mainActivity = activity.get();
            if (mainActivity == null || mainActivity.isFinishing() || mainActivity.isDestroyed()) {
                return;
            }
            Snackbar.make(mainActivity.listRecords, "All records loaded.", Snackbar.LENGTH_LONG).show();
            mainActivity.recordAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            int grantResultsLength = grantResults.length;
            if (grantResultsLength > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                readArzFile();
                readArcFile();
            } else {
                Snackbar.make(listRecords, "You denied write external storage permission.", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mode == MODE_VARIABLES) {
            int index = recordsHistory.indexOf(currentRecord);
            if (index < recordsHistory.size() - 1) {
                showRecord(recordsHistory.get(index + 1), false);
            } else {
                navigationView.setSelectedItemId(R.id.records);
            }
        }
        //super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            if (currentRecord != null) {
                try {
                    currentRecord.saveRecord(variableAdapter.getRecord(), arzFile);
                    Snackbar.make(listRecords, "Saved.", Snackbar.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Snackbar.make(listRecords, e.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.action_exit) {
            finish();
            return true;
        } else if (id == R.id.action_record_history) {
            showRecords(recordsHistory, false);
            return true;
        } else if (id == R.id.hide_empty_variable) {
            item.setChecked(!item.isChecked());
            variableAdapter.setHideEmptyVariable(item.isChecked());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showRecords(List<RecordInfo> recordInfos, boolean updateHistory) {
        CharSequence[] recordIds = new CharSequence[recordInfos.size()];
        for (int i = 0; i < recordInfos.size(); i++) {
            CharSequence name = recordInfos.get(i).getName();
            if (name == null) {
                recordIds[i] = recordInfos.get(i).getNormalizedId();
            } else {
                SpannableStringBuilder builder = new SpannableStringBuilder(name);
                builder.append("\n").append(recordInfos.get(i).getNormalizedId());
                recordIds[i] = builder;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.records)
                .setItems(recordIds, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showRecord(recordInfos.get(which), updateHistory);
                    }
                });
        builder.create().show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        showTab(id);
        return true;
    }

    private void showTab(int id) {
        if (id == R.id.records) {
            listRecords.setVisibility(View.VISIBLE);
            listVariables.setVisibility(View.INVISIBLE);
            listStrings.setVisibility(View.INVISIBLE);
            listTexts.setVisibility(View.INVISIBLE);
            editSearch.setText(recordAdapter.getSearchText());
            toolbar.setTitle(R.string.records);
            toolbar.setSubtitle(null);
            mode = MODE_RECORDS;
        } else if (id == R.id.variables) {
            listRecords.setVisibility(View.INVISIBLE);
            listVariables.setVisibility(View.VISIBLE);
            listStrings.setVisibility(View.INVISIBLE);
            listTexts.setVisibility(View.INVISIBLE);
            editSearch.setText(variableAdapter.getSearchText());
            if (currentRecord != null) {
                toolbar.setTitle(variableAdapter.getRecord().getName(arcFile));
                toolbar.setSubtitle(currentRecord.getNormalizedId());
            } else {
                toolbar.setTitle(R.string.variables);
                toolbar.setSubtitle(null);
            }
            mode = MODE_VARIABLES;
        } else if (id == R.id.strings) {
            listRecords.setVisibility(View.INVISIBLE);
            listVariables.setVisibility(View.INVISIBLE);
            listStrings.setVisibility(View.VISIBLE);
            listTexts.setVisibility(View.INVISIBLE);
            editSearch.setText(stringAdapter.getSearchText());
            toolbar.setTitle(R.string.strings);
            toolbar.setSubtitle(null);
            mode = MODE_STRINGS;
        } else if (id == R.id.texts) {
            listRecords.setVisibility(View.INVISIBLE);
            listVariables.setVisibility(View.INVISIBLE);
            listStrings.setVisibility(View.INVISIBLE);
            listTexts.setVisibility(View.VISIBLE);
            editSearch.setText(textAdapter.getSearchText());
            toolbar.setTitle(R.string.texts);
            toolbar.setSubtitle(null);
            mode = MODE_TEXTS;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        if (mode == MODE_VARIABLES) {
            showVariableEditDialog(position);
        } else if (mode == MODE_RECORDS) {
            showRecord(recordAdapter.getData(position), true);
        } else if (mode == MODE_STRINGS) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            int stringId = stringAdapter.getStringId(position);
            String string = arzFile.getString(stringId);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, string + "|" + stringId));
        } else if (mode == MODE_TEXTS) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String textId = textAdapter.getTextId(position);
            String string = arcFile.getString(textId).toString();
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, string + "|" + textId));
        }
    }

    private void showVariableEditDialog(final int position) {
        final Variable variable = variableAdapter.getData(position);
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.variable_input, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editName = promptView.findViewById(R.id.edit_name);
        final EditText editValue = promptView.findViewById(R.id.edit_value);
        final Spinner dataType = promptView.findViewById(R.id.data_type);
        promptView.findViewById(R.id.btn_0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editValue.setText("0");
            }
        });
        promptView.findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editValue.setText("1");
            }
        });
        promptView.findViewById(R.id.btn_100).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editValue.setText("100");
            }
        });
        editName.setText(variable.getName());
        editValue.setText(variable.getValueString().replace(",", ",\n"));
        if (variable.getDataType() != DataType.StringVar) {
            editValue.setSelectAllOnFocus(true);
            promptView.findViewById(R.id.number_input_layout).setVisibility(View.VISIBLE);
        } else {
            promptView.findViewById(R.id.number_input_layout).setVisibility(View.GONE);
        }
        String[] names = new String[DataType.values().length];
        for (int i = 0; i < names.length; i++) {
            names[i] = DataType.values()[i].name();
        }
        ArrayAdapter<String>adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataType.setAdapter(adapter);
        dataType.setSelection(variable.getDataType().ordinal());
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newName = editName.getText().toString();
                        if (!newName.equals(variable.getName())) {
                            int stringId = arzFile.getStringId(newName);
                            if (stringId != -1) {
                                variable.setId(stringId, newName);
                            } else {
                                Toast.makeText(MainActivity.this, newName + " not found in strings map", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                        String oldValue = variable.getValueString();
                        DataType oldDataType = variable.getDataType();
                        String newValue = editValue.getText().toString().trim().replace(",\n", ",");
                        DataType newDataType = DataType.values()[dataType.getSelectedItemPosition()];
                        try {
                            variable.setDataType(newDataType);
                            variable.setValues(newValue);
                            variableAdapter.notifyItemChanged(position);
                        } catch (NumberFormatException e) {
                            variable.setDataType(oldDataType);
                            variable.setValues(oldValue);
                            Toast.makeText(MainActivity.this, newValue + " parse failed", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        List<RecordInfo> recordInfos = variable.getRecordInfos(arzFile);
        if (recordInfos.size() > 0) {
            alertDialogBuilder.setNeutralButton("View", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (recordInfos.size() == 1) {
                        showRecord(recordInfos.get(0), true);
                    } else {
                        showRecords(recordInfos, true);
                    }
                }
            });
        }

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void showRecord(RecordInfo recordInfo, boolean updateHistory) {
        try {
            currentRecord = recordInfo;
            variableAdapter.setRecord(currentRecord.loadRecord(arzFile, arcFile));
            navigationView.setSelectedItemId(R.id.variables);
            if (updateHistory) {
                recordsHistory.remove(recordInfo);
                recordsHistory.addFirst(recordInfo);
            }
        } catch (IOException e) {
            Snackbar.make(listVariables, e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }
}
