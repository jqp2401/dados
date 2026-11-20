package com.example.dados

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.dados.databinding.ActivityMainBinding
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val handler = Handler(Looper.getMainLooper())

    private var objetivo = 0
    private var lanzando = false
    private var finalizado = false
    private var scheduler: ScheduledExecutorService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initEvent()
    }

    private fun initEvent() {
        binding.btnKubilete.setOnClickListener {
            iniciarTirada()
        }

        binding.etNumero.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                iniciarTirada()
                true
            } else {
                false
            }
        }

        binding.etNumero.doAfterTextChanged {
            binding.etNumero.error = null
        }
    }

    private fun iniciarTirada() {
        if (lanzando) return

        val numero = binding.etNumero.text.toString().toIntOrNull()

        if (numero == null || numero !in 3..18) {
            binding.etNumero.error = getString(R.string.error_numero)
            Toast.makeText(this, R.string.error_numero, Toast.LENGTH_SHORT).show()
            return
        }

        objetivo = numero
        lanzando = true
        finalizado = false

        binding.etNumero.isEnabled = false
        binding.btnKubilete.isEnabled = false
        binding.btnKubilete.alpha = 0.5f

        binding.txtResultado.visibility = View.VISIBLE
        binding.txtResultado.text = getString(R.string.resultado_inicial)

        ocultarTeclado()
        scheduleRun()
    }

    private fun scheduleRun() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        scheduler = executor
        val msc = 1000L

        for (i in 1..5) {
            executor.schedule(
                {
                    val dados = IntArray(3) { Random.nextInt(1, 7) }

                    handler.post {
                        if (finalizado) return@post

                        mostrarDados(dados)
                        val suma = dados.sum()
                        binding.txtResultado.text = suma.toString()

                        if (suma == objetivo) {
                            finalizar(true, suma)
                        } else if (i == 5) {
                            finalizar(false, suma)
                        }
                    }
                },
                msc * i,
                TimeUnit.MILLISECONDS
            )
        }

        executor.shutdown()
    }

    private fun mostrarDados(dados: IntArray) {
        val imagenes = arrayOf(
            binding.imgDado1,
            binding.imgDado2,
            binding.imgDado3
        )

        for (i in dados.indices) {
            selectView(imagenes[i], dados[i])
        }
    }

    private fun selectView(imgV: ImageView, v: Int) {
        when (v) {
            1 -> imgV.setImageResource(R.drawable.dado1)
            2 -> imgV.setImageResource(R.drawable.dado2)
            3 -> imgV.setImageResource(R.drawable.dado3)
            4 -> imgV.setImageResource(R.drawable.dado4)
            5 -> imgV.setImageResource(R.drawable.dado5)
            6 -> imgV.setImageResource(R.drawable.dado6)
        }
    }

    private fun finalizar(acierto: Boolean, suma: Int) {
        if (finalizado) return

        finalizado = true
        lanzando = false

        scheduler?.shutdownNow()
        scheduler = null

        binding.etNumero.isEnabled = true
        binding.btnKubilete.isEnabled = true
        binding.btnKubilete.alpha = 1f

        if (acierto) {
            AlertDialog.Builder(this)
                .setTitle(R.string.acierto_titulo)
                .setMessage(getString(R.string.acierto_mensaje, objetivo, suma))
                .setPositiveButton(R.string.aceptar, null)
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(
                this,
                getString(R.string.fallo_mensaje, suma),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etNumero.windowToken, 0)
        binding.etNumero.clearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduler?.shutdownNow()
        scheduler = null
        handler.removeCallbacksAndMessages(null)
    }
}
