package ar.edu.davinci.tufi_store;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // para ordenar
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat; //para formatear la fecha
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderHistoryActivity extends AppCompatActivity {

    private ImageView backButton;
    private ImageView userButtonHistory;
    private LinearLayout orderHistoryContainer;
    private ProgressBar historyLoadingIndicator;

    private LinearLayout bottomHomeHistory;
    private LinearLayout bottomCategoriesHistory;
    private LinearLayout bottomLocationsHistory;
    private LinearLayout bottomStoreHistory;
    private LinearLayout bottomUsersHistory;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

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

        orderHistoryContainer = findViewById(R.id.order_history_container);
        historyLoadingIndicator = findViewById(R.id.history_loading_indicator);

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

        // Inicializar y configurar listeners para las barras
        backButton = findViewById(R.id.back_button);
        userButtonHistory = findViewById(R.id.user_button_history);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        userButtonHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserPopupMenu(v);
            }
        });

        bottomHomeHistory = findViewById(R.id.bottom_home_history);
        bottomCategoriesHistory = findViewById(R.id.bottom_categories_history);
        bottomLocationsHistory = findViewById(R.id.bottom_locations_history);
        bottomStoreHistory = findViewById(R.id.bottom_store_history);
        bottomUsersHistory = findViewById(R.id.bottom_users_history);

        bottomHomeHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderHistoryActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomCategoriesHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderHistoryActivity.this, StoreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomLocationsHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderHistoryActivity.this, LocationsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        bottomStoreHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderHistoryActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });

        bottomUsersHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderHistoryActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        fetchOrderHistory(); // Carga el historial de compras
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
        Toast.makeText(OrderHistoryActivity.this, getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(OrderHistoryActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Método para cargar el historial de compras desde Firestore
    private void fetchOrderHistory() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.toast_login_required), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        historyLoadingIndicator.setVisibility(View.VISIBLE);

        db.collection("historial_compras")
                .whereEqualTo("userId", user.getUid()) // Filtra por el usuario actual
                .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena por fecha descendente
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        historyLoadingIndicator.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            displayOrderHistory(task.getResult().getDocuments());
                        } else {
                            Log.w("OrderHistoryActivity", "Error al cargar historial de compras: ", task.getException());
                            Toast.makeText(OrderHistoryActivity.this, getString(R.string.toast_history_load_error) + ": " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Método para mostrar el historial de compras
    private void displayOrderHistory(List<DocumentSnapshot> orderDocuments) {
        orderHistoryContainer.removeAllViews(); // Limpia el contenedor

        if (orderDocuments.isEmpty()) {
            TextView noHistoryText = new TextView(this);
            noHistoryText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            noHistoryText.setText(getString(R.string.no_orders_found));
            noHistoryText.setTextSize(18);
            noHistoryText.setGravity(android.view.Gravity.CENTER);
            noHistoryText.setPadding(16, 50, 16, 0);
            orderHistoryContainer.addView(noHistoryText);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (DocumentSnapshot document : orderDocuments) {
            // Crea una CardView para cada orden (compra completa)
            CardView orderCard = new CardView(this);
            LinearLayout.LayoutParams orderCardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            orderCardParams.setMargins(0, 0, 0, 24); // Margen inferior entre órdenes
            orderCard.setLayoutParams(orderCardParams);
            orderCard.setRadius(12);
            orderCard.setCardElevation(6);

            LinearLayout orderLayout = new LinearLayout(this);
            orderLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            orderLayout.setOrientation(LinearLayout.VERTICAL);
            orderLayout.setPadding(16, 16, 16, 16);

            // Detalles de la Orden: ID y Fecha
            TextView orderIdText = new TextView(this);
            orderIdText.setText(String.format(getString(R.string.order_id_format), document.getId()));
            orderIdText.setTextSize(16);
            orderIdText.setTypeface(null, android.graphics.Typeface.BOLD);
            orderIdText.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            orderLayout.addView(orderIdText);

            TextView orderDateText = new TextView(this);
            Date timestamp = document.getDate("timestamp"); // Obtiene el timestamp como Date
            if (timestamp != null) {
                orderDateText.setText(String.format(getString(R.string.order_date_format), dateFormat.format(timestamp)));
            } else {
                orderDateText.setText(getString(R.string.order_date_unavailable));
            }
            orderDateText.setTextSize(14);
            orderDateText.setTextColor(ContextCompat.getColor(this, R.color.text_maincard));
            orderLayout.addView(orderDateText);

            // Contenedor para los ítems dentro de esta orden
            LinearLayout itemsLayout = new LinearLayout(this);
            itemsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            itemsLayout.setOrientation(LinearLayout.VERTICAL);
            itemsLayout.setPadding(0, 8, 0, 0);
            orderLayout.addView(itemsLayout);

            List<Map<String, Object>> items = (List<Map<String, Object>>) document.get("items");
            if (items != null && !items.isEmpty()) {
                for (Map<String, Object> item : items) {
                    String figuritaName = (String) item.get("figuritaName");
                    long quantity = ((Number) item.get("quantity")).longValue();
                    double price = ((Number) item.get("price")).doubleValue();

                    TextView itemText = new TextView(this);
                    itemText.setText(String.format(getString(R.string.order_item_format), quantity, figuritaName, (price * quantity)));
                    itemText.setTextSize(14);
                    itemText.setTextColor(ContextCompat.getColor(this, R.color.text_black));
                    itemText.setPadding(0, 4, 0, 0);
                    itemsLayout.addView(itemText);
                }
            } else {
                TextView noItemsText = new TextView(this);
                noItemsText.setText(getString(R.string.order_no_items));
                noItemsText.setTextSize(14);
                noItemsText.setTextColor(ContextCompat.getColor(this, R.color.text_maincard));
                itemsLayout.addView(noItemsText);
            }

            // Total de la Orden
            TextView orderTotalText = new TextView(this);
            double totalAmount = ((Number) document.get("totalAmount")).doubleValue();
            orderTotalText.setText(String.format(getString(R.string.order_total_format), totalAmount));
            orderTotalText.setTextSize(18);
            orderTotalText.setTypeface(null, android.graphics.Typeface.BOLD);
            orderTotalText.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            orderTotalText.setPadding(0, 16, 0, 0);
            orderLayout.addView(orderTotalText);

            orderCard.addView(orderLayout);
            orderHistoryContainer.addView(orderCard);
        }
    }
}