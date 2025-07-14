package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // Para fusionar datos

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView backButton;
    private ImageView userButtonProfile;
    private TextView profileEmailText;
    private EditText profileNameEdit;
    private EditText profileLastnameEdit;
    private EditText profileAddressEdit;
    private EditText profilePhoneEdit;
    private Button saveProfileButton;
    private Button viewOrderHistoryButton;
    private ProgressBar profileLoadingIndicator;

    private LinearLayout bottomHomeProfile;
    private LinearLayout bottomCategoriesProfile;
    private LinearLayout bottomLocationsProfile;
    private LinearLayout bottomStoreProfile;
    private LinearLayout bottomUsersProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Verificar si el usuario está logueado
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, getString(R.string.toast_login_required), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Inicializar vistas del perfil
        profileEmailText = findViewById(R.id.profile_email_text);
        profileNameEdit = findViewById(R.id.profile_name_edit);
        profileLastnameEdit = findViewById(R.id.profile_lastname_edit);
        profileAddressEdit = findViewById(R.id.profile_address_edit);
        profilePhoneEdit = findViewById(R.id.profile_phone_edit);
        saveProfileButton = findViewById(R.id.save_profile_button);
        viewOrderHistoryButton = findViewById(R.id.view_order_history_button);
        profileLoadingIndicator = findViewById(R.id.profile_loading_indicator);

        // Inicializar y configurar listeners para las barras
        backButton = findViewById(R.id.back_button);
        userButtonProfile = findViewById(R.id.user_button_profile);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        userButtonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserPopupMenu(v);
            }
        });

        bottomHomeProfile = findViewById(R.id.bottom_home_profile);
        bottomCategoriesProfile = findViewById(R.id.bottom_categories_profile);
        bottomLocationsProfile = findViewById(R.id.bottom_locations_profile);
        bottomStoreProfile = findViewById(R.id.bottom_store_profile);
        bottomUsersProfile = findViewById(R.id.bottom_users_profile);

        bottomHomeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomCategoriesProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, StoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomLocationsProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, LocationsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomStoreProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });

        bottomUsersProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "Ya estás en Perfil", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar listener para el botón Guardar
        saveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });

        // Configurar Listener para el botón de Historial de Compras
        viewOrderHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
                startActivity(intent);
            }
        });

        loadProfileData(); // Cargar los datos del perfil al iniciar
    }

    private void showUserPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.user_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_logout) {
                    performLogout();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(ProfileActivity.this, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Método para cargar los datos del perfil desde Firestore
    private void loadProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        profileEmailText.setText(user.getEmail()); // Mostrar email de Firebase Auth

        profileLoadingIndicator.setVisibility(View.VISIBLE);
        DocumentReference userRef = db.collection("users").document(user.getUid());

        userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                profileLoadingIndicator.setVisibility(View.GONE);
                if (documentSnapshot.exists()) {
                    profileNameEdit.setText(documentSnapshot.getString("nombre"));
                    profileLastnameEdit.setText(documentSnapshot.getString("apellido"));
                    profileAddressEdit.setText(documentSnapshot.getString("direccion"));
                    profilePhoneEdit.setText(documentSnapshot.getString("telefono"));
                } else {
                    Toast.makeText(ProfileActivity.this, getString(R.string.toast_profile_not_found), Toast.LENGTH_SHORT).show();

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                profileLoadingIndicator.setVisibility(View.GONE); // Oculta el ProgressBar
                Toast.makeText(ProfileActivity.this, getString(R.string.toast_profile_load_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ProfileActivity", "Error loading profile: " + e.getMessage());
            }
        });
    }

    // Método para guardar los datos del perfil en Firestore
    private void saveProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = profileNameEdit.getText().toString().trim();
        String apellido = profileLastnameEdit.getText().toString().trim();
        String direccion = profileAddressEdit.getText().toString().trim();
        String telefono = profilePhoneEdit.getText().toString().trim();

        profileLoadingIndicator.setVisibility(View.VISIBLE);
        DocumentReference userRef = db.collection("users").document(user.getUid());

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("nombre", nombre);
        profileData.put("apellido", apellido);
        profileData.put("direccion", direccion);
        profileData.put("telefono", telefono);
        profileData.put("email", user.getEmail()); // guarda el email en Firestore

        userRef.set(profileData, SetOptions.merge()) // Usa merge para no sobrescribir otros campos si los hubiera
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        profileLoadingIndicator.setVisibility(View.GONE); // Oculta el ProgressBar
                        Toast.makeText(ProfileActivity.this, getString(R.string.toast_profile_save_success), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        profileLoadingIndicator.setVisibility(View.GONE); // Oculta el ProgressBar
                        Toast.makeText(ProfileActivity.this, getString(R.string.toast_profile_save_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ProfileActivity", "Error saving profile: " + e.getMessage());
                    }
                });
    }
}