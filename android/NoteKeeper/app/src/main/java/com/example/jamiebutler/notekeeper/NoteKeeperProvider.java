package com.example.jamiebutler.notekeeper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.example.jamiebutler.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.example.jamiebutler.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.example.jamiebutler.notekeeper.NoteKeeperProviderContract.Courses;
import com.example.jamiebutler.notekeeper.NoteKeeperProviderContract.CoursesIdColumns;
import com.example.jamiebutler.notekeeper.NoteKeeperProviderContract.Notes;

public class NoteKeeperProvider extends ContentProvider {
    NoteKeeperOpenHelper mDbOpenHelper;

    public static final String AUTHORITY = "content://com.example.jamiebutler.notekeeper.provider";

    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int COURSES = 0;
    public static final int NOTES = 1;
    public static final int NOTES_EXPANDED = 2;

    public static final int NOTES_ROW = 3;

    static {
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Courses.PATH, COURSES);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH, NOTES);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH_EXPANDED, NOTES_EXPANDED);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH + "/#", NOTES_ROW);
    }

    public static final String MIME_FORMAT = "%s/vnd." + NoteKeeperProviderContract.AUTHORITY + ".%s";

    public NoteKeeperProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] whereArgs) {        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        int uriMatch = sUriMatcher.match(uri);
        switch(uriMatch) {
            case COURSES:
                return db.delete(CourseInfoEntry.TABLE_NAME, selection, whereArgs);
            case NOTES:
                return db.delete(NoteInfoEntry.TABLE_NAME, selection, whereArgs);
            case NOTES_EXPANDED:
                throw new RuntimeException("notes_expanded is read only");
            case NOTES_ROW:
                long rowId = ContentUris.parseId(uri);
                String rowSelection = NoteInfoEntry._ID + " = ?";
                whereArgs = new String[]{Long.toString(rowId)};
                return db.delete(NoteInfoEntry.TABLE_NAME, rowSelection, whereArgs);
            default:
                return 0;
        }
    }

    @Override
    public String getType(Uri uri) {
        int uriMatch = sUriMatcher.match(uri);
        switch(uriMatch) {
            case COURSES:
                return String.format(MIME_FORMAT, ContentResolver.CURSOR_DIR_BASE_TYPE, Courses.PATH);
            case NOTES:
                return String.format(MIME_FORMAT, ContentResolver.CURSOR_DIR_BASE_TYPE, Notes.PATH);
            case NOTES_EXPANDED:
                return String.format(MIME_FORMAT, ContentResolver.CURSOR_DIR_BASE_TYPE, Notes.PATH_EXPANDED);
            case NOTES_ROW:
                return String.format(MIME_FORMAT, ContentResolver.CURSOR_ITEM_BASE_TYPE, Notes.PATH);
            default:
                throw new RuntimeException("Impossible mimetype");
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = -1;
        int uriMatch = sUriMatcher.match(uri);
        switch(uriMatch) {
            case NOTES:
                rowId = db.insert(NoteInfoEntry.TABLE_NAME, null, values);
                return ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
            case COURSES:
                rowId = db.insert(CourseInfoEntry.TABLE_NAME, null, values);
                return ContentUris.withAppendedId(Courses.CONTENT_URI, rowId);
            case NOTES_EXPANDED:
                throw new RuntimeException("notes_expanded is read only");
            default:
                return null;
        }
    }

    @Override
    public boolean onCreate() {
        mDbOpenHelper = new NoteKeeperOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        int uriMatch = sUriMatcher.match(uri);
        switch(uriMatch) {
            case COURSES:
                return db.query(CourseInfoEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            case NOTES:
                return db.query(NoteInfoEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            case NOTES_EXPANDED:
                return notesExpandedQuery(db, projection, selection, selectionArgs, sortOrder);
            case NOTES_ROW:
                long rowId = ContentUris.parseId(uri);
                String rowSelection = NoteInfoEntry._ID + " = ?";
                String[] rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.query(NoteInfoEntry.TABLE_NAME, projection, rowSelection, rowSelectionArgs, null, null, null);
            default:
                return null;
        }
    }

    private Cursor notesExpandedQuery(SQLiteDatabase db, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] columns = new String[projection.length];
        for(int index = 0; index < projection.length; index++) {
            columns[index] = (projection[index].equals(BaseColumns._ID) || projection[index].equals(CoursesIdColumns.COLUMN_COURSE_ID)) ? NoteInfoEntry.getQName(projection[index]) : projection[index];
        }
        String tablesWithJoin = NoteInfoEntry.TABLE_NAME + " JOIN " + CourseInfoEntry.TABLE_NAME + " ON " +
                CourseInfoEntry.TABLE_NAME + "." + NoteInfoEntry.COLUMN_COURSE_ID + " = " +
                NoteInfoEntry.TABLE_NAME + "." + CourseInfoEntry.COLUMN_COURSE_ID;
        return db.query(tablesWithJoin, columns, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        int uriMatch = sUriMatcher.match(uri);
        switch(uriMatch) {
            case COURSES:
                return db.update(CourseInfoEntry.TABLE_NAME, values, selection, selectionArgs);
            case NOTES:
                return db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);
            case NOTES_EXPANDED:
                throw new RuntimeException("notes_expanded is read only");
            case NOTES_ROW:
                long rowId = ContentUris.parseId(uri);
                String rowSelection = NoteInfoEntry._ID + " = ?";
                String[] rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.update(NoteInfoEntry.TABLE_NAME, values, rowSelection, rowSelectionArgs);
            default:
                return 0;
        }
    }
}
