package com.example.pruebaiv;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static String mqttHost = "valleyhyena796.cloud.shiftr.io:1883";
    private static String idUsuario = "AppAndroid";
    private static String Topico = "Mensaje";
    private static String User = "valleyhyena796";
    private static String Pass = "Z9NeVBXP2fUQcZpF";

    private ListView lista;
    private TextView textView;
    private EditText editTextMessage, txtNombreCliente, txtTelefonoCliente, txtDireccionEntrega, txtCantidadPedido, txtMensaje;
    private Button botonEnvio, botonEnvioMensaje;
    private Spinner spFrutosSecos;
    private FirebaseFirestore db;

    String[] TipodeFruto = {"Mani", "Nuez", "Pistacho"};

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de Firestore y componentes de la interfaz
        CargarListaFirestore();
        db = FirebaseFirestore.getInstance();
        txtNombreCliente = findViewById(R.id.txtNombreCliente);
        txtTelefonoCliente = findViewById(R.id.txtTelefonoCliente);
        txtDireccionEntrega = findViewById(R.id.txtDireccionEntrega);
        txtCantidadPedido = findViewById(R.id.txtCantidadPedido);
        txtMensaje = findViewById(R.id.txtMensaje);
        lista = findViewById(R.id.lista);

        // Configuración del Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TipodeFruto);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrutosSecos.setAdapter(adapter);

        // Configuración del cliente MQTT
        textView = findViewById(R.id.textView);
        editTextMessage = findViewById(R.id.txtMensaje);
        botonEnvio = findViewById(R.id.botonEnvioMensaje);

        try {
            mqttClient = new MqttClient(mqttHost, idUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());
            mqttClient.connect(options);

            Toast.makeText(this, "Aplicación Conectada al MQTT", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexión Perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payLoad = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payLoad));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega Completa");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

        // Configuración del botón de envío
        botonEnvio.setOnClickListener(view -> {
            String mensaje = editTextMessage.getText().toString();
            try {
                if (mqttClient != null && mqttClient.isConnected()) {
                    mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                    textView.append("\n -" + mensaje);
                    Toast.makeText(MainActivity.this, "Mensaje Enviado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
    }

    public void enviarDatosFirestore(View view) {
        String nombre = txtNombreCliente.getText().toString();
        String telefono = txtTelefonoCliente.getText().toString();
        String direccion = txtDireccionEntrega.getText().toString();
        String cantidad = txtCantidadPedido.getText().toString();

        Map<String, Object> fruto = new HashMap<>();
        fruto.put("nombre", nombre);
        fruto.put("telefono", telefono);
        fruto.put("direccion", direccion);
        fruto.put("cantidad", cantidad);

        db.collection("Frutos Secos")
                .document(nombre)
                .set(fruto)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Datos Enviados Correctamente", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    public void CargarLista(View view){
        CargarListaFirestore();
    }

    public void CargarListaFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("FrutosSecos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()){
                        List<String> listaFrutos = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()){
                            String Linea = "|| " + document.getString("Fruto") + " || "+
                                    document.getString("nombre");
                                    document.getString("cantidad");
                                    document.getString("direccion");
                             listaFrutos.add(Linea) ;

                        }
                        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                             MainActivity.this,
                             android.R.layout.simple_list_item_1,
                             listaFrutos
                        );
                        lista.setAdapter(adaptador);


                    }else {
                        Log.e("Tag", "Eroor al obtener firestore", task.getException());
                    }

                    }
                });
    }
}


