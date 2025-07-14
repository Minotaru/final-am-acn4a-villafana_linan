package ar.edu.davinci.tufi_store;

//Imports para Firebase Authenticator:
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;



public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerText;
    private TextView forgotPasswordText;

    //Para iniciar instancia de Firebase Athentication

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Instancia de Firestore para Profile



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Establece el layout de esta Activity

        // Inicializa Firebase Auth y firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        registerText = findViewById(R.id.register_text);
        forgotPasswordText = findViewById(R.id.forgot_password_text);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lógica para el inicio de sesión
                performLogin();
            }
        });

        registerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Para redirigir a una pantalla de registro
                performRegistration();

            }
        });

        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Para redirigir a una pantalla de recuperación de contraseñas:
                resetPassword();

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Verifica si el usuario ya está autenticado (no es nulo) y actualiza la UI.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // Si el usuario ya está logueado, redirige directamente a MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Cierra LoginActivity para que no se pueda volver atrás
        }
    }




    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_empty_email));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_empty_password));
            return;
        }

        //Explica la lógica del Logín
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Inicio de sesión exitoso
                            Toast.makeText(LoginActivity.this, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish(); // Finaliza LoginActivity
                        } else {
                            // Si falla el inicio de sesión, muestra un mensaje al usuario.
                            Toast.makeText(LoginActivity.this, getString(R.string.toast_login_failed) + ": " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }

    // Metodo para hacer la registración de usuarios

    private void performRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_empty_email));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_empty_password));
            return;
        }

        //Validacion de cantidad de caracteres para la contraseña
        if (password.length() < 6) { // Firebase requiere mínimo 6 caracteres por defecto
            passwordEditText.setError(getString(R.string.error_password_length));
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Registro exitoso, ahora crea el documento de usuario en Firestore
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("email", user.getEmail());
                                userData.put("nombre", "");
                                userData.put("apellido", "");
                                userData.put("direccion", "");
                                userData.put("telefono", "");

                                db.collection("users").document(user.getUid()).set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                             Toast.makeText(LoginActivity.this, getString(R.string.toast_registration_success_profile), Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            // El registro de Auth fue exitoso, pero falló la creación del perfil en Firestore
                                            Toast.makeText(LoginActivity.this, getString(R.string.toast_registration_success_profile_error) + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                                   } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.toast_registration_success), Toast.LENGTH_SHORT).show();
                                }

                            } else {
                            // Si falla el registro, muestra un mensaje al usuario.
                            Toast.makeText(LoginActivity.this, getString(R.string.toast_registration_failed) + ": " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            }
                    }
                });
    }
        // Metodo para resetear la contraseña

        private void resetPassword() {
            String email = emailEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailEditText.setError(getString(R.string.error_empty_email_reset));
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, getString(R.string.toast_reset_password_success), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.toast_reset_password_failed) + ": " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
}
