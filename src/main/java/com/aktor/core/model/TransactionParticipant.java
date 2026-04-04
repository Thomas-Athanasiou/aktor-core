package com.aktor.core.model;

public interface TransactionParticipant
{
    void beginTransaction() throws Exception;

    void commitTransaction() throws Exception;

    void rollbackTransaction() throws Exception;
}
