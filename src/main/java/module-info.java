module com.aktor.core {
    uses com.aktor.core.model.RepositoryFactoryLoader;
    requires java.sql;
    exports com.aktor.core.data;
    exports com.aktor.core.model;
    exports com.aktor.core.exception;
    exports com.aktor.core.util;
    exports com.aktor.core.value;
    exports com.aktor.core.service;
    exports com.aktor.core;
}