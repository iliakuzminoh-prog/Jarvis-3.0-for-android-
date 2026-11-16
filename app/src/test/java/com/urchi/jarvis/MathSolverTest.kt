package com.urchi.jarvis

import org.junit.Assert.assertTrue
import org.junit.Test

class MathSolverTest {
    @Test fun solvesLinearEquationWithImplicitMultiplication() {
        val answer = MathSolver.solve("реши задачу 2x + 3 = 7")
        assertTrue(answer?.contains("x = 2") == true)
    }

    @Test fun solvesEquationWithNegativeCoefficient() {
        val answer = MathSolver.solve("реши -3x + 6 = 0")
        assertTrue(answer?.contains("x = 2") == true)
    }

    @Test fun solvesEquationSpokenInRussian() {
        val answer = MathSolver.solve("реши задачу два икс плюс три равно семь")
        assertTrue(answer?.contains("x = 2") == true)
    }

    @Test fun solvesEquationSpokenInEnglish() {
        val answer = MathSolver.solve("solve equation two x plus three equals seven")
        assertTrue(answer?.contains("x = 2") == true)
    }

    @Test fun evaluatesParenthesesAndPrecedence() {
        val answer = MathSolver.solve("посчитай (2 + 3) * 4")
        assertTrue(answer?.contains("20") == true)
    }

    @Test fun reportsDivisionByZero() {
        val answer = MathSolver.solve("8 / 0")
        assertTrue(answer?.contains("на ноль") == true)
    }

    @Test fun calculatesPercentageOfNumber() {
        val answer = MathSolver.solve("найди 15 процентов от 200")
        assertTrue(answer?.contains("30") == true)
    }

    @Test fun doesNotGuessUnsupportedNonlinearEquation() {
        assertTrue(MathSolver.solve("x * x = 9") == null)
    }

    @Test fun doesNotLabelVariableDivisionAsZeroDivision() {
        assertTrue(MathSolver.solve("x / x") == null)
    }
}
