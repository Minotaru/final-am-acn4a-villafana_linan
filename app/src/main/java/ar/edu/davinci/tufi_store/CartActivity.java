package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.collection.LruCache;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration; // para el listener en tiempo real

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale; // para formatear el dinero (peso argentino)
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private ImageView backButton;
    private ImageView userButtonCart;
    private LinearLayout cartItemsContainer;
    private TextView cartTotalText;
    private Button payButton;
    private ProgressBar cartLoadingIndicator; // Para indicar carga en el carrito

    private LinearLayout bottomHomeCart;
    private LinearLayout bottomCategoriesCart;
    private LinearLayout bottomLocationsCart;
    private LinearLayout bottomStoreCart;
    private LinearLayout bottomUsersCart;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private ListenerRegistration cartListener; // Para el listener en tiempo real de Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

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

        cartItemsContainer = findViewById(R.id.cart_items_container);
        cartTotalText = findViewById(R.id.cart_total_text);
        payButton = findViewById(R.id.pay_button);
        cartLoadingIndicator = findViewById(R.id.cart_loading_indicator);

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

        // Configurar listener para el botón "Pagar"
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulatePaymentAndClearCart();
            }
        });

        // Inicializar y configurar listeners para las barras
        backButton = findViewById(R.id.back_button);
        userButtonCart = findViewById(R.id.user_button_cart);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        userButtonCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserPopupMenu(v);
            }
        });

        bottomHomeCart = findViewById(R.id.bottom_home_cart);
        bottomCategoriesCart = findViewById(R.id.bottom_categories_cart);
        bottomLocationsCart = findViewById(R.id.bottom_locations_cart);
        bottomStoreCart = findViewById(R.id.bottom_store_cart);
        bottomUsersCart = findViewById(R.id.bottom_users_cart);

        bottomHomeCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomCategoriesCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartActivity.this, StoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomLocationsCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartActivity.this, LocationsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomStoreCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CartActivity.this, "Ya estás en el Carrito", Toast.LENGTH_SHORT).show();
            }
        });

        bottomUsersCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CartActivity.this, "Botón de Perfil clicado en Carrito", Toast.LENGTH_SHORT).show();
            }
        });


        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            final DocumentReference cartRef = db.collection("carritos").document(currentUser.getUid());
            cartLoadingIndicator.setVisibility(View.VISIBLE);

            cartListener = cartRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@androidx.annotation.Nullable DocumentSnapshot snapshot,
                                    @androidx.annotation.Nullable FirebaseFirestoreException e) {
                    cartLoadingIndicator.setVisibility(View.GONE);
                    if (e != null) {
                        Log.w("CartActivity", "Listen failed.", e);
                        Toast.makeText(CartActivity.this, getString(R.string.toast_cart_load_error) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists() && snapshot.contains("items")) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) snapshot.get("items");
                        if (items != null) {
                            displayCartItems(items);
                        } else {
                            displayEmptyCart();
                        }
                    } else {
                        displayEmptyCart();
                    }
                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cartListener != null) {
            cartListener.remove();
        }
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
        Toast.makeText(CartActivity.this, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(CartActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Métodos para mostrar y gestionar el carrito
    private void displayCartItems(List<Map<String, Object>> cartItems) {
        cartItemsContainer.removeAllViews();
        double totalAmount = 0.0;

        if (cartItems.isEmpty()) {
            displayEmptyCart();
            return;
        }

        for (Map<String, Object> item : cartItems) {
            String figuritaId = (String) item.get("figuritaId");
            String albumTitle = (String) item.get("albumTitle");
            String figuritaName = (String) item.get("figuritaName");
            String imageUrl = (String) item.get("imageUrl");

            double price = ((Number) item.get("price")).doubleValue();
            long quantity = ((Number) item.get("quantity")).longValue();

            totalAmount += (price * quantity);

            // Crear la card para cada ítem del carrito (similar a figuritas en StoreActivity)
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16); // Margen inferior entre cards
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(8);
            cardView.setCardElevation(4);

            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            innerLayout.setOrientation(LinearLayout.HORIZONTAL); // Horizontal para imagen y detalles
            innerLayout.setPadding(16, 16, 16, 16);
            innerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL); // Centrar verticalmente

            ImageView figuritaImageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.figurita_image_width),
                    getResources().getDimensionPixelSize(R.dimen.figurita_image_height)
            );
            imageParams.setMarginEnd(16); // Margen a la derecha de la imagen
            figuritaImageView.setLayoutParams(imageParams);
            figuritaImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            figuritaImageView.setAdjustViewBounds(true);

            imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
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
                    Log.e("CartActivity", "Error loading image: " + error.getMessage());
                }
            });

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0, // Ocupa el espacio restante
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // Peso para expandirse
            ));
            textLayout.setOrientation(LinearLayout.VERTICAL);

            TextView albumTitleTextView = new TextView(this);
            albumTitleTextView.setText(albumTitle);
            albumTitleTextView.setTextSize(16);
            albumTitleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            albumTitleTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            textLayout.addView(albumTitleTextView);

            TextView figuritaNameTextView = new TextView(this);
            figuritaNameTextView.setText(figuritaName);
            figuritaNameTextView.setTextSize(14);
            figuritaNameTextView.setTextColor(ContextCompat.getColor(this, R.color.text_maincard));
            textLayout.addView(figuritaNameTextView);

            TextView itemPriceTextView = new TextView(this);
            itemPriceTextView.setText(String.format(Locale.getDefault(), "Unitario: $%.2f", price));
            itemPriceTextView.setTextSize(14);
            itemPriceTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            textLayout.addView(itemPriceTextView);

            TextView quantityTextView = new TextView(this);
            quantityTextView.setText(String.format(Locale.getDefault(), "Cantidad: %d", quantity));
            quantityTextView.setTextSize(14);
            quantityTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            textLayout.addView(quantityTextView);

            TextView subtotalTextView = new TextView(this);
            subtotalTextView.setText(String.format(Locale.getDefault(), "Subtotal: $%.2f", (price * quantity)));
            subtotalTextView.setTextSize(16);
            subtotalTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            subtotalTextView.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            textLayout.addView(subtotalTextView);

            innerLayout.addView(figuritaImageView);
            innerLayout.addView(textLayout);

            cardView.addView(innerLayout);
            cartItemsContainer.addView(cardView);
        }

        cartTotalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", totalAmount));
    }

    private void displayEmptyCart() {
        cartItemsContainer.removeAllViews();
        TextView emptyCartText = new TextView(this);
        emptyCartText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        emptyCartText.setText(getString(R.string.cart_empty_message));
        emptyCartText.setTextSize(18);
        emptyCartText.setGravity(android.view.Gravity.CENTER);
        emptyCartText.setPadding(16, 50, 16, 0);
        cartItemsContainer.addView(emptyCartText);
        cartTotalText.setText(String.format(Locale.getDefault(), "Total: $%.2f", 0.0));
    }

    private void simulatePaymentAndClearCart() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference cartRef = db.collection("carritos").document(user.getUid());

        // Para vaciar el carrito, actualizamos el campo 'items' con una lista vacía

        cartRef.update("items", new ArrayList<>()) // Establece el array de ítems a vacío
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(CartActivity.this, getString(R.string.toast_payment_success), Toast.LENGTH_LONG).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CartActivity.this, getString(R.string.toast_payment_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("CartActivity", "Error al simular pago y vaciar carrito: ", e);
                    }
                });
    }

}