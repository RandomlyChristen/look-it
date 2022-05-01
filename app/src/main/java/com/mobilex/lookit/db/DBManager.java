package com.mobilex.lookit.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mobilex.lookit.utils.Memo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DBManager {
    static final String DB_LOOKIT = "LookIt.db";
    static final String TABLE_USERS = "Users";
    static final String TABLE_NOTES = "Notes";

    Context applicationContext;

    private String lastCredential;
    public String getLastCredential() { return lastCredential; }

    private static DBManager myDBManager = null;
    private final SQLiteDatabase myDatabase;

    private DBManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        myDatabase = context.openOrCreateDatabase(DB_LOOKIT, Context.MODE_PRIVATE, null);

        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS +
                "(userid TEXT PRIMARY KEY," +
                "userpw TEXT)");

        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTES +
                "(noteid INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "content TEXT," +
                "imagefile TEXT," +
                "userid TEXT NOT NULL," +
                "shareid TEXT," +
                "CONSTRAINT userid_fk FOREIGN KEY(userid) REFERENCES "+ TABLE_USERS + "(userid)," +
                "CONSTRAINT shareid_fk FOREIGN KEY(shareid) REFERENCES " + TABLE_USERS + "(userid))");
    }

    public static DBManager getInstance(Context context) {
        if (myDBManager == null) {
            myDBManager = new DBManager(context);
        }

        return myDBManager;
    }

    public boolean register(String id, String pw) {
        Cursor duplicatedIds = myDatabase.query(TABLE_USERS, new String[]{"userid"},
                "userid = ?", new String[]{id}, null, null, null);

        if (duplicatedIds.getCount() != 0) return false;
        duplicatedIds.close();

        ContentValues values = new ContentValues();
        values.put("userid", id);
        values.put("userpw", pw);

        long result = myDatabase.insert(TABLE_USERS, null, values);

        return result != -1;
    }

    public boolean login(String id, String pw) {
        Cursor matchedPws = myDatabase.query(TABLE_USERS, new String[]{"userpw"},
                "userid = ?", new String[]{id}, null, null, null);

        if (matchedPws.getCount() == 0) return false;
        matchedPws.moveToNext();
        String matchedPw = matchedPws.getString(0);
        matchedPws.close();

        if (!matchedPw.equals(pw)) return false;
        lastCredential = id;
        return true;
    }

    public boolean newNote(String title, String content, String json) {
        String filename;
        File file;

        // 온라인 일 때 문제 발생
        do {
            filename = System.currentTimeMillis() + ".json";
            file = dataFile(filename);
        } while (file.exists());

        try {
            FileOutputStream fos = applicationContext.openFileOutput(
                    filename, Context.MODE_PRIVATE
            );
            fos.write(json.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("imagefile", filename);
        values.put("userid", lastCredential);
        long result = myDatabase.insert(TABLE_NOTES, null, values);

        return result != -1;
    }

    public ArrayList<Memo> getMyMemos() {
        ArrayList<Memo> result = new ArrayList<>();

        for (Cursor memos : new Cursor[]{
                myDatabase.query(TABLE_NOTES, null,
                        "userid = ?", new String[]{lastCredential},
                        null, null, null),
                myDatabase.query(TABLE_NOTES, null,
                        "shareid = ?", new String[]{lastCredential},
                        null, null, null)}) {

            while (memos.moveToNext()) {
                int noteId = memos.getInt(0);
                String title = memos.getString(1);
                String content = memos.getString(2);
                String filename = memos.getString(3);
                String userId = memos.getString(4);
                String shareId = memos.getString(5);
                Memo memo = new Memo(noteId, title, content, filename, userId, shareId);

                result.add(memo);
            }

            memos.close();
        }
        return result;
    }

    public boolean updateMemo(int id, String title, String content, String shareId) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("shareid", shareId);

        return myDatabase.update(TABLE_NOTES, values,
                "noteid = ?", new String[]{String.valueOf(id)}) != -1;
    }

    public boolean moveMemo(int memoId, String lastFilename, String json) {
        String filename;
        File file;

        // 온라인 일 때 문제 발생
        do {
            filename = System.currentTimeMillis() + ".json";
            file = dataFile(filename);
        } while (file.exists());

        try {
            FileOutputStream fos = applicationContext.openFileOutput(
                    filename, Context.MODE_PRIVATE
            );
            fos.write(json.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("imagefile", filename);

        if (myDatabase.update(TABLE_NOTES, values,
                "noteid = ?", new String[]{String.valueOf(memoId)}) != -1) {
            dataFile(lastFilename).delete();
            return true;
        }
        return false;
    }

    public boolean deleteMemo(int memoId, String filename) {
        dataFile(filename).delete();
        return myDatabase.delete(TABLE_NOTES, "noteid = ?",
                new String[]{String.valueOf(memoId)}) == 1;
    }

    public ArrayList<Memo> searchFromMemos(ArrayList<Memo> memos, String[] queries) {
        ArrayList<Memo> result = new ArrayList<>();

        for (Memo memo : memos) {
            String title, content;
            title = memo.getTitle();
            content = memo.getContent();

            boolean check = true;

            for (String query : queries) {
                if (!title.contains(query) || !content.contains(query)) {
                    check = false;
                    break;
                }
            }

            if (check) result.add(memo);
        }

        return result;
    }

    public File dataFile(String filename) {
        return new File(applicationContext.getFilesDir().getAbsolutePath() + '/' + filename);
    }

    public String getJsonFromFile(String filename) {
        try {
            FileInputStream fis = applicationContext.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader buffrd = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = buffrd.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
