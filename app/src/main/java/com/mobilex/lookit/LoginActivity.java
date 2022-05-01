package com.mobilex.lookit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mobilex.lookit.db.DBManager;

import static android.Manifest.permission.CAMERA;

public class LoginActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 200;

    private EditText inputId;
    private EditText inputPw;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        permission();

        dbManager = DBManager.getInstance(getApplicationContext());

        inputId = findViewById(R.id.login_id_input);
        inputPw = findViewById(R.id.login_pw_input);

        Button regBtn = findViewById(R.id.login_reg_btn);
        regBtn.setOnClickListener(view -> register());

        Button loginBtn = findViewById(R.id.login_login_btn);
        loginBtn.setOnClickListener(view -> login());
    }


    private void register() {
        String id, pw;
        id = inputId.getText().toString();
        pw = inputPw.getText().toString();

        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "0이 아닌 길이의 ID 또는 PW가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!dbManager.register(id, pw)) {
            Toast.makeText(LoginActivity.this,
                    id + "는 사용할 수 없는 ID 입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(LoginActivity.this,
                "계정이 생성되었습니다! 로그인하세요.", Toast.LENGTH_SHORT).show();
        inputId.setText("");
        inputPw.setText("");
    }

    private void login() {
        String id, pw;
        id = inputId.getText().toString();
        pw = inputPw.getText().toString();

        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(LoginActivity.this,
                    "올바른 길이의 ID 또는 PW가 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!dbManager.login(id, pw)) {
            Toast.makeText(LoginActivity.this,
                    "ID 또는 PW가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(LoginActivity.this,
                "로그인 성공! " + id + "님 환영합니다.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
    }

    private void permission() {
        if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_CODE);
            finish();
        }
    }
}