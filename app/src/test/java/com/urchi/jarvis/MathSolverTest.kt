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

    @Test fun evaluatesParenthesesAndPrecedence() {
        val answer = MathSolver.solve("посчитай (2 + 3) * 4")
        assertTrue(answer?.contains("20") == true)
    }

    @Test fun reportsDivisionByZero() {
        val answer = MathSolver.solve("8 / 0")
        assertTrue(answer?.contains("на ноль") == true)
    }

    @Test fun doesNotGuessUnsupportedNonlinearEquation() {
        assertTrue(MathSolver.solve("x * x = 9") == null)
    }
}
