package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.graphics.Bitmap; // Importación necesaria para Bitmap
import android.os.Bundle;
import android.view.MenuItem; // Importado para el PopupMenu
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu; // Importado para el PopupMenu
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Importación necesaria para CardView
import androidx.collection.LruCache; // Importación necesaria para LruCache
import androidx.core.content.ContextCompat; // Importación necesaria para ContextCompat

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader; // Importación necesaria para ImageLoader
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class LocationsActivity extends AppCompatActivity {

    private LinearLayout locationsContainer;
    private ProgressBar loadingIndicator;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader; // Instancia de ImageLoader

    private ImageView menuButton;
    private ImageView userButton;
    private LinearLayout bottomHome;
    private LinearLayout bottomCategories;
    private LinearLayout bottomLocations;
    private LinearLayout bottomStore;
    private LinearLayout bottomUsers;

    // Para Firebase Auth (LogOut)
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

//Reemplazar con la URL real del archivo JSON
    private static final String JSON_URL = "https://raw.githubusercontent.com/DeniseDesu/parcial-1-am-acn4a-villafana_linan-villafana_linan/refs/heads/main/informe/Repositorio%20-%20Volley/locations.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        //Inicializa Firebase Auth para el logout
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        locationsContainer = findViewById(R.id.locations_container);
        loadingIndicator = findViewById(R.id.loading_indicator);
        requestQueue = Volley.newRequestQueue(this); // Inicializa la cola de Volley

        // Inicializa ImageLoader con una caché de Bitmap
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(20); // Caché de 20 imágenes

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });

        //INICIALIZAR Y CONFIGURAR LISTENERS DE BARRAS
        // Top Bar
        menuButton = findViewById(R.id.menu_button);
        userButton = findViewById(R.id.user_button);

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LocationsActivity.this, "Botón de menú clicado en Sucursales", Toast.LENGTH_SHORT).show();

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
                Intent intent = new Intent(LocationsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish(); // Finaliza LocationsActivity para evitar duplicados en la pila si Home es el destino principal.
            }
        });

        // Listener para bottom nav: Tienda
        bottomCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationsActivity.this, StoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
           }
        });

        //Listener para bottom nav: Sucursales
        bottomLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LocationsActivity.this, "Ya estás en Sucursales", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para bottom nav: Carrito
        bottomStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationsActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });

        // Listener para bottom nav: Perfil
        bottomUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LocationsActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        fetchLocations(); // Llama al método para obtener los datos de las sucursales
    }

    // Método para mostrar el menú emergente del usuario (copiado de MainActivity)
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

    // Método para cerrar la sesión y redirigir a la pantalla de login (copiado de MainActivity)
    private void performLogout() {
        mAuth.signOut(); // Cierra la sesión de Firebase
        Toast.makeText(LocationsActivity.this, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LocationsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchLocations() {
        loadingIndicator.setVisibility(View.VISIBLE); // Muestra el indicador de carga

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                JSON_URL,
                null, // No necesitamos un cuerpo de solicitud para GET
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        loadingIndicator.setVisibility(View.GONE); // Oculta el indicador
                        parseLocations(response); // Procesa la respuesta JSON
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loadingIndicator.setVisibility(View.GONE); // Oculta el indicador
                        Toast.makeText(LocationsActivity.this, getString(R.string.toast_locations_load_error) + ": " + error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace(); // Para depuración
                    }
                }
        );

        requestQueue.add(jsonArrayRequest); // Añade la solicitud a la cola
    }

    private void parseLocations(JSONArray locationsArray) {
        if (locationsArray == null || locationsArray.length() == 0) {
            Toast.makeText(this, getString(R.string.toast_no_locations_found), Toast.LENGTH_SHORT).show();
            return;
        }

        locationsContainer.removeAllViews(); // Limpia cualquier vista previa

        for (int i = 0; i < locationsArray.length(); i++) {
            try {
                JSONObject locationObject = locationsArray.getJSONObject(i);

                String imageUrl = locationObject.getString("imageUrl");
                String address = locationObject.getString("address");
                String phone = locationObject.getString("phone");
                String hours = locationObject.getString("hours");

                // Crear dinámicamente la CardView para cada sucursal
                CardView cardView = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 16); // Margen inferior entre cards
                cardView.setLayoutParams(cardParams);
                cardView.setRadius(8); // Radio para las esquinas redondeadas
                cardView.setCardElevation(4); // Elevación de la sombra

                LinearLayout innerLayout = new LinearLayout(this);
                innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                innerLayout.setOrientation(LinearLayout.VERTICAL);
                innerLayout.setPadding(16, 16, 16, 16); // Padding interno de la card
                innerLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL); // Centrar contenido

                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                        getResources().getDimensionPixelSize(R.dimen.location_image_width), // Define estas dimensiones en dimens.xml
                        getResources().getDimensionPixelSize(R.dimen.location_image_height)
                );
                imageView.setLayoutParams(imageParams);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Ajuste de la imagen
                imageView.setAdjustViewBounds(true);

                // Cargar imagen con ImageLoader de Volley
                imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                        if (response.getBitmap() != null) {
                            imageView.setImageBitmap(response.getBitmap());
                        } else {
                            // Si no hay imagen, o en caso de error, usa un placeholder
                            imageView.setImageResource(R.drawable.ic_store);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // En caso de error al cargar la imagen, usa un drawable de error
                        imageView.setImageResource(R.drawable.ic_store);
                        error.printStackTrace(); // Para depuración
                    }
                });

                imageView.setContentDescription(getString(R.string.location_image_description, address)); // Accesibilidad

                TextView addressTextView = new TextView(this);
                addressTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                addressTextView.setText(address);
                addressTextView.setTextSize(16);
                addressTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));
                addressTextView.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                addressTextView.setPadding(0, 8, 0, 0);

                TextView phoneTextView = new TextView(this);
                phoneTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                phoneTextView.setText(phone);
                phoneTextView.setTextSize(14);
                phoneTextView.setTextColor(ContextCompat.getColor(this, R.color.text_maincard));
                phoneTextView.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                phoneTextView.setPadding(0, 4, 0, 0);

                TextView hoursTextView = new TextView(this);
                hoursTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                hoursTextView.setText(hours);
                hoursTextView.setTextSize(14);
                hoursTextView.setTextColor(ContextCompat.getColor(this, R.color.text_maincard));
                hoursTextView.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                hoursTextView.setPadding(0, 4, 0, 0);


                // Añadir vistas al layout interno de la card
                innerLayout.addView(imageView);
                innerLayout.addView(addressTextView);
                innerLayout.addView(phoneTextView);
                innerLayout.addView(hoursTextView);

                // Añadir el layout interno a la CardView
                cardView.addView(innerLayout);

                // Añadir la CardView al contenedor principal
                locationsContainer.addView(cardView);

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.toast_json_parse_error) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}