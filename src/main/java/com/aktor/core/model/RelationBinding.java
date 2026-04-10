package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Model;
import com.aktor.core.Repository;
import com.aktor.core.data.Relation;
import com.aktor.core.service.Management;
import com.aktor.core.service.ManagementRepository;

import java.util.Objects;

public record RelationBinding<MainKey, ForeignKey, ForeignData extends Data<ForeignKey>>(
    String field,
    Class<ForeignData> foreignType,
    Management<ForeignData, ForeignKey> foreignManagement,
    Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
    RelationFactory<MainKey, ForeignKey> relationFactory,
    String mainField,
    String foreignField,
    RelationCardinalityPolicy cardinalityPolicy,
    RelationStoragePolicy storagePolicy,
    RelationCyclePolicy cyclePolicy,
    RelationSavePolicy savePolicy,
    RelationDeletePolicy deletePolicy
)
implements Model
{
    private static final String DEFAULT_MAIN_FIELD = "main_key";
    private static final String DEFAULT_FOREIGN_FIELD = "foreign_key";
    private static final RelationCardinalityPolicy DEFAULT_CARDINALITY_POLICY = RelationCardinalityPolicy.ONE_TO_ONE;
    private static final RelationStoragePolicy DEFAULT_STORAGE_POLICY = RelationStoragePolicy.SEPARATE;
    private static final RelationCyclePolicy DEFAULT_CYCLE_POLICY = RelationCyclePolicy.REJECT;
    private static final RelationSavePolicy DEFAULT_SAVE_POLICY = RelationSavePolicy.CASCADE;
    private static final RelationDeletePolicy DEFAULT_DELETE_POLICY = RelationDeletePolicy.CASCADE;

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Repository<ForeignData, ForeignKey> foreignRepository,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory
    )
    {
        this(
            field,
            foreignType,
            ManagementRepository.noRelations(Objects.requireNonNull(foreignRepository)),
            relationRepository,
            relationFactory,
            DEFAULT_MAIN_FIELD,
            DEFAULT_FOREIGN_FIELD,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Repository<ForeignData, ForeignKey> foreignRepository,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final String mainField,
        final String foreignField
    )
    {
        this(
            field,
            foreignType,
            ManagementRepository.noRelations(Objects.requireNonNull(foreignRepository)),
            relationRepository,
            relationFactory,
            mainField,
            foreignField,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Repository<ForeignData, ForeignKey> foreignRepository,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final String mainField,
        final String foreignField,
        final RelationDeletePolicy deletePolicy
    )
    {
        this(
            field,
            foreignType,
            ManagementRepository.noRelations(Objects.requireNonNull(foreignRepository)),
            relationRepository,
            relationFactory,
            mainField,
            foreignField,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            DEFAULT_SAVE_POLICY,
            deletePolicy
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory
    )
    {
        this(
            field,
            foreignType,
            foreignManagement,
            relationRepository,
            relationFactory,
            DEFAULT_MAIN_FIELD,
            DEFAULT_FOREIGN_FIELD,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final String mainField,
        final String foreignField
    )
    {
        this(
            field,
            foreignType,
            foreignManagement,
            relationRepository,
            relationFactory,
            mainField,
            foreignField,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final RelationSavePolicy savePolicy,
        final RelationDeletePolicy deletePolicy
    )
    {
        this(
            field,
            foreignType,
            foreignManagement,
            relationRepository,
            relationFactory,
            DEFAULT_MAIN_FIELD,
            DEFAULT_FOREIGN_FIELD,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            savePolicy,
            deletePolicy
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final String mainField,
        final String foreignField,
        final RelationSavePolicy savePolicy,
        final RelationDeletePolicy deletePolicy
    )
    {
        this(
            field,
            foreignType,
            foreignManagement,
            relationRepository,
            relationFactory,
            mainField,
            foreignField,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            DEFAULT_CYCLE_POLICY,
            savePolicy,
            deletePolicy
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final RelationFactory<MainKey, ForeignKey> relationFactory,
        final String mainField,
        final String foreignField,
        final RelationCyclePolicy cyclePolicy,
        final RelationSavePolicy savePolicy,
        final RelationDeletePolicy deletePolicy
    )
    {
        this(
            field,
            foreignType,
            foreignManagement,
            relationRepository,
            relationFactory,
            mainField,
            foreignField,
            DEFAULT_CARDINALITY_POLICY,
            DEFAULT_STORAGE_POLICY,
            cyclePolicy,
            savePolicy,
            deletePolicy
        );
    }

    public RelationBinding
    {
        field = requireField(Objects.requireNonNull(field));
        foreignType = Objects.requireNonNull(foreignType);
        foreignManagement = Objects.requireNonNull(foreignManagement);
        relationRepository = Objects.requireNonNull(relationRepository);
        relationFactory = Objects.requireNonNull(relationFactory);
        mainField = requireField(Objects.requireNonNull(mainField));
        foreignField = requireField(Objects.requireNonNull(foreignField));
        cardinalityPolicy = Objects.requireNonNull(cardinalityPolicy);
        storagePolicy = Objects.requireNonNull(storagePolicy);
        cyclePolicy = Objects.requireNonNull(cyclePolicy);
        savePolicy = Objects.requireNonNull(savePolicy);
        deletePolicy = Objects.requireNonNull(deletePolicy);
    }

    private static String requireField(final String field)
    {
        if (field.isBlank())
        {
            throw new IllegalArgumentException("field cannot be blank");
        }
        return field;
    }
}
