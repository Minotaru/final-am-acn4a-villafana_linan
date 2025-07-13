package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.TextView; // Importado para el TextView del título

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;


public class StoreActivity extends AppCompatActivity {

    private ImageView menuButton;
    private ImageView userButton;
    private LinearLayout bottomHome;
    private LinearLayout bottomCategories;
    private LinearLayout bottomLocations;
    private LinearLayout bottomStore;
    private LinearLayout bottomUsers;


    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        mAuth = FirebaseAuth.getInstance();

        menuButton = findViewById(R.id.menu_button);
        userButton = findViewById(R.id.user_button);

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StoreActivity.this, "Botón de menú clicado en Tienda", Toast.LENGTH_SHORT).show();

            }
        });

        userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserPopupMenu(v);
            }
        });

        // Bottom Navigation
        bottomHome = findViewById(R.id.bottom_home);
        bottomCategories = findViewById(R.id.bottom_categories);
        bottomLocations = findViewById(R.id.bottom_locations);
        bottomStore = findViewById(R.id.bottom_store);
        bottomUsers = findViewById(R.id.bottom_users);

        // Listener para Home
        bottomHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StoreActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        // Listerner para Tienda:

        bottomCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StoreActivity.this, "Ya estás en Tienda", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para Sucursales

        bottomLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StoreActivity.this, LocationsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();

            }
        });

        // Listener para Store (Carrito)
        bottomStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StoreActivity.this, "Botón de Carrito clicado en Tienda", Toast.LENGTH_SHORT).show();

            }
        });

        // Listener para Users (Perfil)
        bottomUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StoreActivity.this, "Botón de Perfil clicado en Tienda", Toast.LENGTH_SHORT).show();
            }
        });

    }


    // Método para mostrar el menú emergente del usuario (Se replicó el del MainActivity)
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

    // Método para cerrar la sesion y redirigir a la pantalla de login (Se replicó el del MainActivity)
    private void performLogout() {
        mAuth.signOut(); // Cierra la sesión de Firebase
        Toast.makeText(StoreActivity.this, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(StoreActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

