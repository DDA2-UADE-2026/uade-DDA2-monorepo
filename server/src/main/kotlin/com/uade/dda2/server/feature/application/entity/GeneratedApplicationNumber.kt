package com.uade.dda2.server.feature.application.entity

import org.hibernate.annotations.ValueGenerationType
import org.hibernate.boot.model.relational.Database
import org.hibernate.boot.model.relational.ExportableProducer
import org.hibernate.boot.model.relational.SqlStringGenerationContext
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.generator.GeneratorCreationContext
import org.hibernate.id.Configurable
import org.hibernate.id.enhanced.SequenceStyleGenerator
import java.util.EnumSet
import java.util.Properties

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@ValueGenerationType(generatedBy = ApplicationNumberGenerator::class)
annotation class GeneratedApplicationNumber

/** Registers the sequence with Hibernate schema tooling without making it the entity's UUID id. */
class ApplicationNumberGenerator : BeforeExecutionGenerator, ExportableProducer, Configurable {
    private val sequence = SequenceStyleGenerator()

    override fun configure(context: GeneratorCreationContext, parameters: Properties) {
        sequence.configure(context, Properties().apply {
            putAll(parameters)
            setProperty(SequenceStyleGenerator.SEQUENCE_PARAM, "application_number_seq")
            setProperty(SequenceStyleGenerator.INITIAL_PARAM, "1")
            setProperty(SequenceStyleGenerator.INCREMENT_PARAM, "1")
        })
    }

    override fun registerExportables(database: Database) = sequence.registerExportables(database)
    override fun initialize(context: SqlStringGenerationContext) = sequence.initialize(context)
    override fun getEventTypes(): EnumSet<EventType> = EnumSet.of(EventType.INSERT)
    override fun generate(session: SharedSessionContractImplementor, owner: Any, currentValue: Any?, eventType: EventType): Any =
        sequence.generate(session, owner)
}
