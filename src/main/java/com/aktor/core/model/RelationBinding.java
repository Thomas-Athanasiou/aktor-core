package com.aktor.core.model;

import com.aktor.core.Data;
import com.aktor.core.Model;
import com.aktor.core.Repository;
import com.aktor.core.data.Relation;
import com.aktor.core.service.Management;
import com.aktor.core.service.ManagementRepository;

import java.util.Objects;
import java.util.function.BiFunction;

// TODO TOO MANY CTOR ARGS
public record RelationBinding<MainKey, ForeignKey, ForeignData extends Data<ForeignKey>>(
    String field,
    Class<ForeignData> foreignType,
    Management<ForeignData, ForeignKey> foreignManagement,
    Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
    BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
    String mainField,
    String foreignField,
    RelationSavePolicy savePolicy,
    RelationDeletePolicy deletePolicy
)
implements Model
{
    private static final String DEFAULT_MAIN_FIELD = "main_key";
    private static final String DEFAULT_FOREIGN_FIELD = "foreign_key";
    private static final RelationSavePolicy DEFAULT_SAVE_POLICY = RelationSavePolicy.CASCADE;
    private static final RelationDeletePolicy DEFAULT_DELETE_POLICY = RelationDeletePolicy.CASCADE;

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Repository<ForeignData, ForeignKey> foreignRepository,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory
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
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Repository<ForeignData, ForeignKey> foreignRepository,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
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
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Repository<ForeignData, ForeignKey> foreignRepository,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
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
            DEFAULT_SAVE_POLICY,
            deletePolicy
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory
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
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
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
            DEFAULT_SAVE_POLICY,
            DEFAULT_DELETE_POLICY
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
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
            savePolicy,
            deletePolicy
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
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
            DEFAULT_SAVE_POLICY,
            deletePolicy
        );
    }

    public RelationBinding(
        final String field,
        final Class<ForeignData> foreignType,
        final Management<ForeignData, ForeignKey> foreignManagement,
        final Repository<Relation<MainKey, ForeignKey>, String> relationRepository,
        final BiFunction<MainKey, ForeignKey, Relation<MainKey, ForeignKey>> relationFactory,
        final String mainField,
        final String foreignField,
        final RelationSavePolicy savePolicy
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
            savePolicy,
            DEFAULT_DELETE_POLICY
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
