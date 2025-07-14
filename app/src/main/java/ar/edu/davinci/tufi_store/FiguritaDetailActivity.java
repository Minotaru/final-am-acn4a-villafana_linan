package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.LruCache;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import com.google.firebase.auth.FirebaseAuth;

public class FiguritaDetailActivity extends AppCompatActivity {

    // para los Intent
    public static final String EXTRA_ALBUM_TITLE = "extra_album_title";
    public static final String EXTRA_FIGURITA_NAME = "extra_figurita_name";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_PRICE = "extra_price";

    private TextView detailAlbumTitle;
    private ImageView detailFiguritaImage;
    private TextView detailFiguritaName;
    private TextView detailFiguritaPrice;
    private Button detailComprarButton;

    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    // Elementos del footer
    private ImageView backButton; // Botón de regreso en la barra superior
    private ImageView userButtonDetail;
    private LinearLayout bottomHomeDetail;
    private LinearLayout bottomCategoriesDetail;
    private LinearLayout bottomLocationsDetail;
    private LinearLayout bottomStoreDetail;
    private LinearLayout bottomUsersDetail;

    private FirebaseAuth mAuth; // Para Firebase Auth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_figurita_detail);

        mAuth = FirebaseAuth.getInstance(); // Inicializa Firebase Auth

        // Inicializar vistas de detalle
        detailAlbumTitle = findViewById(R.id.detail_album_title);
        detailFiguritaImage = findViewById(R.id.detail_figurita_image);
        detailFiguritaName = findViewById(R.id.detail_figurita_name);
        detailFiguritaPrice = findViewById(R.id.detail_figurita_price);
        detailComprarButton = findViewById(R.id.detail_comprar_button);

        // Inicializar Volley ImageLoader
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

        // Obtener datos del Intent (Pasaje de datos)
        if (getIntent().getExtras() != null) {
            String albumTitle = getIntent().getStringExtra(EXTRA_ALBUM_TITLE);
            String figuritaName = getIntent().getStringExtra(EXTRA_FIGURITA_NAME);
            String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
            double price = getIntent().getDoubleExtra(EXTRA_PRICE, 0.0); // 0.0 es valor por defecto

            detailAlbumTitle.setText(albumTitle);
            detailFiguritaName.setText(figuritaName);
            String priceFormatted = String.format("Precio: $%.2f", price);
            detailFiguritaPrice.setText(priceFormatted);

            // Cargar imagen con Volley ImageLoader
            imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (response.getBitmap() != null) {
                        detailFiguritaImage.setImageBitmap(response.getBitmap());
                    } else {
                        detailFiguritaImage.setImageResource(R.drawable.ic_store);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    detailFiguritaImage.setImageResource(R.drawable.ic_store);
                    Toast.makeText(FiguritaDetailActivity.this, getString(R.string.toast_image_load_error) + ": " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            // Listener para el botón "Comprar" en la pantalla de detalle
            detailComprarButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(FiguritaDetailActivity.this, getString(R.string.toast_added_to_cart, figuritaName), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(this, getString(R.string.toast_no_figurita_data), Toast.LENGTH_LONG).show();
            finish();
        }


        // Top Bar
        backButton = findViewById(R.id.back_button);
        userButtonDetail = findViewById(R.id.user_button_detail);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Simula el botón de atrás
            }
        });

        userButtonDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserPopupMenu(v);
            }
        });

        // Bottom Navigation (con IDs con sufijo '_detail')
        bottomHomeDetail = findViewById(R.id.bottom_home_detail);
        bottomCategoriesDetail = findViewById(R.id.bottom_categories_detail);
        bottomLocationsDetail = findViewById(R.id.bottom_locations_detail);
        bottomStoreDetail = findViewById(R.id.bottom_store_detail);
        bottomUsersDetail = findViewById(R.id.bottom_users_detail);

        bottomHomeDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FiguritaDetailActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomCategoriesDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FiguritaDetailActivity.this, StoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomLocationsDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FiguritaDetailActivity.this, LocationsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomStoreDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(FiguritaDetailActivity.this, "Botón de Carrito clicado en Detalle de Figurita", Toast.LENGTH_SHORT).show();
            }
        });

        bottomUsersDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FiguritaDetailActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    // Método para mostrar el menú emergente del usuario
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

    // Método para cerrar la sesión y redirigir a la pantalla de login
    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(FiguritaDetailActivity.this, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(FiguritaDetailActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}