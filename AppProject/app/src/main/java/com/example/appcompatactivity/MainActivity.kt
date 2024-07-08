package com.example.appcompatactivity

// Importações necessárias para componentes do Android e Compose
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
import java.util.Calendar

// Importações para permissões, acesso a dados e componentes do Android
import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

// Classe principal da atividade principal (MainActivity)
class MainActivity : AppCompatActivity() {

    // Declaração de variáveis de classe
    private lateinit var recyclerView: RecyclerView  // RecyclerView para exibir os registros
    private lateinit var numberAdapter: NumberAdapter  // Adaptador para a RecyclerView
    private val PERMISSIONS_REQUEST_CODE = 123 // Código para identificar a solicitação de permissão
    private lateinit var spinner: Spinner  // Spinner para selecionar o período de visualização
    private val periods = listOf("total", "year", "month", "week", "day")  // Lista de períodos de visualização
    private val callLogs = mutableMapOf<String, MutableMap<String, Int>>() // Mapa para armazenar os registros de chamadas
    private val contactNames = mutableMapOf<String, String>()  // Mapa para armazenar nomes de contatos associados aos números

    // Método chamado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // Define o layout da atividade como activity_main.xml

        spinner = findViewById(R.id.spinner)  // Referencia o Spinner do layout
        recyclerView = findViewById(R.id.recyclerView)  // Referencia a RecyclerView do layout
        recyclerView.layoutManager = LinearLayoutManager(this)  // Configura o layout da RecyclerView como linear

        val spinnerAdapter = CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, periods)  // Cria um adaptador personalizado para o Spinner com os períodos
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)  // Configura o layout do dropdown do Spinner
        spinner.adapter = spinnerAdapter  // Define o adaptador no Spinner

        // Configura o listener de seleção de item do Spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateRecyclerView(periods[position])  // Atualiza a RecyclerView com base no período selecionado
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Lista de permissões a serem solicitadas
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)  // Adiciona permissão de leitura de registros de chamadas
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)  // Adiciona permissão de leitura do estado do telefone
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)  // Adiciona permissão de leitura de contatos
        }

        // Se houver permissões a serem solicitadas, solicita-as
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            fetchCallLogs()  // Caso contrário, inicia o processo de obtenção dos registros de chamadas
        }
    }

    // Método chamado quando a resposta à solicitação de permissões é recebida
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                fetchCallLogs()  // Se todas as permissões foram concedidas, inicia a obtenção dos registros de chamadas
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()  // Caso contrário, exibe uma mensagem de erro
            }
        }
    }

    // Método para obter os registros de chamadas
    private fun fetchCallLogs() {
        Log.d("MainActivity", "Fetching contact names")  // Log indicando o início da obtenção de nomes de contatos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            fetchContactNames()  // Se a permissão de leitura de contatos estiver concedida, obtém os nomes dos contatos
        }

        // Consulta os registros de chamadas através de um cursor
        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,  // URI para acessar os registros de chamadas
            null,
            null,
            null,
            CallLog.Calls.DEFAULT_SORT_ORDER
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)  // Índice da coluna de números de telefone
            val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)  // Índice da coluna de datas das chamadas

            val currentTime = System.currentTimeMillis()  // Tempo atual em milissegundos
            val calendar = Calendar.getInstance()  // Instância do calendário

            // Itera sobre cada registro de chamada no cursor
            while (cursor.moveToNext()) {
                val number = normalizePhoneNumber(cursor.getString(numberIndex))  // Normaliza o número de telefone da chamada
                val date = cursor.getLong(dateIndex)  // Data da chamada em milissegundos

                calendar.timeInMillis = date
                val callYear = calendar.get(Calendar.YEAR)  // Ano da chamada
                val callMonth = calendar.get(Calendar.MONTH)  // Mês da chamada
                val callWeek = calendar.get(Calendar.WEEK_OF_YEAR)  // Semana do ano da chamada
                val callDay = calendar.get(Calendar.DAY_OF_YEAR)  // Dia do ano da chamada

                calendar.timeInMillis = currentTime
                val currentYear = calendar.get(Calendar.YEAR)  // Ano atual
                val currentMonth = calendar.get(Calendar.MONTH)  // Mês atual
                val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)  // Semana atual
                val currentDay = calendar.get(Calendar.DAY_OF_YEAR)  // Dia atual

                // Se o número não estiver presente nos logs de chamada, adiciona-o com valores padrão
                if (number !in callLogs) {
                    callLogs[number] = mutableMapOf(
                        "total" to 0,
                        "year" to 0,
                        "month" to 0,
                        "week" to 0,
                        "day" to 0
                    )
                }

                // Incrementa o contador de chamadas totais
                callLogs[number]!!["total"] = callLogs[number]!!.getOrDefault("total", 0) + 1

                // Incrementa os contadores de chamadas de acordo com o período atual
                if (callYear == currentYear) {
                    callLogs[number]!!["year"] = callLogs[number]!!.getOrDefault("year", 0) + 1
                    if (callMonth == currentMonth) {
                        callLogs[number]!!["month"] = callLogs[number]!!.getOrDefault("month", 0) + 1
                    }
                    if (callWeek == currentWeek) {
                        callLogs[number]!!["week"] = callLogs[number]!!.getOrDefault("week", 0) + 1
                    }
                    if (callDay == currentDay) {
                        callLogs[number]!!["day"] = callLogs[number]!!.getOrDefault("day", 0) + 1
                    }
                }
            }
        }

        updateRecyclerView("total")  // Atualiza a RecyclerView inicialmente com o período "total"
    }

    // Método para obter nomes de contatos associados aos números de telefone
    private fun fetchContactNames() {
        val resolver: ContentResolver = contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            // Itera sobre cada entrada de contato no cursor
            while (cursor.moveToNext()) {
                val number = normalizePhoneNumber(cursor.getString(numberIndex))  // Normaliza o número de telefone do contato
                val name = cursor.getString(nameIndex)  // Nome do contato

                contactNames[number] = name  // Associa o número ao nome do contato no mapa
            }
        }
        Log.d("MainActivity", "Contact names fetched: ${contactNames.size}")  // Log indica que os nomes de contatos foram obtidos
    }

    // Método para normalizar números de telefone
    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^\\d]"), "")  // Remove todos os caracteres não numéricos
    }

    // Método para atualizar a RecyclerView com base no período selecionado
    private fun updateRecyclerView(period: String) {
        Log.d("MainActivity", "Updating RecyclerView for period: $period")  // Log indica que a RecyclerView está sendo atualizada para o período específico

        // Filtra os registros de chamadas para o período especificado e remove aqueles com contagem zero
        val filteredLogs = callLogs.mapValues { it.value[period] ?: 0 }.filter { it.value > 0 }

        // Ordena os registros filtrados por contagem de chamadas em ordem decrescente
        val sortedLogs = filteredLogs.toList().sortedByDescending { it.second }.toMap()

        // Cria um mapa com chaves como nomes de contatos ou números de telefone, e valores como contagens de chamadas
        val displayLogs = sortedLogs.mapKeys { contactNames[it.key] ?: it.key }

        // Logs para depuração
        Log.d("MainActivity", "Filtered logs: $filteredLogs")
        Log.d("MainActivity", "Sorted logs: $sortedLogs")
        Log.d("MainActivity", "Display logs: $displayLogs")

        // Cria um novo adaptador com os registros de chamadas para exibir na RecyclerView
        numberAdapter = NumberAdapter(displayLogs)
        recyclerView.adapter = numberAdapter  // Define o adaptador na RecyclerView para exibir os dados
    }
}