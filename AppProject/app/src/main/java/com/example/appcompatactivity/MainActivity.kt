package com.example.appcompatactivity


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.appcompatactivity.ui.theme.AppCompatActivityTheme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var numberAdapter: NumberAdapter
    private val PERMISSIONS_REQUEST_CODE = 123 // identificar solicitação de permissão

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) { // verifica se as permissões READ_CALL_LOG e READ_PHONE_STATE foram concedidas
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE), PERMISSIONS_REQUEST_CODE) // se alguma delas não foi concedida, ActivityCompat.requestPermissions é chamada p solicitar as permissões
            } else {
                fetchCallLogs() // se as permissões já foram concedidas, fetchCallLogs() é chamada para acessar os registros de chamadas
            }
        } else {
            fetchCallLogs()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // PackageManager.PERMISSION_GRANTED indica que a permissão foi concedida!
        if (requestCode == PERMISSIONS_REQUEST_CODE) { // se o requestCode corresponde a PERMISSIONS_REQUEST_CODE
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) && // temos que checar se as duas permissões foram concedidas (READ_CALL_LOG e READ_PHONE_STATE)
                (grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED)) { // grantResults armazena os resultados das duas solicitações (grandResults[0] o da primeira e gradResults[1] o da segunda)
                fetchCallLogs()
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCallLogs() {
        val callLogs = mutableMapOf<String, Int>() // map que armazena números de telefone e a frequência de chamadas

        val cursor: Cursor? = contentResolver.query( // usado para consultar os registros de chamadas no dispositivo
            CallLog.Calls.CONTENT_URI, // CallLog.Calls.CONTENT_URI é a URI que especifica a tabela de registros de chamadas
            null,
            null,
            null,
            CallLog.Calls.DEFAULT_SORT_ORDER // ordem dos resultados: por data/hora de chamada
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            while (cursor.moveToNext()) { //itera sobre cada registro de chamada no cursor, armazenando a frequência no map
                val number = cursor.getString(numberIndex)
                callLogs[number] = callLogs.getOrDefault(number, 0) + 1
            }
        }

       // callLogs.forEach { (number, frequency) ->
          //  Log.d("CallLogs", "Número: $number, Frequência: $frequency")
       // }
        numberAdapter = NumberAdapter(callLogs) // cria um NumberAdapter com os dados para exibir na RecyclerView
        recyclerView.adapter = numberAdapter // define o adaptador criado para a RecyclerView, para que os dados sejam exibidos conforme configurado no adaptador
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppCompatActivityTheme {
        Greeting("Android")
    }
}