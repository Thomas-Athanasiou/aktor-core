package com.aktor.core;

import com.aktor.core.data.Mutation;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;

import java.util.Objects;

public class RepositoryMutation<Item extends Data<Key>, Key>
extends RepositoryFrame<Item, Key>
{
    private final Repository<Item, Key> snapshotRepository;

    private final Repository<Mutation<Key, Item>, Key> mutationRepository;

    private final Converter<Mutation<Key, Item>, Mutation<Key, Item>> mutationConverter;

    public RepositoryMutation(
        final Repository<Item, Key> repository,
        final Repository<Mutation<Key, Item>, Key> mutationRepository,
        final Converter<Mutation<Key, Item>, Mutation<Key, Item>> mutationConverter
    )
    {
        this(repository, repository, mutationRepository, mutationConverter);
    }

    public RepositoryMutation(
        final Repository<Item, Key> repository,
        final Repository<Item, Key> snapshotRepository,
        final Repository<Mutation<Key, Item>, Key> mutationRepository,
        final Converter<Mutation<Key, Item>, Mutation<Key, Item>> mutationConverter
    )
    {
        super(repository::get, repository::save, repository::delete, repository::search);
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.mutationRepository = Objects.requireNonNull(mutationRepository);
        this.mutationConverter = Objects.requireNonNull(mutationConverter);
    }

    @Override
    public void save(final Item item) throws SaveException
    {
        final Item from = getSnapshot(item.key());
        super.save(item);
        this.mutationRepository.save(this.toMutation(item.key(), from, item));
    }

    @Override
    public void delete(final Item item) throws DeleteException
    {
        final Item from = getSnapshot(item.key());
        super.delete(item);
        try
        {
            this.mutationRepository.save(this.toMutation(item.key(), from != null ? from : item, null));
        }
        catch (final SaveException exception)
        {
            throw new DeleteException("Failed to save delete mutation", exception);
        }
    }

    private Item getSnapshot(final Key key)
    {
        try
        {
            return this.snapshotRepository.get(key);
        }
        catch (final GetException ignored)
        {
            return null;
        }
    }

    private Mutation<Key, Item> toMutation(final Key key, final Item from, final Item to) throws SaveException
    {
        try
        {
            return this.mutationConverter.convert(new Mutation<>(key, from, to));
        }
        catch (final ConversionException exception)
        {
            throw new SaveException("Failed to convert state to mutation", exception);
        }
    }
}
