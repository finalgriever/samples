package com.example.jamiebutler.notekeeper;

import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.example.jamiebutler.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.example.jamiebutler.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.example.jamiebutler.notekeeper.NoteKeeperProviderContract.Courses;
import com.example.jamiebutler.notekeeper.NoteKeeperProviderContract.Notes;

public class NoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final int LOADER_NOTES = 0;
    public static final int LOADER_COURSES = 1;
    private final String TAG = getClass().getSimpleName();
    public static final String ORIGINAL_NOTE_COURSE_ID = "com.example.jamiebutler.notekeeper.ORIGINAL_NOTE_COURSE_ID";
    public static final String ORIGINAL_NOTE_TITLE = "com.example.jamiebutler.notekeeper.ORIGINAL_NOTE_TITLE";
    public static final String ORIGINAL_NOTE_BODY = "com.example.jamiebutler.notekeeper.ORIGINAL_NOTE_BODY";
    public static final String NOTE_URI = "com.example.jamiebutler.notekeeper.NOTE_URI";
    public static final String NOTE_ID = "com.example.jamiebutler.notekeeper.NOTE_ID";
    public static final int NOTE_ID_NOT_SET = -1;

    private NoteInfo mNote;
    private boolean mIsNewNote;
    private EditText mTextNoteTitle;
    private EditText mTextNoteBody;
    private Spinner mCoursesSpinner;
    private int mNoteId;
    private boolean mCancelling;
    private String mOriginalNoteCourseId;
    private String mOriginalNoteTitle;
    private String mOriginalNoteBody;
    private NoteKeeperOpenHelper mDbOpenHelper;
    private Cursor mNoteCursor;
    private int mCourseIdPos;
    private int mNoteTitlePos;
    private int mNoteTextPos;
    private SimpleCursorAdapter mCoursesAdapter;
    private boolean mCoursesQueryFinished;
    private boolean mNotesQueryFinished;
    private Uri mNoteUri;
    private boolean mFirstLoad;
    private ModuleStatusView mViewModuleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNote = new NoteInfo(null, null, null);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        mDbOpenHelper = new NoteKeeperOpenHelper(this);
        setSupportActionBar(toolbar);
        mCoursesSpinner = findViewById(R.id.spinner_courses);

        mCoursesAdapter = new SimpleCursorAdapter(
                this, android.R.layout.simple_spinner_item,
                null,
                new String[] {CourseInfoEntry.COLUMN_COURSE_TITLE},
                new int[] {android.R.id.text1},
                0);
        mCoursesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mCoursesSpinner.setAdapter(mCoursesAdapter);
        getLoaderManager().initLoader(LOADER_COURSES, null, this);

        readDisplayStateValues();

        mTextNoteTitle = findViewById(R.id.text_note_title);
        mTextNoteBody = findViewById(R.id.text_note_body);

        if(!mIsNewNote)
            getLoaderManager().initLoader(LOADER_NOTES, null, this);

        if(savedInstanceState == null) {
            mFirstLoad = true;
        } else {
            restoreOriginalNoteValues(savedInstanceState);
            mNoteUri = Uri.parse(savedInstanceState.getString(NOTE_URI));
        }

        mViewModuleStatus = findViewById(R.id.module_status);
        loadModuleStatusValues();

        Log.d(TAG, "onCreate");
    }

    private void loadModuleStatusValues() {
        int totalNumberOfModules = 11;
        int completedNumberOfModules = 7;
        boolean[] moduleStatus = new boolean[totalNumberOfModules];
        for(int moduleIndex = 0; moduleIndex < completedNumberOfModules; moduleIndex++) {
            moduleStatus[moduleIndex] = true;
        }
        mViewModuleStatus.setModuleStatus(moduleStatus);
    }

    private void loadCourseData() {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        String[] courseColumns = { CourseInfoEntry._ID, CourseInfoEntry.COLUMN_COURSE_TITLE, CourseInfoEntry.COLUMN_COURSE_ID };
        Cursor cursor = db.query(CourseInfoEntry.TABLE_NAME, courseColumns, null, null, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);
        mCoursesAdapter.changeCursor(cursor);
    }

    private void restoreOriginalNoteValues(Bundle savedInstanceState) {
        mOriginalNoteCourseId = savedInstanceState.getString(ORIGINAL_NOTE_COURSE_ID);
        mOriginalNoteTitle = savedInstanceState.getString(ORIGINAL_NOTE_TITLE);
        mOriginalNoteBody = savedInstanceState.getString(ORIGINAL_NOTE_BODY);
    }

    private void saveOriginalNoteValues() {
        if(mIsNewNote)
            return;

        mOriginalNoteCourseId = mNote.getCourse().getCourseId();
        mOriginalNoteTitle = mNote.getTitle();
        mOriginalNoteBody = mNote.getText();
    }

    private void displayNote() {
        String courseId = mNoteCursor.getString(mCourseIdPos);
        String noteTitle = mNoteCursor.getString(mNoteTitlePos);
        String noteText = mNoteCursor.getString(mNoteTextPos);

        int courseIndex = getIndexOfCourseId(courseId);
        CourseInfo course = DataManager.getInstance().getCourse(courseId);

        mCoursesSpinner.setSelection(courseIndex);
        mTextNoteTitle.setText(noteTitle);
        mTextNoteBody.setText(noteText);
        mNote.setCourse(course);
        mNote.setText(noteText);
        mNote.setTitle(noteTitle);
        if(mFirstLoad) {
            saveOriginalNoteValues();
        }

        CourseEventBroadcastHelper.sendEventBroadcast(this, courseId, "Editing note");
    }

    private int getIndexOfCourseId(String courseId) {
        Cursor cursor = mCoursesAdapter.getCursor();
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        int courseRowIndex = 0;

        boolean more = cursor.moveToFirst();
        while(more) {
            String cursorCourseId = cursor.getString(courseIdPos);
            if(courseId.equals(cursorCourseId))
                    break;
            courseRowIndex++;
            more = cursor.moveToNext();
        }

        return courseRowIndex;
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        mNoteId = intent.getIntExtra(NOTE_ID, NOTE_ID_NOT_SET);
        mIsNewNote = mNoteId == NOTE_ID_NOT_SET;
        if(mIsNewNote) {
            createNewNote();
        }
        Log.i(TAG, "mNoteId: " + mNoteId);
    }

    private void createNewNote() {
        AsyncTask<ContentValues, Integer, Uri> task = new AsyncTask<ContentValues, Integer, Uri>() {
            private ProgressBar mProgressBar;

            @Override
            protected void onPreExecute() {
                mProgressBar = findViewById(R.id.progress_bar);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(1);
            }

            @Override
            protected Uri doInBackground(ContentValues... contentValues) {
                ContentValues insertValues = contentValues[0];
                ContentResolver resolver = getContentResolver();
                simulateLongRunningWork();
                publishProgress(2);
                simulateLongRunningWork();
                publishProgress(3);
                return resolver.insert(Notes.CONTENT_URI, insertValues);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                int progress = values[0];
                mProgressBar.setProgress(progress);
            }

            private void simulateLongRunningWork() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) { }
            }

            @Override
            protected void onPostExecute(Uri uri) {
                super.onPostExecute(uri);
                mNoteUri = uri;
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        };

        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");

        task.execute(values);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ORIGINAL_NOTE_COURSE_ID, mOriginalNoteCourseId);
        outState.putString(ORIGINAL_NOTE_TITLE, mOriginalNoteTitle);
        outState.putString(ORIGINAL_NOTE_BODY, mOriginalNoteBody);
        outState.putString(NOTE_URI, mNoteUri.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCancelling) {
            Log.i(TAG, "Cancelling note at position: " + mNoteId);
            if(mIsNewNote) {
                deleteNewNoteFromDatabase();
            } else {
                // storePreviousNoteValues();
            }
        } else {
            saveNote();
        }
        Log.d(TAG, "onPause");
    }

    private void deleteNewNoteFromDatabase() {
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                ContentResolver resolver = getContentResolver();
                int deleted = resolver.delete(mNoteUri, null, null);
                if(deleted == 0) {
                    throw new RuntimeException("Failed to delete the new note");
                }
                return null;
            }
        };
        task.execute();
    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mOriginalNoteCourseId);
        mNote.setCourse(course);
        mNote.setTitle(mOriginalNoteTitle);
        mNote.setText(mOriginalNoteBody);
    }

    private void saveNote() {
        String courseId = selectedCourseId();
        saveNoteToDatabase(courseId, mTextNoteTitle.getText().toString(), mTextNoteBody.getText().toString());
    }

    private String selectedCourseId() {
        int selectedPosition = mCoursesSpinner.getSelectedItemPosition();
        Cursor cursor = mCoursesAdapter.getCursor();
        cursor.moveToPosition(selectedPosition);
        int courseIdPos = cursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_ID);
        return cursor.getString(courseIdPos);

    }

    private void saveNoteToDatabase(final String courseId, final String noteTitle, final String noteText) {
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(NoteInfoEntry.COLUMN_COURSE_ID, courseId);
                values.put(NoteInfoEntry.COLUMN_NOTE_TITLE, noteTitle);
                values.put(NoteInfoEntry.COLUMN_NOTE_TEXT, noteText);

                int updated = resolver.update(mNoteUri, values, null, null);
                if(updated == 0) {
                    throw new RuntimeException("Failed to delete the new note");
                }
                return null;
            }
        };
        task.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbOpenHelper.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        } else if (id == R.id.action_set_reminder) {
            showReminderNotification();
        } else if (id == R.id.action_cancel) {
            mCancelling = true;
            finish();
        } else if (id == R.id.action_next) {
            moveNext();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showReminderNotification() {
        String noteText = mTextNoteBody.getText().toString();
        String noteTitle = mTextNoteTitle.getText().toString();
        NoteReminderNotification.notify(this, noteTitle, noteText, mNoteId);

        Intent intent = new Intent(this, NoteReminderReceiver.class);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT, noteText);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, mNoteId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long currentTimeInMilliseconds = SystemClock.elapsedRealtime();
        final long ONE_HOUR = 60 * 60 * 1000;
        final long TEN_SECONDS = 10 * 1000;
        long alarmTime = TEN_SECONDS + currentTimeInMilliseconds;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, alarmTime, pendingIntent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_next);
        item.setEnabled(mNoteId < DataManager.getInstance().getNotes().size() -1);
        return super.onPrepareOptionsMenu(menu);
    }

    private void moveNext() {
        saveNote();
        mNoteId++;
        mNote = DataManager.getInstance().getNotes().get(mNoteId);
        saveOriginalNoteValues();
        displayNote();
        invalidateOptionsMenu();
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mCoursesSpinner.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String body = "Check out what I learned in the pluralsight course \"" + course.getTitle() + "\"\n" + mTextNoteBody.getText().toString();

        Intent mailIntent = new Intent(Intent.ACTION_SEND);
        mailIntent.setType("message/rfc2822");
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        mailIntent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(mailIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if(id == LOADER_NOTES) {
            loader = createLoaderNotes();
        } else if(id == LOADER_COURSES) {
            loader = createLoaderCourses();
        }
        return loader;
    }

    private CursorLoader createLoaderCourses() {
        mCoursesQueryFinished = false;
        String[] courseColumns = { CourseInfoEntry._ID, CourseInfoEntry.COLUMN_COURSE_TITLE, CourseInfoEntry.COLUMN_COURSE_ID };
        return new CursorLoader(this, Courses.CONTENT_URI, courseColumns, null, null, CourseInfoEntry.COLUMN_COURSE_TITLE);
    }

    private CursorLoader createLoaderNotes() {
        mNotesQueryFinished = false;
        String[] noteColumns = {
                Notes.COLUMN_COURSE_ID,
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT
        };
        mNoteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, mNoteId);
        return new CursorLoader(this, mNoteUri, noteColumns, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(loader.getId() == LOADER_NOTES)
            loadFinishedNotes(data);
        else if(loader.getId() == LOADER_COURSES) {
            mCoursesAdapter.changeCursor(data);
            mCoursesQueryFinished = true;
            displayNoteWhenQueriesFinished();
        }
    }

    private void loadFinishedNotes(Cursor cursor) {
        mNoteCursor = cursor;
        mCourseIdPos = cursor.getColumnIndex(NoteInfoEntry.COLUMN_COURSE_ID);
        mNoteTitlePos = cursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TITLE);
        mNoteTextPos = cursor.getColumnIndex(NoteInfoEntry.COLUMN_NOTE_TEXT);
        mNoteCursor.moveToFirst();
        mNotesQueryFinished = true;
        displayNoteWhenQueriesFinished();
    }

    private void displayNoteWhenQueriesFinished() {
        if(mNotesQueryFinished && mCoursesQueryFinished) {
            displayNote();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(loader.getId() == LOADER_NOTES) {
            if(mNoteCursor != null) {
                mNoteCursor.close();
            }
        } else if(loader.getId() == LOADER_COURSES) {
            mCoursesAdapter.changeCursor(null);
        }
    }
}
