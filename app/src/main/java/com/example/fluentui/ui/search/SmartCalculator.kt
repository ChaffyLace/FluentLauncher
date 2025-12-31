package com.example.fluentui.ui.search

import java.text.DecimalFormat
import kotlin.math.pow

class SmartCalculator {
    fun evaluate(expression: String): String? {
        val cleanExpr = expression.replace(" ", "").replace(",", ".")
        if (!cleanExpr.matches(Regex("^[0-9+\\-*/().^]+$"))) return null

        return try {
            val result = object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < cleanExpr.length) cleanExpr[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < cleanExpr.length) throw RuntimeException("Unexpected: " + ch.toChar())
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        when {
                            eat('+'.code) -> x += parseTerm() // addition
                            eat('-'.code) -> x -= parseTerm() // subtraction
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        when {
                            eat('*'.code) -> x *= parseFactor() // multiplication
                            eat('/'.code) -> x /= parseFactor() // division
                            else -> return x
                        }
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()

                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) {
                        x = parseExpression()
                        eat(')'.code)
                    } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                        while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                        x = cleanExpr.substring(startPos, pos).toDouble()
                    } else {
                        throw RuntimeException("Unknown char")
                    }

                    if (eat('^'.code)) x = x.pow(parseFactor())

                    return x
                }
            }.parse()

            val format = DecimalFormat("#.##########")
            format.format(result)
        } catch (e: Exception) {
            null
        }
    }
}