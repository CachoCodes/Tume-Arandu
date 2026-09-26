package com.guarani.mathdemo.ui

import kotlin.math.*

// @spec spec://modules/learning/FEAT-010-learning-demo#exercises
internal class ArithmeticExpressionParser(private val source: String) {
    private var cursor = 0

    fun evaluate(): Double {
        require(source.isNotBlank())
        val value = parseExpression()
        skipSpaces()
        require(cursor == source.length && value.isFinite())
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipSpaces()
            value = when (source.getOrNull(cursor)) {
                '+' -> { cursor++; value + parseTerm() }
                '-', '−' -> { cursor++; value - parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parsePower()
        while (true) {
            skipSpaces()
            value = when (source.getOrNull(cursor)) {
                '*', '×' -> { cursor++; value * parsePower() }
                '/', '÷' -> {
                    cursor++
                    val divisor = parsePower()
                    require(divisor != 0.0)
                    value / divisor
                }
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        skipSpaces()
        if (source.getOrNull(cursor) == '+' || source.getOrNull(cursor) in listOf('-', '−')) {
            val negative = source[cursor++] != '+'
            val value = parsePower()
            return if (negative) -value else value
        }
        val value = parseFactor()
        skipSpaces()
        return if (source.getOrNull(cursor) == '^') { cursor++; value.pow(parsePower()) } else value
    }

    private fun parseFactor(): Double {
        skipSpaces()
        when (source.getOrNull(cursor)) {
            '+' -> { cursor++; return parseFactor() }
            '-', '−' -> { cursor++; return -parseFactor() }
            '(' -> {
                cursor++
                val value = parseExpression()
                skipSpaces()
                require(source.getOrNull(cursor) == ')')
                cursor++
                return value
            }
        }
        if (source.getOrNull(cursor) == 'π') { cursor++; return PI }
        if (source.getOrNull(cursor)?.isLetter() == true) {
            val start = cursor
            while (source.getOrNull(cursor)?.isLetter() == true) cursor++
            val function = source.substring(start, cursor)
            skipSpaces()
            require(source.getOrNull(cursor) == '(')
            cursor++
            val argument = parseExpression()
            skipSpaces()
            require(source.getOrNull(cursor) == ')')
            cursor++
            val radians = argument * PI / 180
            return when (function) {
                "sin" -> sin(radians)
                "cos" -> cos(radians)
                "tan" -> { require(abs(cos(radians)) > 1e-12); tan(radians) }
                "sqrt" -> { require(argument >= 0); sqrt(argument) }
                else -> error("Unknown function")
            }
        }
        val start = cursor
        var hasDigit = false
        var hasPoint = false
        while (true) {
            val character = source.getOrNull(cursor) ?: break
            when (character) {
                in '0'..'9' -> { hasDigit = true; cursor++ }
                '.' -> { require(!hasPoint); hasPoint = true; cursor++ }
                else -> break
            }
        }
        require(hasDigit)
        return source.substring(start, cursor).toDouble()
    }

    private fun skipSpaces() {
        while (source.getOrNull(cursor)?.isWhitespace() == true) cursor++
    }
}

