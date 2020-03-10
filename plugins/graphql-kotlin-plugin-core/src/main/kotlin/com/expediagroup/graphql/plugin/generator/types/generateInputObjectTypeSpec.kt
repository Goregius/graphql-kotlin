package com.expediagroup.graphql.plugin.generator.types

import com.expediagroup.graphql.plugin.generator.GraphQLClientGeneratorContext
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.InputObjectTypeDefinition

internal fun generateInputObjectTypeSpec(context: GraphQLClientGeneratorContext, inputObjectDefinition: InputObjectTypeDefinition): TypeSpec {
    val inputObjectTypeSpecBuilder = TypeSpec.classBuilder(inputObjectDefinition.name)
    inputObjectTypeSpecBuilder.modifiers.add(KModifier.DATA)
    inputObjectDefinition.description?.content?.let { kdoc ->
        inputObjectTypeSpecBuilder.addKdoc(kdoc)
    }

    val constructorBuilder = FunSpec.constructorBuilder()
    inputObjectDefinition.inputValueDefinitions.forEach { fieldDefinition ->
        val kotlinFieldType = generateKotlinTypeName(context, fieldDefinition.type)
        val fieldName = fieldDefinition.name

        val propertySpec = PropertySpec.builder(fieldName, kotlinFieldType)
            .initializer(fieldName)
            .build()
        inputObjectTypeSpecBuilder.addProperty(propertySpec)
        constructorBuilder.addParameter(propertySpec.name, propertySpec.type)
    }
    inputObjectTypeSpecBuilder.primaryConstructor(constructorBuilder.build())

    val inputTypeObjectTypeSpec = inputObjectTypeSpecBuilder.build()
    context.typeSpecs.add(inputTypeObjectTypeSpec)
    return inputTypeObjectTypeSpec
}
