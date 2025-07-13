package ar.edu.davinci.tufi_store;

import android.content.Intent; // Para redirigir a LoginActivity
import android.view.MenuItem; // Para manejar el clic en el elemento del menú
import android.widget.PopupMenu; // Para mostrar el menú emergente

import com.google.firebase.auth.FirebaseAuth; //Para integrar firebase authenticator

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Referencias a los elementos de la Card 1 + import clases
    TextView figurita1TitleTextView;
    ImageView figurita1ImageView;
    TextView figurita1NameTextView;
    Button figurita1ComprarButton;


    // Referencias a los elementos de la Card 2
    TextView figurita2TitleTextView;
    ImageView figurita2ImageView;
    TextView figurita2NameTextView;
    Button figurita2ComprarButton;

    //Referencia al ícono de usuario del navbar
    private ImageView userButton;

    //Referencia al boton del footer Sucursarles
    private LinearLayout bottomLocations;

    //Referencia al botón del footer Tienda
    private LinearLayout bottomCategories;

    //Se instancia el firebase authenticator
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_tufi), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        //Para inicializar Firebase Autenticator
        mAuth = FirebaseAuth.getInstance();



        // Obtener referencias a los elementos de la Card 1 (figurita1)
        figurita1TitleTextView = findViewById(R.id.figurita1_title);
        figurita1ImageView = findViewById(R.id.figurita1_image);
        figurita1NameTextView = findViewById(R.id.figurita1_name);
        figurita1ComprarButton = findViewById(R.id.figurita1_comprar);

        // Configurar listeners para los botones de la Card 1 (figurita1) + import VIEW class & Toast class
        figurita1ComprarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Comprar Figurita 1", Toast.LENGTH_SHORT).show();

            }
        });


        // Obtener referencias a los elementos de la Card 2 (figurita2)
        figurita2TitleTextView = findViewById(R.id.figurita2_title);
        figurita2ImageView = findViewById(R.id.figurita2_image);
        figurita2NameTextView = findViewById(R.id.figurita2_name);
        figurita2ComprarButton = findViewById(R.id.figurita2_comprar);

        // Configurar listeners para los botones de la Card 2 (figurita2)
        figurita2ComprarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Comprar Figurita 2", Toast.LENGTH_SHORT).show();

            }
        });


        // --- Código para agregar dinámicamente un elemento: el título "Últimos agregados" ---

        // 1. Obtener una referencia al LinearLayout principal dentro del ScrollView
                LinearLayout linearLayoutPrincipal = (LinearLayout) findViewById(R.id.card_quienes).getParent();

        // 2. Obtener una referencia a la CardView "Quiénes somos?" para insertar después
                androidx.cardview.widget.CardView cardQuienes = findViewById(R.id.card_quienes);

        // 3. Crear el Elemento: TextView para el título
                TextView tituloUltimosAgregados = new TextView(this);
                LinearLayout.LayoutParams tituloLayoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                tituloLayoutParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                tituloLayoutParams.setMargins(0, 80, 0, 32); // Margen superior e inferior
                tituloUltimosAgregados.setLayoutParams(tituloLayoutParams);
                tituloUltimosAgregados.setText("Últimos agregados");
                tituloUltimosAgregados.setTextSize(23);
                tituloUltimosAgregados.setTypeface(null, android.graphics.Typeface.BOLD);
                tituloUltimosAgregados.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                tituloUltimosAgregados.setBackgroundColor(ContextCompat.getColor(this, R.color.holo_blue_light));
                tituloUltimosAgregados.setPadding(16, 8, 16, 8); // Padding para el texto dentro del fondo

        // 4. Obtener el índice de la CardView "Quiénes somos?"
                int indexCardQuienes = linearLayoutPrincipal.indexOfChild(cardQuienes);

        // 5. Insertar el TextView justo después de la CardView "Quiénes somos?"
                linearLayoutPrincipal.addView(tituloUltimosAgregados, indexCardQuienes + 1);


        // Obtener referencia al LinearLayout del botón de Tienda
        bottomCategories = findViewById(R.id.bottom_categories);

        // Configurar listener para el botón de Tienda
        bottomCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });
                

        // Obtener referencia al LinearLayout del botón de Sucursales en el Bottom Navigation
        bottomLocations = findViewById(R.id.bottom_locations);

        // Configurar listener para el botón de Sucursales
        bottomLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LocationsActivity.class);
                startActivity(intent);
            }
        });


        //Referencia al ImageView del botón del ícono de usuario en navbar
        userButton = findViewById(R.id.user_button);

        //Configuración del listener para el botón del ícono de usuario
        userButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserPopupMenu(v);
            }
        });

    }

    //Metodo para mostrar el menu emergente del ícono del usuario del navbar
    private void showUserPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        //Inflar el menú desde el archivo XML
        popup.getMenuInflater().inflate(R.menu.user_menu, popup.getMenu());

        // Setea el listener para hacer clic en el elemento del menú
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Verificar qué elemento del menú fue seleccionado
                if (item.getItemId() == R.id.action_logout) {
                    performLogout(); // Llama al método para cerrar sesión
                    return true;
                }
                return false;
            }
        });
        popup.show(); // Mostrar el menú
    }

    //Metodo para cerrar al sesión y redirigir a la pantalla de loggin
    private void performLogout() {

        mAuth.signOut(); // Cierra la sesión de Firebase

        Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show(); // Mensaje al cliquear

        //Redirige a la pantalla de LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); // Inicia LoginActivity
        finish(); // Finaliza MainActivity
    }



}