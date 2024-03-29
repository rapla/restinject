module org.rapla.restinject {
    exports org.rapla.inject.generator;
    exports org.rapla.inject.raplainject;
    exports org.rapla.inject.scanning;
    exports org.rapla.inject;
    exports org.rapla.logger;
    exports org.rapla.rest;
    exports org.rapla.rest.client;
    exports org.rapla.rest.client.swing;
    exports org.rapla.rest.gson;
    exports org.rapla.rest.server.jsonpatch;
    exports org.rapla.rest.server.provider.filter;
    exports org.rapla.rest.server.provider.json;
    exports org.rapla.rest.server.provider.resteasy;
    exports org.rapla.scheduler;
    exports org.rapla.scheduler.sync;
    requires javax.inject;
    requires java.ws.rs;
    requires io.reactivex.rxjava3;
    requires java.annotation;
    requires java.compiler;
    requires java.logging;
    requires org.slf4j;
    requires com.google.gson;
    requires resteasy.jaxrs;
    requires java.validation;
}