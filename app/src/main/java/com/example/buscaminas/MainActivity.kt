package com.example.buscaminas

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.random.Random
import android.view.Gravity

class MainActivity : ComponentActivity() {
    private lateinit var tiempoTextView: TextView // Vista que muestra el temporizador
    private lateinit var mensajeDerrotaTextView: TextView // Vista que muestra el mensaje de derrota
    private var segundos = 0 // Contador de segundos para el temporizador
    private var corriendo = false // Indica si el temporizador está corriendo
    private val manejador = Handler(Looper.getMainLooper()) // Manejador para controlar la ejecución del temporizador

    private var primerClick = true // Indica si es el primer clic del usuario en el tablero
    private var tamanoTablero = 8 // Tamaño del tablero (número de filas y columnas)
    private var cantidadMinas = 10 // Número de minas en el tablero
    private var posicionesMinas = mutableSetOf<Pair<Int, Int>>() // Conjunto que guarda las posiciones de las minas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos las vistas del temporizador y el mensaje de derrota
        tiempoTextView = findViewById(R.id.time)
        mensajeDerrotaTextView = findViewById(R.id.lossMessage)
        val botonReiniciar = findViewById<Button>(R.id.restartButton)

        // Configuración del botón de reinicio
        botonReiniciar.setOnClickListener {
            reiniciarJuego()
        }

        val spinnerDificultad = findViewById<Spinner>(R.id.spinId)
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)

        // Opciones de dificultad del juego
        val dificultad = arrayOf("Facil", "Medio", "Dificil")
        val arrayAdp = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, dificultad)
        spinnerDificultad.adapter = arrayAdp

        spinnerDificultad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                reiniciarTemporizador()
                iniciarTemporizador()

                // Ajustar el tamaño del tablero y la cantidad de minas según la dificultad seleccionada
                when (dificultad[position]) {
                    "Facil" -> {
                        tamanoTablero = 8
                        cantidadMinas = 10
                    }
                    "Medio" -> {
                        tamanoTablero = 16
                        cantidadMinas = 40
                    }
                    "Dificil" -> {
                        tamanoTablero = 24
                        cantidadMinas = 99
                    }
                }

                primerClick = true // Reiniciar la bandera del primer clic
                dibujarTablero(gridLayout, tamanoTablero)
                mensajeDerrotaTextView.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        iniciarTemporizador() // Iniciar el temporizador desde el comienzo
    }

    // Método que dibuja el tablero con el tamaño seleccionado
    private fun dibujarTablero(gridLayout: GridLayout, tamano: Int) {
        gridLayout.removeAllViews()
        gridLayout.columnCount = tamano
        gridLayout.rowCount = tamano

        gridLayout.post {
            val anchoTablero = gridLayout.width
            val altoTablero = gridLayout.height
            val tamanoBoton = Math.min(anchoTablero, altoTablero) / tamano

            for (i in 0 until tamano) {
                for (j in 0 until tamano) {
                    val boton = Button(this)
                    boton.setBackgroundResource(R.drawable.button_border)

                    // Ajustar el tamaño del texto del botón según la dificultad
                    val tamanoTexto = when (tamano) {
                        8 -> 24f // Fácil
                        16 -> 18f // Medio
                        24 -> 14f // Difícil
                        else -> 20f
                    }
                    boton.textSize = tamanoTexto

                    // Eliminar el padding para que el texto ocupe todo el botón
                    boton.setPadding(0, 0, 0, 0)
                    boton.gravity = Gravity.CENTER // Centrar el texto en el botón
                    boton.minHeight = 0
                    boton.minimumHeight = 0
                    boton.minWidth = 0
                    boton.minimumWidth = 0

                    // Color del texto
                    boton.setTextColor(resources.getColor(R.color.black))

                    // Parámetros de layout para ajustar el tamaño del botón
                    boton.layoutParams = GridLayout.LayoutParams().apply {
                        width = tamanoBoton
                        height = tamanoBoton
                        columnSpec = GridLayout.spec(j)
                        rowSpec = GridLayout.spec(i)
                    }

                    // Lógica para voltear las celdas cuando el usuario hace clic
                    boton.setOnClickListener {
                        if (primerClick) {
                            primerClick = false
                            generarMinas(tamano, cantidadMinas, i, j)
                        }
                        voltearCelda(boton, i, j, gridLayout)
                    }

                    gridLayout.addView(boton) // Añadir el botón al tablero
                }
            }
        }
    }

    // Generar posiciones aleatorias para las minas, evitando la celda del primer clic
    private fun generarMinas(tamano: Int, cantidadMinas: Int, filaClicada: Int, columnaClicada: Int) {
        posicionesMinas.clear()
        while (posicionesMinas.size < cantidadMinas) {
            val fila = Random.nextInt(tamano)
            val columna = Random.nextInt(tamano)

            if (fila != filaClicada || columna != columnaClicada) {
                posicionesMinas.add(Pair(fila, columna))
            }
        }
    }

    // Contar el número de minas adyacentes a una celda
    private fun contarMinasAdyacentes(fila: Int, columna: Int, tamano: Int): Int {
        var cuenta = 0
        for (i in -1..1) {
            for (j in -1..1) {
                val nuevaFila = fila + i
                val nuevaColumna = columna + j
                if (nuevaFila in 0 until tamano && nuevaColumna in 0 until tamano && posicionesMinas.contains(Pair(nuevaFila, nuevaColumna))) {
                    cuenta++
                }
            }
        }
        return cuenta
    }

    // Voltear una celda al hacer clic en ella
    private fun voltearCelda(boton: Button, fila: Int, columna: Int, gridLayout: GridLayout) {
        if (posicionesMinas.contains(Pair(fila, columna))) {
            boton.text = "💣"
            boton.setTextColor(android.graphics.Color.RED)
            boton.setBackgroundColor(android.graphics.Color.YELLOW)
            finDelJuego(gridLayout)
        } else {
            val minasAdyacentes = contarMinasAdyacentes(fila, columna, tamanoTablero)

            if (minasAdyacentes > 0) {
                boton.text = minasAdyacentes.toString()
                boton.setTextColor(android.graphics.Color.BLACK)
            } else {
                boton.text = ""
                expandirCeldasVacias(fila, columna, gridLayout)
            }

            boton.setBackgroundColor(android.graphics.Color.WHITE)
            boton.isEnabled = false
        }
    }

    // Expande las celdas vacías cuando no hay minas cercanas
    private fun expandirCeldasVacias(fila: Int, columna: Int, gridLayout: GridLayout) {
        for (i in -1..1) {
            for (j in -1..1) {
                val nuevaFila = fila + i
                val nuevaColumna = columna + j
                if (nuevaFila in 0 until tamanoTablero && nuevaColumna in 0 until tamanoTablero) {
                    val boton = gridLayout.getChildAt(nuevaFila * tamanoTablero + nuevaColumna) as Button
                    if (boton.isEnabled) {
                        val minasAdyacentes = contarMinasAdyacentes(nuevaFila, nuevaColumna, tamanoTablero)
                        if (minasAdyacentes == 0) {
                            boton.text = ""
                            boton.setBackgroundColor(android.graphics.Color.WHITE)
                            boton.isEnabled = false
                            expandirCeldasVacias(nuevaFila, nuevaColumna, gridLayout)
                        } else {
                            boton.text = minasAdyacentes.toString()
                            boton.setTextColor(android.graphics.Color.BLACK)
                            boton.setBackgroundColor(android.graphics.Color.WHITE)
                            boton.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    // Maneja el fin del juego, deshabilitando todas las celdas
    private fun finDelJuego(gridLayout: GridLayout) {
        corriendo = false
        mensajeDerrotaTextView.visibility = View.VISIBLE
        mensajeDerrotaTextView.bringToFront()
        mensajeDerrotaTextView.animate().alpha(1.0f).duration = 500 // Animación para mostrar el mensaje de derrota

        for (i in 0 until gridLayout.childCount) {
            val boton = gridLayout.getChildAt(i) as Button
            boton.isEnabled = false
        }
    }

    // Reinicia el juego cuando el usuario pulsa el botón de reinicio
    private fun reiniciarJuego() {
        primerClick = true
        mensajeDerrotaTextView.visibility = View.GONE

        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        dibujarTablero(gridLayout, tamanoTablero)

        reiniciarTemporizador()
        iniciarTemporizador()
    }

    // Inicia el temporizador del juego
    private fun iniciarTemporizador() {
        corriendo = true
        manejador.postDelayed(actualizarTemporizador, 1000)
    }

    // Actualiza el temporizador
    private val actualizarTemporizador = object : Runnable {
        override fun run() {
            if (corriendo) {
                segundos++
                val minutos = segundos / 60
                val segundosRestantes = segundos % 60
                tiempoTextView.text = String.format("⏳ %02d:%02d", minutos, segundosRestantes)
                manejador.postDelayed(this, 1000)
            }
        }
    }

    // Reinicia el temporizador
    private fun reiniciarTemporizador() {
        segundos = 0
        tiempoTextView.text = "00:00"
        corriendo = false
        manejador.removeCallbacks(actualizarTemporizador)
    }
}