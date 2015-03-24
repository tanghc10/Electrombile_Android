package com.xunce.electrombile.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avos.avoscloud.SaveCallback;
import com.xunce.electrombile.R;


public class LoginActivity extends Activity implements View.OnClickListener {

    EditText username;
    EditText password;
    TextView findpassword;
    TextView register;
    Button login;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();

    }

    private void initView(){
        username = (EditText) findViewById(R.id.et_username);
        password = (EditText) findViewById(R.id.et_password);
        findpassword = (TextView) findViewById(R.id.btn_findpwd);
        register = (TextView) findViewById(R.id.btn_register);
        login = (Button) findViewById(R.id.btn_login);
        login.setOnClickListener(this);
        findpassword.setOnClickListener(this);
        register.setOnClickListener(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btn_login:
                //do
                String login_username = username.getText().toString();
                String login_pwd = password.getText().toString();
                if("".equals(login_username) || "".equals(login_pwd)){
                    Toast.makeText(getApplicationContext(), "用户名密码不能为空",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    AVUser.logInInBackground(login_username, login_pwd, new LogInCallback<AVUser>() {
                        @Override
                        public void done(AVUser avUser, AVException e) {
                            if(avUser != null){
                                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                                startActivity(intent);
                                LoginActivity.this.finish();
                                Toast.makeText(getApplicationContext(), "登陆成功",
                                        Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(), "用户名或密码错误",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                break;
            case R.id.btn_findpwd:
                //do intent another activity
                Intent intent1 = new Intent(LoginActivity.this,ForgetActivity.class);
                startActivity(intent1);
                break;
            case R.id.btn_register:
                //do intent another activity
                Intent intent2 = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent2);
                this.finish();
                break;
            default:break;

        }
    }
}
