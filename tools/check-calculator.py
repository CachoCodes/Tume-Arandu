#!/usr/bin/env python3
"""Compile the actual stdlib-only calculator parser with the cached Kotlin compiler."""
# @spec spec://modules/learning/FEAT-010-learning-demo#exercises
from pathlib import Path
import os, subprocess, tempfile
root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
jars = [p for group in ('org.jetbrains.kotlin', 'org.jetbrains.kotlinx', 'org.jetbrains', 'org.jetbrains.intellij.deps') for p in (cache/group).rglob('*.jar')]
compiler = next(p for p in jars if p.name.startswith('kotlin-compiler-embeddable-'))
stdlib = next(p for p in jars if p.parent.parent.parent.name == 'kotlin-stdlib')
java = str(Path(os.environ['JAVA_HOME'])/'bin/java') if 'JAVA_HOME' in os.environ else 'java'
with tempfile.TemporaryDirectory() as directory:
    work = Path(directory)
    test = work/'Check.kt'
    test.write_text('''package com.guarani.mathdemo.ui
import kotlin.math.abs
import com.guarani.mathdemo.course.*
fun main() {
    val cases = mapOf("sin(30)" to .5, "cos(60)" to .5, "tan(45)" to 1.0,
        "sqrt(2)^2" to 2.0, "2^3^2" to 512.0, "-2^2" to -4.0,
        "(3/5)÷(4/5)" to .75, "π" to Math.PI, "2+3×4" to 14.0)
    cases.forEach { (expression, expected) -> check(abs(ArithmeticExpressionParser(expression).evaluate()-expected) < 1e-10) { expression } }
    listOf("tan(90)", "sqrt(-1)", "1/0", "sin(", "foo(1)", "2..3", "2^99999").forEach {
        check(runCatching { ArithmeticExpressionParser(it).evaluate() }.isFailure) { it }
    }
    val exercise = InputExercise("test", "standard_number", "question", "basic hint", (1..9).map { "step $it" }, listOf("1"))
    check(exercise.helpSteps() == listOf("basic hint", "step 1", "step 2", "step 3", "step 9"))
    check(exercise.copy(solutionSteps = listOf("answer")).helpSteps() == listOf("basic hint", "answer"))
    println("Calculator: 9 valid expressions and 7 errors passed")
}
''')
    classpath = os.pathsep.join(map(str, [compiler] + [p for p in jars if p != compiler]))
    subprocess.run([java, '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-classpath', str(stdlib), '-d', str(work/'classes'), str(root/'app/src/main/kotlin/com/guarani/mathdemo/ui/ArithmeticExpressionParser.kt'), str(root/'app/src/main/kotlin/com/guarani/mathdemo/course/Course.kt'), str(test)], check=True)
    subprocess.run([java, '-cp', os.pathsep.join([str(work/'classes'),str(stdlib)]), 'com.guarani.mathdemo.ui.CheckKt'], check=True)
