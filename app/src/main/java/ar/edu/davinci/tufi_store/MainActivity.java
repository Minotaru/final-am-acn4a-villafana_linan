package ar.edu.davinci.tufi_store;

import android.content.Intent; // Para redirigir a LoginActivity
import android.view.MenuItem; // Para manejar el clic en el elemento del menú
import android.widget.PopupMenu; // Para mostrar el menú emergente
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth; //Para integrar firebase authenticator

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.collection.LruCache;

//Necesario para implementar Firestore

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //Referencia al ícono de usuario del navbar
    private ImageView userButton;

    //Referencia al boton del footer Sucursarles
    private LinearLayout bottomLocations;

    //Referencia al botón del footer Tienda
    private LinearLayout bottomCategories;

    //Se instancia el firebase authenticator
    private FirebaseAuth mAuth;

    //Para usar firestore
    private LinearLayout ultimosAgregadosContainer; // Contenedor para figuritas dinámicas
    private FirebaseFirestore db; // Instancia de Firestore
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private ProgressBar mainLoadingIndicator;


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


        //Para iniciar Firestore
        db = FirebaseFirestore.getInstance(); // Inicializa Firestore
        ultimosAgregadosContainer = findViewById(R.id.ultimos_agregados_container); // Obtiene el nuevo contenedor
        mainLoadingIndicator = findViewById(R.id.main_loading_indicator);

        requestQueue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(20);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });

        fetchFiguritasDestacadas(); // Llama al nuevo método para cargar figuritas desde Firestore


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
                tituloLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;
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

    // Metodo para sección de ultimos agregados con imagenes dinamicas

    private void fetchFiguritasDestacadas() {
        mainLoadingIndicator.setVisibility(View.VISIBLE); // Muestra el indicador de carga

        db.collection("figuritas") // Es el nombre de la coleccion donde se guardan mis figuritas en Firestore
                .limit(2) // Limita a 2 figuritas, para simular "últimos agregados"
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        mainLoadingIndicator.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            ultimosAgregadosContainer.removeAllViews();

                            LinearLayout currentRow = null; // Para organizar 2 figuritas por fila
                            int figuritaCount = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (figuritaCount >= 2) break; // Asegura que no se muestren más de 2

                                Figurita figurita = document.toObject(Figurita.class);

                                if (figuritaCount % 2 == 0) { // Si es una posición par (0, 2, 4...), crea una nueva fila
                                    currentRow = new LinearLayout(MainActivity.this);
                                    currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    ));
                                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                                    currentRow.setWeightSum(2);
                                    ultimosAgregadosContainer.addView(currentRow);
                                }

                                // Crear dinámicamente la CardView para cada figurita
                                CardView cardView = new CardView(MainActivity.this);
                                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                        0, // Ancho 0 para usar weight
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        1.0f // Peso de 1 para que ocupe la mitad del ancho
                                );
                                cardParams.setMargins(8, 8, 8, 8); // Margen alrededor de cada card
                                cardView.setLayoutParams(cardParams);
                                cardView.setRadius(8);
                                cardView.setCardElevation(4);

                                LinearLayout innerLayout = new LinearLayout(MainActivity.this);
                                innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                innerLayout.setOrientation(LinearLayout.VERTICAL);
                                innerLayout.setPadding(8, 8, 8, 8); // Padding interno de la card
                                innerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

                                // Título del Álbum
                                TextView albumTitleTextView = new TextView(MainActivity.this);
                                albumTitleTextView.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                albumTitleTextView.setText(figurita.getAlbumTitle());
                                albumTitleTextView.setTextSize(16);
                                albumTitleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
                                albumTitleTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_black));

                                // Imagen de la Figurita
                                ImageView figuritaImageView = new ImageView(MainActivity.this);
                                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                                        getResources().getDimensionPixelSize(R.dimen.figurita_image_width),
                                        getResources().getDimensionPixelSize(R.dimen.figurita_image_height)
                                );
                                figuritaImageView.setLayoutParams(imageParams);
                                figuritaImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                figuritaImageView.setAdjustViewBounds(true);
                                figuritaImageView.setPadding(0, 8, 0, 8);

                                // Cargar imagen con ImageLoader de Volley
                                imageLoader.get(figurita.getImageUrl(), new ImageLoader.ImageListener() {
                                    @Override
                                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                        if (response.getBitmap() != null) {
                                            figuritaImageView.setImageBitmap(response.getBitmap());
                                        } else {
                                            figuritaImageView.setImageResource(R.drawable.ic_store);
                                        }
                                    }

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        figuritaImageView.setImageResource(R.drawable.ic_store);
                                        Log.e("MainActivity", "Error loading image: " + error.getMessage());
                                    }
                                });

                                // Nombre de la Figurita
                                TextView figuritaNameTextView = new TextView(MainActivity.this);
                                figuritaNameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                figuritaNameTextView.setText(figurita.getFiguritaName());
                                figuritaNameTextView.setTextSize(14);
                                figuritaNameTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_maincard));
                                figuritaNameTextView.setPadding(0, 4, 0, 4);

                                // Precio de la Figurita
                                TextView priceTextView = new TextView(MainActivity.this);
                                priceTextView.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                String priceFormatted = String.format("Precio: $%.2f", figurita.getPrice());
                                priceTextView.setText(priceFormatted);
                                priceTextView.setTextSize(14);
                                priceTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_black));
                                priceTextView.setPadding(0, 4, 0, 8);

                                // Botón Comprar
                                Button comprarButton = new Button(MainActivity.this);
                                comprarButton.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                comprarButton.setText(getString(R.string.buttom_comprar_cat));
                                comprarButton.setBackgroundResource(R.drawable.rounded_button_background);
                                comprarButton.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.app_name));
                                comprarButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(MainActivity.this, getString(R.string.toast_added_to_cart, figurita.getFiguritaName()), Toast.LENGTH_SHORT).show();
                                    }
                                });

                                // Añadir vistas al layout interno de la card
                                innerLayout.addView(albumTitleTextView);
                                innerLayout.addView(figuritaImageView);
                                innerLayout.addView(figuritaNameTextView);
                                innerLayout.addView(priceTextView);
                                innerLayout.addView(comprarButton);

                                // Añadir el layout interno a la CardView
                                cardView.addView(innerLayout);

                                // Añadir la CardView a la fila actual
                                if (currentRow != null) {
                                    currentRow.addView(cardView);
                                }
                                figuritaCount++;
                            }

                            if (figuritaCount == 0) {
                                TextView noFiguritasText = new TextView(MainActivity.this);
                                noFiguritasText.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ));
                                noFiguritasText.setText(getString(R.string.no_figuritas_found_main));
                                noFiguritasText.setTextSize(18);
                                noFiguritasText.setGravity(Gravity.CENTER);
                                noFiguritasText.setPadding(16, 50, 16, 0);
                                ultimosAgregadosContainer.addView(noFiguritasText);
                            }

                        } else {
                            Log.w("MainActivity", "Error al cargar figuritas destacadas: ", task.getException());
                            Toast.makeText(MainActivity.this, getString(R.string.toast_figuritas_load_error_main) + ": " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

}