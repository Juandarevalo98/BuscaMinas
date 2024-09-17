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
    private lateinit var timeTextView: TextView
    private lateinit var lossMessageTextView: TextView
    private var seconds = 0
    private var running = false
    private val handler = Handler(Looper.getMainLooper())

    private var firstClick = true
    private var boardSize = 8
    private var mineCount = 10
    private var minePositions = mutableSetOf<Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeTextView = findViewById(R.id.time)
        lossMessageTextView = findViewById(R.id.lossMessage)

        val spinnerId = findViewById<Spinner>(R.id.spinId)
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)

        val dificultad = arrayOf("Facil", "Medio", "Dificil")
        val arrayAdp = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, dificultad)
        spinnerId.adapter = arrayAdp

        spinnerId.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetTimer()
                startTimer()

                when (dificultad[position]) {
                    "Facil" -> {
                        boardSize = 8
                        mineCount = 10
                    }
                    "Medio" -> {
                        boardSize = 16
                        mineCount = 40
                    }
                    "Dificil" -> {
                        boardSize = 24
                        mineCount = 99
                    }
                }

                firstClick = true
                drawGrid(gridLayout, boardSize)
                lossMessageTextView.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        startTimer()
    }

    private fun drawGrid(gridLayout: GridLayout, size: Int) {
        gridLayout.removeAllViews()
        gridLayout.columnCount = size
        gridLayout.rowCount = size

        gridLayout.post {
            val gridWidth = gridLayout.width
            val gridHeight = gridLayout.height
            val buttonSize = Math.min(gridWidth, gridHeight) / size

            for (i in 0 until size) {
                for (j in 0 until size) {
                    val button = Button(this)
                    button.setBackgroundResource(R.drawable.button_border)

                    // Ajuste dinámico del tamaño del texto
                    val textSize = when (size) {
                        8 -> 24f // Fácil
                        16 -> 18f // Medio
                        24 -> 14f // Difícil
                        else -> 20f
                    }
                    button.textSize = textSize

                    // Eliminar el padding para que el texto ocupe todo el espacio
                    button.setPadding(0, 0, 0, 0)

                    // Aseguramos que el texto esté centrado
                    button.gravity = Gravity.CENTER

                    // Ajustar el tamaño del texto para que ocupe todo el botón
                    button.minHeight = 0
                    button.minimumHeight = 0
                    button.minWidth = 0
                    button.minimumWidth = 0

                    // Establecer color negro para mejor contraste
                    button.setTextColor(resources.getColor(R.color.black))

                    // Configuración de layoutParams para ajustar el tamaño de las celdas
                    button.layoutParams = GridLayout.LayoutParams().apply {
                        width = buttonSize
                        height = buttonSize
                        columnSpec = GridLayout.spec(j)
                        rowSpec = GridLayout.spec(i)
                    }

                    // Asignar listener para voltear la celda
                    button.setOnClickListener {
                        if (firstClick) {
                            firstClick = false
                            generateMines(size, mineCount, i, j)
                        }
                        flipCell(button, i, j, gridLayout)
                    }

                    gridLayout.addView(button)
                }
            }
        }
    }


    private fun generateMines(size: Int, mineCount: Int, clickedRow: Int, clickedCol: Int) {
        minePositions.clear()
        while (minePositions.size < mineCount) {
            val row = Random.nextInt(size)
            val col = Random.nextInt(size)

            if (row != clickedRow || col != clickedCol) {
                minePositions.add(Pair(row, col))
            }
        }
    }

    private fun countAdjacentMines(row: Int, col: Int, size: Int): Int {
        var count = 0
        for (i in -1..1) {
            for (j in -1..1) {
                val newRow = row + i
                val newCol = col + j
                if (newRow in 0 until size && newCol in 0 until size && minePositions.contains(Pair(newRow, newCol))) {
                    count++
                }
            }
        }
        return count
    }

    private fun flipCell(button: Button, row: Int, col: Int, gridLayout: GridLayout) {
        if (minePositions.contains(Pair(row, col))) {
            button.text = "💣"
            button.setTextColor(android.graphics.Color.RED)
            button.setBackgroundColor(android.graphics.Color.YELLOW)
            gameOver(gridLayout)
        } else {
            val mineCount = countAdjacentMines(row, col, boardSize)

            if (mineCount > 0) {
                button.text = mineCount.toString()
                button.setTextColor(android.graphics.Color.BLACK)
            } else {
                button.text = ""
                expandEmptyCells(row, col, gridLayout)
            }

            button.setBackgroundColor(android.graphics.Color.WHITE)
            button.isEnabled = false
        }
    }

    private fun expandEmptyCells(row: Int, col: Int, gridLayout: GridLayout) {
        for (i in -1..1) {
            for (j in -1..1) {
                val newRow = row + i
                val newCol = col + j
                if (newRow in 0 until boardSize && newCol in 0 until boardSize) {
                    val button = gridLayout.getChildAt(newRow * boardSize + newCol) as Button
                    if (button.isEnabled) {
                        val mineCount = countAdjacentMines(newRow, newCol, boardSize)
                        if (mineCount == 0) {
                            button.text = ""
                            button.setBackgroundColor(android.graphics.Color.WHITE)
                            button.isEnabled = false
                            expandEmptyCells(newRow, newCol, gridLayout)
                        } else {
                            button.text = mineCount.toString()
                            button.setTextColor(android.graphics.Color.BLACK)
                            button.setBackgroundColor(android.graphics.Color.WHITE)
                            button.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun gameOver(gridLayout: GridLayout) {
        running = false
        lossMessageTextView.visibility = View.VISIBLE
        lossMessageTextView.bringToFront()
        lossMessageTextView.animate()
            .alpha(1f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(500)
            .start()

        for (i in 0 until gridLayout.childCount) {
            val child = gridLayout.getChildAt(i)
            if (child is Button) {
                child.isEnabled = false
            }
        }
    }

    private fun startTimer() {
        running = true
        handler.post(object : Runnable {
            override fun run() {
                if (running) {
                    val minutes = seconds / 60
                    val secs = seconds % 60
                    val time = String.format("⏳ %02d:%02d", minutes, secs)
                    timeTextView.text = time
                    seconds++
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun resetTimer() {
        running = false
        handler.removeCallbacksAndMessages(null)
        seconds = 0
        updateTimerText()
    }

    private fun updateTimerText() {
        val minutes = seconds / 60
        val secs = seconds % 60
        val time = String.format("%02d:%02d", minutes, secs)
        timeTextView.text = time
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}