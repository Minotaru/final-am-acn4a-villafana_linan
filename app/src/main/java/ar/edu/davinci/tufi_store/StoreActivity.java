package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable; // Importado para el TextWatcher del campo de busqueda
import android.text.TextWatcher; // Importado para el TextWatcher del campo de busqueda
import android.util.Log; // Importado para Log (depuración)
import android.view.MenuItem;
import android.view.View;
import android.widget.Button; // Importado para los botones de las cards de figuritas
import android.widget.EditText; // Importado para el campo de búsqueda
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar; // Importado para el ProgressBar
import android.widget.TextView; // Importado para el TextView del título
import android.widget.Toast;

import androidx.annotation.NonNull; // Importado para @NonNull asi se evitan valores nulos dentro del proyecto
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat; // Importado para ContextCompat
import androidx.collection.LruCache; // Importado para LruCache (para ImageLoader)


import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.android.volley.VolleyError;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;
import java.util.List;


public class StoreActivity extends AppCompatActivity {

    private ImageView menuButton;
    private ImageView userButton;
    private LinearLayout bottomHome;
    private LinearLayout bottomCategories;
    private LinearLayout bottomLocations;
    private LinearLayout bottomStore;
    private LinearLayout bottomUsers;

    //Para Firebase FireStore
    private EditText searchEditText;
    private LinearLayout figuritasContainer;
    private ProgressBar storeLoadingIndicator;

    private FirebaseFirestore db; // Instancia de Firestore
    private List<Figurita> allFiguritas; // Lista para almacenar todas las figuritas
    private RequestQueue requestQueue; // Necesario para ImageLoader de Volley
    private ImageLoader imageLoader; // Necesario para ImageLoader de Volley

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance(); // Inicializa Firestore
        allFiguritas = new ArrayList<>(); // Inicializa la lista de figuritas

        searchEditText = findViewById(R.id.search_edit_text);
        figuritasContainer = findViewById(R.id.figuritas_container);
        storeLoadingIndicator = findViewById(R.id.store_loading_indicator);

        // Inicializar Volley RequestQueue y ImageLoader para cargar imágenes
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

        // Configurar listener para el campo de búsqueda
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* No se usa */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFiguritas(s.toString()); // Llama al método de filtrado
            }

            @Override
            public void afterTextChanged(Editable s) { /* No se usa */ }
        });

        // Llamar a la función para cargar las figuritas desde Firestore
        fetchFiguritasFromFirestore();

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

    // Metodo para utilizar Firestore en la tienda

    private void fetchFiguritasFromFirestore() {
        storeLoadingIndicator.setVisibility(View.VISIBLE); // Muestra el ProgressBar

        db.collection("figuritas") // Accede a la colección "figuritas"
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        storeLoadingIndicator.setVisibility(View.GONE); // Oculta el ProgressBar
                        if (task.isSuccessful()) {
                            allFiguritas.clear(); // Limpia la lista antes de añadir nuevas
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Mapea los datos del documento a un objeto Figurita
                                Figurita figurita = document.toObject(Figurita.class);
                                allFiguritas.add(figurita);
                                Log.d("StoreActivity", "Figurita cargada: " + figurita.getFiguritaName()); // Log para depuración
                            }
                            displayFiguritas(allFiguritas); // Muestra todas las figuritas inicialmente
                        } else {
                            Log.w("StoreActivity", "Error al cargar figuritas: ", task.getException());
                            Toast.makeText(StoreActivity.this, getString(R.string.toast_figuritas_load_error) + ": " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void filterFiguritas(String searchText) {
        List<Figurita> filteredList = new ArrayList<>();
        if (searchText.isEmpty()) {
            filteredList.addAll(allFiguritas); // Si el campo de búsqueda está vacío, muestra todas
        } else {
            for (Figurita figurita : allFiguritas) {
                // Filtra por el título del álbum (ignora mayúsculas/minúsculas)
                if (figurita.getAlbumTitle().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredList.add(figurita);
                }
            }
        }
        displayFiguritas(filteredList); // Muestra las figuritas filtradas
    }

    private void displayFiguritas(List<Figurita> figuritasToDisplay) {
        figuritasContainer.removeAllViews(); // Limpia el contenedor antes de añadir las cards

        if (figuritasToDisplay.isEmpty()) {
            TextView noResultsText = new TextView(this);
            noResultsText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            noResultsText.setText(getString(R.string.no_figuritas_found));
            noResultsText.setTextSize(18);
            noResultsText.setGravity(android.view.Gravity.CENTER);
            noResultsText.setPadding(16, 50, 16, 0);
            figuritasContainer.addView(noResultsText);
            return;
        }

        // Lógica para organizar las cards en pares (dos por fila)
        LinearLayout currentRow = null;
        for (int i = 0; i < figuritasToDisplay.size(); i++) {
            Figurita figurita = figuritasToDisplay.get(i);

            if (i % 2 == 0) { // Si es una posición par (0, 2, 4...), crea una nueva fila
                currentRow = new LinearLayout(this);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setWeightSum(2);
                figuritasContainer.addView(currentRow);
            }

            // Crear dinámicamente la CardView para cada figurita
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    0, // Ancho 0 para usar weight
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // Peso de 1 para que ocupe la mitad del ancho
            );
            cardParams.setMargins(8, 8, 8, 8); // Margen alrededor de cada card
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(8);
            cardView.setCardElevation(4);

            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setPadding(8, 8, 8, 8); // Padding interno de la card
            innerLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

            // Título del Álbum
            TextView albumTitleTextView = new TextView(this);
            albumTitleTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            albumTitleTextView.setText(figurita.getAlbumTitle());
            albumTitleTextView.setTextSize(16);
            albumTitleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            albumTitleTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));

            // Imagen de la Figurita
            ImageView figuritaImageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.figurita_image_width), // Define estas dimensiones
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
                        // Placeholder si la imagen no carga
                        figuritaImageView.setImageResource(R.drawable.ic_store);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    // Imagen de error si falla la carga
                    figuritaImageView.setImageResource(R.drawable.ic_store);
                    Log.e("StoreActivity", "Error loading image: " + error.getMessage());
                }
            });

            // Nombre de la Figurita
            TextView figuritaNameTextView = new TextView(this);
            figuritaNameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            figuritaNameTextView.setText(figurita.getFiguritaName());
            figuritaNameTextView.setTextSize(14);
            figuritaNameTextView.setTextColor(ContextCompat.getColor(this, R.color.text_maincard));
            figuritaNameTextView.setPadding(0, 4, 0, 4);

            // Precio de la Figurita
            TextView priceTextView = new TextView(this);
            priceTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            // Formatear el precio a dos decimales
            String priceFormatted = String.format("Precio: $%.2f", figurita.getPrice());
            priceTextView.setText(priceFormatted);
            priceTextView.setTextSize(14);
            priceTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            priceTextView.setPadding(0, 4, 0, 8);


            // Botón Comprar
            Button comprarButton = new Button(this);
            comprarButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            comprarButton.setText(getString(R.string.buttom_comprar_cat)); // Reutiliza el string "Comprar"
            comprarButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast.makeText(StoreActivity.this, getString(R.string.toast_added_to_cart, figurita.getFiguritaName()), Toast.LENGTH_SHORT).show();

                }
            });

            // Añadir vistas al layout interno de la card
            innerLayout.addView(albumTitleTextView);
            innerLayout.addView(figuritaImageView);
            innerLayout.addView(figuritaNameTextView);
            innerLayout.addView(priceTextView); // Añade el precio
            innerLayout.addView(comprarButton);

            // Añadir el layout interno a la CardView
            cardView.addView(innerLayout);

            // Añadir la CardView a la fila actual
            if (currentRow != null) {
                currentRow.addView(cardView);
            }
        }
    }

}

