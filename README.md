# Aktor Core

Aktor is a small, modular application framework and data layer built around reusable repositories, relations, search, transactions, and model mapping

`core` is the storage-agnostic heart of the Aktor data model.

## Status

This framework is currently in the `alpha` stage of development.

That means:
- the core architecture is real and actively used
- the main design direction is intentional
- the APIs are still being hardened
- some app integrations are still being migrated onto the cleaner repository surface

It is a good time for:
- architectural feedback
- experimentation
- early adoption in controlled projects

It is not yet at a stable development stage.

It defines:
- typed data records through `Data<Key>`
- record mapping through `Mapper`
- field-to-storage mapping through `FieldResolver`
- repository contracts through `Repository`
- SQL-backed core repository support through `RepositorySql`
- repository composition through `RepositoryCache`, `RepositoryAggregate`, `RepositoryReadOnly`, and `RepositoryRotating`
- relation orchestration through `RelationBinding`, `RelationProvider`, `RelationProviderResolver`, and `RelationProcessor`
- repository loading through `RepositoryProvider`
- management loading through `ManagementProvider`
- transactional management through `ManagementRepository`

The main goal is simple:

- keep the framework simple to understand and use
- keep the domain model independent from storage details
- let repositories stay generic
- let relations be described once and synchronized consistently

## Core Ideas

### 1. Data Is A Record With A Logical Key

Every persisted model implements:

```java
public interface Data<Key>
{
    Key key();
}
```

The logical identifier is always `key()`.

That means the model can stay stable even if the physical storage column is named differently, such as:
- `key`
- `id`
- `workflow_key`

The mapping is handled by `FieldResolver`, not by changing the model.

### 2. Storage Mapping Is Explicit

`FieldResolver` maps logical record fields to physical storage fields.

Typical usage:

```java
FieldResolver.mapped(MyData.class)
FieldResolver.mapped(MyData.class, Map.of("key", "id"))
```

This same resolver is reused across:
- reads
- writes
- key lookup
- search SQL
- total-count SQL
- schema generation

That keeps mapping logic in one place instead of scattering it across repositories and converters.

### 2.1 Configuration Is Tree-Shaped

`Configuration` exposes direct child lookup by key.

Common accessors are:
- `getString(key)`
- `getLong(key)`
- `getInteger(key)`
- `getBoolean(key)`
- `getConfiguration(key)`
- `has(key)`
- `keys()`

`getConfiguration(key)` returns a nested configuration view, so callers can walk the tree explicitly instead of encoding structure into dotted paths.

For example:

```java
configuration
    .getConfiguration("entity")
    .getConfiguration("workflow-main")
    .getConfiguration("storage")
    .getString("table");
```

This is the preferred shape for config-driven repositories and managements.

### XML Repository Config Example

The repository layer is usually described as a tree of named entities.

```xml
<configuration>
    <entity name="exercise">
        <aggregate kind="cache" cacheWriteSourceCount="1">
            <sources>
                <source name="exercise-sqlite" />
                <source name="exercise-csv" />
            </sources>
        </aggregate>
    </entity>

    <entity name="exercise-sqlite">
        <storage kind="sqlite" table="exercise" />
    </entity>

    <entity name="exercise-csv">
        <storage kind="csv" resource="exercise_catalog" />
    </entity>

    <entity name="template">
        <storage kind="sqlite" table="template" />
        <management kind="repository" />
        <relation field="exercises" target="exercise" />
    </entity>
</configuration>
```

In this example:
- `exercise` is a cache aggregate over SQLite and CSV
- `exercise-sqlite` is the writable source
- `exercise-csv` is the read-only seed source
- `template` is a normal storage entity with relation bindings

### 3. Relations Are Storage-Agnostic

Relations are described through `RelationBinding`.

A binding defines:
- the owning field
- the foreign type
- how foreign items are managed
- how links are stored
- how link rows are created
- which main/foreign relation fields are used
- what happens on delete

This is intentionally above any concrete database table design.

The relation layer works with:
- `Repository<Relation<MainKey, ForeignKey>, String>`
- `Management<ForeignData, ForeignKey>`

So the relation contract does not need to know whether storage is:
- SQL
- SQLite
- in-memory
- something else

That also means a relation can span different storages.

For example:
- a parent item can be managed from one repository/storage
- the foreign item can be managed from another repository/storage
- the relation links themselves can live in a third repository/storage

As long as each side implements the core contracts, the relation layer can orchestrate them together.

### 4. Management Handles Data And Relations Together

`ManagementRepository` coordinates:
- saving the main item
- synchronizing relations after save
- enforcing relation delete policy before delete
- cleaning relation links after delete
- transaction orchestration across participants

Delete flow is:

1. `relationProcessor.beforeDelete(item)`
2. `repository.delete(item)`
3. `relationProcessor.afterDelete(item)`

That separation makes delete behavior much easier to reason about.

For application code, the main user-facing CRUD service is `Management<Item, Key>`.

That is the API layer meant to be consumed by higher-level code for:
- `get`
- `search`
- `save`
- `delete`

Repositories are the persistence mechanism underneath.
`Management<>` is the service-level abstraction that coordinates persistence and relation behavior.

`RepositoryProvider` is the repository-facing loader API.
`ManagementProvider` sits above it and builds managements from repositories, including nested managements for related entities.

That split keeps repository loading focused on storage selection, while management loading owns relation orchestration.

## Relation Features

### RelationProvider

`RelationProvider` is the runtime object that loads and synchronizes a single relation binding.

It supports:
- `single(mainKey)`
- `many(mainKey)`
- `save(mainKey, value)`
- `beforeDelete(mainKey)`
- `delete(mainKey)`

`single(...)` is used for singular `Data<?>` relations.

`many(...)` is used for `Data<?>[]` relations.

### RelationProviderResolver

`RelationProviderResolver` is the field-to-provider registry for one owning type.

It:
- resolves relation providers by field name
- saves all relations for a record
- runs relation delete checks
- runs relation cleanup after delete
- participates in transactions

### RelationCardinalityPolicy

The core relation layer supports:
- `ONE_TO_ONE`
- `ONE_TO_MANY`

Current semantics:
- `ONE_TO_ONE`: at most one related row is allowed for a parent key
- `ONE_TO_MANY`: multiple related rows are allowed for a parent key

Default policy is `ONE_TO_ONE`.

### RelationStoragePolicy

The core relation layer supports:
- `INLINE`
- `SEPARATE`

Current semantics:
- `INLINE`: relation values are read and written through inline storage semantics
- `SEPARATE`: relation values are stored in the relation repository

Default policy is `SEPARATE`.

### RelationDeletePolicy

The core relation layer supports:
- `CASCADE`
- `NULL`
- `RESTRICT`

Current semantics:
- `CASCADE`: delete relation links when the parent is deleted
- `NULL`: leave relation storage untouched
- `RESTRICT`: block parent deletion if related links exist

Default policy is `CASCADE`.

### Mapped Relation Fields

`RelationBinding` can map relation storage fields explicitly:
- `mainField`
- `foreignField`

That allows relation repositories to work with shapes beyond only `main_key` / `foreign_key`.

This is useful for:
- classic relation tables
- same-table relation views
- legacy schemas with custom field names

## Mapping And Repositories

### Mapper

`Mapper` converts row data into record instances.

It combines:
- `FieldResolver`
- record metadata
- `ValueConverter`
- optional `RelationProviderResolver`

The mapper:
- resolves the logical key from storage
- converts scalar values
- loads related `Data<?>` and `Data<?>[]` fields through relation providers

`Mapper` also implements `Converter<Map<String, String>, Item>`.

That is important because the framework depends on the conversion contract, not specifically on reflection.

So the default reflective mapper can be replaced when needed, for example with:
- a hand-written converter
- a generated converter
- a no-reflection optimized mapper
- a specialized mapper for a constrained runtime

In other words, `Mapper` is the default implementation, not the only possible implementation.

### RepositorySql

`RepositorySql` is the generic SQL repository implementation in `core`.

Its public API is intentionally narrow:
- `RepositorySql(connection, type, tableName, driverName)`
- `RepositorySql(connection, type, tableName, fieldResolver, driverName)`
- `RepositorySql(connection, type, tableName, relationProviderResolver, driverName)`
- `RepositorySql(connection, type, tableName, fieldResolver, relationProviderResolver, driverName)`

It constructs the default mapper internally instead of asking callers to assemble one manually.

That helps prevent:
- mapper/repository drift
- inconsistent field mapping
- custom parser sprawl in app code

### Repository Composition

Core now includes a few higher-level repository shapes:

- `RepositoryReadOnly` for single-source read-only views
- `RepositoryRotating` for source rotation
- `RepositoryAggregate` for multi-source aggregation
- `RepositoryCache` for cache-style fan-out reads with controlled write-through

`RepositoryCache` supports a configurable `cacheWriteSourceCount`, so reads can span multiple sources while writes only go to the configured leading sources.

This is useful when one source is writable and another is read-only, such as a SQLite source plus a CSV seed source.

## Design Principles

### Logical Model First

The domain record should describe the model:
- `Template template`
- `ValueField[] fields`
- `Workflow workflow`

Storage translation should happen below that layer.

### Repositories Should Stay Generic

If app code cannot use the simple repository constructors, it often means:
- a projection row is pretending to be a full entity
- singular relations are leaking storage details
- mapping logic lives in the wrong layer

The direction of the framework is to shrink those leaks over time.

### Relation Logic Should Be Centralized

Relations should not be reimplemented ad hoc in each app.

The relation stack exists so that:
- loading
- saving
- deletion checks
- relation cleanup
- transaction participation

all follow one consistent model.

## Typical Flow

1. Define a record implementing `Data<Key>`.
2. Create a `FieldResolver` if storage field names need overrides.
3. Build relation bindings for nested `Data<?>` / `Data<?>[]` fields.
4. Build a `RelationProviderResolver`.
5. Load a repository through `RepositoryProvider`.
6. Load a management through `ManagementProvider`.
7. Wrap it in `ManagementRepository` if you need the service-level CRUD orchestration directly.

### Example: Nested Data Entity

```java
public record LineItem(
    Long key,
    String sku,
    Integer quantity
) implements Data<Long>
{
}

public record Order(
    Long key,
    String customerName,
    LineItem[] items
) implements Data<Long>
{
}
```

In this example:
- `Order` is the parent entity
- `LineItem[] items` is a nested relation
- the model stays fully typed
- storage details for `items` are described through relation bindings, not inside the records

At the relation layer, `items` would usually be wired with a `RelationBinding<Long, Long, LineItem>` and handled through `RelationProviderResolver`.

## What Lives Outside Core

Core is intentionally storage-agnostic at the model layer, but concrete storage adapters live outside it.

Examples:
- `android/core` provides `RepositorySqlite`
- app modules provide schema choices and relation repositories

Core defines the contracts and orchestration model.
Adapters provide the storage implementation.

Because of that split, relations are not limited to one storage backend. A single aggregate can coordinate:
- SQL-backed entities
- SQLite-backed entities
- in-memory relation links
- or any other repository implementation that follows the core interfaces

Configuration for these pieces is tree-based as well. A typical entity block can describe:
- `storage`
- `aggregate`
- `wrapper`
- `management`
- `relation`

## Current Strengths

- storage-agnostic relation model
- unified field mapping
- generic record mapping
- delete-policy-aware relation handling
- transaction-aware management layer
- clean separation between model, repository, and relation synchronization

## Current Direction

The framework is being hardened toward:
- fewer public escape-hatch constructors
- more resolver-driven defaults
- less app-side repository boilerplate
- clearer singular relation handling
- more consistent row-shape modeling where projections are necessary

That keeps the domain clean while making storage adapters stricter and easier to reason about.
