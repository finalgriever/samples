package com.example.jamiebutler.notekeeper;

import android.app.assist.AssistStructure;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataManagerTest {
    static DataManager sDataManager;

    @BeforeClass
    public static void setUpClass() {
        sDataManager = DataManager.getInstance();
    }

    @Before
    public void setUp() {
        sDataManager.getNotes().clear();
        sDataManager.initializeExampleNotes();
    }

    @Test
    public void createNewNote() {
        final CourseInfo course = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteBody = "This is the body text of my test note";

        int noteIndex = sDataManager.createNewNote();
        NoteInfo newNote = sDataManager.getNotes().get(noteIndex);
        newNote.setCourse(course);
        newNote.setTitle(noteTitle);
        newNote.setText(noteBody);

        NoteInfo expectedNote = sDataManager.getNotes().get(noteIndex);
        Assert.assertEquals(course, expectedNote.getCourse());
        Assert.assertEquals(noteTitle, expectedNote.getTitle());
        Assert.assertEquals(noteBody, expectedNote.getText());
    }
    
    @Test
    public void findSimilarNotes() {
        final CourseInfo course = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteText1 = "This is the body text of my test note";
        final String noteText2 = "This is the body of my second test note";
        
        int noteIndex1 = sDataManager.createNewNote();
        NoteInfo newNote1 = sDataManager.getNotes().get(noteIndex1);
        newNote1.setCourse(course);
        newNote1.setTitle(noteTitle);
        newNote1.setTitle(noteText1);


        int noteIndex2 = sDataManager.createNewNote();
        NoteInfo newNote2 = sDataManager.getNotes().get(noteIndex2);
        newNote2.setCourse(course);
        newNote2.setTitle(noteTitle);
        newNote2.setTitle(noteText2);

        int foundIndex1 = sDataManager.findNote(newNote1);
        int foundIndex2 = sDataManager.findNote(newNote2);

        Assert.assertEquals(noteIndex1, foundIndex1);
        Assert.assertEquals(noteIndex2, foundIndex2);
    }

    @Test
    public void createNewNoteWithData() {
        final CourseInfo course = sDataManager.getCourse("android_async");
        final String noteTitle = "Test note title";
        final String noteText = "This is the body of my test note";

        int noteIndex = sDataManager.createNewNote(course, noteTitle, noteText);
        NoteInfo newNote = sDataManager.getNotes().get(noteIndex);

        Assert.assertEquals(course, newNote.getCourse());
        Assert.assertEquals(noteTitle, newNote.getTitle());
        Assert.assertEquals(noteText, newNote.getText());
    }
}