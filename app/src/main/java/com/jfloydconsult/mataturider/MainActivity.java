package com.jfloydconsult.mataturider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jfloydconsult.mataturider.common.AppConstants;
import com.jfloydconsult.mataturider.model.Rider;
import com.jfloydconsult.mataturider.utility.SurveyLab;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {
    Button btnSignIn, btnRegister;
    public SurveyLab mSurveyLab;
    RelativeLayout rootLayout;

    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    DatabaseReference dbUsers;
    private FirebaseAuth.AuthStateListener mAuthListener;
    SpotsDialog waitingDialog;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //before setContentView
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_main);
        waitingDialog = new SpotsDialog(MainActivity.this);
        // Init firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        dbUsers = db.getReference(AppConstants.CUSTOMERS);
        //Init View
        rootLayout = findViewById(R.id.root_layout);
        btnRegister = findViewById(R.id.activity_main_btnRegister);
        btnSignIn = findViewById(R.id.activity_main_btnSignIn);
        mAuth = FirebaseAuth.getInstance();

        //Event
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    startActivity(new Intent(MainActivity.this, Home.class));
                    finish();
                }
            }
        };
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });
    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("REGISTER");
        dialog.setMessage("Please use email to register");

        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.layout_registration, null);

        //TextInputEditText
        final TextInputEditText editEmail = v.findViewById(R.id.layout_registration_emailEditText);
        final TextInputEditText editPassword = v.findViewById(R.id.layout_registration_passwordEditText);
        final TextInputEditText editName = v.findViewById(R.id.layout_registration_nameEditText);
        final TextInputEditText editPhone = v.findViewById(R.id.layout_registration_phoneEditText);

        dialog.setView(v);

        //Set Button
        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
waitingDialog.show();
                // check validation
                String emailId = editEmail.getText().toString();
                if(TextUtils.isEmpty(editName.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter email", Snackbar.LENGTH_LONG)
                            .show();
                }
                if(TextUtils.isEmpty(editPhone.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter phone number", Snackbar.LENGTH_LONG)
                            .show();
                }
                boolean  isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(emailId).matches();
                if (!isValid) {
                    Snackbar.make(rootLayout, "Invalid Email address, ex: abc@example.com", Snackbar.LENGTH_LONG)
                            .show();
                }
                if(TextUtils.isEmpty(editPassword.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_LONG)
                            .show();
                }
                if(editPassword.getText().toString().length()<6){
                    Snackbar.make(rootLayout, "Password too short !!!", Snackbar.LENGTH_LONG)
                            .show();
                }

                // Register new user
                mAuth.createUserWithEmailAndPassword(editEmail.getText().toString(), editPassword.getText().toString())
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d("MainActivity", "createUserWithEmail:success");
                                    String uid = mAuth.getUid();
                                    Rider customer = new Rider();
                                    customer.setFullName(editName.getText().toString());
                                    customer.setPhone(editPhone.getText().toString());
                                    customer.setEmail(editEmail.getText().toString());

                                    //Use uid to key
                                    dbUsers.child(uid)
                                            .setValue(customer)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Snackbar.make(rootLayout, "Account Created Successfully.",
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Snackbar.make(rootLayout, "Failed "+e.getMessage(),
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                // ...
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(rootLayout, "Failed "+e.getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        waitingDialog.dismiss();
        dialog.show();
    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("SIGN IN");
        dialog.setMessage("Please use email to sign in");

        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.layout_login, null);

        //TextInputEditText
        final TextInputEditText editEmail = v.findViewById(R.id.layout_login_emailEditText);
        final TextInputEditText editPassword = v.findViewById(R.id.layout_login_passwordEditText);

        dialog.setView(v);

        //Set Button
        dialog.setPositiveButton("Sign In", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Set disable button Sign In if is processing
                btnSignIn.setEnabled(false);
                // check validation
                String emailId = editEmail.getText().toString();
                boolean  isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(emailId).matches();
                if (!isValid) {
                    Snackbar.make(rootLayout, "Invalid Email address, ex: abc@example.com", Snackbar.LENGTH_LONG)
                            .show();
                }
                if(TextUtils.isEmpty(editPassword.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_LONG)
                            .show();
                }
                if(editPassword.getText().toString().length()<6){
                    Snackbar.make(rootLayout, "Password too short !!!", Snackbar.LENGTH_LONG)
                            .show();
                }

                waitingDialog.show();
                // Register new user
                mAuth.signInWithEmailAndPassword(editEmail.getText().toString(), editPassword.getText().toString())
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    waitingDialog.dismiss();
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d("MainActivity", "signInWithEmailAndPassword:success");
                                    startActivity(new Intent(getApplicationContext(), Home.class));
                                    finish();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSignIn.setEnabled(true);
                        Snackbar.make(rootLayout, "Failed "+e.getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        btnSignIn.setEnabled(true);
        waitingDialog.dismiss();
        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }
}