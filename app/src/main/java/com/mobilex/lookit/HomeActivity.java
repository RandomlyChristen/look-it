package com.mobilex.lookit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobilex.lookit.db.DBManager;
import com.mobilex.lookit.utils.Memo;
import com.mobilex.lookit.utils.SimpleBackground;

import java.util.ArrayList;
import java.util.Vector;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv-native");
    }

    private static ArrayList<Memo> selectedMemos = new ArrayList<>();
    public static ArrayList<Memo> getSelectedMemos() { return selectedMemos; }

    private static class FocusedMemo {
        static int mode;
        static int id;
        static String title, content, filename;
    }

    public static String[] getFocusedTitleAndContent() {
        return new String[] {FocusedMemo.title, FocusedMemo.content};
    }

    private static final int MODE_NEW = 1;
    private static final int MODE_MOD = 2;

    private ActivityResultLauncher<Intent> memoImageCaptor;
    private ArrayAdapter<Memo> memoArrayAdapter;
    private DBManager dbManager;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        memoImageCaptor = getMemoImageCaptor();
        dbManager = DBManager.getInstance(getApplicationContext());

        Button addBtn = findViewById(R.id.home_add_btn);
        addBtn.setOnClickListener(view -> writeNewMemo());

        ListView memoList = findViewById(R.id.home_notes_list);
        memoArrayAdapter = getMemoArrayAdapter();
        memoList.setAdapter(memoArrayAdapter);

        searchInput = findViewById(R.id.home_search_input);
        Button searchBtn = findViewById(R.id.home_search_btn);
        searchBtn.setOnClickListener(view -> refreshMemoList());

        Button lookBtn = findViewById(R.id.home_look_btn);
        lookBtn.setOnClickListener(view -> lookIt());

        refreshMemoList();
    }

    private ActivityResultLauncher<Intent>
    getMemoImageCaptor() {
        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) return;
                    String capturedJson = CaptureActivity.getInterestJson();
                    if (capturedJson == null) {
                        Toast.makeText(HomeActivity.this, "현재 위치에 붙일 수 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (FocusedMemo.mode == MODE_NEW) {
                        new SimpleBackground<Boolean>(this, "새 메모를 작성 중") {
                            @Override
                            public Boolean run() {
                                return dbManager.newNote(FocusedMemo.title, FocusedMemo.content, capturedJson);
                            }
                            @Override
                            public void after(Boolean result) {
                                if (result) {
                                    Toast.makeText(HomeActivity.this, "새 메모를 작성했습니다", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(HomeActivity.this, "DB 접근할 수 없습니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                        };
                    }

                    if (FocusedMemo.mode == MODE_MOD) {
                        new SimpleBackground<Boolean>(this, "메모를 새로 붙이는 중") {
                            @Override
                            public Boolean run() {
                                return dbManager.moveMemo(FocusedMemo.id, FocusedMemo.filename,
                                        capturedJson);
                            }

                            @Override
                            public void after(Boolean result) {
                                if (result) {
                                    Toast.makeText(HomeActivity.this, "메모를 옮겼습니다", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(HomeActivity.this, "DB 접근할 수 없습니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                        };
                    }

                    refreshMemoList();
                });
    }

    private void writeNewMemo() {
        new Dialog(this) {{
            setContentView(R.layout.write_dialog);
            EditText titleInput = findViewById(R.id.write_title_input);
            EditText contentInput = findViewById(R.id.write_content_input);
            Button attachBtn = findViewById(R.id.write_attach_btn);
            Context thisActivity = HomeActivity.this;

            attachBtn.setOnClickListener((view -> {
                FocusedMemo.mode = MODE_NEW;
                FocusedMemo.title = titleInput.getText().toString();
                FocusedMemo.content = contentInput.getText().toString();

                if (FocusedMemo.title.isEmpty() || FocusedMemo.content.isEmpty()) {
                    Toast.makeText(thisActivity, "제목과 내용은 비어있을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    memoImageCaptor.launch(new Intent(thisActivity, CaptureActivity.class));
                    dismiss();
                }
            }));
        }}.show();
    }

    private void refreshMemoList() {
        String query = searchInput.getText().toString();

        new SimpleBackground<ArrayList<Memo>>(this, "조회 중") {
            @Override
            public ArrayList<Memo> run() {
                return dbManager.getMyMemos();
            }

            @Override
            public void after(ArrayList<Memo> result) {
                if (!query.isEmpty()) {
                    result = dbManager.searchFromMemos(result, query.split(" "));
                }

                memoArrayAdapter.clear();
                memoArrayAdapter.addAll(result);
                selectedMemos = result;
            }
        };
    }

    private void showMemoDetail(Memo memo) {
        new Dialog(this) {{
            setContentView(R.layout.edit_dialog);
            EditText titleInput = findViewById(R.id.edit_title_input);
            titleInput.setText(memo.getTitle());
            EditText contentInput = findViewById(R.id.edit_content_input);
            contentInput.setText(memo.getContent());
            EditText shareInput = findViewById(R.id.edit_share_input);
            shareInput.setText(memo.getShareId());
            Button editBtn = findViewById(R.id.edit_edit_btn);
            Activity thisActivity = HomeActivity.this;

            if (memo.isEditable(dbManager.getLastCredential())) {
                editBtn.setText("메모 수정");
                editBtn.setEnabled(true);
                editBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String title = titleInput.getText().toString();
                        String content = contentInput.getText().toString();

                        if (title.isEmpty() || content.isEmpty()) {
                            Toast.makeText(thisActivity, "제목과 내용은 비어있을 수 없습니다", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String shareId = shareInput.getText().toString();
                        shareId = shareId.isEmpty() ? null : shareId;

                        if (shareId != null &&
                                shareId.equals(dbManager.getLastCredential())) {
                            Toast.makeText(thisActivity, "자신에게는 공유할 수 없습니다", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String finalShareId = shareId;
                        new SimpleBackground<Boolean>(thisActivity, "수정 중") {
                            @Override
                            public Boolean run() {
                                return dbManager.updateMemo(memo.getMemoId(), title, content, finalShareId);
                            }

                            @Override
                            public void after(Boolean result) {
                                if (result) {
                                    memo.setTitle(title);
                                    memo.setContent(content);
                                    memo.setShareId(finalShareId);
                                    dismiss();
                                    Toast.makeText(thisActivity, "메모를 수정했습니다", Toast.LENGTH_SHORT).show();
                                    refreshMemoList();
                                } else {
                                    Toast.makeText(thisActivity, "공유할 아이디를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                        };
                    }
                });
            }
         }}.show();
    }

    private ArrayAdapter<Memo> getMemoArrayAdapter() {
        return new ArrayAdapter<Memo>(this, R.layout.memo_list_item) {
            @SuppressLint("InflateParams")
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                Memo memo = getItem(position);

                if (convertView == null) {
                    convertView =
                            ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                    .inflate(R.layout.memo_list_item, null);
                }

                TextView titleView = convertView.findViewById(R.id.memo_item_title);
                TextView contentView = convertView.findViewById(R.id.memo_item_content);

                titleView.setText(memo.getTitle());
                contentView.setText(memo.getContent());

                LinearLayout memoDetailBtn = convertView.findViewById(R.id.memo_item_detail_btn);
                memoDetailBtn.setOnClickListener(view -> showMemoDetail(memo));

                if (memo.isEditable(dbManager.getLastCredential())) {
                    Button deleteBtn = convertView.findViewById(R.id.memo_item_delete_btn);
                    deleteBtn.setEnabled(true);
                    deleteBtn.setOnClickListener(view ->
                            new SimpleBackground<Boolean>(HomeActivity.this, "삭제 중") {
                        @Override
                        public Boolean run() {
                            return dbManager.deleteMemo(memo.getMemoId(), memo.getFilename());
                        }
                        @Override
                        public void after(Boolean result) {
                            if (result) {
                                remove(memo);
                                Toast.makeText(HomeActivity.this, "삭제했습니다", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    Button moveBtn = convertView.findViewById(R.id.memo_item_move_btn);
                    moveBtn.setEnabled(true);
                    moveBtn.setOnClickListener(view -> {
                        FocusedMemo.mode = MODE_MOD;
                        FocusedMemo.id = memo.getMemoId();
                        FocusedMemo.title = memo.getTitle();
                        FocusedMemo.content = memo.getContent();
                        FocusedMemo.filename = memo.getFilename();
                        memoImageCaptor.launch(new Intent(HomeActivity.this, CaptureActivity.class));
                    });
                }

                return convertView;
            }
        };
    }

    private void lookIt() {
        startActivity(new Intent(this, LookActivity.class));
    }
}