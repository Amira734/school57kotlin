package ru.tbank.education.school.lesson10.homework

import kotlin.reflect.KParameter
import kotlin.reflect.full.*

object DocumentationGenerator {

    fun generateDoc(obj: Any): String {
        val kClass = obj::class

        if (kClass.findAnnotation<InternalApi>() != null) {
            return "Документация скрыта (InternalApi)."
        }

        val docClass = kClass.findAnnotation<DocClass>()
            ?: return "Нет документации для класса."

        val sb = StringBuilder()

        sb.appendLine("=== Документация: ${kClass.simpleName} ===")
        sb.appendLine("Описание: ${docClass.description}")
        sb.appendLine("Автор: ${docClass.author}")
        sb.appendLine("Версия: ${docClass.version}")
        sb.appendLine()


        val internalConstructorProperties = kClass.primaryConstructor
            ?.parameters
            ?.filter { it.findAnnotation<InternalApi>() != null }
            ?.mapNotNull { it.name }
            ?.toSet()
            ?: emptySet()

        val properties = kClass.memberProperties
            .filter { it.name !in internalConstructorProperties }
            .filter { it.findAnnotation<InternalApi>() == null }

        if (properties.isNotEmpty()) {
            sb.appendLine("--- Свойства ---")
            for (property in properties) {
                sb.appendLine("- ${property.name}")

                val docProperty = property.findAnnotation<DocProperty>()
                if (docProperty != null) {
                    sb.appendLine("  Описание: ${docProperty.description}")
                    if (docProperty.example.isNotBlank()) {
                        sb.appendLine("  Пример: ${docProperty.example}")
                    }
                }
            }
            sb.appendLine()
        }


        val ignoredMethodNames = setOf("toString", "equals", "hashCode", "copy")

        val methods = kClass.memberFunctions
            .filter { it.findAnnotation<InternalApi>() == null }
            .filter { it.name !in ignoredMethodNames }
            .filterNot { it.name.startsWith("component") }

        if (methods.isNotEmpty()) {
            sb.appendLine("--- Методы ---")

            for (method in methods) {
                val paramsSignature = method.parameters
                    .filter { it.kind == KParameter.Kind.VALUE }
                    .joinToString(", ") { "${it.name}: ${it.type}" }

                sb.appendLine("- ${method.name}($paramsSignature)")

                val docMethod = method.findAnnotation<DocMethod>()
                if (docMethod != null) {
                    sb.appendLine("  Описание: ${docMethod.description}")
                }

                val valueParams = method.parameters
                    .filter { it.kind == KParameter.Kind.VALUE }

                if (valueParams.isNotEmpty()) {
                    sb.appendLine("  Параметры:")
                    for (param in valueParams) {
                        val docParam = param.findAnnotation<DocParam>()
                        val description = docParam?.description ?: "Нет описания"
                        sb.appendLine("    - ${param.name}: $description")
                    }
                }

                val returnsDescription = docMethod?.returns ?: "Нет описания"
                sb.appendLine("      Возвращает: $returnsDescription")
            }
        }

        return sb.toString().trimEnd()
    }
}
